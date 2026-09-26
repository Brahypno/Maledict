# 01 — 抑郁符文规格

状态：**模型待确认**。按仓库的工作约定，先钉死状态转移，确认后再动代码。
文案（中文 codex / tooltip / 英文派生）等用户提供，本页不含成品文案。

## 1. 定位与配方

- 物品 id：`maledict:rune_of_melancholia`，中文名「抑郁符文」。
- 配方（runeworking，污染石档）：

```java
new RunicWorkbenchRecipeBuilder(MaledictItems.RUNE_OF_MELANCHOLIA.get(), 1)
        .setPrimaryInput(ItemRegistry.TAINTED_ROCK_TABLET.get(), 1)
        .setSecondaryInput(SpiritTypeRegistry.UMBRAL_SPIRIT.spiritShard.get(), 16)
```

污染石符板是 Malum 现成物品，不新增符板。Malum 自己的 25 条 runeworking 配方档位如下，
本符文落在第三档，16 这个数字与同档一致，成本差异全部由「幽影精魂比基础精魂稀有」承担：

| 符板 | 精魂 | Malum 里的例子 |
| --- | --- | --- |
| 符文木符板 | 基础精魂 ×32 | `rune_of_haste` / `rune_of_loyalty` / `rune_of_motion` / `rune_of_warding` |
| 灵魂木符板 | 基础精魂 ×32 | `rune_of_the_aether` / `rune_of_the_arena` / `rune_of_the_hells` / `rune_of_the_seas` |
| **污染石符板** | 基础精魂 ×16 | `rune_of_culling` / `rune_of_dexterity` / `rune_of_volatile_distortion` 等八枚 |
| 虚空符板 | 基础精魂 ×16 | `rune_of_the_heretic` / `rune_of_toughness` 等 |

两半效果：**适应**（麻木，第 2 节）与**延迟**（钝，第 3 节），同属这一枚符文。

数值全部写死为类内常量，**不做 config**（用户决定）。

## 2. 效果一：适应（麻木）

### 2.1 键

一条「伤害消息」（key）= **伤害类型 + 来源**：

- 伤害类型 = `DamageSource#getMsgId()`。注意它是死法的 id，不是攻击者的 id：原版僵尸近战与
  蠹虫近战都是 `mob`，区别只在攻击者的显示名。所以只靠 `getMsgId()` 分不开僵尸和蠹虫。
- 来源 = 造成伤害的生物**种类**（`EntityType` 的注册名）。取 `getEntity()`，为空时退到
  `getDirectEntity()`（箭矢、火球之类），都为空（摔落、火、毒）则只有伤害类型。
- 拼成 `"<msgId>|<entityType>"`，例如 `mob|minecraft:zombie`、`arrow|minecraft:skeleton`、
  `fall`。

「来源」取种类而不是个体，是刻意的：这正是一群僵尸共享同一格窗口、因而越打越钝的原因。
若要改成逐个个体（每只僵尸各记各的），只需要换掉键的构造，状态机不用动。

### 2.2 状态机

窗口容量 = **适应 1**，即只记得最近挨过的那一条消息。算法与 `DamageAdaptation` 完全一致
（**先记录、再判定**，计数在判定之后自增），该类的语义已冻结并有 `DamageAdaptationTest` 覆盖，
但玩家版需要单独的账本实现，见 2.3。

判定倍率：本次挨打**之前**就已经在窗口里的消息才吃 `e^-(它在窗口里挨过的次数)`；刚被记进来的
这条是全额。窗口装不下时挤掉最久没挨的那条，**被挤出去的那条计数一并作废**。

| 序列 | 倍率 |
| --- | --- |
| 僵尸A 打、僵尸A 打、僵尸A 打（或五只僵尸交替打，同一个键） | `1` → `e⁻¹` → `e⁻²` |
| 僵尸 → 蠹虫 → 僵尸 | `1` → `1` → `1` |
| 僵尸 → 僵尸 → 蠹虫 → 僵尸 | `1` → `e⁻¹` → `1` → `1` |

换成数字：`e⁻¹ = 0.368`、`e⁻² = 0.135`、`e⁻³ = 0.050`、`e⁻⁵ = 0.0067`。

两点后果，都是有意接受的强度（幽影精魂极稀有）：

- **单一伤害类型下会一路钝到零**。被一群僵尸围着打，几秒之后就打不动了。
- **混合伤害会持续打断适应**。任何一种别的伤害插进来就把窗口里那一格挤掉、计数作废，回到全额。
  这让「适应 1」在混战里天然自我限制，也是它不需要衰减或封顶的原因（用户决定：不做收敛旋钮）。

### 2.3 存储与清账

- 账本（窗口 + 各消息计数）以 **NBT 存在符文物品本身**上，不放 capability、不放玩家身上。
- 因此：摘下来再戴上账目还在；被无常没收、掉在地上、换人捡走，账目跟着物品走。
- **死亡清除**：佩戴者死亡时把账本清空。
- 只在佩戴时记账与判定；没戴的时候挨打不记账也不减伤。

### 2.4 反馈

不走自定义 MobEffect，因此没有 HUD 图标。用 Malum 符文的老写法：`addExtraTooltipLines` 里
一条静态描述（对照 `RuneOfRottenBoneItem` 的 `positiveEffect`）。不加粒子与音效。

### 2.5 落点

`LivingHurtEvent`（护甲前，与仓库其它逻辑一致）：

```java
event.setAmount(event.getAmount() * ledger.adapt(key(event.getSource()), ADAPTATION_LEVEL));
```

## 3. 效果二：延迟（钝）

### 3.1 池子模型

**先对冲，再排队。** 新来的伤害先吃掉攒着的治疗，新来的治疗先吃掉攒着的伤害，抵不完的才真的排进
池子 —— 所以两个池子不会同时有值（HUD 上不会同时出现紫色段和绿色段），一剂治疗等于先把还没落地的
痛抹掉。这是用户提的做法：储存的回血与储存的受伤先自己扣一下。

释放各按「比例 + 地板」走，**两个池子的比例不同**：

```
release = min(池子, 池子 × 比例 + RELEASE_FLOOR)
pendingDamage -= release;   // 比例 0.025，半衰期 ≈ 1.4 秒
pendingHeal   -= release;   // 比例 0.010，半衰期 ≈ 3.5 秒
```

掉血快一档是刻意的：痛得跟上战斗节奏，再慢就等于长时间无敌。回血慢一档也是刻意的：一剂金苹果
不该在血条上炸开。

**地板（`RELEASE_FLOOR = 0.02`，0.4 点/秒）是为了剪掉指数的长尾**：纯指数是「前期特别快、后期特别慢」，
9.87 点的池子要 273 tick（约 13.6 秒）才归零，最后半颗心要五秒多、最后那点渣还要八秒；加地板后同一个
池子约 5 秒结清，而开局那一下几乎没变（首 tick 从 0.247 只涨到 0.267）。收敛性也没丢：地板的等效
不动点是负的，池子一定在有限 tick 内归零，稳态长度从 `R(1-k)/k` 变成 `(R(1-k) - 地板)/k`。

两处例外要注意：满血时释放出来的治疗作废（不囤积），血量归零则走死亡。同一 tick 内**先结算治疗、
再结算伤害**，让治疗有机会把死亡那一线拉回来。

### 3.2 入队与结算

落点已按 Forge 47.4.23 的源码逐行核对（`LivingEntity#actuallyHurt`，玩家走 `Player#actuallyHurt`）：

| 环节 | 落点 | 处理 |
| --- | --- | --- |
| 适应 | `LivingHurtEvent`（在 `actuallyHurt` 开头，护甲与附魔**之前**） | `setAmount(amount × 倍率)`；比例是护甲前的比例 |
| 伤害入队 | `LivingDamageEvent`（护甲、附魔、**吸收都算完之后**，`setHealth` 之前） | 记 `pendingDamage += amount`，`setAmount(0)`；入队的就是那一刻真要扣的血，吸收不会被重复扣。入队跳过了原版那段 `if (f1 != 0)`，所以自己补一次 `causeFoodExhaustion` |
| 治疗入队 | `LivingHealEvent` | 记 `pendingHeal += amount`，`setAmount(0)`（与取消等价，都命中 `heal` 开头的 `return`） |
| 每 tick 结算 | `PlayerTickEvent`（服务端、END） | 先治疗再伤害，按 3.1 释放 |
| 释放 | 一律 `setHealth` | 入队时已经扣过护甲/附魔/吸收，再过一遍伤害管线会重复减伤；更要紧的是下面那条 |
| 致死那一下 | `setHealth(0)` + `die(source)` | 死因取这批最早那条来源；掉落、死亡消息、击杀归属都照原版 `die()` 走 |

**涓流一律不许走 `hurt`。** 池子是按 tick 一点点放的，一旦写成「血量不够就走 `hurt`」，低血量时
就会变成每隔十几 tick 给自己来一下：受伤动画一直闪、无敌帧被反复续上（连怪都打不动你了），而护甲
又把每次那一点点削掉一截 —— 血条卡在濒死线上、池子慢慢漏完，玩家全程「停在受伤状态」。这是首版
真踩到的坑，见第 9 节。

代价：延迟致死**不吃原版的不死图腾**（`checkTotemDeathProtection` 只在 `hurt` 里被调用）。这是
有意的取舍 —— 涓流必须安静，不能为了图腾顺手给自己挂无敌帧。

指数池的累计释放量收敛到初始池子大小，所以池子不会被无限拖着不还。入队时必须连 `DamageSource`
一起存（当前这批里最早那条），否则延迟伤害致死时死因与击杀归属是错的；它不落盘，重启后退回无主伤害。

白名单：`DamageTypeTags.BYPASSES_INVULNERABILITY`（`/kill`、掉出世界）**不入队**，立即结算。
判断照抄 `RottenBoneEvents`。

已知与原版的差异（都很小，记在这里免得以后当成 bug）：延迟掉的伤害不计入原版的 `DAMAGE_TAKEN`
统计；伤害在入队时（而不是落地时）扣饱食度。

连锁反应（用户确认接受为 flavor，不当 bug）：`getHealth()` 始终是真实血量，只是变化得慢，所以
一切读血量的逻辑都跟着后移 —— `HalfHealth` 判定的半血触发、启蒙之年在半血时刷魂息虚空等等。

### 3.3 与装备的关系

- 符文只是**开关**：戴着的时候伤害与治疗才入队。
- 队列本身**不绑定符文**：一旦入队必须还清，摘下符文、甚至符文被没收，池子照常结算。
- **死亡清除**两个池子。

### 3.4 HUD

用 Forge 的官方注册方式（不是核心 mixin）：`RegisterGuiOverlaysEvent`（MOD 总线、`Dist.CLIENT`）
+ `IGuiOverlay`，`event.registerAbove(VanillaGuiOverlay.PLAYER_HEALTH.id(), ...)`。

**画在原版红心自己的格子上，不是额外一行。** 这一条与 dreamtinker 的 shellheart 有本质区别：
shellheart 是**额外血池**，所以它另起一行、并给 `gui.leftHeight` 加高把护甲/饥饿行挤上去；
延迟池不是额外血量，它是同一条血条上「还没落地的部分」，所以它必须复用原版红心的坐标，
**不新增行、不碰 `gui.leftHeight`**。

- 注册**两个**显示（待扣除、待治疗），两个都画在原版红心的格子上，各自染色。
- 待扣除：从 `ceil(血量) - ceil(待扣除)` 到 `ceil(血量)` 的那几个**半心**染成**邪恶精魂紫 `#792CEC`**（正在离开）。
- 待治疗：从 `ceil(血量)` 往上、最多到血条顶端（`ceil(最大生命/2) × 2` 个半心）染成**神圣精魂粉 `#EE2C88`**。
  注意吸收心（黄心）画在**整条生命条之后**，不是紧接当前血量，所以这个区间天然不会盖到它们。
- 坐标逐行照抄原版：`x = 屏宽/2 - 91 + (下标 % 10) × 8`，`y = 屏高 - 39 - (下标 / 10) × 行高`，
  `行高 = max(10 - (行数 - 2), 3)`，`行数 = ceil((最大生命 + 吸收) / 2 / 10)`。低血抖动
  （`血量 + 吸收 ≤ 4` 时用同一个种子 `tick × 312871` 抖 0/1 px）与再生心的 `y - 2` 也要复刻，
  否则抖动那几帧染色会和红心错开。
- **染色不能用原版红心贴图**：原版颜色烘焙在贴图里（满心 `241,24,24`），`setColor` 是乘法，
  只会越染越黑。所以自备一张白色心形遮罩（满心 / 左半心，`assets/maledict/textures/gui/
  delayed_vitals_hearts.png`，生成脚本在 `art/melancholia/tools/`），再用 `setColor` 上色。
- Maledict 目前没有任何 GUI overlay，这是第一处。

### 3.5 同步

**事件驱动 + 客户端本地推演**（不逐 tick 发包）：

- 池子的衰减是确定性公式，客户端用同一个 `RELEASE_PER_TICK` 自己往下走，因此两次「入队」之间
  服务端**一个包都不用发**，HUD 仍然每帧平滑变化。
- 服务端发包的时机只有四种：入队（同一 tick 内多次入队合并成一包）、清零（死亡）、
  每 40 tick 一次的校正（仅在池子非空时）、以及玩家登录/换维度时的一次性同步。
- 代价：待结算期间约 0.5 包/秒/人，逐 tick 同步是 20 包/秒/人。

## 4. 两半的顺序

同一记伤害上**先适应、再延迟**：`LivingHurtEvent` 里先按倍率削减，进到 `LivingDamageEvent` 的
才是削减后的量，入队的自然也是削过的值。反过来会让「入队的量」和「实际掉的血」对不上，
减伤看起来像失效。

链路的完整顺序：`LivingHurtEvent`（适应）→ 护甲/附魔 → `LivingDamageEvent`（入队）→ 池子按 tick 释放。

## 5. 常量表

| 常量 | 值 | 说明 |
| --- | --- | --- |
| `ADAPTATION_LEVEL` | `1` | 窗口容量，写死 |
| `RELEASE_PER_TICK` | `0.025` | 半衰期 ≈ 28 tick ≈ 1.4 秒；稳态池子 = 入队速率 / 0.025 |
| 池子上限 | 无 | 靠指数释放天然收敛 |
| 适应衰减 / 计数封顶 | 无 | 靠窗口 1 格自挤 |

## 6. 未决

1. **我替用户写的几处文案**，等过目或替换：物品 tooltip 一行
   （`malum.gui.curio.effect.maledict.melancholia`）、两个条目的 description
   （「自由的创作」取自用户原文，「痛得慢一些」是我写的）、以及全部英文派生。
2. **codex 坐标**：`UMBRAL_EXPERIMENT_X/Y = (4, 10)` 是按现有几页的间距估的，进游戏看着挤就挪。
3. **进游戏实测**：致命一击改走原版 `hurt` 之后要确认 —— 掉落只发生一次、死因与击杀归属正确、
   图腾能救、无敌帧不会把该还的账永远卡住。
4. **血条配色**：现在是幽影紫 `(0.42, 0.29, 0.63, 0.85)` 与苍白绿 `(0.55, 0.88, 0.62, 0.85)`，
   进游戏看着不顺眼就改这两个常量。
5. **符文贴图是占位图**，等美术覆盖同名文件；生成脚本在 `art/melancholia/tools/`。

## 7. 已定

| 项 | 结论 | 来源 |
| --- | --- | --- |
| 死亡清除范围 | 适应 NBT 与两个池子一起清；符文换主人（别人捡走）不清账 | 用户未反对，按默认落地 |
| 同步方式 | 事件驱动（入队 / 清零 / 进世界）+ 每 40 tick 校正，客户端本地推演衰减 | 用户授权由实现方挑性能最好的一种 |
| codex 归属 | 虚空魔法部分新开一页「幽影精魂的实验」，抑郁符文挂在它的选择页里 | 用户指定 |
| 符文页文案 | 正文「我好忧愁 我好忧愁 我好忧愁 我好忧愁」，按哀歌弓那页的重复短语写法 | 用户提供 |
| 血条位置 | 画在原版红心自己的格子上，不新增行、不动 `gui.leftHeight` | 用户指出 shellheart 是额外血池，与本机制不同 |
| 血条贴图 | 自备白色心形遮罩（满/左半/右半）再 `setColor`；原版红心的颜色烘焙在贴图里，染不动 | 查 Forge 源码得出的结论 |
| 致命一击 | `setHealth(0)` + `die(source)`，**不走 `hurt`**（走了会每十几 tick 给自己挂一次受伤动画与无敌帧） | 首版实测踩坑后改 |
| 数值 | 全部写死为类内常量，不做 config | 用户指定 |

## 8. 落地清单

| 文件 | 作用 |
| --- | --- |
| `common/item/RuneOfMelancholiaItem.java` | 符文物品；适应账本读写 stack NBT；消息键构造 |
| `common/entity/DamageAdaptation.java` | 复用无常的窗口算法，新增「连次数一起恢复」的重载（旧行为不变） |
| `common/vitals/DelayedVitalsPool.java` | 两个池子的纯数学 |
| `common/vitals/DelayedVitalsHearts.java` | 血条染色段的半心数学 |
| `common/vitals/DelayedVitals.java` | 挂在玩家身上的 capability（池子、来源、脏标记、`releasing`） |
| `common/vitals/DelayedVitalsEvents.java` | 适应 / 入队 / 结算 / 死亡清除 / 进世界同步 |
| `network/DelayedVitalsPacket.java` + `MaledictNetwork` | 3 号包，PLAY_TO_CLIENT |
| `client/DelayedVitalsClient.java` | 收包覆盖 + 客户端本地推演 |
| `client/DelayedVitalsOverlay.java` + `MaledictGuiOverlays.java` | 两条血条显示与注册 |
| `art/melancholia/tools/*.py` | 符文占位贴图与心形遮罩的生成脚本 |
| 测试 | `DelayedVitalsPoolTest`、`DelayedVitalsHeartsTest`、`DamageAdaptationTest` 新增两条 |

## 9. 首次进游戏后的修补

| 症状 | 原因 | 处理 |
| --- | --- | --- |
| 创造栏里找不到这枚符文 | `MaledictCreativeTabs` 是一枚一枚 `output.accept` 列的，加物品时漏了这一行 | 补 `RUNE_OF_MELANCHOLIA` |
| 饰品栏装不上（不算符文） | Curios 的槽位靠 `curios:rune` **物品标签**认，标签在 `MaledictItemTags` 里 | 补进 `CURIOS_RUNE`，重跑 `runData` 生成 `data/curios/tags/items/rune.json` |
| codex 新页与已有条目重叠 | `SetupMalumCodexEntriesEvent` **没有任何重叠判断**：它只是让你往 `VOID_ENTRIES` 里塞一个带 x/y 的条目，屏幕照着坐标画 | 原坐标 (4,10) 正好是 Malum 的 `void.weight_of_worlds`，挪到 (6,12)，接在自己 x=6 那一列下面 |
| 装备着受伤会**一直停在受伤状态**，直到池子漏完 | 首版把「这一下会致死」的释放交回 `hurt`。池子是按 tick 放的，血量低于单次释放量时就变成每隔十几 tick 一次 `hurt`：受伤动画一直闪、无敌帧被反复续上（怪都打不动你），护甲又削掉每次那一点，血条卡在濒死线 | `deliver` 改回一律 `setHealth`，只有真把血压到 0 时才显式 `die(source)`；连带删掉 capability 上的 `releasing` 标志与三处守卫 |
| 半颗心染色**错位到格子的左半边**（形状本身是对的，是右半心） | 遮罩图里左半心与右半心都从格子的第 0 列开始盖。左半心本来就该占第 0..4 列，右半心该占第 4..8 列（中间那列两边共用） | 生成脚本里右半心加 `RIGHT_HALF_INSET = 4` 的横向偏移，重跑脚本 |
| 回血在血条上「炸开」 | 一剂金苹果会灌进一大坨治疗，而当初两个池子共用 1.4 秒半衰期，前 1.4 秒就放掉一半 | 回血单独用更慢的比例（半衰期 ≈ 3.5 秒），并且入队时先和待扣除对冲 |
| 掉血期间**红心一直闪**（不是我们发的受伤动画） | 原版 `ForgeGui#renderHealth`：`health < lastHealth && player.invulnerableTime > 0` 就把 `healthBlinkTime` 续 20 tick。我们的涓流每 tick 都在掉血，战斗中又长期有无敌帧，两个条件同时成立 → 一直续。**任何持续掉血都会这样**，跟伤害来源无关 | 用户实测排除：红心正常，闪的是**角色模型歪倒 + 全身通红** |
| 角色**歪倒 + 全身通红**很久 | 红色覆盖看 `hurtTime > 0 \|\| deathTime > 0`，而**歪倒只看 `deathTime > 0`**。真凶是「一直在挨打」：符文把伤害吞进池子（所以既没死、也没死亡界面），原版照样每次播受伤反应；打你的东西一停、池子漏完，红和紫段一起消失 —— 紫色待扣除段与红色同时结束，正是「伤害停了」的证据 | **不改代码**。已实测排除血条闪烁（红心正常），剩下的就是原版挨打反馈本身 |
| 致命一击走 `hurt` 会把玩家钉在受伤状态 | 池子是按 tick 放的，血量低于单次释放量时就成了每隔十几 tick 一次 `hurt`：受伤动画一直闪、无敌帧被反复续上（怪也打不动你），护甲又削掉每次那一点 | 见上一条：`deliver` 一律 `setHealth`，只有真把血压到 0 时才 `die(source)` |

**明确不做**：不替「别的模组取消了 `LivingDeathEvent`、把人留在 0 血」这种中间态兜底（用户决定：那是对方的状态机）。
只把坑记在这里 —— 客户端一旦在某个 tick 末看到 0 血，`deathTime` 就会变成 > 0，而
`LivingEntity` 里只有 `++deathTime`、**没有任何地方复位**，于是模型会一直歪着通红直到重生换实体。
将来真要为它兜底，唯一正确的时机是**同一个 tick 内**（在 `die()` 返回后立刻补血），
否则客户端已经看到了 0。

| 症状 | 根因 | 处理 |
| --- | --- | --- |
| **有待扣除紫心时，客户端一直处于受伤状态**（红覆盖 + 视角顿挫 + 血条闪），不是十 tick | `ServerPlayer#tick:480` 每次血量变化就发一次 `ClientboundSetHealthPacket`；我们的涓流每 tick 掉血 → 每 tick 发一次。而 `LocalPlayer#hurtTo` 把「血量同步值比当前低」**一律当成挨打**：`hurtDuration = 10; hurtTime = 10`（红 + 顿挫）并把 `invulnerableTime` 刷成 20（`ForgeGui` 的血条闪烁条件正是「血量变低且 invulnerableTime > 0」）。于是每 tick 被认成挨打一次，动画被反复续上 | 客户端 `DelayedVitalsClient#suppressDrainHurtReaction`：**认得出这一下掉血是自己池子漏的**（池子非空 + 血量比上一帧低）就把 `hurtTime` 与 `invulnerableTime` 按回 0。服务端一行没改，音效/击退/无敌帧照旧。副作用：漏账期间真挨打那一下的闪红也会被按掉（音效照旧） |

已验过的日志证据（`MelancholiaDiagnostics`，诊断用）：漏账期间出现过
`CLIENT hurt-reaction hurtTime 8->9` / `6->9` —— 不是新挨打，而是**动画被续上**（新挨打会是 `0->9`）。
那个诊断类与五处调用点在修复确认后已经全部删掉，别再从日志里找它。

## 10. 待游戏内确认

1. **红 + 顿挫**是否确实只在「有东西在持续打你」的时候出现：复现时看一眼紫色待扣除段是不是同时在涨，
   把打你的东西停下来，红应当在半秒内退掉、紫段再慢慢漏完。
2. **延迟致死的手感**：单 tick 释放量 ≥ 当前血量就当场结清，所以「血少 + 欠账多」会直接死。
   如果觉得太容易暴毙，调 `DAMAGE_RELEASE_PER_TICK` 把痛摊得更薄。
3. 我替用户写的几处文案（tooltip 一行、两个 description、英文派生）等过目。
4. codex 坐标 `(6,12)` 进游戏看挤不挤。

## 11. 已否掉的方案（留档，别再走一遍）

| 方案 | 为什么否掉 |
| --- | --- |
| 致命一击交回原版 `hurt()` | 池子按 tick 释放，会变成每隔十几 tick 给自己一次 `hurt`：受伤动画不断、无敌帧被反复续上、护甲二次减伤。用户实测确认过 |
| 在 `LivingAttackEvent` 就把挨打取消掉以抹掉受伤反馈 | 那样护甲/附魔/吸收/无敌帧/击退全部失效，只能拿到**减伤前**的数值入池，等于推翻「入队的是最终伤害」这条根基 |
| 把血条闪烁当成 bug 去修 | 实测血条红心正常；而且闪烁是原版 `healthBlinkTime`，任何持续掉血都会触发，没有模组接口可关 |
| 替别的模组的死亡中间态兜底 | 用户明确否掉：那是对方的状态机。坑已记在第 9 节 |


虚空页的占用情况（反汇编 `VoidProgressionScreen` 数出来的）：Malum 自己 25 条 ——
`weight_of_worlds`(4,10)、`edge_of_deliverance`(5,10)、`malignant_pewter`(3,9)、
`erosion_scepter`(3,11)、`malignant_stronghold_armor`(4,12)、`runes`(0,11) 等；
Maledict 5 条 —— `obelisks`(-1,8)、`runeworking`(6,10)、`incursus_blade`(6,11)、
`elegy_bow`(2,13)、`vicissitude_rite`(8,13)。**往这一页加条目之前先把这张表列出来，别猜坐标。**

顺带发现（还没决定）：Malum 把幽影精魂和它自己那批虚空符文都藏在
`malum:hidden_items/black_crystal` 后面（`ItemTagRegistry.HIDDEN_UNTIL_BLACK_CRYSTAL`），
而 Maledict 的两枚白镴符文与这枚抑郁符文都没有进任何隐藏标签 —— 也就是说抑郁符文会先于它的
材料在 JEI / 创造栏里露脸。要不要跟着藏，等用户定。

