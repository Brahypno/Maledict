# 21 — 适应效果、幽影精魂与压血节奏

2026-09-21 用户需求（第十六轮），三条：

1. 无常的精魂掉落物再加一枚幽影精魂。
2. 无常增加一个**适应效果**：记录 damage message，已记录的 message 再次命中时伤害指数递减；
   默认「适应二」，也就是能记两种 message，可通过 config 调整。
3. 一阶段现在有一条「释放改血弹之后一段时间不攻击」，取消它；并把一阶段弹的攻击伤害改成 0.5。

第 3 条经两轮确认后只落一半，见第三节的结论；第 2 条的递减倍率用户明确「不用 config」，
改用自然指数；适应本身也只按次数记账、不设时限，配置项只有「适应几」一个，见第二节。

## 一、幽影精魂 ×1（需求 1）

`src/main/resources/data/maledict/spirit_data/entity/first_vicissitude.json`（手写资源，
不是 runData 产物）在原有八种之外追加一条：

| 精魂 | 数量 |
| --- | --- |
| sacred / wicked / arcane / eldritch / aerial / aqueous / earthen / infernal | 各 6（不变） |
| **umbral（幽影）** | **1** |

- 数量按用户答复取字面的「加一个」：不跟着八种凑 6，表里因此是「八种各 6 + 幽影 1」。
- 总数 48 → **49** 点灵魂强度。提尔锋的额外伤害是 `totalSpirits × 2`（Malum
  `TyrvingItem#hurtEvent`），所以从 96 变成 **98**；灵魂暴露后击杀放出的精魂里也会多这一枚。
- 主类型仍是 `eldritch`，`registry_name` 不变。
- 测试 `FirstVicissitudeSpiritDataTest` 从「八种各 6、不许有幽影」改为「九种、
  八种各 6 且幽影 1、合计 49」；这正是它存在的意义——手写资源写错不会报错，只会在游戏里
  静静地少掉奖励。

## 二、适应效果（需求 2）

### 记的是什么

「damage message」就是死亡消息用的那个 id，也就是 `DamageSource#getMsgId()`：

| 打过来的方式 | message |
| --- | --- |
| 玩家近战（剑、斧、空手，含跳劈） | `player` |
| 玩家射出的箭 | `arrow` |
| 本模组的镰刀横扫 / 神侵恶刃 | `scythe_sweep` |
| 别的生物 | `mob` |
| 爆炸、火焰、药水…… | `explosion` / `on_fire` / `magic` 等 |

同一种 message 只占一格：**拿剑砍和拿斧砍是同一格**，这是「message」而不是「物品」的必然结果，
也和死亡消息的粒度一致。

### 递减规则

已记账的 message 第 n 次命中吃 **e⁻⁽ⁿ⁻¹⁾**：第一次见到它照常全额（那一下就是「记下来」本身），
第二次 ×e⁻¹ ≈ 0.368，第三次 ×e⁻² ≈ 0.135，第四次 ≈ 0.050……

- 用户答复「自然对数会不会最简单？这个不用 config」，所以这里**没有**衰减倍率配置项：
  底数就是自然常数，指数是已经挨过的次数。
- 换算成 `float` 后指数一大就自然落到 0，那一击也就没有伤害可言；挡住它的下一道是本来就有的
  **每击上限**（最大生命的 20% / 5% / 1%，按难度，见 `getVitalityDamageLimit`），两条互不冲突。

### 适应几、记满了怎么办

- 这个 Boss 是「适应几」由 config `firstVicissitude.adaptationLevel` 决定：**默认 2**（即「适应二」），
  这个数字同时就是它能记住几种 damage message；范围 0–16，**0 等于整个效果关闭**。
  它就是这个 Boss 自己的配置项，本轮**只加这一个**。
- 记满之后再遇到没见过的 message，顶掉**最早记下的那一条**（FIFO，不按最近一次）。
  于是轮着用三种以上伤害类型打，每一下都是新消息、也就一直是全额——两格适应只压得住两种；
  一直用同一种打才会一路递减。
- 读不出 message（null / 空串）时既不减伤也不占格：不能因为读不到就把伤害悄悄漏掉。

### 没有时限（与无敌帧无关）

适应**只按次数记账，与时间无关**：整场遭遇战里一直有效，唯一的清账时机是这场打完
（回到未参战）。所以本轮没有、也不需要第二个 config。

用户提到的「时间」是无常**自己那 20 tick**，也就是下面这层，与适应无关；它当前已经是
「窗口没走完时挨打就重新计算」的，本轮一个字没改：

- 每次**有效**命中都会把 `VicissitudeVitality` 的 `nextHit` 重算为
  `now + getVitalityHitInterval()`（无常没有覆写这个方法，即 20 tick），写在
  `VicissitudeBossEntity#hurt` 的 `accepted` 分支里（`afterAcceptedHit`），
  `setHealth` 那次事务里的 `afterDamage` 也会写同一个值；
- 这 20 tick 内落下的后续伤害乘 `1 - 剩余 / 20`（`getInvulnerabilityDamageScale`）：
  刚打完几乎为 0，窗口走完回到 1.0；**这些窗口内的命中同样会把窗口重新推到 `now + 20`**，
  所以连续挨打时窗口一直不会走完；
- 唯一不刷新的是「同一 tick 内的重复命中」：那时剩余正好是 20，系数算出来是 0，
  `adjustedAmount <= 0` 直接让这一记 `hurt` 返回 false——不掉血、也不延长窗口，
  与原版无敌帧里被忽略的那些命中一样；
- 这是**另一层**，适应只叠在它之上：适应不读它、也不改它，两者互不影响。

### 挂在哪里

- `modifyIncomingDamage` 的**最后一档**：部位倍率 → 非玩家 ×0.5 → 适应。
  三者互相独立，谁先谁后不影响乘积；一阶段免疫的伤害在 `hurt` 里就被挡掉，走不到这里。
- 实际生效区间是**二阶段**：`isDamageImmune` 本来就挡掉一阶段、转场与死亡过程，
  也挡掉 `source.getEntity() == null` 的无来源伤害（环境爆炸、掉出世界之类）。
  也就是说，无常只在它真的会掉血的那些打击上记账——环境伤害既不扣血，也不进适应账。
- 账目是新类 `common/entity/DamageAdaptation`（纯记账，不碰世界），
  `DamageAdaptationTest` 直接钉住上面全部规则。
- 存档：写进实体 NBT 的 `DamageAdaptation` 列表（`Message` + `Hits`，顺序即记下的先后）。
  区块卸载再回来、存档重载都不该把适应洗白；读回时次数非正或消息为空的坏行一律丢掉。
- 清账只有一条路：**一阶段名单空满 100 tick 回到未参战**（`tickEmptyEncounter`）时
  与名单、死亡记录一起清空——适应是「这一场」的账，没有超时、没有冷却。

## 三、取消压血喘息（需求 3）

### 取消的

`PRESS_RECOVERY_TICKS`（30 tick）整套删掉：

- `FirstVicissitudeBossEntity#onPressLanded` 与配套的 `dealsPlayerDamage` 不再存在；
- 弹体侧的两处回调（`VicissitudeSpiritBoltEntity#onImpact`、
  `VicissitudeLightOrbEntity#collect`）一并去掉；
- `tickPhaseOneCombat` 不再因为「刚压过血」跳过这一槽，胸/环也不会被作废重起。

于是压血弹落地之后，下一轮释放只按原本的基础攻击槽与前摇走，不再额外隔 30 tick。

### 没动的

- **压血行为不变**：扇射弹、齐射弹、追踪球命中仍然只把目标压到 1 血。
- **一阶段弹的伤害仍是 0**：用户第一版需求写的是「把一阶段弹的攻击伤害改成 0.5」，
  追问后确认——一阶段所有弹都只压血、本身不造成伤害（二阶段的同款扇射/齐射弹才是
  `attackDamage × 0.75` 的普通伤害），用户答复「如果你确定一阶段只压血无伤害，那就这样」。
  因此这一条**不改伤害数值、不改弹体类型、不改伤害类型**，只落实「取消喘息」。
  若之后要的是「压 1 血之外再补 0.5 伤害」，只需要在
  `VicissitudeSpiritBoltEntity#onImpact` 的压血分支里补一次 `hurt`，伤害类型沿用该分支
  已有的 `damageSources().mobProjectile(this, boss)`。
- **一阶段仍恒用 light 伤害档**（`hurtParticipant`）：喘息窗口没了之后，胸/环那两下
  更依赖护甲、附魔与伤害上限把它们挡住，这正是 18 选 light 档的理由，本轮不动。

## 四、实现位置

- `src/main/resources/data/maledict/spirit_data/entity/first_vicissitude.json`：幽影 ×1。
- `common/entity/DamageAdaptation`：记账与换算（`adapt` / `clear` / `snapshot` / `restore`）。
- `common/entity/FirstVicissitudeBossEntity`：`damageAdaptation` 字段、`modifyIncomingDamage`
  最后一档、`adaptationLevel()`、`tickEmptyEncounter` 清账、`saveAdaptation` / `loadAdaptation`；
  同时删除 `PRESS_RECOVERY_TICKS`、`pressRecoveryTicks`、`onPressLanded`、`dealsPlayerDamage`。
- `common/entity/VicissitudeSpiritBoltEntity`、`common/entity/VicissitudeLightOrbEntity`：去掉压血回调。
- `config/MaledictConfig`：`firstVicissitude.adaptationLevel`（默认 2，0–16）。
- 测试：`common/entity/DamageAdaptationTest`（新增）、`data/FirstVicissitudeSpiritDataTest`（改）。

## 五、验证

| 命令 | 结果 |
| --- | --- |
| `.\gradlew compileJava test --offline` | BUILD SUCCESSFUL；54 项测试全绿（含新增 `DamageAdaptationTest` 10 项、`FirstVicissitudeSpiritDataTest` 改后的 5 项） |
| `.\gradlew runData --offline` | 不需要：本轮不动战利品表，精魂表是手写资源 |

未在游戏内验证：提尔锋对无常的额外伤害是否变成 98、灵魂暴露击杀是否多放一枚幽影；
适应在真实对局里的手感（同一种打几下开始不痛、配置改成 0/3 的效果）；取消喘息后
一阶段的压迫感与存活率。这几项都需要真实客户端。
