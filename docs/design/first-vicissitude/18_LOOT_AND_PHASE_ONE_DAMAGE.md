# 18 — 掉落、非玩家减伤与一阶段伤害修订

2026-09-20 用户需求（第十五轮），五条：

1. 把永恒之年改成类似下界之星的额外掉落，仅玩家击杀获得。
2. 战利品额外 8 种精魂各 6，走 malum 提尔锋认识的那个战利品表。
3. 受到非玩家伤害固定减伤 50%。
4. 一阶段对玩家造成伤害的时候不要用 medium 或 final，用 lighterDamageMethod，而且不要在 sethealth
   射弹后立刻放出。
5. 战利品额外虚无板石 6、虚空盐 6，走标准战利品。

本文覆盖 07、12 中与本轮冲突的条目。

## 一、启蒙之年：玩家击杀才发的专属掉落（需求 1）

用户口中的「永恒之年」就是本模组的 `maledict:age_of_enlightenment`（游戏内中文名「启蒙之年」）；
仓库里不存在第二个叫「永恒之年」的物品，本轮按这个理解实现。

- 从四张常规战利品表里**移除**启蒙之年那一池，改为在实体侧发放：覆写
  `FirstVicissitudeBossEntity#dropCustomDeathLoot`，与下界之星同一条路——原版凋灵就是在
  这个方法里把下界之星 `spawnAtLocation` 出去，并 `setExtendedLifetime()` 让它不自然消失。
- **仅玩家击杀**：只看原版传来的 `recentlyHit`（`lastHurtByPlayerTime > 0`，即最后 100 tick 内
  有玩家，含玩家射出的弹体与玩家驯服的宠物对它造成过伤害）。不是玩家杀的就不发。
  与下界之星的历史行为一致，也是用户这条需求里唯一可判定的「玩家击杀」定义。
- 等级仍跟难度走：I–IV 级（NBT `Maldict:EnlightenmentLevel` = 0–3）。这个数字原本写在
  掉落表 JSON 里，现在归 `BossDifficulty#enlightenmentLevel()`，仍是唯一权威。
- 写等级只有 `AgeOfEnlightenmentItem#create(int)` 一个入口，键名与夹取都在那里。
- 仍然受 `doMobLoot` 与 `VicissitudeBossEntity#dropAllDeathLoot` 的一次性事务约束：
  只有真死那一次、且 `shouldDropLoot()` 成立时才会走到。

## 二、标准战利品（需求 5）

四张 `maledict:entities/first_vicissitude*` 表的内容改为：

| 奖励 | 数量 | 条目 |
| --- | --- | --- |
| 深岩珍金块 | ×2 | `malum:block_of_cthonic_gold`（不变） |
| 虚无板石 | ×6 | `malum:null_slate`（新增） |
| 虚空盐 | ×6 | `malum:void_salts`（新增） |

- 三池都是 `rolls 1`、固定数量、必掉，不吃抢夺也不要求击杀者。
- 「虚无版石」按 Malum 的物品 `malum:null_slate`（Null Slate，中文「虚无板石」）实现；
  「虚空盐」是 `malum:void_salts`（Void Salts），注意物品 ID 是复数。
- 四张表内容现在完全相同，但四个表 ID 仍然保留（SIMPLE 用的就是实体默认路径，其余三档
  在存档、`/loot` 与既有文档里被引用过）。要改数量只动 `MaledictEntityLoot` 里那三个常量。
- `src/generated/resources/data/maledict/loot_tables/entities/*.json` 由 runData 重生成，
  未手工编辑。

## 三、Malum 精魂表（需求 2）

「malum 提尔锋认识的那个战利品表」指 Malum 的逐实体精魂掉落数据
`data/<namespace>/spirit_data/entity/*.json`：提尔锋的额外伤害就是读它的
`totalSpirits`（`伤害 = totalSpirits × 2`，见 Malum `TyrvingItem#hurtEvent`），
灵魂暴露后击杀放出的精魂也按它结算（`SpiritHarvestHandler`）。它不是原版意义下的
`loot_tables`，所以需求 5 才要特别说「走标准战利品」。

新增 `src/main/resources/data/maledict/spirit_data/entity/first_vicissitude.json`：

| 字段 | 值 |
| --- | --- |
| `registry_name` | `maledict:first_vicissitude` |
| `primary_type` | `eldritch`（八种里的本命灵，与法典里「邪术精魂在最底下」一致） |
| `spirits` | sacred / wicked / arcane / eldritch / aerial / aqueous / earthen / infernal，各 6 |

- 八种就是本模组「八种精魂」那一套（与神侵恶刃灌注配方同一组），幽影（umbral）不在其中。
  **第十六轮修订**：用户要求「再加一个幽影精魂」，表里因此在八种之后追加 umbral ×1，
  总数 48 → 49 点灵魂强度（提尔锋额外伤害 96 → 98），见 [21](21_ADAPTATION_UMBRAL_AND_PRESS_PRESSURE.md)。
- 总数 48 点灵魂强度：提尔锋在能触发魔法的伤害类型上每一下额外 96 点魔法伤害
  （`totalSpirits × 2`），这是这张表的直接后果（第十六轮追加幽影之后是 49 点 / 98 点）。
- 精魂只在 Malum 认定的「灵魂暴露」状态下击杀才爆出（需要用带 `malum:soul_hunter_weapon`
  标签的武器，例如各类镰刀、法杖、提尔锋、灵魂染钢系列）。本模组的神侵恶刃目前不在那个
  标签里，所以拿它砍死无常不会触发精魂爆发——这张表只负责「掉多少、掉哪几种」。
  若希望本模组的武器也能触发，把物品加进
  `data/malum/tags/items/soul_hunter_weapon.json` 即可，本轮没有擅自改动 Malum 的标签。
- 文件放在本模组自己的命名空间下：Malum 的读取器扫的是所有命名空间的
  `spirit_data/entity`，真正的主键是 JSON 里的 `registry_name`，文件名只要求全局唯一。
- 放在 `src/main/resources`（不是 runData 产物）：Malum 的这张表不是能做数据生成的注册表。
- 没有这张表时，Malum 会给所有非幼年生物套 `DEFAULT_BOSS_SPIRIT_DATA`（邪术 2 点），
  本轮把它换成明确的一份。

## 四、非玩家伤害减伤 50%（需求 3）

`FirstVicissitudeBossEntity#modifyIncomingDamage` 在部位倍率之后多乘一档：

- 算「玩家的账」：`getEntity()` 或 `getDirectEntity()` 是玩家（本人近战、玩家射出的弹体、
  玩家点燃的爆炸），照常全额。
- 其余来源（别的生物、环境爆炸、别人的宠物）固定 ×0.5。
- 一阶段本来就对一切伤害免疫（`isDamageImmune`），所以这一档实际生效区间是二阶段与
  非玩家实体造成的伤害；真正的上限仍是真生命的每击伤害上限。

## 五、一阶段伤害与压血后的喘息（需求 4）

**伤害档**：`hurtParticipant` 对玩家的档位改为「一阶段恒为 LIGHT，二阶段才按难度」。
一阶段的伤害只有胸口「命运标记」与环「星轨裁定」两处，而这两处都发生在压血弹之后：
用 medium/final 会持续补偿直到指定数值真的扣掉，护甲在这时等于不存在，被压到 1 血的
玩家必死。改用 `DamageProbe.lighterDamageMethod` 后，护甲、附魔与伤害上限照常参与结算。
二阶段维持 07/11 第十轮的难度表不变。

**压血后不立刻释放**：一阶段的扇射球与追踪球都只压血、不掉血，真正会杀人的是那两次
普通伤害；追踪球最多飞 200 tick，常常正好在释放帧前几 tick 才落地，于是「压到 1 血」与
「释放」看起来是同一瞬间。现在的处理是：

- 压血弹（`VicissitudeSpiritBoltEntity` 的压血分支、`VicissitudeLightOrbEntity#collect`）
  在把**玩家**压到 1 血后回调 `FirstVicissitudeBossEntity#onPressLanded`。
- 回调把 `PRESS_RECOVERY_TICKS`（30 tick，一个基础攻击槽）记进喘息窗口：
  窗口内不开始新动作，并且把已经起手、还没释放的胸口/环技能直接作废（预兆一并撤掉），
  下一轮重新起手。压血球与羽片齐射本身不造成伤害，不打断。
- 于是压血之后的第一次伤害释放至少隔着「30 tick 窗口 + 完整前摇」，玩家有一次喘息与自救机会。

这条按「压血落地之后不许紧接着释放」实现；若用户的本意是「压血弹射出之后不许紧接着
追击」，只需把回调点从落地改到出手，其余逻辑不变。

> **第十六轮已整条取消**（[21](21_ADAPTATION_UMBRAL_AND_PRESS_PRESSURE.md)）：
> 用户要求去掉「释放改血弹后一段时间不攻击」。`onPressLanded`、`PRESS_RECOVERY_TICKS`
> 与两处弹体回调全部删除，压血弹落地后只按原本的攻击槽与前摇走；压血行为本身、
> 「一阶段弹伤害为 0」与一阶段恒用 light 档都不变。上面这段作为历史记录保留。

## 六、实现位置

- `common/entity/FirstVicissitudeBossEntity`：`dropCustomDeathLoot`、`BossDifficulty`
  新增 `enlightenmentLevel`、`modifyIncomingDamage`、`hurtParticipant`、`onPressLanded`、
  `pressRecoveryTicks`、`PRESS_RECOVERY_TICKS`。
- `common/entity/VicissitudeSpiritBoltEntity`、`common/entity/VicissitudeLightOrbEntity`：压血回调。
- `common/item/AgeOfEnlightenmentItem#create(int)`：唯一写等级入口。
- `data/MaledictEntityLoot`：三池常规奖励，启蒙之年那一池移除。
- `src/main/resources/data/maledict/spirit_data/entity/first_vicissitude.json`。
- `src/test/java/.../data/FirstVicissitudeSpiritDataTest`：钉住八种 ×6、主类型合法、无幽影。
- 顺手修回工作区里 `IncursusBladeItem#applyTieredDamage` 被改坏的一行
  （`DamageProbe(target, source, damage)`，不是合法 Java），恢复为 HEAD 的
  `DamageProbe.lighterDamageMethod(target, source, damage)`；该文件本轮无其它改动。

> 第十六轮：上表里的 `onPressLanded`、`pressRecoveryTicks`、`PRESS_RECOVERY_TICKS` 与两处
> 弹体压血回调均已删除（见 [21](21_ADAPTATION_UMBRAL_AND_PRESS_PRESSURE.md)），其余条目仍然有效。

## 七、验证

| 命令 | 结果 |
| --- | --- |
| `.\gradlew compileJava test --offline` | BUILD SUCCESSFUL；7 个测试类全绿（含新增 5 项） |
| `.\gradlew runData --offline` | BUILD SUCCESSFUL；四张表重写为珍金块 2 / 虚无板石 6 / 虚空盐 6，启蒙之年已消失 |

未在游戏内验证：玩家击杀与非玩家击杀各一次核对启蒙之年是否发放、四档等级；
虚无板石与虚空盐的实际掉落；非玩家伤害是否恰好减半；一阶段被压到 1 血后胸/环是否
确实被推迟；用提尔锋攻击无常时的额外伤害与击杀时的精魂爆发（需要真实对局）。
