# 无常：战斗音乐与自定义血条

> 状态：**代码已接线，游戏内尚未验证**。2026-10-04 完成两阶段血条图稿替换与离线布局检查。
> 血条当前规格见 §2.6–2.7；实机未验证事项仍按 [03 §5](03_ENGINEERING_AND_VERIFICATION.md) 记录。

参考实现取自两套同类模组（用户提供）：

- `_maledict_ref/cataclysm`（`CMBossInfoServer` + `MessageUpdateBossBar` + `CustomBossBar` + `BossMusicPlayer`）
- `_maledict_ref/legendary`（`LMBossInfoServer` + `MessageUpdateBossBar` + `CustomBossBar` + `BossMusicPlayer`）

两套的骨架完全一样，下面凡是"参考实现"都指这个骨架。

---

## 0. 结论速览

**状态：未验证。音乐和血条都还没有在客户端里跑过一次。**

这一句要放在最前面，因为它被绕过一次，代价是一轮返工：

> 「`compileJava` / `test` 通过」**不等于**这两个功能能用。
> 落地音乐时我拿一个绿色的测试套件当成了证据，而当时**新代码一行测试都没有**——
> 那 70 个测试全是项目原有的。结果 `volume = 0` 那个必现的 bug 一路过关，
> 直到实机一声不响才被发现（原因见 §1.5 第 8 条）。

现在有的自动化覆盖，只有**两条纯粹的不变量**，都**不能**证明功能可用：

| 测试 | 覆盖 | **不覆盖** |
| --- | --- | --- |
| `FadeEnvelopeTest` | 淡入系数起点非 0（返工那次的**一半**原因） | `canStartSilent()` 那一半；事件 id、`sounds.json`、.ogg、注册表接线 |
| `VicissitudeThemeTest` | 多 Boss 时的选曲规则 | 「哪只算 engaged」「客户端读不读得到同步值」等一切游戏内行为 |

两条都是为了让不变量可测才把逻辑抽出来的，属于**半个网**。真正的验证手段只有
`runClient` + §4 的清单；没打勾之前，这里一律记「未验证」
（[03 §5](03_ENGINEERING_AND_VERIFICATION.md) 的规矩）。

| 功能 | 要写的代码 | 要出的资产 | 要新网络包吗 |
| --- | --- | --- | --- |
| 1/2 阶段音乐 | `MaledictSounds` 注册类、`VicissitudeBossMusic` 客户端播放器、`sounds.json`、客户端配置项 | 2 个 `.ogg` | **不要**。阶段已经躺在 `SynchedEntityData` 里了 |
| 自定义血条 | `VicissitudeBossEvent`（`ServerBossEvent` 子类）、`BossBarStylePacket`、客户端渲染类 + 事件订阅 | 2 组 PNG；或者完全不画贴图，纯代码绘制 | **要**。血条 UUID 只有服务端知道 |

音乐这条不需要网络包，是 Maledict 比参考实现省事的地方：Cataclysm / Legendary 的 Boss 阶段没进同步数据，
所以它们必须自己发包通知客户端切歌；`FirstVicissitudeBossEntity` 的 `DATA_STAGE` 和
`DATA_PHASE_TWO_REACHED` 本来就是 `SynchedEntityData`，客户端直接读就行。

### 0.1 落地文件一览

| 组件 | 文件 |
| --- | --- |
| 声音事件注册 | `registry/MaledictSounds.java`（在 `Maledict` 里注册） |
| 声音定义 | `assets/maledict/sounds.json` + `sounds/music/vicissitude_phase_{one,two}.ogg` |
| 客户端播放器 | `client/VicissitudeBossMusic.java` + `client/VicissitudeMusicSound.java` |
| 挂载点 | `client/MaledictClientRenderEvents.java`（复用它已有的 96 格扫描） |
| 血条样式同步 | `network/BossBarStylePacket.java` + `MaledictNetwork#sendBossBarStyle` |
| 带皮肤号的血条 | `common/entity/VicissitudeBossEvent.java` |
| 客户端重绘 | `client/VicissitudeBossBarOverlay.java` |
| 接线 | `FirstVicissitudeBossEntity`（字段类型、`completeTransition()`、`readAdditionalSaveData()`） |
| 配置 | `MaledictConfig.CLIENT_SPEC` 的 `bossMusic` / `bossMusicVolume` |
| 字幕文案 | `MaledictLanguage`（生成器）→ `runData` → `src/generated/.../lang/*.json` |
| 资产署名 | `src/main/resources/CREDITS.md`（随 jar 分发） |

---

## 1. 音乐

### 1.1 阶段映射

| 服务端阶段 | 客户端读到的 | 放哪首 |
| --- | --- | --- |
| `DORMANT` | `getStage() == DORMANT` | 不放。此时只是座雕像，音乐一响就剧透了 |
| `PHASE_ONE` | `getStage() == PHASE_ONE` | 一阶段主题，**不循环**（见下） |
| `TRANSITION`（60 tick **= 3 秒**） | `getStage() == TRANSITION` | 一阶段主题继续淡出。**这里放不下任何独立曲子** |
| `PHASE_TWO` | `isPhaseTwoVisual() == true` | 二阶段主题 |
| `DYING`（80 tick = 4 秒） | `getStage() == DYING` 或 `getHealth() <= 0` | 停（淡出） |

**`TRANSITION` 只有 3 秒。** `TRANSITION_TICKS = 60`，20 tick/秒 ⇒ 3 秒，时间轴是
`0-14 悬停 / 15-29 折翼 / 30-44 落羽 / 45 换装 / 45-59 混合`。这么短的窗口**不可能承载一首独立的
过渡曲**——初稿在这里排了一首 2:55 的圣咏，是没算 tick 到秒的换算。正确做法：

- 一阶段主题在 `TRANSITION` 里**继续放完并开始淡出**，二阶段主题在 `PHASE_TWO` 第一拍进。
- 那 3 秒是**音效**的活（换武器、落羽、核心闪光的 `SoundRegistry.SOUL_SHATTER` 已经在
  `tickTransitionEffects()` 里了），不是音乐的活。

**一阶段不需要循环。** 一阶段是**固定计时**，不是打到某个血量才转阶段：

| 难度 | 一阶段时长 | 现有素材 Fantasy Choir 2（126 s）够吗 |
| --- | ---: | --- |
| SIMPLE | 90 s | 够，余 36 s |
| DIFFICULT | 70 s | 够 |
| COMPLETE | 50 s | 够 |
| EXTREME | 37.5 s | 够 |

所以一阶段曲设 `looping = false` 就行，全部难度都在 126 秒内结束。
**唯一的例外**是 `tickEmptyEncounter()`：场上没人 100 tick 后 Boss 退回 `DORMANT`，
玩家回头再打会重新计时。那时候曲子如果已经放完，重新淡入一次即可——不用为它做循环。

二阶段**不一样**：它没有时间上限，打到死为止，所以二阶段曲必须能循环（或足够长到不用循环）。

### 1.2 要加的文件

```
src/main/resources/assets/maledict/
├── sounds.json                                   ← 新建
└── sounds/
    └── music/
        ├── vicissitude_phase_one.ogg             ← 新建
        └── vicissitude_phase_two.ogg             ← 新建
```

`sounds.json`：

```json
{
  "music.vicissitude.phase_one": {
    "subtitle": "sounds.maledict.music.vicissitude.phase_one",
    "sounds": [
      { "name": "maledict:music/vicissitude_phase_one", "stream": true }
    ]
  },
  "music.vicissitude.phase_two": {
    "subtitle": "sounds.maledict.music.vicissitude.phase_two",
    "sounds": [
      { "name": "maledict:music/vicissitude_phase_two", "stream": true }
    ]
  }
}
```

三个要点：

- **`"stream": true` 不能省。** 不加的话整首曲子会被解码进内存常驻；加了之后走 `AudioStream`，
  边读边放。`Sound.shouldStream()` 就是读这个字段，默认 `false`。代价是**同一个 sound 同时只允许
  4 个实例**（[MC Wiki](https://minecraft.wiki/w/Sounds.json)），Boss 音乐只要 1 个，无影响。
- **文件名里不能有任何空白字符。** 这是会炸整个文件的坑，MC Wiki 的原话是：
  *"Failure to follow this guideline in at least one entry will result in the **entire sounds.json being
  ignored**, in favor of vanilla sounds."* 所以 `vicissitude_phase_one.ogg` 行，`vicissitude phase one.ogg` 不行。
- 事件 id 里带点号（`music.vicissitude.phase_one`）没问题，原版自己就是 `music.game` / `music.credits`。

**另外，`sounds.json` 里没有"循环"这个字段。** 字段只有
`name / volume / pitch / weight / stream / attenuation_distance / preload / type` 八个，
Ogg 文件里的 `LOOPSTART` / `LOOPLENGTH` 元数据**也不被读取**。一首曲子放完就是放完了，
循环只能靠代码里的 `TickableSoundInstance` 重放——这正是 §1.4 那个写法的原因，不是可选项。

### 1.3 注册

新建 `org/brahypno/maledict/registry/MaledictSounds.java`，仿 `MaledictMobEffects` 的写法：

```java
package org.brahypno.maledict.registry;

public final class MaledictSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, Maledict.MODID);

    public static final RegistryObject<SoundEvent> VICISSITUDE_MUSIC_PHASE_ONE =
            register("music.vicissitude.phase_one");
    public static final RegistryObject<SoundEvent> VICISSITUDE_MUSIC_PHASE_TWO =
            register("music.vicissitude.phase_two");

    /** 可变距离事件：音乐不走衰减，但定长事件的 16 格半径对音效才是默认值，别混用。 */
    private static RegistryObject<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(
                ResourceLocation.fromNamespaceAndPath(Maledict.MODID, name)));
    }

    private MaledictSounds() {
    }
}
```

在 `Maledict` 构造函数里跟着其它注册器加一行：

```java
MaledictSounds.SOUND_EVENTS.register(modBus);
```

字幕文案走数据生成器，在 `MaledictLanguage` 的 `zh_cn` / `en_us` 两段里各加两条
（`sounds.maledict.music.vicissitude.phase_one` / `.phase_two`），然后跑 `runData`。
**不要手改 `src/generated`。**

### 1.4 客户端播放器

新建 `org/brahypno/maledict/client/VicissitudeBossMusic.java`，并加一个 `AbstractTickableSoundInstance` 子类
`VicissitudeMusicSound`。

播放器的形状（照抄参考实现的骨架）：

```java
public final class VicissitudeBossMusic {
    private static final int FADE_TICKS = 40;
    @Nullable private static VicissitudeMusicSound active;

    /** 每客户端 tick 调一次；boss 为 null 表示本 tick 附近没有正在打的无常。 */
    public static void tick(@Nullable FirstVicissitudeBossEntity boss) {
        SoundEvent wanted = boss == null ? null : musicFor(boss);
        if (active != null && (wanted == null || active.getSoundEvent() != wanted)) {
            active.fadeOut();          // 让它自己淡出，不要 stop() 硬切
            active = null;
        }
        if (active == null && wanted != null) {
            active = new VicissitudeMusicSound(wanted);
        }
        if (active != null && !Minecraft.getInstance().getSoundManager().isActive(active)) {
            Minecraft.getInstance().getSoundManager().play(active);
        }
    }

    private static SoundEvent musicFor(FirstVicissitudeBossEntity boss) {
        if (boss.getHealth() <= 0.0F || boss.getStage() == VicissitudeBossStage.DYING) {
            return null;
        }
        if (boss.isPhaseTwoVisual()) {
            return MaledictSounds.VICISSITUDE_MUSIC_PHASE_TWO.get();
        }
        return boss.getStage() == VicissitudeBossStage.DORMANT
               ? null
               : MaledictSounds.VICISSITUDE_MUSIC_PHASE_ONE.get();
    }
}
```

挂载点就是现成的 `MaledictClientRenderEvents#onClientTick`：它已经在客户端 tick 里扫 96 格内的
`FirstVicissitudeBossEntity`，把最近的那只（`stage != DORMANT` 且没死）挑出来交给
`VicissitudeBossMusic.tick` 即可，不用新开一个 tick 处理器。

**挑哪只**：多只无常同时存在时取离玩家最近的一只。音乐是全局的，两首叠着放一定是 bug。

### 1.5 已核实的 API 事实与坑

下面几条是从 1.20.1 Forge 47.4.23 的反编译源码里逐条读出来的，不是凭印象：

1. **`delay` 必须保持 0。** `SoundEngine` 用 `requiresManualLooping(sound) = sound.getDelay() > 0`
   来分流；`looping == true && delay == 0` 才会走 `shouldLoopAutomatically`，也就是 OpenAL 的
   硬件循环——那才是无缝的。参考实现两个都显式写了 `this.delay = 0`。
2. **音源选 `SoundSource.MUSIC` 还是 `RECORDS` 要想清楚。** 参考实现用 `RECORDS`（跟着"唱片机/音符盒"
   滑条走）；`MUSIC` 跟着"音乐"滑条走，玩家把音乐调 0 就真的一点都不放——这更符合大多数人的预期。
   两条路都合法，**建议 `MUSIC` + 自己的音量配置项**。任何一种，玩家把对应滑条拉到 0 都会自动静音，
   `SoundEngine` 会直接跳过播放，不用自己判断。
3. **`attenuation` 设 `Attenuation.NONE`。** 音乐不该有空间衰减，两个参考实现都这么写。
4. **`Minecraft#getMusicManager()#stopPlaying()` 是必须的。** 常见误解是给血条设
   `setPlayBossMusic(true)` 就能压掉原版背景音乐——**在 1.20.1 不是**。读源码：
   `BossHealthOverlay.shouldPlayMusic()` 只被 `Minecraft#getSituationalMusic()` 用在**末地**，
   主世界/下界/其它维度完全不受影响。所以要么像参考实现那样每 100 tick 调一次
   `getMusicManager().stopPlaying()`，要么就接受原版环境音乐和 Boss 主题偶尔重叠。
5. **`SoundEvent.createVariableRangeEvent` 和 `createFixedRangeEvent` 别用错。** 音乐用前者。
6. **`AbstractSoundInstance#volume` 是乘数**，最终音量 = `volume × sound.volume × 音源滑条`。
   自己的配置项乘进 `volume` 就行。
7. **声道数决定要不要做空间衰减。** MC Wiki 写得很直白：单声道（mono）会按距离衰减，
   双声道（stereo）**音量不随距离变化**（"for example music, ambient sounds"）。
   所以音效一律 mono、Boss 音乐一律 stereo——这不是风格选择，是引擎行为。
   反正我们还会显式设 `Attenuation.NONE`，双保险。
8. **循环不是无缝的，除非素材本身无缝。** OpenAL 的硬件循环会从缓冲区末尾直接跳回开头，
   所以**不要在尾部做淡出**，也不要在头尾补静音——那样每一圈都会听到一次断口或空拍。
   正确做法是把越过循环点的混响尾巴绕回开头（见 §3.6）。选曲时优先挑标了 `loop` / `seamless` 的条目。

### 1.6 配置项

`MaledictConfig.CLIENT_SPEC` 里 `firstVicissitude` 段加两个：

```java
VICISSITUDE_BOSS_MUSIC = clientBuilder
        .comment("无常 Boss 战的专属 BGM。关掉之后战斗里不会有任何本模组的音乐。")
        .define("bossMusic", true);
VICISSITUDE_BOSS_MUSIC_VOLUME = clientBuilder
        .comment("BGM 音量倍率，1.0 为原样。原版「音乐」滑条仍然独立生效。")
        .defineInRange("bossMusicVolume", 1.0D, 0.0D, 1.0D);
```

README 的 Configuration 一节现在写着「`config/maledict-client.toml` holds the encounter's screenshake」，
加完之后要一起改。

### 1.7 多只无常同时在场

**规则：正在打的无常里，推进得最远的那只说了算。** 只要有一只进了二阶段，就放二阶段曲。
规则本身在 `VicissitudeTheme#select`（纯函数，不碰 Minecraft），由 `VicissitudeThemeTest` 覆盖。

为什么不是"离我最近的那只"——初版就是那么写的，有两个毛病：

| 毛病 | 后果 |
| --- | --- |
| 沉眠的雕像参与"最近"比较 | 最近那只还在 `DORMANT` 就返回"不放音乐"，**哪怕二十格外正有一只在一阶段开打**。音乐整个哑掉 |
| 两只阶段不同时按距离选 | 玩家在两只之间走动，"最近"来回翻 → 曲子来回切 → 每次都是 2 秒交叉淡化，**听感糊掉** |

改成"最远推进"之后结果是**单调**的：阶段只前进不后退，同一批无常只在真正发生阶段推进
（或有 Boss 死掉、退回沉眠）时才换曲——而那正是我们想让它变的时候。

**代价，说清楚**：放弃了"跟着我在打的那只走"。两只阶段不同、而你正贴着落后那只打时，
听到的会是先进那只的曲子。这是有意接受的取舍，换来的是绝不抖。

### 1.7.1 什么时候压根不放

**两道门都得过，缺一不放。** 条件写死在 `VicissitudeTheme.Participant`（`counts()`）。

| 门 | 条件 | 为什么 |
| --- | --- | --- |
| **Boss 自己** | 活着，且阶段既不是 `DORMANT` 也不是 `DYING` | `DORMANT` 就是「**对任何生物都没有仇恨、一阶段都还没开始**」——那时它只是座雕像，音乐一响就剧透了 |
| **距离** | 离本地玩家 ≤ **48 格** | 没有这道门的话，八十格外别人在打，你在这边挖矿也会被放 BGM |

**关于"仇恨"的口径**：判定用的是**阶段**，不是真的去读 `getTarget()`。原因很实际——
`Mob#getTarget()` 是服务端字段，**没有同步**；`phaseOneTargets` / `phaseTwoParticipants`
那两个 Set 同样只在服务端。客户端读得到的只有 `getStage()` / `isPhaseTwoVisual()` /
`getHealth()` / `getRenderAction()` / `getHurtTicks()` 和距离。

所以：刷怪蛋直接安排到一阶段的、或者仪式召唤出来的，只要进了 `PHASE_ONE` 就算「开始了」，
哪怕当下还没锁定目标。**"一阶段开始了但没有目标"仍然放**，这是有意的。

**48 这个数是取的 Boss 自己的 `damageRange` 默认值**——超出这个距离，伤害线性衰减到 72 格归零，
也就是"已经打不动它了"。正常交战距离在 7 格上下。**写死是没办法**：
`engagementRange` / `damageRange` 都是 COMMON 配置，客户端连服务器时读不到服务端那份，
拿本地那份又不一定一样。要调就改 `VicissitudeBossMusic.COMBAT_RADIUS`。

**已知的小尾巴（没处理）**：48 是硬边界。玩家恰好在边界上来回走的话，音乐会 2 秒淡出、
2 秒淡入地脉动。真遇到了就加回滞——起播 48、保持到 56，大约四行。**现在没加**，
因为没实机听过，不想凭空多一个半径。

**另一个根因，没动**：`tickEmptyEncounter()`（没人打满 100 tick 就退回 `DORMANT`）
**只在 `DORMANT, PHASE_ONE` 分支里调用**。进了 `PHASE_TWO` 之后没有对应的复位，
所以一只没人打的二阶段无常会一直停在 `PHASE_TWO`。距离门能挡住"听不到"，
但**它本身不是一个"脱战"状态**——要不要让二阶段也会脱战，是 Boss 行为的设计问题，
不该由音乐这条线单方面决定，见 §4 那一条待定项。

---

## 2. 自定义血条

### 2.1 为什么这里非得要一个网络包

`FirstVicissitudeBossEntity` 现在用的是：

```java
private final ServerBossEvent bossEvent = new ServerBossEvent(
        getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
```

`ServerBossEvent` 在 1.20.1 **只有三个参数的公开构造函数**，血条 UUID 是内部
`Mth.createInsecureUUID()` 随机生成的（`BossEvent(UUID, ...)` 那个构造函数存在的，但
`ServerBossEvent` 没往外露）。客户端拿到的只有 `LerpingBossEvent`，`getId()` 就是这个随机 UUID。

所以客户端**没法**靠实体 UUID 认出"这条血条是我的"。两条出路：

- **A（推荐，参考实现的做法）**：`ServerBossEvent` 子类，在 `addPlayer` / 样式变更时把
  `getId()` 连同样式号发给客户端。稳、显式、可扩展。
- **B（零包）**：用 `BossEvent#getColor()` + `getOverlay()` 的组合当指纹，一阶段用
  `PURPLE/NOTCHED_6`、二阶段用 `PURPLE/NOTCHED_10`。两个字段都随 `setColor`/`setOverlay`
  同步到客户端。省一个包，但语义被挪用了，别的模组撞组合就会串味。**只有在明确不想加包时才用。**

下面按 A 写。

### 2.1.1 实现状态（血条接线 2026-09-27；图稿重做 2026-10-04）

> 落地 ≠ 验证。血条和音乐一样**没有实机跑过**，见 §0。

血条部分已实现，见 §0.1。当前图稿以 Boss 的紫晶头、断环和肩甲为中央缩略图标，
一阶段覆羽遮血，二阶段脱羽露出血槽。底条仍为 256×16、装饰层为 256×32。
Java 居中偏移、血槽位置及叠放间距已随图稿更新，具体见 §2.6–2.7。

**没有做的事**（都是有意的，不是漏了）：

- **没有设 `setDarkenScreen(true)`**。原版血条默认不压暗屏幕，加了会改变现有观感。
  想要那种"进战斗画面变暗"的效果，在 `VicissitudeBossEvent` 构造函数里加一行即可。
- **没有加"残影条"**（掉血时那条慢慢追上来的白条）。`LerpingBossEvent#getProgress()` 已经是
  插值过的，要做残影得自己存上一帧的值，是另一个功能。

### 2.2 服务端：`VicissitudeBossEvent`

```java
package org.brahypno.maledict.common.entity;

/** 带样式号的血条：样式号只在服务端是真值，客户端由 BossBarStylePacket 镜像。 */
public final class VicissitudeBossEvent extends ServerBossEvent {
    public static final int STYLE_PHASE_ONE = 0;
    public static final int STYLE_PHASE_TWO = 1;

    private int style;

    public VicissitudeBossEvent(Component name, int style) {
        super(name, BossBarColor.PURPLE, BossBarOverlay.PROGRESS);
        this.style = style;
        setDarkenScreen(true);
    }

    public void setStyle(int style) {
        if (style == this.style) {
            return;
        }
        this.style = style;
        for (ServerPlayer player : getPlayers()) {
            MaledictNetwork.sendBossBarStyle(player, getId(), style);
        }
    }

    /** 入队即补发：玩家可能是二阶段才第一次看见这条血条。 */
    @Override
    public void addPlayer(ServerPlayer player) {
        MaledictNetwork.sendBossBarStyle(player, getId(), style);
        super.addPlayer(player);
    }

    @Override
    public void removePlayer(ServerPlayer player) {
        MaledictNetwork.sendBossBarStyle(player, getId(), BossBarStylePacket.STYLE_NONE);
        super.removePlayer(player);
    }
}
```

`FirstVicissitudeBossEntity` 里改三处：

1. 字段类型换成 `VicissitudeBossEvent`，初值 `STYLE_PHASE_ONE`。
2. `onIncomingAttack` 里 `setStage(PHASE_ONE)` 那一段（`stage == DORMANT` 分支）加
   `bossEvent.setStyle(STYLE_PHASE_ONE);`——从 DORMANT 醒来时血条才出现，这一步其实是幂等的，
   写在这里是为了将来加"待机版血条"时有个落点。
3. `completeTransition()` 里 `setStage(PHASE_TWO)` 旁边加
   `bossEvent.setStyle(STYLE_PHASE_TWO);`。**这是二阶段换血条皮肤的唯一时机。**

`startSeenByPlayer` / `stopSeenByPlayer` 里的 `addPlayer` / `removePlayer` 不用动，子类已经覆盖了。

### 2.3 网络包

`BossBarStylePacket`（`PLAY_TO_CLIENT`，注册号顺延成 4）：

```java
public record BossBarStylePacket(UUID bar, int style) {
    public static final int STYLE_NONE = -1;
    // encode: writeUUID(bar) + writeVarInt(style)
    // handle:  DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
    //              () -> () -> VicissitudeBossBarOverlay.accept(packet))
}
```

`MaledictNetwork` 加 `registerMessage(4, ...)` 和两个发送辅助方法（单个玩家 / 一批玩家），
形状照抄现有的 `sendEffect`。

### 2.4 客户端渲染

```java
@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE,
        value = Dist.CLIENT)
public final class VicissitudeBossBarOverlay {
    private static final Map<UUID, Integer> STYLES = new HashMap<>();

    public static void accept(BossBarStylePacket packet) { /* 写/删 STYLES */ }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBossBarProgress(CustomizeGuiOverlayEvent.BossEventProgress event) {
        Integer style = STYLES.get(event.getBossEvent().getId());
        if (style == null) {
            return;                    // 不是我们的血条，交还原版
        }
        event.setCanceled(true);       // 拦掉原版那条
        STYLES.get(...)... draw(...)
        event.setIncrement(BAR_ADVANCE);   // 不设的话下一条血条会叠上来
    }
}
```

订阅时必须带 `priority = EventPriority.HIGHEST`——参考实现两个都是这么写的，别的模组也可能在
同一个事件上动手。

**几何要照抄原版**，否则和别的 BOSS 血条对不齐。1.20.1 原版
（`BossHealthOverlay#render`）：

```java
int x = guiWidth / 2 - 91;          // event.getX() 给的就是这个
int y = 12;                          // event.getY()
int increment = 10 + font.lineHeight;   // 默认 19
// 名字画在 (guiWidth/2 - font.width(name)/2, y - 9)
// 血条本体 182 × 5
// 超过 guiHeight / 3 之后不再画后续血条
```

`event.getX()` **不包含**你自己的 baseOffsetX/Y，要自己加。`event.getBossEvent().getProgress()`
在 `LerpingBossEvent` 上已经是插值过的（100 ms 缓动），掉血动画白送，不用自己做。

**客户端地图要在断线时清空**，否则 UUID 会一直攒着。挂一个
`ClientPlayerNetworkEvent.LoggingOut` 清 `STYLES` 就行。

### 2.5 贴图规格（参考实现那一套）

参考实现的参数是：

```
CustomBossBar(baseTex, overlayTex,
              baseHeight, baseTextureHeight,    // 5, 16
              baseOffsetX, baseOffsetY,          // 血条本体相对 (x, y) 的偏移
              overlayOffsetX, overlayOffsetY,    // 装饰框相对 (x, y) 的偏移
              overlayWidth, overlayHeight,       // 装饰框画多大
              verticalIncrement,                 // 一般 25~30
              progressWidth,                     // 182
              textColor)
```

绘制是两次 `blit`（用的是 9 参重载，声明贴图尺寸 256 × baseTextureHeight）：

```java
// 空槽
gui.blit(baseTex, x + offX, y + offY, 0, 0, 182, 5, 256, 16);
// 填充
gui.blit(baseTex, x + offX, y + offY, 0, 5, filled, 5, 256, 16);
// 装饰框（挖空中间）
gui.blit(overlayTex, x + offX + ovOffX, y + ovOffY + offY, 0, 0,
         ovWidth, ovHeight, ovWidth, ovHeight);
```

每个阶段仍有底条与装饰层两张 PNG：

| 文件 | 尺寸 | 当前内容 |
| --- | --- | --- |
| `first_vicissitude_bar_phase_one.png` | 256 × 16 | 空槽与填充各 182 × 5；一阶段客户端不绘制 |
| `first_vicissitude_bar_phase_one_frame.png` | 256 × 32 | 晶体头、断环、肩甲及覆盖整个血槽的叠层覆羽 |
| `first_vicissitude_bar_phase_two.png` | 256 × 16 | 紫色血量填充，使用原有插值进度 |
| `first_vicissitude_bar_phase_two_frame.png` | 256 × 32 | 同一 Boss 的头肩缩略图标、骨翼与外露血槽 |

2026-10-04 根据用户要求重新制作图稿，原有项目血条没有作为图稿输入。
中央图标表达 Boss 的非人晶体头、头后断环及骨白叠层肩甲，无眼鼻口。
实际图案占 192×30，放在 256×32 画布的 (32,1)，保持紧凑的像素 HUD 尺度。

**当前几何**（Java 与打包脚本一致）：

| 常量 | 值 | 说明 |
| --- | --- | --- |
| `BAR_WIDTH / BAR_HEIGHT` | 182 / 5 | 沿用原版底条尺寸 |
| `BAR_OFFSET` | (0, 19) | 底条相对事件坐标 |
| `OVERLAY_SIZE` | 256 × 32 | 装饰层尺寸，1:1 绘制 |
| `OVERLAY_OFFSET` | (−37, 0) | 装饰层与原版底条在屏幕中央对齐 |
| `BAR_IN_OVERLAY` | (37, 19) | 血槽在装饰层画布内的位置 |
| `INCREMENT` | 43 | 图稿高度 + 名称 9px + 间隙 2px |

血槽为 x37..219、y19..24（右/下边界不包含）；底边描线被下轨盖住，
两侧实际可见 4 行。中央头肩图标及两端护甲盖在血量之上。
名称仍在 `event.getY()−9`，位于图稿上方，不能画进晶体头内。

一阶段只画覆羽图稿与名称；不绘制空槽或填充，羽片间的负空间不会泄露血量。
二阶段才绘制底条及按血量裁切的填充。
这是客户端显示变化；服务端血量、阶段同步及转阶段时机保持原有行为。

### 2.6.1 多只无常同时在场

每个实体仍有独立的血条 UUID 和阶段皮肤号。
一条 UI 的占位为 `[y−9,y+32)`；下一条名称的上沿为 `y+43−9=y+34`，
比当前图稿下沿多留 2px。原版按 `event.getIncrement()` 叠放，无额外排序逻辑。
原版屏幕高度三分之一处的截断规则保持。

### 2.7 图稿源与可复现打包

图稿源和完整提示词在 [boss-bar](../../../art/first-vicissitude/boss-bar/README.md)。
两张透明图稿由内置 `image_gen.imagegen` 制作；
`art/first-vicissitude/tools/make_boss_bar.py` 负责裁到可见轮廓、最近邻适配尺寸、
统一 24 色及二值透明像素，并重新输出四张游戏 PNG。
图稿原件保留，不使用旧几何占位脚本重建装饰。

```powershell
python art/first-vicissitude/tools/make_boss_bar.py
```

- 输出四张游戏贴图到 `src/main/resources/assets/maledict/textures/gui/boss_bar/`。
- 两阶段概览、叠放检查和实测尺寸报告在 `build/first-vicissitude-review/boss-bar/`。
- 六档检查图为 `build/first-vicissitude-review/boss_bar_preview.png`。
- `preview/` 保留原有固定文件，本轮不增加模型预览图。

**本轮已验证**：`compileJava --offline` 通过；贴图尺寸、透明像素、血槽位置、
名称与叠放位置经过离线检查。二阶段两侧抽样血槽共 490 个像素，
其中 392 个透明（四行完整透明，第五行为覆盖底边的下轨）。
**尚未验证**：游戏内资源重载、不同 GUI 缩放下的实际显示、实战阶段切换。
所有血条检查图都是离线 UI 合成图，不是游戏截图。
