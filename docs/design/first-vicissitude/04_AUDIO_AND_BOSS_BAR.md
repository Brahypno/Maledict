# 无常：战斗音乐与自定义血条

> 状态：**设计稿，未实现**。本文只定方案与接口，代码尚未落地。
> 落地之后按 [03 §5](03_ENGINEERING_AND_VERIFICATION.md) 的规矩，把「测过 / 没测过」补回去。

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

### 2.1.1 实现状态（血条已落地，2026-09-27）

> 落地 ≠ 验证。血条和音乐一样**没有实机跑过**，见 §0。

血条部分全部实现，见 §0.1 的文件表。**贴图是"预览版"**：结构正确（两轨夹槽、卡扣、挂件、护套），
但装饰是脚本画的几何图形，不是手绘像素画。**换图不用改 Java**——覆盖
`first_vicissitude_bar_phase_{one,two}_frame.png`（256×32），保持
`BAR_IN_OVERLAY = (7, 16)` 那条槽留透即可。base（256×16、两条 182×5）同理。

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

所以每个阶段要出两张图：

| 文件 | 尺寸 | 内容 |
| --- | --- | --- |
| `first_vicissitude_bar_phase_one.png` | 256 × 16 | v0–5 空槽、v5–10 填充两条 182 × 5 实心条 |
| `first_vicissitude_bar_phase_one_frame.png` | 256 × 32 | overlay：上下两条贯通轨 + 卡扣 + 挂件 + 护套，大部分透明 |
| `first_vicissitude_bar_phase_two.png` | 256 × 16 | 二阶段配色，填充偏赤 |
| `first_vicissitude_bar_phase_two_frame.png` | 256 × 32 | 二阶段：断口更宽、卡扣开裂、护套外扩 |

**实际采用的几何**（Java 侧必须和脚本里的常量一致，这几个数字是照 256 × 32 overlay 那一档取的）：

| 常量 | 值 | 说明 |
| --- | --- | --- |
| `BAR_WIDTH / BAR_HEIGHT` | 182 / 5 | 原版就是这个尺寸，不能改 |
| `BAR_OFFSET` | (1, 7) | 血条本体相对 `event.getX()/getY()` |
| `OVERLAY_SIZE` | 256 × 32 | overlay 贴图尺寸，1:1 绘制 |
| `OVERLAY_OFFSET` | (−6, −9) | overlay 相对 `event.getX()/getY()` |
| `INCREMENT` | 32 | `event.setIncrement()`，**等于 overlay 高度**，见下 |

**overlay 内血条的位置是 `BAR_OFFSET − OVERLAY_OFFSET = (7, 16)`**，即画布 y 16..21。
**画检查图时必须按这个贴**，否则看到的是假的（初稿就在这里贴错过一次）。
两条轨落在 y = 14 与 y = 21（`RAIL_TOP_Y / RAIL_BOT_Y`），血条走中间那条槽。

原版名字画在 `y − 9`，overlay 顶边在 `y − 2`，中间留 7 px，不会顶到字。

### 2.6.1 多只无常同时在场

血条这边**天然就是每只一份**：每个实体有自己的 `VicissitudeBossEvent`（自己的随机 UUID）、
自己的皮肤号，客户端按 UUID 查表画。原版会把多条血条竖着叠起来，不需要额外处理。

但**叠放的间距必须够**，这是初版算错的地方：

原版的循环是每画完一条就 `y += event.getIncrement()`，而一条血条的实际占位是
`[y − 9, y + 23)`——名字在 `y − 9`（9 px 高），overlay 有 32 高。所以：

```
下一条的 overlay 顶边 = (y + INCREMENT) − 9
要求 ≥ y + 23   ⇒   INCREMENT ≥ 32
```

初版取了 25（照抄同类模组的数字），于是**下一条的名字会落进上一条 overlay 的底部 7 px 里**——
而我们的中央挂件下尖正好在那儿（同类模组的 overlay 中间那几行基本是空的，所以它们用 25 没出事）。

**现在 `INCREMENT = OVERLAY_HEIGHT`**，脚本和 Java 两边都按这个不变量写死。
副作用是占位变高：屏幕高 `guiHeight / 3` 以外不再画后续血条，按常见的 240 高算这里能站 3 条，够用。

**没有做的事**：多只无常时血条名字都是"无常"，玩家分不清哪条是哪只。要给它们排序号或者
按距离区分，是另一个功能，现在没做。

### 2.7 贴图由脚本生成，不做手工修图

`art/first-vicissitude/tools/make_boss_bar.py`（PIL，和
`art/melancholia/tools/make_delayed_vitals_hearts.py` 同一套路）：

```powershell
python art/first-vicissitude/tools/make_boss_bar.py
```

- 输出四张贴图到 `src/main/resources/assets/maledict/textures/gui/boss_bar/`
- 检查图输出到 `build/first-vicissitude-review/boss_bar_preview.png`
  （**不进 `preview/`**，按 `art/first-vicissitude/AGENTS.md` 那里只留固定五个文件）
- 检查图把两阶段 × 三档血量（100% / 55% / 18%）拼在一起，并画出原版名字的位置

#### overlay 的结构是量出来的，不是猜的

初稿把 overlay 画成"血条上下各一条细虚线"，看起来完全不像那么回事。后来把同类模组
（Cataclysm / Legendary Monsters）的 overlay 贴图拉下来逐行统计**不透明像素数**，结构才清楚：

```
maledictus overlay 256x32        每行不透明像素数
  y= 7   196  #################################################   <- 满宽轨
  y= 8   194  ################################################   <- 满宽轨
  y= 9   104  ##########################                          <- 血条所在，只有挂件横穿
  y=10   106  ##########################
  y=11    94  #######################
  y=12   104  ##########################
  y=13    98  ########################
  y=14   190  ###############################################   <- 满宽轨
  y=15   186  ##############################################    <- 满宽轨
```

**两条贯通全宽的轨把血条夹在中间**，血条走那条槽；只有卡扣和挂件会横穿血条。
harbinger（y=6/7 与 y=13/14）是同一个模式。这才是这类血条"有东西"的原因——
**光在血条上下画一圈虚线是出不来的**。

另外量到的两条事实：

- **base 里装的是两条实心条**，不是一条：v0–5 是**空槽**（填充色的暗版），v5–10 是**填充**（亮色渐变）。
  空槽要和填充**拉开足够亮度差**，否则整条读起来是一根灰线（初稿就是这样）。
- **base 贴图只有 280–950 字节**——它们是原版 `bars.png` 的形状换色，不含细节。
  **全部装饰都在 overlay 里**（800–4600 字节）。

#### 设计取自律动

Boss 头后那道环是"4 个**不等长**断片、缺口永久存在"（01_SPEC §1），所以两条轨就是同样四段不等长的
断环摊平，缺口位置一致，缺口处放卡扣。挂件落在各段中点，中央一个大挂件对应胸腔结构。
色板直接用 01_SPEC 的深黑紫 `#19151F` / 灰紫 `#49404F` / 冷骨白 `#B8B8C4` / 冷白 `#E6EDF5` /
暗紫能量 `#51436D`。

> ⚠️ **这批图是"结构正确、能用的程序员美术"，不是美术资源。**
> 它复刻的是**布局和层次**——两条轨道、卡扣、挂件、护套、血条从挂件后面穿过。
> 它复刻不了参考里那些**有机装饰**：晶体簇、羽叶、兽角、雕花。那些是手绘像素画。
>
> 要提升观感，最有效的一步不是调参，是**往 overlay 里加真正的手绘挂件**——
> 脚本已经把位置、尺寸、透明关系都定好了，画一张 256 × 32 的图替换 `_frame.png` 即可，
> Java 侧一行都不用改。

### 2.6 零美术方案

如果暂时不想出图，血条可以完全用代码画，一样能分阶段：

```java
gui.fill(x, y, x + 182, y + 5, 0xFF120A1C);                    // 底槽
gui.fillGradient(x, y, x + filled, y + 5, accentTop, accentBottom);  // 进度
gui.renderOutline(x - 1, y - 1, 184, 7, 0xFF6A4FA8);            // 描边
```

`fill` / `fillGradient` / `renderOutline` 都在 `GuiGraphics` 上，1.20.1 有。
一阶段用冷紫、二阶段换赤金，配一条更长的"残影条"（用 `LerpingBossEvent` 的 `getProgress()`
和上一帧值做差）就能有个像样的 BOSS 血条，代价是零资产。
**建议先落这个版本验证管线，美术图之后再换上去——两者共用同一个渲染入口，换图不动逻辑。**

### 2.8 两阶段 overlay：中央眼睛（草稿，待确认）

> **状态：草稿（2026-10-04）。未画图、未改 Java。** 下面的坐标按
> `VicissitudeBossBarOverlay.java` 的现有常量推导，不是估的。

#### 为什么中央槽是眼睛

`01_SPEC.md:15` 把「**真正贯通的胸腔**」列为必须始终可辨认的六条特征之一；头核则是
「被分离外壳遮盖大半……死亡时才完整展示」（`01_SPEC.md:24`）。所以中央槽做成
「**甲片围出一个洞、洞里一只眼**」，不是往血条上贴一个恐怖符号，而是把本体的一条身份特征正面微缩：

- 一阶段：甲片合拢、眼闭、双翼盖住血条 → 胸腔是封的
- 二阶段：甲片张开、眼在洞里睁开、血条露出 → 胸腔是通的

整条血条就是「**壳体开合**」走一遍，和头壳、胸洞是同一套动作。

#### 锚点（实测，来自 Java）

| 常量 | 值 | 出处 |
| --- | --- | --- |
| `BAR_OFFSET` | (1, 7) | `VicissitudeBossBarOverlay.java:57-58` |
| `OVERLAY_OFFSET` | (−6, −9) | 同上 `:60-61`，**相对 `event.getX()/getY()` 的绝对值** |
| `BAR_IN_OVERLAY` | (7, 16) | 同上 `:64-65`（= `BAR_OFFSET − OVERLAY_OFFSET`） |
| `OVERLAY_SIZE` | 256 × 32 | 1:1 绘制 |

画布坐标（overlay 贴图内）：

| 区域 | 范围 | 说明 |
| --- | --- | --- |
| 血条 | **u 7..188，v 16..20** | 182 × 5 |
| 血条中心 | **u 97.5**（97 与 98 之间） | 偶数宽的中央件以此为轴 |
| 名字带 | **v 0..8** | 名字与 overlay 顶边同为 `eventY − 9` |
| **可用装饰竖带** | **v 9..31（23 行）** | 血条上方只有 7 行，下方 11 行 |
| 现有轨 | v 14 / v 21 | `RAIL_TOP_Y / RAIL_BOT_Y`，血条走中间那条槽 |

两条硬约束，都因为 **overlay 是最后画的**（`:152` 注释原文：overlay 最后画，盖住血条两端和名字两侧）：

1. **名字带必须透**：`v 0..8 × u 84..112` 不能有不透明像素，否则会切掉「无常」两个字的一角。
   （名字宽度随文案变，规则是"overlay 在名字实际覆盖处必须透明"。）
2. **不透明即遮盖**。一阶段靠这条把血条藏起来；二阶段则必须保证 `v 16..20 × u 7..188`
   除中央件以外**全部透明**，否则血条两端被糊住。

#### 一阶段：合拢的翅膀 + 闭合的眼

- 双翼取**高位 V 翼**姿态（`01_SPEC.md:117`：躯干近直立、头下 8°、双臂张开约 20°、高位 V 翼），
  羽尖横压过血条轴；羽毛带两处断口（模型里有 `_FEATHER_1…4 / _BROKEN_1…2`，`01_SPEC.md:163`）。
- 两翼在中央留一道**竖缝**，缝里是合拢的头壳：一条暗缝线 + 缝沿一道冷白高光。
  **不要画成带睫毛的人眼**——那是「红眼／恶魔」那一档的俗套，也在 `01_CONCEPT_AND_VISUAL.md:21` 的避免清单里。
- **血条必须完全隐形**：`v 16..20 × u 7..188` 内 alpha 全 255。满血时这 182 × 5 整条都是亮填充，
  漏一个像素就是一条亮线。
- 中央件允许局部压住 v 14 / v 21 两条轨，但**轨在中央件两侧必须连续**——参考实现就是贯通轨
  从中央挂件后面穿过去。

#### 二阶段：张开的甲片 + 睁开的眼

- 中央件 = **胸洞正面微缩**。胸甲／胸侧／腹甲是「宽阔内面、边缘倒角和实心侧壁」，肩部是
  「有平面与厚度的六边形护片」（`01_SPEC.md:178-179`）。所以形状词汇是
  **六边形 + 倒角平板 + 1px 暗缝**，不是圆弧。这条也更好读：16px 上一块倒角平板一眼就是甲，
  一个圆环会糊成一个按钮。
- 眼用**头壳语言**：壳片（`HEAD_SHELL_LEFT / _RIGHT / _TOP`，`01_SPEC.md:159`）当睑与虹膜环，
  头核当瞳孔。左右壳分开 → **竖缝／竖瞳**，不是上下开的人眼；这是最不像人眼、也最
  「神圣但不对」的读法。
- **瞳孔偏心 1px**：第一眼是"一只眼"，盯着看才发现它没在看你。这是这套视觉里唯一被允许的错位。
- 甲片围出的空腔要真的读成洞（对应 `01_SPEC.md:181` 的「中央 12/12 条射线贯通」），
  所以洞里除瞳孔外留空。

#### 换图时机：不新增时机

复用 §2.2 里那一次 `completeTransition()` → `setStyle(STYLE_PHASE_TWO)`，**不按血量判断**：
`01_SPEC.md:70` 写着转场期间免伤，那 60 tick 里血量不会变，血条没有信息可报；卡在转场结束换图，
揭幕的那一刻正好是玩家重新能打的那一刻。

白送的一条：填充是从左往右铺的，血量掉到 50% 时前沿正好躲到中央件后面，血条被"注视"切成两段。
这是布局自带的，不用写动画。

#### 校验

`_frame.png` 改完跑一遍，两条硬约束都能查：

```python
from PIL import Image
import numpy as np

bar = np.asarray(Image.open("first_vicissitude_bar_phase_one_frame.png").convert("RGBA"))[..., 3]
print("一阶段血条窗口全不透明:", (bar[16:21, 7:189] == 255).all())

for phase in ("one", "two"):
    a = np.asarray(Image.open(f"first_vicissitude_bar_phase_{phase}_frame.png").convert("RGBA"))[..., 3]
    print(phase, "名字带透明:", (a[0:9, 84:113] == 0).all())
```

#### 待定

- **瞳孔颜色**：冷白 `#E6EDF5` 完全在色板内；精魂绿 `#00FF93` 最"精魂"，也正是武器宝石的绿，
  但按 `01_CONCEPT_AND_VISUAL.md:31`「不用高饱和」的精神算擦边。**需要拍一个。**
- 一阶段那道缝要不要偶尔抽动 1px（成本近零）。
- 本节按"左右壳分开 → 竖瞳"写；若改用上壳掀起（横缝，更像人眼），上面那条结论要跟着改。

#### 文档订正（顺带发现）

- §2.5 的代码草图用的是**参考实现的累计偏移**（`x + offX + ovOffX`），而本项目 Java 用的是
  **相对 `event.getX()/getY()` 的绝对偏移**（`:153-154`）。两套约定相差 `BAR_OFFSET = (1, 7)`。
- 因此 §2.5 里「overlay 顶边在 `y − 2`」与 Java 的 `OVERLAY_OFFSET_Y = −9` 矛盾；
  §2.6.1 的 `[y − 9, y + 23)` 才是对的。**本节坐标一律以 Java 为准**：overlay 顶边 = `eventY − 9`，
  中央件可用竖带是 `v 9..31`，不是 `v 0..31`。

---

## 3. 音频资产许可

> 本节是**结论**。每一条结论的原始出处、逐字引文和"哪些没核实"，
> 在 [05 音频许可调研](05_BGM_LICENSING_RESEARCH.md) 里。要给法务看就拿那一份。

目标很具体：**把音频文件本身打包进 mod jar，公开发布**（CurseForge / Modrinth / GitHub）。
这比"在视频里当 BGM"严格得多——很多免版税曲库允许后者、不允许前者。判断标准是
**"我能不能把这个 .ogg 文件复制进我的发行物"**，不是"我能不能听"。

### 3.1 三个安全等级

| 等级 | 许可 | 能直接打进 jar 吗 | 署名 |
| --- | --- | --- | --- |
| **A 最稳** | CC0 1.0 / Public Domain | 能。商用、修改、再分发都不需要许可 | 法律上不需要，礼貌上建议写 |
| **B 可用** | CC-BY 3.0 / 4.0 | 能 | **必须**，且要放在玩家找得到的地方 |
| **C 灰** | Pixabay、YouTube 音频库、Epidemic Sound 之类 | 见 §3.4 | 视平台而定 |

**先说一个最容易踩的误解："免版税"（royalty-free）不等于"无版权"（copyright-free），
更不等于公有领域。** Kevin MacLeod 自己的 FAQ 就把这句写死了：
"It's not Copyright Free / It's not in the Public Domain"。
真正省心的只有 CC0 和公有领域。

### 3.2 选曲：先定需求

**需求不是"中式"。** [01_SPEC §1](01_SPEC.md) 对这个 Boss 的定义是：

> 神圣遗骸制造的失败实验体，被未完成的命运重新唤醒。情绪是**冷静、无奈、不得已**——
> **不是神本身，也不是恶魔或传统的骷髅死神**。
>
> 第一阶段是**高位圣像**，第二阶段是**前倾的执行者**；死亡时揭示拼接结构，最后**熄灭而不是爆炸**。

色板：冷黑紫 `#19151F` / 灰紫 `#49404F` / 冷骨白 `#B8B8C4` / 暗紫能量 `#51436D`。

翻译成音乐要求：

| | 一阶段（高位圣像） | 二阶段（前倾的执行者） |
| --- | --- | --- |
| 情绪 | 疏离、静止、仪式感 | 有推进力，但依然冷 |
| 编制 | 人声圣咏、管风琴、稀疏钢琴、低音弦 | 加定音鼓、低音铜管、不谐和音簇 |
| 不要 | 旋律太满、"史诗"级配器、大调 | 金属 / 失真吉他 / 舞曲节奏 |
| 硬约束 | 必须能循环（见 §1.5 第 8 条） | 同上 |

**"不是恶魔"这一条排除掉了大半个游戏 Boss 曲库**——那类曲子默认写的是"反派登场"，
情绪是愤怒或张牙舞爪。无常的情绪是**职责**，不是愤怒。宁可选冷淡、有距离感的，别选凶的。

### 3.2.1 已定（2026-09-27）

试听之后定的两首：

| 阶段 | 曲目 | 许可 | 处理 |
| --- | --- | --- | --- |
| 一 | **Fantasy Choir 2.wav**（cesisco，126 s） | CC0 | **不循环**，不剪辑。24 bit → 转 16 bit Ogg |
| 二 | **Danse macabre**（Saint-Saëns / Stokowski 1925，419 s） | PD | **切掉前 44.6 s 引子**，见下 |

**为什么二阶段切在 44.6 s 而不是 30 s。** 实测这条录音的 RMS 能量包络（ffmpeg 转单声道后逐窗算）：

| 时间 | 段落 |
| --- | --- |
| 0:00–0:13 | 竖琴午夜钟声（十二下） |
| 0:13–0:41 | 独奏小提琴，死神主题 |
| 0:41–0:44.5 | 独奏收尾的低谷（RMS 掉到 0.022） |
| **0:44.6** | **乐队全奏进入**（RMS 0.043 → 0.142，45.6 冲到 0.225） |
| 6:32 | 公鸡打鸣，骷髅散场 |
| 6:49+ | 衰减到静音 |

切 30 s 会切在独奏中段；**44.6 s 才是舞曲真正的起点**，也正是"前倾的执行者"该出现的那一拍。

```powershell
# 去掉引子，只留舞曲
ffmpeg -ss 44.6 -i danse.ogg -ac 2 -ar 44100 -c:a libvorbis -q:a 6 phase_two.ogg
```

**《骷髅之舞》讲的是什么**（决定它配不配得上这个 Boss）：午夜，死神拉响小提琴，死者爬起来跳舞，
公鸡一叫就散场。独奏小提琴用变格定弦（E 弦降到 E♭）+ 大量三全音——中世纪叫它"魔鬼音程"；
木琴代表骨头相撞；中段引用安魂弥撒的《末日经》。**这支曲子里死神是个按点上班的乐手，不是怪物**：
他把人叫出来跳舞，天亮就收工。**是职责，不是愤怒**——正对 01_SPEC 那句"不是神本身，也不是恶魔"。

⚠️ 这是 1925 年虫胶唱片的转录，71 kbps，**底噪明显**。做恐怖 BOSS 战多半是加分，但要知道这件事。

**被切掉的 44.6 s 引子不必浪费，但也没有非用不可的位置**：`TRANSITION` 只有 3 秒（§1.1），
塞不进去；`PHASE_ONE_WARMUP_TICKS = 20`（1 秒）同理。**要留就等以后加"唤醒"演出时再说。**

**其余候选（未采用，留档）**

| 阶段 | 曲目 | 作者 / 录音 | 页面 | 许可 |
| --- | --- | --- | --- | --- |
| 一 | Gymnopédie No. 1 | Satie | [Commons](https://commons.wikimedia.org/wiki/File:Gymnopedie_No._1..ogg) | **CC0** |
| （无位置） | Breves dies hominis | 12–13 世纪，Magdalen Kadel 唱 | [OGA](https://opengameart.org/content/breves-dies-hominis) | **CC0** |
| 二 | Boss Battle Music | Juhani Junkala | [OGA](https://opengameart.org/content/boss-battle-music) | **CC0** |
| 二 | Night on Bald Mountain | Mussorgsky / Skidmore College | [05 §2.6 A10](05_BGM_LICENSING_RESEARCH.md) | **PD** |
| 二 | Mars, the Bringer of War | Holst / USAF Band | [05 §2.6 A4](05_BGM_LICENSING_RESEARCH.md) | PD（美国政府作品 §105，**只在美国成立**） |

**第二梯队：CC-BY，要署名，但音色更准**

| 阶段 | 曲目 | 许可 | 备注 |
| --- | --- | --- | --- |
| 一 + 二 | **Toccata and Fugue in D minor, BWV 565**（Norbert Schenk 管风琴，2017） | CC BY 4.0 | **一个文件横跨两阶段**：慢引子 → 快赋格。管风琴 = 神圣 + 恐怖，对"神圣遗骸"几乎是直译；有 VRT 归档的商用授权声明，权利链干净 |
| 一 | Views From Atop the Jade Kings Throne | CC BY 3.0 | 二胡、锣、弦乐。**东方色彩在这里是加分项，不是前提** |
| 二 | Samurai Nights | CC BY 4.0 | 附全部单轨 loop；打散重拼时可以只用它的鼓和弦 |
| 二 | Colossal Boss Battle Theme | CC BY 3.0 | 人声吟唱 + 重鼓，含 loop 版 |
| 鼓层 | Taiko drums (seamless loop) | CC BY 3.0 | 太鼓无缝循环，给上面任意一首加鼓 |

**第三梯队：纯素材，拿来自己叠**

Dark Shrine Loop（CC0）、Drums of Dawn（CC BY 3.0）、Ritual（CC BY 3.0）、
Phlegmlee 恐怖音效包（CC0）等，见 [05 §2](05_BGM_LICENSING_RESEARCH.md)。

### 3.2.2 完整候选清单

**[05 §2](05_BGM_LICENSING_RESEARCH.md) 是全部家底**，一共 44 条：

| 出处 | 条数 | 内容 |
| --- | ---: | --- |
| [05 §2.1–2.3](05_BGM_LICENSING_RESEARCH.md) | 19 | OGA / itch.io / FMA 上可免费商用的游戏配乐 |
| [05 §2.6 A1–A17](05_BGM_LICENSING_RESEARCH.md) | 17 | **公有领域古典录音**：Holst《行星》组曲、Berlioz、Mussorgsky、Saint-Saëns、Satie、Bach、Chopin… |
| [05 §2.6 B1–B7](05_BGM_LICENSING_RESEARCH.md) | 6 | 中文/东方素材（清帝国国歌《鞏金甌》、锣、笛、太鼓采样）——**整组可以跳过** |
| [05 §2.6 C1–C2](05_BGM_LICENSING_RESEARCH.md) | 2 | 需指名署名的（Holst《火星》Skidmore 版、Bach 管风琴版） |
| [05 §6.2 R14–R17](05_BGM_LICENSING_RESEARCH.md) | 4 | 看似能用、实际**不能**用的反例，附原因 |

> **校正记录**：本节初稿把"中式/东方"当成了硬需求，顺位排反了——把真正对调性的公有领域古典
> （05 §2.6）压到了附录，把东方色彩的（05 §2.6 B 组）提到了首位。那个前提**不是需求方提的**，
> 是调研指令里自行加入的。现已按 01_SPEC §1 的原始定义重排。

**想自己在 OGA 上翻**（授权逐条目标注，可以按 CC0 过滤）：

- [战斗向 CC0](https://opengameart.org/art-search-advanced?keys=boss&field_art_type_tid%5B%5D=12&field_art_licenses_tid%5B%5D=4&sort_by=count&sort_order=DESC)
- [阴森向 CC0](https://opengameart.org/art-search-advanced?keys=dark&field_art_type_tid%5B%5D=12&field_art_licenses_tid%5B%5D=4&sort_by=count&sort_order=DESC)
- [仪式/圣咏向 CC0](https://opengameart.org/art-search-advanced?keys=ritual+OR+choir+OR+chant+OR+sacred&field_art_type_tid%5B%5D=12&field_art_licenses_tid%5B%5D=4&sort_by=count&sort_order=DESC)
- [管风琴/古典向 CC0](https://opengameart.org/art-search-advanced?keys=organ+OR+orchestral+OR+strings&field_art_type_tid%5B%5D=12&field_art_licenses_tid%5B%5D=4&sort_by=count&sort_order=DESC)

### 3.2.3 循环与衔接

一阶段铺底就好，别选旋律太满的；二阶段才上鼓和铜管。
两首**调性接近**（同主音或差五度）交接会顺很多；实在不搭，就在过场那 60 tick 留白，
宁可断一下也不要硬接。**同一位作者/同一张专辑里的两首**天然共享调性，
如果能找到这样的组合，比分别挑两首"最合适"的更省事——
上面第一梯队里 Fantasia Choir 和 Juhani Junkala 都各自成套，值得先去里面翻一遍。

### 3.3 可用但要署名：CC-BY

**Kevin MacLeod / Incompetech**（[FAQ](https://incompetech.com/music/royalty-free/faq.html)）——
许可 **CC-BY 4.0**，明确不是公有领域。它的 FAQ 里点名了电子游戏的署名方式：

> **Video Games** — Most commonly, credits are placed on a "Credits" screen found in the settings menu.

也就是说**可以**打进 mod，但必须在玩家能找到的地方给署名。格式是它给的模板：

```
"<曲名>" Kevin MacLeod (incompetech.com)
Licensed under Creative Commons: By Attribution 4.0
https://creativecommons.org/licenses/by/4.0/
```

Maledict 现在没有 credits 界面，只有 README。**放进 README 的 Credits 段 + jar 内附一份
`CREDITS.md` 是够的**（玩家打开 jar 或仓库都能看到），但如果以后加了游戏内 credits 界面，
CC-BY 的条目应该搬进去。

CC-BY 还有两条容易被忽略的硬性要求（读 legalcode 得出的）：

- **CC BY 3.0 §4(a)**：许可 URI 必须随**每一份副本**一起分发。所以 `CREDITS.md` 要放
  `src/main/resources/` 里**打进 jar**，不能只放在 GitHub 仓库。
- **CC BY 4.0**：如果你改了它（**转成 Ogg、为了循环做过裁剪，都算改**），必须在署名里**说明改了什么**。
  一行字的事：`converted to Ogg Vorbis; trimmed for looping`。

这两条是把"CC-BY 也能用"变成"CC-BY 别嫌麻烦"的地方。**如果一首 CC0 的能达到八成效果，就别为了那两成去碰 CC-BY。**

### 3.4 不建议直接打包的

**Pixabay** —— 授权叫 Pixabay Content License，不要求署名，商用也允许，但条款里有两句很关键
（[全文](https://scancode-licensedb.aboutcode.org/pixabay-content.html)，原文引用）：

> The Pixabay License does not allow:
> Sale or distribution of Content e.g. as a posters, digital prints, **music files** or physical products,
> **without adding any additional elements or otherwise adding value**

后半句"除非添加了其它元素或增加了价值"是唯一的出口。一首曲子嵌进一个 mod 里，算不算"增加了价值"？
**大概率算，但这是解释，不是白纸黑字。** 而且同一页还有一句：

> items in the Content, such as identifiable people, logos, brands, **audio samples** etc. may be subject to
> additional copyrights ... Pixabay does not represent or warrant that such consents or licenses have been obtained

也就是说 **Pixabay 不保证上传者自己没拿别人的素材**。真出了事，责任在你。

还有一个时间点值得知道：**2019 年 1 月之前上传的 Pixabay 素材是真的 CC0**（当时的服务条款 §3 明确
以 CC0 释出）。2019 年之后换成自家的 Content License，才有上面那句"Standalone / 保持原样"的限制。
所以如果你看中某首，先查它的上传日期——**2019 年前的老素材反而比新的干净**。

再加一条：Content License 里保留了 Pixabay 随时 *"cancel or change the licenses"* 的权利。
CC0 是不可撤销的弃权，这个不是。

**结论：Pixabay 可以当"实在找不到 CC0 时的备选"，但别当成"零风险"。**

**其它同样不建议的**（下表全部读的是各家的原始条款；三条够不着的我标了"未核实"）：

| 来源 | 能不能打包进 jar | 依据 |
| --- | --- | --- |
| **Bensound 免费档** | ❌ **不行** | 免费许可只覆盖"免费的网络视频 / 直播"和非营收教育用途，**游戏不在范围内**；而且这个免费许可**可撤销** |
| **Bensound 专业档** | ⚠️ 游戏在范围内，但仍不行 | 条款确实列了"background music for a video game"，但 §5.1(ix) 禁止 *"permit a third party to use or copy the Audio Asset(s)"*——**发一个 jar 就是给每个用户一份副本** |
| **Uppbeat（付费）** | ⚠️ 大概可以 | "Permitted Media … (iv) online games or applications"，且"Permitted Distribution … (v) any web-based game or application which can be downloaded and distributed online"。但要求**在订阅期内**完成同步，且 §5.3 禁止把 Content 当自己的产品转卖。断订阅之后还在发行就说不清了 |
| **YouTube 音频库** | ❌ **未核实，别用** | 授权文本只在 YouTube Studio 里逐条显示，公开页面是客户端渲染的，抓不到。没有可引用的条款就没有可辩护的立场 |
| **Epidemic Sound / Artlist** | ❌ **未核实** | 条款页 404 或纯 JS，够不着。两者普遍是"订阅期内可用于你的内容"，把音频打进发行物是另一回事 |
| **freepd.com** | ❌ 站点已永久关闭 | 2008–2025，很多 OGA 条目里的 freepd 链接已经死了，它的 PD 声明再也无法复核 |

**古典曲目另算。** **作曲**是公有领域，**录音**不是——同一个作品，柏林爱乐的录音有版权，某个 CC0 的业余录音没有。
所以"贝多芬是公有领域"这句话对你要做的事**毫无帮助**，你打包的是录音。

- **Musopen 不是授权方，是索引站。** 它自己的 FAQ 写着
  *"Musopen cannot guarantee that any music uploaded by its users is, in fact, in the public domain"*；
  而且它标成 "Public Domain" 的其实是 **CC Public Domain Mark 1.0**——**那不是 CC0**，
  PD Mark 是一个"我认为它没有版权"的标签，**不授予任何权利**。它的许可筛选里还混着 CC BY-NC/ND。
- **IMSLP 不实用**：它是按**加拿大**法律清理版权的，而且它的 Protected Recordings 是
  非商用 + 禁演绎 + 禁公开演出。
- Wikimedia Commons 上要**两个标签**才算干净：作曲一个、**录音一个**。只挂了作曲标签的条目，
  **录音并没有被清理**。
- 美国录音的版权年限可以查 [17 U.S.C. §1401](https://www.copyright.gov/title17/92chap14.html)：
  1926 年以前出版的是公有领域，1926 年的到 **2027 年 1 月 1 日**才进入，1947–56 年的要等 110 年。
  要找公有领域录音，优先看美国政府作品（§105，如美国海军陆战队乐队的录音）和明确标了 CC0 的条目。

> ⚠️ **如果你打算写脚本批量抓 Commons，先看这条。**
> Commons 上 `File:Satie - Gnossienne 1.ogg` 和 `File:Gnossienne 3 (Satie).ogg` 两条，
> wikitext 里写的是"作曲 `{{pd-old}}` / **演奏** `{{self|cc-by-sa-3.0|GFDL}}`"，
> 但 **API 聚合出来的 `License` 字段是 `"pd"`、`LicenseShortName` 是 `"Public domain"`** ——
> 作曲标签把聚合值压过去了。**照 API 字段抓的脚本会把这两条当成公有领域发出去，实际是 CC BY-SA 3.0。**
> 要自动化就**解析 wikitext，或者读每个文件自己的 `rel="license"` 锚点**，
> 永远不要读聚合的 `License` / `LicenseShortName`。
> 同理，`{{PD-old}}` 只说明作曲是老的；它挂在 2006 年的演奏上就是错标。
> 完整案例见 [05 §6.2](05_BGM_LICENSING_RESEARCH.md) 的 R14–R17。

**AI 生成音乐**（Suno / Udio 之类）不是答案：训练数据的合法性本身还在诉讼中，
而且免费档普遍禁止商用，生成物的可版权性也不确定。要走这条路等于把风险从"别人的版权"
换成"平台条款 + 未决诉讼"。

### 3.5 落地清单

1. 建 `CREDITS.md`，列出每首曲子的：曲名 / 作者 / 来源 URL / 许可 / 取用日期 / **改过什么**。
   **CC0 也照写**——不是为了合规，是为了以后有人来主张权利时你能一秒拿出出处。
   CC0 的 deed 自己就写了 *"Creative Commons has not verified the copyright status of any work to which
   CC0 has been applied"*，所以**每个 CC0 条目页都存一份 HTML 或 PDF 快照 + 日期**。
   这是你全部的举证材料，十分钟的事。
2. **放两份**：仓库根的 `CREDITS.md` 给人看，`src/main/resources/CREDITS.md` **打进 jar**。
   CC BY 3.0 §4(a) 要求许可 URI 随每一份副本分发，只放 GitHub 不算。
3. `README.md` 的 License 一节加一句：**代码是 LGPL-3.0-only，音频资产许可见 CREDITS.md**。
   不加这句，别人会默认整个 jar（包括音乐）都是 LGPL。
4. 音频文件改名成 `vicissitude_phase_one.ogg` 这种自己的命名，**文件名里绝对不能有空格**（见 §1.2）。
   CREDITS.md 里写清映射：`vicissitude_phase_one.ogg ← Lotus Pond (BiteMe Games, CC0)`。

**调研的原始材料**都在 [`source/research/`](source/research/) 下：主报告
[05](05_BGM_LICENSING_RESEARCH.md)、itch.io/FMA 与 Musopen/IMSLP 的分头调研、
以及 `musopen-raw/`（22 份抓下来的页面快照 + 抓取脚本）。
这些是 04 §3 每条结论的出处，**别删**；真嫌占地方（1.3 MB）也只删 `musopen-raw/`。

### 3.6 转码与循环

**格式：Ogg Vorbis（`.ogg`）。** 16 bit / 44.1 kHz / 立体声就够，别往上加。

```powershell
# Boss 音乐：立体声，q6 ≈ 192 kbps
ffmpeg -i in.wav -ac 2 -ar 44100 -c:a libvorbis -q:a 6 out.ogg

# 音效：单声道（单声道才会随距离衰减）
ffmpeg -i in.wav -ac 1 -ar 44100 -c:a libvorbis -q:a 5 out.ogg
```

`-q:a` 的范围是 −1 到 10，公式是 `16×(q+4)` / `32×q` / `64×(q−4)` kbps。

**体积预算**（按上面的码率算的）：2 分钟的曲子 q4 ≈ 1.9 MB、q5 ≈ 2.4 MB、q6 ≈ 2.9 MB。
两首 Boss 主题合计 **5–6 MB**，对 mod 来说完全可以接受。整个新增音频**控制在 10 MB 以内**。

**循环点怎么做才对**：

- **不要**在头尾补静音——每循环一圈就会听到一次空拍。
- **不要**在尾部做淡出——循环回去的瞬间会"啪"一下。
- 正确做法：把越过循环点的混响尾巴**绕回开头**，然后把循环终点按这个量往前挪，
  这样接缝两边的采样是对得上的。
- 素材本身标了 `loop` / `seamless` 就省了这一步（§3.2 里那两个 ★ 都是）。
- 验循环用 `mpv --loop-file=inf out.ogg`，听十几圈，接缝处有任何一次"咔"都说明没对上。

⚠️ **没验证过的一条**：`stream: true` 到底会不会把整首解码后的 PCM 常驻内存
（2 分钟立体声理论上是 ~21 MB）。对两首 Boss 主题无所谓，**但别在这个 mod 里塞一套十首的流式原声带**。

---

## 4. 验收清单

做完之后至少要进游戏确认这些：

- [ ] 沉睡状态的雕像**不放**音乐
- [ ] 第一刀打响、进入 `PHASE_ONE` 的瞬间音乐淡入
- [ ] 过场 60 tick 里音乐不断（或按 §1.1 的选择在过场开场切）
- [ ] `PHASE_TWO` 起二阶段主题，且血条皮肤同时换成二阶段
- [ ] 打死之后音乐淡出，不是硬切
- [ ] 玩家跑出 96 格再回来，音乐重新淡入
- [ ] 两只无常同时存在时不叠放
- [ ] 「音乐」滑条拉到 0 时完全静音
- [ ] 原版背景音乐在战斗中被压掉（若采纳 §1.5 第 4 条的 `stopPlaying()`）
- [ ] 二阶段才第一次靠近的玩家，看到的血条是二阶段皮肤（验证 `addPlayer` 补发）
- [ ] 血条和别的模组的 BOSS 血条同时存在时不重叠、不错位
- [ ] 断线重连后血条样式回到正确值

### 多只无常同时在场（单测覆盖规则，**未实机验证**）

- [ ] 两只同阶段：音乐不切来切去
- [ ] 一只一阶段 + 一只二阶段：放的是二阶段曲，且走来走去**不抖**
- [ ] 最近那只还是沉眠雕像、二十格外有一只在打：**照样放音乐**（初版会哑掉）
- [ ] 两条血条竖直叠放，**下一条的名字不压在上一条 overlay 上**
- [ ] 杀掉先进的二阶段那只之后，音乐退回一阶段曲而不是停掉
- [ ] 两只分别在不同玩家身边时，两个客户端各听各的（选曲是每客户端算的）

### 什么时候不该放（§1.7.1，**未实机验证**）

- [ ] 站在一只 `DORMANT` 的雕像旁边：**没有音乐**
- [ ] 远处（>48 格）有人在打：**没有音乐**；走近到 48 格内才开始淡入
- [ ] 战斗中拉开到 48 格外：淡出；走回来：淡入
- [ ] 边界上来回走：听有没有脉动（有的话加回滞，见 §1.7.1）

### 待定，没做

- [ ] `PHASE_TWO` 没有"没人打就复位"的逻辑（`tickEmptyEncounter` 只管 `DORMANT/PHASE_ONE`）。
      距离门挡住了"听得到"，但 Boss 本身不会脱战。**要不要让二阶段也脱战是 Boss 行为的设计问题**，
      不在音乐这条线里单方面决定。
