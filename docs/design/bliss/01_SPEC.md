# 01 — 无忧符文规格

状态：**已落地**。模型里有过两读的三处（倍率叠加、强制命中的范围、抬档盖到哪一层）由用户逐条选定，
结论见第 6 节；数值与文案均已定稿。

## 1. 定位与配方

- 物品 id：`maledict:rune_of_bliss`，中文名「无忧符文」，英文 `Rune of Bliss`。
- 与抑郁符文（`rune_of_melancholia`）成对：抑郁把痛磨钝（自己少挨），无忧把两个人的命运绑在一起
  （谁都躲不掉，而且血差多少就重多少）。两枚都在幽影精魂那一页、同一档工艺。
- 配方（runeworking，虚空档 + 32 枚幽影精魂）：

```java
new RunicWorkbenchRecipeBuilder(MaledictItems.RUNE_OF_BLISS.get(), 1)
        .setPrimaryInput(ItemRegistry.VOID_TABLET.get(), 1)
        .setSecondaryInput(SpiritTypeRegistry.UMBRAL_SPIRIT.spiritShard.get(), 32)
```

Malum 自己的 25 条 runeworking 里，虚空档是「虚空石符板 + 16 枚基础精魂」；本符文沿用符板、
精魂数翻倍，翻倍换的是「幽影精魂比基础精魂稀有得多」。抑郁符文是污染石档 + 16 幽影，
无忧比它再贵一档。

32 枚能凑出来是因为 `UmbralSpiritStackSize` 已经把幽影精魂修回 64 堆叠 —— 那条修复的动机正是
「符文工作台的 `IngredientWithCount` 要求单格 `count >= 需求`」，别把它当成纯手感改动。

## 2. 效果一：分担命运（倍率）

按用户给的公式原样落地（`SharedFate`，纯数学、可单测）：

```java
float attackerHealthRatio = attacker.getHealth() / attacker.getMaxHealth();
float targetHealthRatio   = target.getHealth()   / target.getMaxHealth();
float healthDifference    = Math.abs(attackerHealthRatio - targetHealthRatio);
float damageMultiplier    = 1.0F + 0.70F * healthDifference;
float finalDamage         = baseDamage * damageMultiplier;
```

| 攻方血量比 | 守方血量比 | 倍率 |
| --- | --- | --- |
| 1.0 | 1.0 | 1.00（不做任何事） |
| 1.0 | 0.5 | 1.35 |
| 1.0 | 0.0 | 1.70（到顶） |
| 0.5 | 0.5 | 1.00 |

两条性质，都是刻意的：

- **方向对称**：公式里攻守互换结果一样，所以它是「这一对关系」的属性，不是「每一方各乘一次」——
  两边都戴着也不会变成平方（用户选定）。`SharedFateTest` 把对称性钉成了测试。
- **只加不减**：倍率永远 `≥ 1`，血量比例相同就是原样。

参与条件（缺一不参与，倍率按 1 算）：攻守**都是生物**、不是自己打自己、**至少一边戴着**符文、
在服务端。摔落/火/毒这类没有攻击者的伤害没有可比的血量比，也不存在「攻击命中」，一律不介入。

落点：`LivingHurtEvent`（`EventPriority.LOWEST`）`setAmount(amount × 倍率)`。

**同一记上两半的先后**：抑郁符文的适应挂在默认优先级（NORMAL），无忧的倍率挂在 LOWEST，
所以同一记伤害是「先适应、后乘倍率」—— 入队到延迟池的是乘过倍率的量，两枚符文一起戴不会互相拆台。
无忧在 `LivingDamageEvent` 上也只处理「被取消」的那一种情况，而抑郁在 NORMAL 那一步就把量入队并清零了，
所以轮到无忧时看到的量已经是 0，不会重复介入。

## 3. 效果二：强制命中

`LivingAttackEvent` / `LivingHurtEvent` / `LivingDamageEvent` 三个事件各挂 **LOWEST**，
接到**已经被取消**的那一个，就用 ChangeLib 补打一记：

```java
DamageProbe.lighterDamageMethod(victim, source, 补打的量);
```

- **为什么是 LOWEST**：这一符文要的是「最后说话」。别人的减伤、免疫、闪避都已经表过态，
  到我们这里还剩多少就是多少；也只有跑到最后，`isCanceled()` 才是最终答案。
- **为什么是 `lighterDamageMethod`（medium 档但不要求打满）**：承诺是「打得中」，不是「打得动」。
  `lighterDamageMethod` 是 medium 梯级 + `compensateDamage = false`：拿到第一笔真实掉血就收手；
  会一路补到指定数值的 `mediumDamageMethod` 则会去撬防御，对「让攻击能命中」来说过头了。
  注意「收手」只发生在**已经掉血**之后：取消方若照旧取消，梯级仍会往下爬（见下面的代价表）。
- **补多少**：`LivingAttackEvent` / `LivingHurtEvent` 用 `事件量 × 倍率`；`LivingDamageEvent` 用事件量
  （护甲/附魔/吸收都算完的终值，而且 `LivingHurtEvent` 已经放过行、倍率乘过了，再乘就是乘两遍）。
- **防重入**：探针自己会再走一遍 `hurt` 与这三个事件，取消方多半照旧取消，于是
  「取消 → 补打 → 又取消 → 再补打」可以无限递归。`BlissRuneEvents` 用一枚静态 `forcing` 闸
  在补打期间让三个处理器全部让路（伤害事件在服务端单线程上跑，一个布尔足够）。

已知代价（写在明处，不算 bug）：

| 代价 | 原因 |
| --- | --- |
| 补打期间**别的**生物也挨了一下，那一记不享受符文 | 闸门是全局的，不按实体记账；换成按实体记账也会在「探针顺手打了别人」时同样漏掉 |
| 直接调 `actuallyHurt` 的伤害不带倍率 | 那条路不发 `LivingHurtEvent`，倍率没有落点 |
| `isInvulnerableTo`、无敌帧、死亡这些**不经过事件**的原版早退不补打 | 它们不是「被取消的事件」，是原版自己的状态机；符文的承诺只覆盖「被取消的那一下」 |
| 补打是**第二记伤害**：原版那一路早已 `return false`，音效、击退、附魔、抢夺、击杀归属、不死图腾都不会走 | 被取消的那一记在原版流程里没有落点，只能另起一记 |
| 别的模组会把同一击**记两次账** | 探针重走一遍 `hurt` 与三个事件，别人的处理器会再跑一次（抑郁符文的适应、连击计数等） |
| 取消方照旧取消时，medium 梯级会继续爬到 `setHealth`／私有字段／NBT 写入 | 那是 medium 档自己的梯级；此时这一击**不经过护甲、附魔、吸收**。用户明确接受：这是「强制命中」换来的强度 |

## 4. 效果三：神侵恶刃整体抬一档

`IncursusBladeItem.DamageTier`：`LIGHT → MEDIUM → FINAL`（刀自己的档由精魂数决定，见
`MEDIUM_DAMAGE_LEVEL = 7` / `FINAL_DAMAGE_LEVEL = 11`）。佩戴者的每一记都 `raised()` 一级，
已经在顶就还是顶。

覆盖「以这把刀名义打出的伤害」的全部出口（用户选定口径统一）：

| 出口 | 原本的档 |
| --- | --- |
| 近战主伤害（`scythe_melee`） | 随精魂成长 |
| 魔法通道 / 冻结通道 | 随精魂成长 |
| 投掷返程撞人（`scythe_sweep`） | 写死 medium |
| 飞升横扫（`scythe_sweep`） | 写死 medium |

读档的是**攻击者**（`source.getEntity()`）：一把没喂饱的刀戴上符文就能立刻用中档。

## 5. 常量表

| 常量 | 值 | 说明 |
| --- | --- | --- |
| `SharedFate.BONUS_PER_RATIO` | `0.70F` | 比例差满格的额外伤害，倍率上限 1.70 |
| 补打档位 | `DamageProbe.lighterDamageMethod` | medium 梯级、partial 即止 |
| 事件优先级 | `EventPriority.LOWEST` | 三个事件一致 |
| 抬档幅度 | `DamageTier.raised()` 一级 | LIGHT→MEDIUM→FINAL，到顶不再抬 |

数值全部写死为类内常量，不做 config（与抑郁符文一致）。

## 6. 已定（用户逐条确认）

| 项 | 结论 |
| --- | --- |
| 物品 id | `rune_of_bliss`（贴图沿用用户给的那张，见第 7 节） |
| 两边都戴 | 倍率**只乘一次**，不叠乘 |
| 强制命中范围 | 只在**存在攻击者**的伤害上生效；摔落/火/毒不介入 |
| 抬档范围 | **所有**神侵恶刃伤害都抬，含两记写死 medium 的技能命中 |
| 书页文案 | `description`＝「无爱亦无忧愁」，正文＝用户给的两句都排上 |
| 配方 | 虚空石符板 + 32 幽影精魂 |

## 7. 文案与贴图

按钮名 / 书页（虚空之书「幽影精魂的实验」选择页里的第二枚，与抑郁符文同一页、同一写法：
标题正文页 + 符文工艺配方页）：

| 键 | 中文 | 英文 |
| --- | --- | --- |
| 物品名 | 无忧符文 | Rune of Bliss |
| `.description` | 无爱亦无忧愁 | Untouched by Love or Sorrow |
| 正文 `.1` | 可是，有什么办法呢？ 谁在爱， 谁就应该与他所爱的人 分担命运 永恒的坠落，无爱亦 无忧愁，即是最终的虚无。 | But what's to be done? Whomsoever loves should share the burden of destiny with the ones he loves. Permanent falling, untouched by love or sorrow, the ultimate Nihility. |

中文正文按仓库规矩每 13 个可见字打一个空格（句读空格照留，长句补断点），实测分段
`10 / 4 / 10 / 4 / 9 / 12` 字，全部 ≤ 13。

tooltip 两行（`positiveEffect`，键前缀 `malum.gui.curio.effect.`）：

| 键 | 中文 | 英文 |
| --- | --- | --- |
| `maledict.bliss.damage` | 增加对血量比例不同的造成、受到的伤害。 | Increases the damage dealt to and taken from those whose proportion of health differs from yours. |
| `maledict.bliss.forced_hit` | 强制使双方的攻击可以命中。 | Forces both sides' attacks to be able to land. |

贴图：**用户提供的成品**（原始文件名 `nihility_gold_d_up2_left1_16x16.png`），落在
`src/main/resources/assets/maledict/textures/item/runes/rune_of_bliss.png`，16×16、21 色、
168 个不透明像素；alpha 轮廓与 `rune_of_melancholia.png` / `rune_of_rotten_bone.png` **逐字节相同**
（同一套符文剪影），所以不是占位图，`art/melancholia/tools/make_rune_of_melancholia.py`
那类生成脚本这里不需要。

## 8. 落地清单

| 文件 | 作用 |
| --- | --- |
| `common/item/RuneOfBlissItem.java` | 符文物品：两行 tooltip + `isEquipped` |
| `common/curio/SharedFate.java` | 倍率与血量比的纯数学 |
| `common/curio/BlissRuneEvents.java` | 三个事件（LOWEST）、倍率落点、取消补打、防重入闸 |
| `common/item/IncursusBladeItem.java` | `DamageTier`、`damageTier`、`raisedByBliss`、`dealTieredDamage` |
| `common/entity/IncursusScytheBoomerangEntity.java` | 投掷返程撞人那一记走抬档 |
| `common/combat/IncursusBladeEnchantments.java` | 飞升横扫那一记走抬档 |
| `registry/MaledictItems.java`、`registry/MaledictCreativeTabs.java` | 注册与创造栏 |
| `data/MaledictRecipes.java`、`MaledictItemTags.java`、`MaledictItemModels.java`、`MaledictLanguage.java` | 配方 / `curios:rune` 标签 / 模型 / 中英文案 |
| `client/MaledictCodexEntries.java` | 「幽影精魂的实验」选择页加上第二枚 |
| 测试 | `SharedFateTest`（倍率与对称性）、`IncursusBladeTierTest`（抬档一级一级、到顶不回绕） |

## 9. 进游戏要看的

1. **补打的手感**：把另一方的 `LivingAttackEvent` 取消掉（或找一个会取消命中的模组），确认这一记
   仍然掉血、且只掉一次；再确认补打**没有**把无敌帧/受伤动画续上。顺带盯两件事：别的模组有没有
   对同一击记两次账，以及「取消方照旧取消」时这一击是不是顺着梯级绕过了护甲（见第 3 节代价表，
   这是有意接受的强度，不是 bug，但要在游戏里看一眼实际数值）。
2. **倍率数值**：满血打残血应当明显更重（到顶 1.70），半血对半血应当完全无感。
3. **两边都戴**：确认倍率只乘一次（不是平方）。
4. **神侵恶刃**：未喂饱的刀戴上符文应当直接走中档；喂满 7、11 两级各看一眼档位是否刚好抬一级，
   投掷返程与飞升横扫也应跟着抬。
5. 书页两行 13 字断句在游戏里排得好不好看（「谁就应该与他所爱的人 / 分担命运」这一处是硬断的）。

## 10. 已否掉的方案（留档，别再走一遍）

### 10.1 「拒绝否决」：把 `setCanceled(false)` 当成强制命中

**否掉的是这个替代方案，采用的是补打（用户选定）。** 先把原版落点钉在这里，免得以后重新翻源码：

```
LivingEntity#hurt:1059   LivingAttackEvent        ← 整个 hurt 的第一句，连 isInvulnerableTo 都在它后面
LivingEntity#hurt:1060   isInvulnerableTo / 客户端 / 已死 / 火免
LivingEntity#hurt:1077   ShieldBlockEvent（盾牌格挡）
LivingEntity#hurt:1101   无敌帧：invulnerableTime > 10 → 只吃差额，或整击 return false
LivingEntity#hurt:1106/1112 → actuallyHurt
  actuallyHurt:1617      LivingHurtEvent          ← 护甲之前
  actuallyHurt:1619-22   护甲 / 附魔 / 吸收
  actuallyHurt:1632      LivingDamageEvent        ← 写血量之前
  actuallyHurt:1635      setHealth
（玩家同构：Player#hurt:812 发 LivingAttackEvent，Player#actuallyHurt:909/915 发另外两个）
```

替代写法是在 LOWEST 上 `event.setCanceled(false)`，让原版自己把这一记走完。它的好处：

- **一记就是原版那一记**：护甲、音效、击退、附魔、抢夺、击杀归属、不死图腾全部照旧；
- 不产生第二记伤害，别的模组不会对同一击记两次账；
- 不需要 `forcing` 防重入闸（那段里别的生物不会被静音）；
- 不依赖 ChangeLib，也不会顺着 medium 梯级爬到绕过防御的写入。

它的短处（也就是保留补打的理由）：

| 短处 | 说明 |
| --- | --- |
| 对「用 `isInvulnerableTo` 实现的免疫」无能为力 | 免疫检查在事件之后（1060/1616），事件层根本没有落点；补打靠探针的梯级能硬打进去 |
| 同优先级注册顺序排在我们之后的取消仍会赢 | 原版没有比 LOWEST 更后的优先级；两条路共有这个尾巴 |
| 要覆盖无敌帧得额外清 `invulnerableTime` | 又是一处强度决定（见 10.2） |

### 10.2 无敌帧清零（`invulnerableTime = 0`）

`LivingAttackEvent` 排在无敌帧判定之前，所以在这个事件里清 `invulnerableTime` 是可行的
（`FirstVicissitudeBossEntity.hurtParticipant` 用的就是这一手），能让「连着打」每一下都真落地。

**明确不做**（用户选定）：被围殴时挨的伤害会成倍上升，强度变化太大；原版无敌帧照旧生效，
符文的承诺仍然只覆盖「被取消的那一下」。

### 10.3 用 mixin／AT 直接绕过 `isInvulnerableTo`

否掉：那已经不是「命中」而是「无视免疫」，创造模式、火免、别的模组的无敌阶段都会被一起打穿。
留在这里只为说明为什么 `isInvulnerableTo` 那两条早退在两条路里都没动。
