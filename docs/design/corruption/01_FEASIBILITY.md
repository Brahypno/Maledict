# 01 — 腐败符文可行性评审

对象：`corruption_rune_mechanic_design.md`（用户提供，Downloads）。
环境：Minecraft 1.20.1 / Forge 47.4.23 / Curios / Malum（本仓库 Maledict）。

状态：**结论已给；读法已由 Human 逐条裁定（完整裁定见 §3）；第一版（原文 1–8 步）已落地，见 §7。**
剩下的全是数值层：单次收的封顶、一次 gain 折算多少腐败（D→A 比例已暂定 50%，转化顺序见 §3.1 待点头）。
已知后果一条：对「自己接管血量」的实体（本仓库的无常 BOSS）收是空操作 —— 已决定不特判。

本文只回答「能不能做、坑在哪」，不落数值、不写实现。

---

## 0. 结论

**能做。** 原文第 10 节的 MVP（1–8 步）全部落在 Forge 事件 + 实体持久数据上，**不需要 Mixin**；
原文第 16 节的 12 步里，只有「吸收」（第 10 步）和「自然回血单独记账」需要 Mixin 或改设计。

本仓库已经有同型实现可以直接照抄结构，不是从零摸索：

| 需要的机制              | 仓库里已有的先例                                                                                |
|--------------------|-----------------------------------------------------------------------------------------|
| 回血拦截               | `DelayedVitalsEvents#onLivingHeal`（抑郁符文就在 `LivingHealEvent` 上入队）                        |
| 直写血量的「非伤害掉血」+ 显式死亡 | `DelayedVitalsEvents#deliver`（不致命的 `setHealth`，只在压到 0 时自己 `die`）                        |
| 挂在任意实体上的数值状态       | `DelayedVitals`（capability + `AttachCapabilitiesEvent`）、`VicissitudeVitalityCapability` |
| 攻击方佩戴判定            | `BlissRuneEvents#multiplierOf`（`CuriosApi` 查攻守两侧）                                       |
| 「一次挥砍只算一段」的去重      | `IncursusBladeAttack` 的 `LAST_ATTACK_TICK`（`getPersistentData` 记 gameTime）              |
| 血条上的第二套血量显示        | `DelayedVitalsOverlay` + `DelayedVitalsHearts` + `DelayedVitalsPacket`                  |
| 自定义效果 + 符文物品注册     | `MaledictMobEffects`、`MaledictItems`（已有 8 枚符文）                                          |
| Mixin 通路（只有必要时才用）  | `maledict.mixins.json` 已有 `FoodDataMixin`                                               |

有 **3 条原先必须先钉死**，现已逐条定下（完整裁定见 §3）：

1. **「收不可致死」+「D + A ≤ 当前血量」两条合起来等于处决，不等于不致死。**
   血量掉到 10，而 D+A 恰好也被夹到 10，此时收一下 = 把目标打到最低可存活线，
   紧接着这一记普通攻击落地 —— 目标必死。这不是「腐败自己杀人」的字面违反，
   但和 Invariant E 的意图（「致命一击应该来自真正的攻击」）能读成两回事。
   要么给单次收的强度封顶，要么给 D+A 占最大血量的比例封顶。**这是设计决定，不是技术限制。**
   Human：不是问题
   → **已定：D+A 允许等于当前血量（悬崖本身不是问题）；另给单次收的强度封顶**（见 §3 第 1 条）。

2. **「伤害不清腐败，只夹到血量以内」的夹取顺序没定：D 先夹还是 A 先夹？**
   两者都满足原文约束 `D + A ≤ 当前血量`，但收益完全相反：A 先夹 = 目标越挨打，攻击方的收成越薄；
   D 先夹 = 收成被保护到最后。原文没写，必须先钉。


3. **「一次挥砍算几段」没定。** 神侵恶刃一次挥砍会发多次受伤事件（本体 `scythe_sweep` + 奥术通道
   在同一个 `LivingHurtEvent` 里再打一记 `minecraft:magic` + `DamageProbe` 的补打）。
   按最直白的写法，一次挥砍会**收两遍**、标记也会刷两遍。仓库已为同类问题写过工作约定
   （见 `AGENTS.md` 的 `adaptInBatch`），这里要照办。
   Human：按伤害次数算，按挥砍算不可能
   → **已定：按伤害次数算，不做挥砍级去重。** 后果要认下来：奥术通道是嵌在同一记里的第二段
   `LivingHurtEvent`（`minecraft:magic`，无直接实体、攻击者为造成实体），会再收一次 ——
   即**一次神侵恶刃挥砍最多收两次**。总收成仍被 A 兜住（收一次就扣一次 A），不会无限叠。

兼容上真正会咬人的三点（详见第 4 节）：抑郁符文的延迟池用 `setHealth` 结算、朽骨符文复活也是
`setHealth`（事件都看不见）；**无常 BOSS 的血量不是血量**（`getHealth/setHealth/heal` 全被账本接管，
直写血量会被静默忽略）。

---

## 1. 钩子盘点（原文 §11 的答案底座）

### 1.1 回血 —— 有事件，但语义比原文假设的窄

`net.minecraftforge.event.entity.living.LivingHealEvent`，由 `ForgeEventFactory#onLivingHeal`
发在 `LivingEntity#heal(float)` 的**第一行**（Forge 47.4.23 的
`patches/net/minecraft/world/entity/LivingEntity.java.patch`）：

```java
public void heal(float amount) {
    amount = ForgeEventFactory.onLivingHeal(this, amount);  // ← 事件在这里
    if (amount <= 0)
        return;                                // 取消 = 返回 0 = 血不动
    float f = this.getHealth();
    if (f > 0.0F)
        this.setHealth(f + amount);               // setHealth 夹到 [0, maxHealth]
}
```

结论（对原文 §11.1 / §15.1 的正面回答）：

- **有稳定钩子，不需要 Mixin。**
- **事件早于一切判定**：满血、已死、`amount ≤ 0` 时事件照发，但血不会动。
  所以「实际回血量」必须自己算：

  ```text
  effective = (health > 0) ? min(amount, maxHealth - health) : 0
  ```

  直接拿 `event.getAmount()` 记账是错的 —— 满血连喝三瓶药水会凭空长出一堆腐败。
- **取消即无回血**（`onLivingHeal` 取消时返回 0，`heal` 随即 return），所以被取消的那一记不算 gain。
- **覆盖到的路径**（全部实测自 1.20.1 反编译源）：
  瞬间治疗（`MobEffect#applyInstantenousEffect` → `heal`，药水/喷溅/滞留/区域云都走它）、
  生命恢复（`MobEffect#applyEffectTick` → `heal(1)`）、
  饥饿自然回血（`FoodData#tick` → `heal(1)` 或 `heal(saturation/6)`）、
  其它模组只要老老实实调 `heal()`。
  **注意瞬间治疗对亡灵是 `hurt`，不经过 `heal`，天然不会被记成 gain —— 这是对的。**
- **漏掉的路径**：任何直接 `setHealth` / 直接改同步血量的写法。仓库自己就有两处（见第 4 节）。
- **分不出来源**：事件只给实体和数量。本仓库 `FoodDataMixin` 的注释已经把这条写死了：
  「没有任何事件能区分回血来源（生命恢复与饥饿回血都是一次 `heal(1)`）」。
  原文 §8「自然回血不产生新腐败」因此**不是免费的**：要么接受自然回血也算 gain，
  要么复用 `FoodDataMixin` 的卡点自己打标（那就是 Mixin 方案）。
  Human：语义放宽，不必区分
  → **已定：不区分来源，任何走 `heal()` 的有效回血都算 gain**（自然回血、生命恢复、药水一视同仁）。

### 1.2 掉血 —— 两个点，各自的位置很关键

| 事件                  | 发出位置                       | 此时 `amount` 是什么 | 取消的后果                           |
|---------------------|----------------------------|-----------------|---------------------------------|
| `LivingHurtEvent`   | `actuallyHurt` 开头，**护甲之前** | 护甲前的量           | 返回 0 → 整个 `actuallyHurt` return |
| `LivingDamageEvent` | 护甲、附魔、**吸收都扣完之后、写血之前**     | 接下来真要从血量里扣掉的数   | 返回 0 → 不写血量                     |

- 两者都在 `hurt()` 的免疫/无敌帧闸门**之内**，所以「打不中的那一下不会结算」是免费的。
- `LivingDamageEvent` 是**夹取唯一精确的落点**：此刻血量还没被写，
  投影血量 = `max(0, getHealth() - event.getAmount())`，一分不差。
- ~~**注解式 `@SubscribeEvent` 能收到已取消的事件**~~ ← **这条原先写错了，已更正，见本节末尾。**
- `LivingAttackEvent` 对**玩家受害者不发**（`ForgeHooks#onLivingAttack` 对 `Player` 直接放行），
  符文标记别挂在那一个事件上。
- Human：我印象里@SubscribeEvent不是默认不接受canceled吗？那BlissRuneEvents可能有问题；就damage计算咯

#### 更正：注解式 `@SubscribeEvent` **默认收不到**已取消的事件

核对 EventBus 6.2.33 源码后确认（用户是对的）：

```java
// net/minecraftforge/eventbus/ASMEventHandler.java
public void invoke(Event event) {
    if (!event.isCanceled() || subInfo.receiveCanceled()) {   // ← 注解路径在这里过滤
        if (filter == null || filter == ((IGenericEvent) event).getGenericType())
            handler.invoke(event);
    }
}
```

`SubscribeEvent.receiveCanceled()` 默认 `false`，所以**被取消的事件根本不会进注解处理器**。
（编程式 `addListener` 走的是另一条路：`EventBus` 里那张 `checkCancelled` 过滤表，
只有 `addListener(..., receiveCancelled=false, ...)` 才套用 —— 结论一样，位置不同。
原先只查到这一条就下了结论，漏了 `ASMEventHandler`。）

对本符文的两条推论：

1. 被取消的命中「还算不算」要主动决定：默认是**不算**（事件不进来，什么都不会发生）。
   Human：不管他 → **已定：不做特殊处理，被取消就不结算**（不写 `receiveCanceled`）。
2. 「收放哪个事件」按 Human 的意见落到 `LivingDamageEvent`：
   Human：就 damage 计算咯 → **已定：收与夹都挂 `LivingDamageEvent`**（见 2 节 11.2 的修订）。

#### 顺带发现（另一个特性，不在本次范围）

`BlissRuneEvents` 的三个处理器都是 `@SubscribeEvent(priority = EventPriority.LOWEST)`、
**没有写 `receiveCanceled = true`**，而它们的核心分支恰好是「接到已经被取消的那一个就补打」。
按上面的机制，别的模组在 NORMAL/HIGH 取消时，这三个处理器**根本不会被调用**，补打分支不可达。
`docs/design/bliss/01_SPEC.md` 第 9 节「进游戏要看的」第 1 条正是要验证这个（尚未验证）。
修法是给这三个注解补上 `receiveCanceled = true`（代码里的两个分支本来就是按「能收到取消」写的）。
**已确认（Human：这是 bug，不补就永远不会触发），本次一并修掉**，改动只在这三个注解上。

### 1.3 吸收 —— 没有钩子（原文 §3.2 的怀疑是对的）

- 吸收量是 `SynchedEntityData` 上的一个字段，不是属性、不是效果。
- 来源：`AbsorptionMobEffect#addAttributeModifiers` 直接 `setAbsorptionAmount(+4×(amp+1))`，
  `removeAttributeModifiers` 直接减回去；消耗发生在 `actuallyHurt` 里。
- `MobEffectEvent.Added` 存在，但**发在效果真正生效之前**
  （patch 插在 `activeEffects.get(...)` 之后、`put(...)` / `addAttributeModifiers` 之前），
  而且效果**更新**（`update`）与指令路径（`forceAddEffect`）都不发。
- 想精确记账只有两条路：Mixin `setAbsorptionAmount`，或者退一步只在 `MobEffectEvent.Added`
  上按 `4×(amp+1)` 记账（并接受指令/其它模组写吸收看不见）。
- **建议照原文 §10 的意见：MVP 不做，留到第 10 步再单独评估。**
- Human：那可以不着急做这个
  → **已定：不进 MVP，后置。**

### 1.4 正面效果 / 属性修饰符

- 效果：`MobEffectEvent.Added / Remove / Expired / Applicable` 都有；
  `Added` 在重复施加（`getOldEffectInstance() != null`）时也会发，所以「刷新时长」会被误记成新 gain，
  要按旧实例判重。指令给效果走 `forceAddEffect`，**不发 `Added`**。
- 属性修饰符：没有可辨识的事件，只能在摸到实体时采样。原文 §3.4「先不做」是对的。

---

## 2. 对原文 §11 与 §15 的逐条回答

**11.1 / §15.1 能否不用 Mixin 干净地拦截回血？**
能，`LivingHealEvent`。但它是「尝试回血」事件，不是「回血完成」事件：满血、已死、被取消都会发，
要靠自己算有效回血量（见 1.1）。

**§15.2 能否可靠拿到「实际恢复的血量」？**
能，在同一事件里算 `health > 0 ? min(amount, maxHealth - health) : 0`。
不能可靠拿到的是**来源**（生命恢复 / 饥饿回血 / 药水在事件里长得一模一样）。
另外 `setHealth` 直写的回血（别的模组、指令、本仓库的延迟池）拿不到，只能挂账面上不管。

**11.2 / §15.4 攻击、普通伤害、收，什么顺序最稳？**
推荐 **B：先收，再让原版伤害落地**；落点按 Human 的意见取 **`LivingDamageEvent`**。

- `LivingDamageEvent` 发在护甲、附魔、吸收都算完之后、**写血之前**，所以「先收后打」照样成立：
  我们改完血量，原版紧接着 `setHealth(getHealth() - f1)`，读到的是收完之后的血量。
- 和挂在 `LivingHurtEvent`（护甲之前）的区别只有两点，都不影响结算结果：
  一是「谁取消能挡掉收」——在 `LivingAttackEvent` / `LivingHurtEvent` 被取消的命中根本走不到
  `LivingDamageEvent`，默认就不收（Human：不管他）；二是收发生在吸收**扣完之后**，
  被吸收吃掉的那一记照样会走到这里（事件无条件发，`f1` 可以是 0）。
- 不取 `LivingHurtEvent` 的另一个理由是对的：它比 `LivingDamageEvent` 更早、更容易被别的模组截断，
  而收成与这一记的伤害量无关（只看 A），没有理由抢在护甲之前。
- 反过来走 A（等普通伤害落地再收）只能挂在 tick 上，而且那一记可能已经把人打死，收成无处安放。
- 与原文 §6.1 的「不可致死」配合时 B 更自然：收把目标压到最低线，真正的死因仍是这一记普通攻击。

**11.3 / §15.3 收用直写血量，还是另发一个 DamageSource？**
**直写血量更安全**，而且仓库已经踩过这条路：

- 另发 `DamageSource` 会走 `hurt()`：被无敌帧挡、被吸收吃掉、被别的模组减伤/取消、会触发图腾、
  会重复触发本符文自己的 `LivingHurtEvent`（递归）。这些都是负面的。
- 直写 `setHealth` 的代价仓库已经写在 `DelayedVitalsEvents#deliver` 的注释里：
  不吃图腾、不续无敌帧、没有受伤反馈 —— 那一段就是同一个取舍的既有结论。
- 血量本身是 `SynchedEntityData` 字段，直写**不会**造成客户端不同步；
  玩家客户端只是看不到红屏与受伤音效（那是 `hurt` 的演出）。后续要给 HUD 就照
  `DelayedVitalsPacket` 的模式补包。
- 「不可致死」用 `newHealth = max(minSurvivable, health - collapse)` 实现是安全的；
  真正的死亡仍旧走原版路径（本记普通攻击），死亡事件、掉落、成就都不受影响。

**11.4 / §15.6 夹取放哪，怎么不每 tick 全表扫？**

- 掉血：`LivingDamageEvent` 里用投影血量夹（精确，且只在真挨打时跑）。
- 回血：`LivingHealEvent` 里不需要夹（血量只增）。
- 死亡/重生：`LivingDeathEvent` 清账。
- 剩下的漏网（`setHealth` 直写、`/kill`、生命上限变化）**不需要**每 tick 扫：
  收的时候自己会夹一次（`collapse = min(A, health - minSurvivable)`），
  所以账面漂移最多让收成变小，不会造成不安全的结果。想要账面干净，
  就维护一份「有腐败的实体名册」，只对名册里的实体低频对表（抑郁符文的池子就是按这个思路做的）。
- 成本上真正的建议是：**平时不要为每个生物付出任何 tick 开销**，只在事件里动。

**11.5 / §15.5 状态存哪？**

| 方案                                      | 持久化                  | 克隆/换维度                   | 客户端  | 评价                                                                                             |
|-----------------------------------------|----------------------|--------------------------|------|------------------------------------------------------------------------------------------------|
| `entity.getPersistentData()`（ForgeData） | 自动随实体 NBT 存          | 随实体走；不随 `restoreFrom` 复制 | 不同步  | 最省：只有被碰过的实体才创建 CompoundTag；仓库已有 4 处这么用                                                         |
| Forge Capability                        | 随实体 NBT（`ForgeCaps`） | 同上                       | 可另发包 | 仓库数值状态的既有写法（`DelayedVitals`、`VicissitudeVitalityCapability`）；挂到所有 `LivingEntity` 会有每次实体创建的固定开销 |
| `SynchedEntityData`                     | 不持久                  | ——                       | 自同步  | 需要 Mixin 加字段；MVP 不需要                                                                           |

推荐：**MVP 用 capability（与仓库一致、便于以后加同步），字段先只有 `fallen` + `D` + `A` + 时间戳**。
若嫌「给每个生物挂一个对象」重，可以先用 `getPersistentData()`，两者以后可换。

**§12 死亡 / 重生 / 换维度 / 克隆**

- 玩家死亡重生：Forge 不会替你复制 capability（重生走 `restoreFrom`，只搬原版字段），
  「死亡即清空」正好是默认行为 —— 与原文推荐一致，但要在代码里写明这是**有意**的。
- 非玩家实体：`Entity#restoreFrom` 也不搬 ForgeData/capability，所以**实体转化**
  （村民→僵尸村民、猪→僵尸猪灵之类）会丢腐败。默认丢是合理的，要保留就接 `LivingConversionEvent`。
- 换维度：玩家实体不重建（无影响）；其它实体随存档 NBT 走，不会重复也不会重置。

**11.6 / §15.8 `Fallen` 用 MobEffect、腐败用附加数据，干净吗？**

干净，而且是最省的做法：

- `Fallen` 用 MobEffect：图标/时长/`hasEffect` 查询/同步全部免费；`MobEffectEvent.Remove`
  与 `Expired` 顺便给出「窗口被牛奶提前关掉」与「自然到期」两个点。
- 腐败数值不能塞进 MobEffect（要连续量、要跨窗口存活），必须另外挂 —— 原文的拆分是对的。
- 注意：`MobEffect` 的构造函数是 `protected`，跨包注册要借匿名子类（`MaledictMobEffects` 里有先例）。

**§15.7 主要兼容风险**

| 情况      | 结论                                                        |
|---------|-----------------------------------------------------------|
| 其它模组的回血 | 走 `heal()` 的自动生效；直写 `setHealth` 的看不见（原文说可以不管，同意）          |
| 吸收      | 没有钩子，见 1.3；MVP 不做                                         |
| 伤害被取消   | 默认收不到（注解式 `receiveCanceled=false`）：在 `LivingAttackEvent` / `LivingHurtEvent` 被取消的命中走不到 `LivingDamageEvent`，也就不结算 —— 即 Human 的「不管他」 |
| 无敌帧     | `LivingHurtEvent` 在无敌帧闸门之内，连击被挡就等于没收成 —— 这正好符合「要回来打一下才能收」 |
| 防死机制    | 收不致死（夹到最低线），所以不会和防死机制抢；真正致死的仍是普通攻击，图腾照常生效                 |

**§15.9 哪些必须 Mixin？**
MVP 里**一处都不需要**。只有两处值得以后为了精度上 Mixin：吸收的精确记账
（`setAbsorptionAmount`）、自然回血单独分类（`FoodData#tick` 的头尾打标，仓库已有先例）。

**§15.10 有没有让血量模型根本不安全的边界？**
没有。两处原本会变成「另一个机制」的地方已由裁定收口（处决悬崖用单次收封顶挡掉、夹取先夹 D），
剩下的都是数值层的事。

---

## 3. 读法裁定表（Human 已逐条选定）

按仓库的工作约定（`AGENTS.md` 最后一节），一条一条确认完再动代码。

| # | 问题 | 裁定 | 备注 |
| --- | --- | --- | --- |
| 1 | 收的致死性、要不要封顶 | **可以封顶**（Human：可以）；D+A 允许等于当前血量，处决悬崖本身**不是问题**（Human：不是问题） | 单次收的封顶数值待定，属于数值层 |
| 2 | 血量下降时先夹 D 还是先夹 A | **先夹 D**（Human：好） | 让「攒起来的债」落在攻击方的收益上 |
| 3 | 一次挥砍算几段 | **按伤害次数算**，不做挥砍级去重（Human：按伤害次数算，按挥砍算不可能） | 一次神侵恶刃挥砍最多收两次（本体 + 奥术通道），总量被 A 兜住 |
| 4 | 谁能收 | **任意佩戴者**，且 **`Fallen` 过期后仍然能收**（Human：任意 + 过期后能收） | 原文明说「过期不删除已有腐败」，收只看 A 在不在；攻击方因此有回头补刀的窗口 |
| 5 | 被取消的命中 | **不管他**：不写 `receiveCanceled`，被取消的命中不结算 | 见 §1.2 的更正 |
| 6 | 哪些 gain 算 | **不区分来源**：走 `heal()` 的有效回血一律算 | 自然回血/生命恢复/药水一视同仁；满血溢出部分仍要扣掉 |
| 7 | Dormant → Active 的转化规则 | **每次新 gain 把「旧 D」的 50% 转入 A**（比例暂定 50%） | 具体序列见 §3.1 —— **转化顺序要你点头**（新 gain 不参与本次转化） |
| 8 | 状态存哪 | 未表态；**建议按仓库惯例用 capability**（字段只有 `fallen` / `D` / `A` / 时间戳） | 若嫌「给每个生物挂对象」重，可先用 `getPersistentData()`，两者以后可换 |
| 9 | 收在无常 BOSS / 自定义血量实体上怎么办 | **统一处理，不特判**：`Fallen` 照下，收照走 `getHealth` / `setHealth`（Human：下 `Fallen` 是统一的，有没有效果是另一回事；不能假定所有 boss 都是自定义血量） | 本仓库 BOSS 因此会「攒得住、收不动」（它的账本忽略外部 `setHealth`）—— 这是实体自己的事，不是 bug；别的模组若用原版血量则一切正常 |
| 10 | 一次 gain 折算多少腐败 | **仍未定**（数值层） | 原文「a corresponding amount」；第一版建议 1:1 起手再调 |

### 3.1 D→A 转化的具体序列（**顺序待你点头**）

一次「有效增益」的定义：走 `heal()` 且**有效回血量 > 0**（满血溢出不算 —— 否则空喝药水也能推进腐败）。

```text
onGain(g):                       // g = 本次有效增益折算出的腐败量
    A += 0.5 × D                 // 只动「旧的 D」：本次新来的那一份不参与本次转化
    D  = 0.5 × D + g             // 转化剩下的留在 D，再加上本次新的
    clamp: D + A ≤ 当前血量        // 见 §2 的 11.4
```

为什么本次新 gain 不参与本次转化：原文 §2.2 与 §5 写的是「First gain → create Dormant Corruption」，
Invariant B 又要求「第一次增益通常仍然有用」。若把新 gain 也算进去，第一次治疗就当场把
自己的一半转成 A，等于第一次就受罚。

追踪（每次有效增益折算 g = 4，血量充足）：

```text
第 1 次:  A += 0.5×0 = 0     D = 0.5×0 + 4 = 4     → (D=4,   A=0)    第一次全休眠
第 2 次:  A += 0.5×4 = 2     D = 0.5×4 + 4 = 6     → (D=6,   A=2)
第 3 次:  A += 0.5×6 = 3     D = 0.5×6 + 4 = 7     → (D=7,   A=5)
第 4 次:  A += 0.5×7 = 3.5   D = 0.5×7 + 4 = 7.5   → (D=7.5, A=8.5)
...
稳态:     D → 2g = 8（饱和），A 每次 +≈g，无自身上限，只被「D+A ≤ 当前血量」夹住
```

结论：D 饱和、A 随「反复求增益」线性累积 —— 正是 Invariant C（反复依赖增益 → 危险上升）。
**待确认的只有一件事：`A += 0.5 × D` 用的是转化前的旧 D（上表如此），不是加完新 gain 之后的 D。**

---

## 4. 与本仓库既有机制的相互作用（点名）

| 机制                                  | 冲突点                                                                                                                                                            | 处理                                                                                        |
|-------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------|
| 抑郁符文 `DelayedVitalsEvents`          | 治疗入队时被 `setAmount(0)`，**释放时用 `setHealth`** → 腐败看不到这笔回血；延迟伤害同样用 `setHealth` → 掉血夹取看不到                                                                           | 要么在 `DelayedVitalsEvents` 里显式回调腐败记账，要么接受「戴抑郁符文时腐败长得慢」并写进文档                                |
| 朽骨符文 `RottenBoneEvents`             | 免死时 `setHealth(HEAL)` 复活、并给生命上限加永久减益                                                                                                                           | 同样的事件盲区；生命上限下降不影响模型（约束跟的是当前血量）                                                            |
| **无常 BOSS `VicissitudeBossEntity`** | `getHealth/setHealth/heal` 全被 `VicissitudeVitality` 账本接管：`setHealth` 只有在 `vitality.writing > 0`（即处于伤害事务内）才记账，**平时直写等于没写**；`heal` 是重写的，但它自己会发 `LivingHealEvent` | **已定：统一处理，不特判实体类型。** `Fallen` 照下（别的模组用原版血量的 boss 因此能正常工作），收也照走 `getHealth`/`setHealth`。对本仓库 BOSS 的结果就是「攒得住、收不动」—— 那是它自己接管了血量，不是本符文的 bug，文档写明即可 |
| 神侵恶刃 `IncursusBladeAttack` / 奥术通道   | 一次挥砍多发受伤事件（本体 + 奥术 + probe 补打），会重复标记/重复收                                                                                                                       | 按第 3 节第 3 条在同一个 tick 去重；`IncursusBladeAttack` 的 `LAST_ATTACK_TICK` 是现成写法                  |
| 延迟血条 HUD                            | 第二套血量的显示已经解决过（`DelayedVitalsOverlay`）                                                                                                                          | 以后要把 D/A 画在血条上时直接照抄                                                                       |

---

## 5. MVP 落点（对应原文第 16 节）
| 原文步骤               | 落点                                                                                             | 结论               |
|--------------------|------------------------------------------------------------------------------------------------|------------------|
| 1 `Fallen` 标记      | 按攻击方 curios 判定（照 `BlissRuneEvents#multiplierOf`）；建议与收同挂 `LivingDamageEvent`，同一处理器里**先收、再刷新标记**；效果注册进 `MaledictMobEffects` | 可做               |
| 2 回血拦截             | `LivingHealEvent`，算有效回血量；不区分来源（裁定 6）                                                              | 可做               |
| 3 Dormant 存储       | 实体 capability（建议）或 `getPersistentData()`                                                     | 可做               |
| 4 Dormant → Active | 纯算术，转化规则见 §3 第 7 条（**仍未定**）                                                                    | 可做（先定规则）         |
| 5 符文收取             | `LivingDamageEvent`（顺序 B、写血之前）；**按伤害次数算**，不做挥砍去重（裁定 3、5）                                       | 可做               |
| 6 不可致死             | `setHealth(max(minSurvivable, health - collapse))`；单次收的强度封顶（裁定 1，数值待定）                        | 可做               |
| 7 夹取               | `LivingDamageEvent` 用投影血量，**先夹 D**（裁定 2）；收的时候再夹一次                                                | 可做               |
| 8 净化               | 名册 + 低频 tick（只对有名册的实体）                                                                         | 可做               |
| 9 基础同步 / 粒子        | `MaledictNetwork` + `ServerLevel#sendParticles`                                                | 可做，MVP 甚至可以不同步数值 |
| 10 吸收              | 无钩子                                                                                            | **后置**（裁定：不着急做）  |
| 11 正面效果            | `MobEffectEvent.Added`（注意刷新与指令路径）                                                              | 可做，但建议后置         |
| 12 HUD             | 照 `DelayedVitalsOverlay`                                                                       | 可做，与机制无关         |

原文说「第一版做到第 8 步就能评估核心循环」—— 同意，而且照上表第 1–8 步确实不需要 Mixin。

---

## 6. 进游戏要看的（首次自测清单）

1. 满血连喝三瓶治疗药水：腐败**不增长**（验证有效回血量算法）。
2. 残血喝一瓶治疗 II：只按真实回血量记账。
3. 挂着 `Fallen` 打一套神侵恶刃：每段伤害各收一次（本体 + 奥术通道 = 两次），总收成不超过当时的 A。
4. 收完立刻被打死：死因是普通攻击，掉落/图腾/死亡事件正常。
5. 目标血量 2、A=5：收之后仍是可存活血量（不出现「凭空死亡」）。
6. 对无常 BOSS 全套流程：确认 `Fallen` 正常挂上、腐败正常攒（预期：收不动它的血）——
   这是「不特判实体类型」的已知后果，不是 bug。
7. 戴抑郁符文的玩家互相打：延迟池与腐败账目对不上时，明确是「有意不管」还是接了回调。
8. 死亡重生、换维度、实体转化（僵尸村民转化）：账目按预期清零或保留。

---

## 7. 已落地（第一版实现）

代码：

| 文件 | 内容 |
| --- | --- |
| `common/corruption/CorruptionState.java` | D/A 账目：50% 转化（用**旧 D**）、先夹 D、收（账上有的／单次上限／不致死三重约束）、**惰性净化**、NBT |
| `common/corruption/CorruptionData.java` | 挂在所有 `LivingEntity` 上的 capability；账目惰性创建，没腐败过的实体不留对象、存的也是空 tag |
| `common/corruption/CorruptionRuneEvents.java` | `LivingHealEvent` 记账（有效回血量）+ `LivingDamageEvent` 收 → 落标记 → 夹取；收的粒子反馈；**这枚符文所有预设值都在这** |
| `common/item/RuneOfTheFallenItem.java` | 堕落符文 `maledict:rune_of_the_fallen`（**白镴档**，邪术精魂） |

`fallen` 效果**没有自己的类**：它没有行为，只有预设值 + 一条属性修饰符声明，所以是 `MaledictMobEffects`
里的匿名子类（实例初始化块里调 `addAttributeModifier`，因为那是 `protected`，跨包只有子类体能调），
预设值放在 `CorruptionRuneEvents`。同类的既有写法：`BLESSING_OF_LIFE`、`RIPENING` 都是空花括号的匿名效果。

接线：`MaledictMobEffects.fallen`、`MaledictItems`、`MaledictCreativeTabs`、`MaledictItemTags`（`curios:rune`）、
`MaledictItemModels`、`MaledictRecipes`（恶念白镴符板 + 64 活肉）、`MaledictLanguage`、
`MaledictCodexEntries`（挂在「虚空符文工艺」选择页的第三枚，与演进凝滞、朽骨同页）。

### 7.1 实现期定的子裁定（原文没写，先按最省的方式定了，要改就说）

| 项 | 取值 | 理由 |
| --- | --- | --- |
| `FALLEN_DURATION_TICKS` | 200 | 窗口够长，看得见循环，又不跨整场战斗 |
| 堕落等级 | **每次命中涨一级，最多 10 级**（等级 = 效果 amplifier + 1） | Human 定；等级本身就写在效果上，客户端直接显示「堕落 II / III…」 |
| 恶念转化 | **每级 +0.02**（满载 +0.2），落在**目标**身上 | Human 定；见 §7.3 |
| `MAX_COLLAPSE_PER_HIT` | 6.0 | 裁定 1 的「单次收封顶」，先给 3 颗心 |
| `MIN_SURVIVABLE_HEALTH` | 1.0 | 半颗心；收永不致死 |
| `CORRUPTION_PER_HEAL` | 1.0 | 一记有效回血记 1 点，1:1 最直观 |
| 净化 | 空闲 100 tick 后每 40 tick 扣 1 点，**先 D 后 A** | 与「先夹 D」同向 |
| 净化的实现方式 | **完全不 tick**：只在这几个事件里按经过的 tick **补算** | 见 7.3；没有 `ServerTickEvent`、没有名册、没有全表扫描 |
| 净化计时 | 只有 gain 会推后它 | 「反复求增益 = 一直不净化」 |
| 收的触发面 | 佩戴者造成的**每一段**伤害实例（含箭、含奥术通道的第二段） | 裁定 3「按伤害次数算」 |
| 自伤 | 攻击者 == 受害者时不结算 | 与无忧符文的写法一致 |
| 清账 | 不挂 `LivingDeathEvent` | 实体消失／重生换实例即自然清空，也避免和朽骨符文的免死抢死亡事件 |

### 7.2 待补

- **配方副料是我猜的**：白镴符板定下来了，但「活肉 ×64」是照朽骨符文抄的，要换说一声。
- **贴图是占位的**：`rune_of_the_fallen.png` 现在复制的是抑郁符文，`fallen.png`（效果图标）复制的是衰朽 —— 等美术。
- **文案已由作者定稿**：典籍页描述／正文（正文已按 13 字一空格排版）、效果说明「生命回复会堕落成伤口」、
  符文 tooltip「使对方的生命回复堕落成伤口」，英文按中文派生（`Healing decays into wounds.` /
  `Makes the target's healing decay into wounds.`，与正文里的 `decays into a pang of pain` 同一套词）。
- 原文第 9–12 步（同步、吸收、正面效果、HUD）未做，按裁定后置。

### 7.3 恶念转化（`malum:malignant_conversion`）挂在哪、为什么

Human 的裁定：**每级堕落给目标 +0.02 恶念转化，最多 10 级**（满载 +0.2）。落地在
`MaledictMobEffects.FALLEN`（匿名效果子类）+ `CorruptionRuneEvents` 的 `FALLEN_*` 预设值。

先查清了这个属性是什么（Malum 那边的实现，不是猜的）：

- 它是一条**百分比转化率**属性，Malum 用数据包 `data/malum/malignant_conversion_data/*.json` 配置：
  每条给出 `source_attribute(s)`、`ratio`、`ignore_base_value` 与 `target_attributes`。
  默认那几份把 **Iron's Spells 的法力/法术强度/抗性**、`lodestone:magic_proficiency`、
  Malum 自己的 `soul_ward_*` 按比例**消耗掉**，换成 `minecraft:generic.armor`、
  `armor_toughness`、`lodestone:magic_resistance`。
- 换算由 `MalignantConversionHandler` 在 `LivingEvent.LivingTickEvent` 上做，属性值一变就重算；
  属性本身由 Malum 通过 `EntityAttributeModificationEvent.getTypes()` 发给**所有有属性的实体类型**。
- 所以这条属性天然就是「**魔法属性 → 沉重护甲**」—— 与作者写的「由金中的金……堕落为原始而沉重的铅」
  是同一件事。

三个落地决定：

1. **挂在目标身上**（效果修饰符只能落在效果携带者身上，也正是「它自己堕落」的读法）。
2. **整条修饰符交给原版效果机制**：匿名子类的实例初始化块里
   `addAttributeModifier(AttributeRegistry.MALIGNANT_CONVERSION.get(), FALLEN_MODIFIER_ID.toString(),
   FALLEN_CONVERSION_PER_LEVEL, ADDITION)`，量由原版按 `基础值 × (amplifier + 1)` 缩放；加、摘、换等级
   都发生在效果增删与更新时（`onEffectUpdated` 先 remove 再 add，按 id 摘旧挂新）。修饰符是**永久**的
   （原版 `MobEffect#addAttributeModifiers` 用的就是 `addPermanentModifier`），随实体 Attributes NBT 存下来。
3. **构造期取属性就行（有实测撑腰）。** 第一版我按反编译源码行号推断「效果事件先跑、属性事件后跑」，**错了**。
   临时探针实测（datagen，47.4.23）的顺序是
   `sound_event → fluid → block →` **`attribute`** `→` **`mob_effect`** `→ particle_type → item → entity_type → …`，
   并且在 `mob_effect` 事件里 `AttributeRegistry.MALIGNANT_CONVERSION.get()` **取得到值**。
   机制上：`getKnownRegistries()` 由 `markKnown()` 填充，而它是在**某个注册表第一次被写入条目**时调用的
   （Forge 插在 `MappedRegistry#register(...)` 里），不是构造时 —— 顺序 = 原版 bootstrap 里谁先落第一个值。
   （`BuiltInRegistries` 的声明行号 `MOB_EFFECT` 123 / `ATTRIBUTE` 172 说明不了任何事，别再对着行号推顺序。）
   仓库里 `AgeOfDarknessEffect` 早就是这么写的（构造期取 Malum 与 Lodestone 的属性），因此这里照同一个写法。
   **教训：这类「注册期能不能引用别人的注册项」的问题，先 grep 仓库里有没有现成用法，再去翻反编译源码。**
4. **涨级与续时借 Lodestone 的 `EntityHelper`**：`amplifyEffect(instance, entity, 1, MAX_AMPLIFIER)`
   （涨一级、封顶、不降级）＋ `extendEffect(instance, entity, DURATION, DURATION)`（刷回 200，不缩短）。
   两者最后都走 `syncEffect` → 原版 `LivingEntity#onEffectUpdated(effect, true, entity)`，
   所以**属性修饰符会跟着换成新等级的量**，客户端也同步拿到新等级。
   仓库里 `AgeOfEnlightenmentEvents`、`IncursusBladeEffects` 用的就是同一套 helper。

已知后果（都是有意接受或无害的）：

- **护甲挡不住收**：收是直写血量，恶念转化换来的护甲对收一分钱作用都不起 —— 这正是
  「越堕落越硬，但欠的账照收」。
- 只有身上真有那些 `source_attribute` 的实体才会有可见变化（玩家、法师怪）；普通僵尸转化量是 0，
  挂上去也没有副作用。
- **一次神侵恶刃挥砍 = 两次伤害实例 = 涨两级**（沿用它「按伤害次数算」的裁定）；
  10 级大约五、六刀就到顶，窗口断档后从 I 级重来。

### 7.4 净化的性能账（为什么没有「净化的 tick」）

`CorruptionState#purify` **不是 tick 任务**，是纯函数式的补算：记两个时间戳（`lastGainTick`、`purifiedThrough`），
下次谁碰到这份账目（回血／挨打／被收）时，用 `(now - start) / 40` 一次算出这段时间该扣多少，一次扣掉。

- **没有任何常驻开销**：没有 `ServerTickEvent`、没有「有腐败的实体」名册、没有遍历世界里的实体 ——
  原文 §11.4 担心的「每 tick 全表扫」在这套写法下不存在。
- **单次代价 O(1)**：一次除法 + 两三个 `min/max`，且只在本来就发生的事件里跑。
- **结果与「一直在跑」等价**：数值只在被读到的时刻有意义，补算出来的就是那一刻应有的值。
- 已知代价：数值不会在「没人碰它」的时候自己走动。所以以后要做实时 HUD（第 12 步）时，
  要么在同步包里顺手带上当前值，要么维护一份**只含有腐败实体**的名册、每 20 tick 结算一次 ——
  那是为了「显示」，不是为了「状态正确」，等真要做 HUD 时再选。

---

## 8. 已否掉的方案（留档，别再走一遍）

### 8.1 用 `attacker instanceof Player` 省掉 curios 查表

**否掉（Human，2026-10-03）。** 动机是性能：damage 路径上，每个「有生物攻击者」的命中都要问一次
curios（`RuneOfTheFallenItem.isEquipped`）。但代码里没有任何地方假定攻击者是玩家 —— capability 挂在所有
`LivingEntity` 上，穿戴判定只认 `LivingEntity`，加这一刀会在别的模组给生物开 curios、或替生物戴上符文时
**静默漏掉它们**，换来的只是省一次查表，不值。

「实际上只有玩家戴得上」是数据包层面的现实（`data/maledict/curios/entities/maledict_entities.json`
目前只列 `minecraft:player`，且只开了 `delusion` 槽），不是代码该固化的假设。
