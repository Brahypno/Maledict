# 三 Boss 攻防数值清单

> **权威声明**：本表全部由代码读出，代码优先于设计文档。凡文档与代码冲突，本文写代码值，并在同节用「文档与代码不一致」标出。
> 引用格式 `(文件名:行号)`；`原版` 表示数值来自 Minecraft 1.20.1 本体，仓库里没有行号（见文末「不确定项」）。
> 配置项统一写 `组.键 = 默认值`，路径在 `src/main/java/org/brahypno/maledict/config/MaledictConfig.java`。
> 行号取自编写时的工作区；若这些文件仍在被并行修改，**按符号名/常量名搜索**比按行号跳转更可靠。

---

## 概览表

| Boss | 实体 id | 生命池 | 护甲 | 基础攻击 | 移速 | 跟随 / 交战范围 | 伤害等级 combatProtectionGrade | 单次命中上限公式 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 第一化身 | `maledict:first_vicissitude` (MaledictEntities.java:79) | 500 / 750 / 1000 / 1500（随 `BossDifficulty`，与世界难度无关）(FirstVicissitudeBossEntity.java:3201-3204) | 15 (FirstVicissitudeBossEntity.java:312) | `ATTACK_DAMAGE` 基础 8.0 + 二阶段武器词条 (FirstVicissitudeBossEntity.java:310) | 属性 0.30；实飞 0.38 / 二阶段 0.52 (FirstVicissitudeBossEntity.java:313,183-184) | `FOLLOW_RANGE` 96 属性 (FirstVicissitudeBossEntity.java:315)；实际交战半径 = config `firstVicissitude.engagementRange = 96.0` (MaledictConfig.java:147) | **D4** `CONSISTENCY_WRITE` (VicissitudeBossEntity.java:159) | `真生命上限 × r × 部位权重`，r = 0.05 / 0.03 / 0.01 / 0.01 (FirstVicissitudeBossEntity.java:2416-2420) |
| 巨匠造物主 | `maledict:demiurge_statue` (MaledictEntities.java:87) | 400 / 600 / 800（随世界难度 easy / normal / hard）(DemiurgeDifficulty.java:14-20) | 10 (DemiurgeStatueEntity.java:127) | 冲撞 config `demiurge.chargeDamage = 7.0` (MaledictConfig.java:72) | 0.30 (DemiurgeStatueEntity.java:52,128) | `FOLLOW_RANGE` 48 (DemiurgeStatueEntity.java:128)；保持距离 12 (DemiurgeCombatGoal.java:17) | **D5** `RECOVERY_SOURCE` (DemiurgeStatueEntity.java:185) | `真生命上限 × 0.03` (DemiurgeStatueEntity.java:154) |
| 腐枯凋灵 | `maledict:weeping_wither` (MaledictEntities.java:37) | 400 固定（不随难度）(WeepingWitherEntity.java:106) | 8 (WeepingWitherEntity.java:107) | 凋灵之首直击 12 / 已凋零目标 14 (WeepingWitherCombat.java:13-17) | 原版 0.6 × 1.1 位移加成 = 0.66 (WeepingWitherEntity.java:497) | 原版 `FOLLOW_RANGE` 40；祭坛拴绳 24 / 归位 20 (WeepingWitherEntity.java:209,312-317) | **D3** `STORAGE_WRITE` (WeepingWitherEntity.java:138) | `min(伤害, 最大生命 × 0.05)` = 20 (WeepingWitherEntity.java:513,543) |

---

## 一、第一化身（First Vicissitude）

### 1.1 基础数值

| 项 | 值 | 来源类型 | 位置 |
| --- | --- | --- | --- |
| 实体注册尺寸 | 1.6 × 4.375 | 常量 | MaledictEntities.java:82 |
| `MAX_HEALTH` 基础 | 1000 | 常量 `BASE_MAX_HEALTH` | VicissitudeBossEntity.java:44,89 |
| 难度生命写入方式 | `MAX_HEALTH` 上一条固定 UUID 的 `ADDITION` 修饰符，delta = 难度生命 − 1000 | 运行时 | FirstVicissitudeBossEntity.java:318-329 |
| 真生命池 | 取 `bossDifficulty.maxHealth()`，与属性解耦（账本捕获） | 常量 | FirstVicissitudeBossEntity.java:2410-2412 |
| 护甲 | 15.0 | 属性 | FirstVicissitudeBossEntity.java:312 |
| 击退抗性 | 1.0 | 属性 | FirstVicissitudeBossEntity.java:311 |
| `ATTACK_DAMAGE` 基础 | 8.0 | 属性 | FirstVicissitudeBossEntity.java:310 |
| `MOVEMENT_SPEED` | 0.3 | 属性（飞行时不用） | FirstVicissitudeBossEntity.java:313 |
| `FLYING_SPEED` | 0.45 | 属性（不用，速度由常量决定） | FirstVicissitudeBossEntity.java:314 |
| 实际飞行速度上限 | 0.38 / 二阶段 0.52，转向混合系数 0.2 | 常量 | FirstVicissitudeBossEntity.java:183-185 |
| `FOLLOW_RANGE` | 96.0 | 属性 | FirstVicissitudeBossEntity.java:315 |
| 交战半径 | `firstVicissitude.engagementRange = 96.0`（4–256） | 配置 | MaledictConfig.java:140-147 |
| 满伤半径 | `firstVicissitude.damageRange = 48.0`，1.5× 处归零（72 格） | 配置 | MaledictConfig.java:148-155；衰减 FirstVicissitudeBossEntity.java:1972-1987 |
| 适应窗口 | `firstVicissitude.adaptationLevel = 2`（0 关闭） | 配置 | MaledictConfig.java:156-167 |
| 经验奖励 | 1000 | 常量 | FirstVicissitudeBossEntity.java:305 |
| 阶段一免伤 | 是（DORMANT 与 PHASE_ONE 全程） | 逻辑 | FirstVicissitudeBossEntity.java:2435-2445 |

**难度表**（`BossDifficulty`，由刷怪蛋 / 召唤决定，不读世界难度）

| 难度 | 阶段一 tick | 真生命上限 | 二阶段武器 | 玩家伤害档 | 缴饰品 |  Enlightenment 等级 | 位置 |
| --- | ---: | ---: | --- | --- | --- | ---: | --- |
| SIMPLE | 1800 (90 s) | 500 | 染魂钢镰刀 | LIGHT | 否 | 0 | FirstVicissitudeBossEntity.java:3201 |
| DIFFICULT | 1400 (70 s) | 750 | 救赎之锋 | LIGHT | 否 | 1 | FirstVicissitudeBossEntity.java:3202 |
| COMPLETE | 1000 (50 s) | 1000 | Incursus Blade（各词条 3） | MEDIUM | 是 | 2 | FirstVicissitudeBossEntity.java:3203 |
| EXTREME | 750 (37.5 s) | 1500 | Incursus Blade（各词条 9） | MEDIUM | 是 | 3 | FirstVicissitudeBossEntity.java:3204 |

> 难度**不提供伤害倍率**，只改生命、阶段一时长、玩家伤害档（`DamagePress`）与武器。难度之间的数值差其实体现在二阶段武器属性上（`+3` / `+9`），不在难度枚举里。

**二阶段攻击属性 D = 8 + 武器词条**：COMPLETE 11.0、EXTREME 17.0（`IncursusBladeItem.setStat` 写 `attack_damage` = 3.0 / 9.0，FirstVicissitudeBossEntity.java:2944-2947）。SIMPLE / DIFFICULT 的镰刀加成来自 Malum 物品数据，仓库内无行号，见「不确定项」。

### 1.2 防御机制

| 机制 | 数值 / 公式 | 位置 |
| --- | --- | --- |
| 单次命中基准上限 | `真生命上限 × r`，r 按难度 | FirstVicissitudeBossEntity.java:2415-2422 |
| 身体（权重 1.0） | 25 / 22.5 / 10 / 15（500·5% / 750·3% / 1000·1% / 1500·1%） | 同上 |
| 头部外壳（1.25） | 31.25 / 28.125 / 12.5 / 18.75 | VicissitudeRig.java:76；VicissitudeBossEntity.java:268-271 |
| 翼根（0.75） | 18.75 / 16.875 / 7.5 / 11.25 | VicissitudeRig.java:77,79 |
| 外翼（0.5） | 12.5 / 11.25 / 5 / 7.5 | VicissitudeRig.java:78,80 |
| 部位权重基准 | 躯干 1.0 / 头 1.25 / 翼根 0.75 / 外翼 0.5；无法定位时 BODY | VicissitudeRig.java:74-80；回退 FirstVicissitudeBossEntity.java:1965 |
| 一次请求的允许额度 | `基准上限 × 1.25`（留给头部） | VicissitudeBossEntity.java:45,148-150 |
| 每击上限结算顺序 | 两道门 → 部位修正（非玩家 ×0.5、适应、距离衰减）→ 单次上限 → `BossDamage.resolve`（护甲/魔法/吸收/事件）→ 最终再取 min(limit) | VicissitudeBossEntity.java:351-360,395-399 |
| 非玩家来源 | ×0.5，再进适应与距离衰减 | FirstVicissitudeBossEntity.java:1894,1910-1920 |
| 无活体攻击者 | 距离衰减跳过（满伤） | FirstVicissitudeBossEntity.java:1972-1976 |
| 护甲 / 魔法减免 | `getDamageAfterMagicAbsorb(getDamageAfterArmorAbsorb(dmg))`，之后 `onLivingHurt` / `onLivingDamage` 再各过一次 | VicissitudeBossEntity.java:390-391；BossDamage.java:33-45 |
| 命中门（第二道） | 每次有效命中把门推到 `now + 20` tick，门内后续伤害被拒 | VicissitudeBossEntity.java:280-282,348-350；VicissitudeVitality.java:40-47 |
| 原版无敌帧门 | `invulnerableTime > 0` 即拒（同 tick 批次除外） | VicissitudeBossEntity.java:336-339 |
| 免伤阶段 · DORMANT + PHASE_ONE | 全程免伤（PHASE_ONE 时长 1800/1400/1000/750 tick；DORMANT 不计时，等首击） | FirstVicissitudeBossEntity.java:2444,987-989 |
| 免伤阶段 · TRANSITION | 60 tick | FirstVicissitudeBossEntity.java:74,2441 |
| 免伤阶段 · 召唤仪式 | `isRiteSummoning()` 期间 | FirstVicissitudeBossEntity.java:2439-2440 |
| 免伤条件 · 无来源伤害 | `source.getEntity() == null` 恒免疫 | FirstVicissitudeBossEntity.java:2444 |
| 免伤条件 · 和平难度 | DORMANT 且 PEACEFUL 时免疫 | FirstVicissitudeBossEntity.java:2436-2438 |
| 回血 | 无技能回血；只有脱战回血（下行） | VicissitudeBossEntity.java:427-440 |
| 复活 / 抗死 | 无 reformation；`LivingDeathEvent` 被取消时恢复到 `min(1, 上限)` | VicissitudeVitality.java:27-30；VicissitudeBossEntity.java:466 |
| 击杀尝试 | 100 tick 窗口内第 3 次强制归零；`kill()` / `remove(KILLED)` 走同一条 | VicissitudeBossEntity.java:287-289,625-636 |
| 同 tick 多通道合并 | 同 tick 视为一批，跳过两道门；每通道各自结算后**只扣超过本批已有最大扣血量的差额**，不相加 | VicissitudeBossEntity.java:336-339,400-410；BossDamage.java:25-30 |
| 脱战回血 | 无目标、且无活着的 `lastHurtByMob` 时，每 tick 回 5% 最大生命池（`HEAL_FRACTION_PER_TICK = 0.05F`） | BossRecovery.java:28,43-52；VicissitudeBossEntity.java:563-566 |
| 脱战回血 · 战斗记忆（工作区新增） | 最近 `COMBAT_MEMORY_TICKS = 100` tick 内有过结算伤害时不算脱战，回血推迟到 100 tick 后 | BossRecovery.java:33,58-68；伤害侧打点 VicissitudeBossEntity.java:421 |
| 脱战回血 · 脚本节拍例外 | TRANSITION 与召唤仪式期间不回血（`recoversOutOfCombat()` 覆盖） | FirstVicissitudeBossEntity.java:2424-2432 |
| 脱战回血绝对值 | SIMPLE 25 / DIFFICULT 37.5 / COMPLETE 50 / EXTREME 75 每 tick | 由上两行相乘 |
| 空场重置 | 100 tick 无活体对手 → 回 DORMANT、清名单与适应 | FirstVicissitudeBossEntity.java:89,1014-1035 |

> **文档与代码不一致（每击上限比例）**：`docs/design/first-vicissitude/06_DAMAGE_AND_DPS.md:191-196` 写 SIMPLE 10%（身体 50）、DIFFICULT 5%（37.5），代码是 0.05 / 0.03（`FirstVicissitudeBossEntity.java:2416-2420`），身体上限 25 / 22.5。以代码为准。
> **文档与代码不一致（弹体去重）**：`06_DAMAGE_AND_DPS.md:93-94` 说扇射/齐射/兜底“每轮每目标只算一次”。代码里 `roundDamagedTargets` 去重只发生在 `hurtBySkill`（`FirstVicissitudeBossEntity.java:1621-1629`，用于近战、胸、环、冲刺），弹体走的是 `hurtParticipant`（同文件 1634-1647），**不去重**，同轮多枚弹命中同一目标会各自结算 0.75 D。

### 1.3 攻击 / 技能表

调度总节奏：阶段一开始后 20 tick 预热，之后每 30 tick 一个基础攻击槽（`FirstVicissitudeBossEntity.java:1199-1204`）；偶数槽按 齐射 → 胸 → 环 的轮转尝试特殊技（冷却没好就顺延），奇数槽打扇射（同文件 1213-1218, 1221-1249）。二阶段不再按槽，改逐 tick 按优先级判定（同文件 1252-1335）。

| 技能 | 名义伤害 | 范围 / 半径 | 前摇 → 命中帧 → 总时长（tick） | 冷却 / 间隔 | 触发条件 / 阶段 | 附带效果 | 位置 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 扇射 `WING_RANGED` | 一阶段 **0**（压血到 1）；二阶段 `0.75 × D` | 5 枚直线弹，-24°/-12°/0°/+12°/+24°，0.45 格/tick，寿命 80 | 0 → 12 → 20 | 随基础槽，30 tick | 阶段一奇偶槽兜底；二阶段中近距离兜底 | 每第二个扇射把中间那枚换成追踪球 | FirstVicissitudeBossEntity.java:1361-1380；VicissitudeRig.java:1294 |
| 羽片齐射 `WING_BARRAGE` | 一阶段 **0**；二阶段 `0.75 × D` | 3 波 × 8 枚（每侧 4），散布 `min(30, 6+距离×1.5)` 度，中心留缺口 | 20 释放，+8、+16 各补一波 → 56 | 70 tick | 偶数槽轮转第一顺位 | 每波 8 枚、中心缺口可过 | FirstVicissitudeBossEntity.java:1382-1422；VicissitudeRig.java:1295 |
| 命运标记 `CAST_FROM_CHEST` | `1.0 × D` | 目标脚下半径 **2.0** 格圆（内 0） | 30 → 30 → 50；第 10 tick 锁定落点 | 90 tick | 偶数槽轮转第二顺位，需目标下方 8 格内有地面 | 释放帧单次判定 | FirstVicissitudeBossEntity.java:1473-1490, 2349-2360；VicissitudeRig.java:1296 |
| 星轨裁定 `CAST_FROM_HALO` | `1.0 × D` | 内 **3.0**、外 **6.0** 的断环，**60°** 缺口随机朝向 | 40 → 40 → 60；第 10 tick 锁定 | 80 tick | 偶数槽轮转第三顺位，需地面 | 环心、环外与缺口内安全 | FirstVicissitudeBossEntity.java:1492-1512, 2361-2370；VicissitudeRig.java:1297 |
| 横斩 `SLASH_HORIZONTAL` | `1.0 × D` | 前向带 0.6 – 4.3 格，横向半宽 1.25 | 7–11 为命中窗 → 释放帧 8 → 20 | 20 tick | 距离 ≤ 5.75（`MELEE_COMMIT_RANGE`） | 每轮每目标一次 | FirstVicissitudeBossEntity.java:1311-1335, 1532-1575；VicissitudeRig.java:1299,1392 |
| 竖劈 `SLASH_VERTICAL` | `1.25 × D` | 0.6 – 4.4 格，半宽 1.25 | 14–18 命中窗 → 14 → 30 | 30 tick；每第 3 次近战 | 第三次近战槽 | 同上 | FirstVicissitudeBossEntity.java:1320-1328；VicissitudeRig.java:1300,1393 |
| 重击 `HEAVY_ATTACK` | `1.75 × D` | 0.6 – 4.5 格，半宽 1.25 | 24–28 命中窗 → 24 → 50 | 120 tick，优先级最高 | 近战槽第一顺位 | 单次最高伤害 | FirstVicissitudeBossEntity.java:1315-1318,1596-1603；VicissitudeRig.java:1301,1394 |
| 掷镰 `SCYTHE_THROW` | `1.0 × D`（回程 0） | 6–24 格；去程 0.8 格/tick，最多 20 tick / 16 格；回程 1.1 格/tick，总寿命 100 | 16 → 16 → 28，接 `SCYTHE_RECOVER`（10 tick） | 120 tick | 6–24 格、持械、无在飞镰、有视线 | 去程命中一次即返回 | FirstVicissitudeBossEntity.java:1283-1296,1711-1726；VicissitudeScytheProjectileEntity.java:39-43,134-165 |
| 冲刺 `DASH` | `1.0 × D` | 8–20 格触发；推进 10 格 / 12 tick，扫掠框膨胀 0.6 | 20 释放 → 50 总时长 | 160 tick | 8–20 格、持械、有视线 | 每目标一次；撞墙即停 | FirstVicissitudeBossEntity.java:1664-1705；VicissitudeRig.java:1302 |
| 远程兜底 `RANGED_FALLBACK` | `0.75 × D` | 3 枚直线弹，±10°，0.65 格/tick，寿命 120 | 16 → 16 → 28 | 60 tick | 距离 > 24 格 | 二阶段才有伤害 | FirstVicissitudeBossEntity.java:1424-1437；VicissitudeRig.java:1305 |
| 追踪球 `VicissitudeLightOrbEntity` | 一阶段 0（命中把目标压到 `min(血, 1)`） | 穿地形追踪，寿命 200 tick，同屏上限 2 | 随扇射生成 | — | 仅阶段一，`fanCount` 为奇数的那一次 | 压血，不造成伤害 | VicissitudeLightOrbEntity.java:39,70-77；FirstVicissitudeBossEntity.java:1455-1461 |
| 格挡实体 | `max(10, D)`，不走去重 | 身体扫掠框 | 每 tick | — | 二阶段、挡路的非玩家实体 | 顺手追加一次挥手动画 | FirstVicissitudeBossEntity.java:2858-2876 |
| 弹体上限 | 非追踪球 ≤ 48、追踪球 ≤ 2 | 96 格内计数 | — | — | 达上限直接跳过生成 | — | FirstVicissitudeBossEntity.java:90-91,1447-1458 |

**阶段时间轴**：DORMANT（首击转 PHASE_ONE）→ PHASE_ONE（1800/1400/1000/750 tick，全程免伤）→ TRANSITION 60 tick（第 45 tick 装武器）→ PHASE_TWO（可受伤）。死亡序列 80 tick（36 tick 翼根崩、60 tick 核心熄灭）(FirstVicissitudeBossEntity.java:73-75, 1064-1077, 1816-1830)。

**动作时长总表**（`VicissitudeRig.Action`，`释放帧 / 总时长`）：扇射 12/20、齐射 20/56、胸 30/50、环 40/60、横斩 8/20、竖劈 14/30、重击 24/50、冲刺 20/50、掷镰 16/28、收镰 0/10、兜底 16/28 (VicissitudeRig.java:1293-1305)。

**二阶段选招优先级**：掷镰（6–24 格）→ 冲刺（8–20）→ 特殊技轮转 → 兜底（>24 格并冷却好）；≤ 5.75 格进入近战：重击 → 横斩/竖劈 (FirstVicissitudeBossEntity.java:1273-1308,1311-1335)。

---

## 二、巨匠造物主（Demiurge Statue）

### 2.1 基础数值

| 项 | 值 | 来源类型 | 位置 |
| --- | --- | --- | --- |
| 实体注册尺寸 | 5.1 × 4.2（高度随觉醒/蛇行变化，最大 6.4） | 常量 | MaledictEntities.java:90；DemiurgeStatueEntity.java:869；DemiurgeSlither.java:10 |
| 生命池 | 400 / 600 / 800（easy+peaceful / normal / hard） | 常量表 | DemiurgeDifficulty.java:14-20 |
| 生命写入 | `onAddedToWorld` 先按世界难度设 `MAX_HEALTH` 基准值，再由账本捕获 | 运行时 | DemiurgeStatueEntity.java:132-145 |
| 护甲 | 10（属性默认 600 生命时） | 属性 | DemiurgeStatueEntity.java:127 |
| 移速 | 0.30 | 常量 + 属性 | DemiurgeStatueEntity.java:52,128 |
| `FOLLOW_RANGE` | 48 | 属性 | DemiurgeStatueEntity.java:128 |
| 保持距离 | 12 格；>12 才追击，>40 格用 1.25 倍速 | 常量 | DemiurgeCombatGoal.java:17,245-247 |
| 选招朝向锥 | 冲撞 70°、激光 105°；冲撞触发距离 17 格 | 常量 | DemiurgeCombatGoal.java:21,25-26 |
| 阶段一（未觉醒） | `AWAKENING_START < 0` 时免伤，需腐枯之星右击唤醒 | 逻辑 | DemiurgeStatueEntity.java:158-160,617-636 |
| 唤醒时长 | 100 tick（第 64 tick 吼叫） | 常量 | DemiurgeAwakening.java:7-8 |
| 站立 → 地面蛇行过渡 | 40 tick，之后才可战斗 | 常量 | DemiurgeSlither.java:7；DemiurgeStatueEntity.java:208-210 |

**难度表**（读**世界难度**）

| 世界难度 | 生命池 | 伤害倍率 | 对玩家伤害档（本 Boss 打人） | 位置 |
| --- | ---: | ---: | --- | --- |
| PEACEFUL | 400 | 0（不造成伤害） | 不出手 | DemiurgeDifficulty.java:16,24,55 |
| EASY | 400 | `demiurge.easyDamageMultiplier = 0.5` | LIGHT `lighterDamageMethod` | MaledictConfig.java:90；DemiurgeDifficulty.java:52 |
| NORMAL | 600 | `demiurge.normalDamageMultiplier = 1.0` | MEDIUM `mediumDamageMethod(..., false)` | MaledictConfig.java:91；DemiurgeDifficulty.java:53 |
| HARD | 800 | `demiurge.hardDamageMultiplier = 1.5` | FINAL `finalDamageMethod(..., finalNoRemove(), false)` | MaledictConfig.java:92；DemiurgeDifficulty.java:54 |

### 2.2 防御机制

| 机制 | 数值 / 公式 | 位置 |
| --- | --- | --- |
| 单次命中上限 | `真生命上限 × 0.03` | DemiurgeStatueEntity.java:152-155 |
| 绝对值（easy / normal / hard） | **12 / 18 / 24** | 上 × 400/600/800 |
| 一次请求的允许额度 | `上限 × 1.25` = 15 / 22.5 / 30 | VicissitudeBossEntity.java:45,148-150 |
| 部位权重 | 无部位表，恒 1.0（未覆盖 `incomingHitWeight`） | VicissitudeBossEntity.java:306-308 |
| 护甲 / 魔法减免路径 | 与第一化身同一实现：`BossDamage.resolve` → 10 甲 → 魔法吸收 → Forge 事件 | VicissitudeBossEntity.java:385-411；BossDamage.java:33-45 |
| 过甲后的实际单次上限 | 约 10.1 / 16.6 / 22.1（10 甲、0 韧性下的原版公式） | 推算自 DemiurgeStatueEntity.java:150-155 |
| 免伤阶段 · 未觉醒 | 唤醒前全程免伤（`AWAKENING_START < 0`，无固定时长，直到被打磨唤醒） | DemiurgeStatueEntity.java:158-160 |
| 免伤阶段 · 其它 | 无转场、无复活阶段；唤醒期不免疫 | DemiurgeStatueEntity.java:158-160 |
| 回血 | 无技能回血；只有脱战回血（下行） | VicissitudeBossEntity.java:427-440 |
| 同 tick 多通道合并 | 继承基类：同 tick 视为一批，只扣超过本批最大值的差额 | VicissitudeBossEntity.java:336-339,400-410 |
| 脱战回血 | 无目标、且无活着的 `lastHurtByMob` 时每 tick 回 5% 最大生命池（`HEAL_FRACTION_PER_TICK = 0.05F`） | BossRecovery.java:28,43-52；经 DemiurgeStatueEntity.java:646 → VicissitudeBossEntity.java:563-566 |
| 脱战回血 · 战斗记忆（工作区新增） | 最近 100 tick（`COMBAT_MEMORY_TICKS`）内有结算伤害就不算脱战 | BossRecovery.java:33,58-68；打点 VicissitudeBossEntity.java:421 |
| 脱战回血绝对值 | 20 / 30 / 40 每 tick（400 / 600 / 800 的 5%） | 由上两行相乘 |
| 身体断尾 | ≤75% 切 4 节、≤50% 切 8 节、≤25% 切 11 节（上限 11）；治疗不回长；阈值表见「不确定项 6」 | DemiurgeStatueEntity.java:454-464 |
| 死亡序列 | 80 tick（前 20 tick 熄光，26 tick 起落灰，80 tick 移除） | DemiurgeStatueEntity.java:68-73,916-918 |

> **文档与代码不一致（泪雨与横扫基础伤害）**：`docs/design/demiurge-combat.md:3` 写“冲撞、激光、泪雨和横扫的基础伤害分别为 7、6、5、9”。代码 config 默认值是 **7、6、8、10**（`MaledictConfig.java:72,74,76,78`）。以代码为准。
> `demiurge-combat.md:27` 的“每击上限为 3% 最大生命，简单/普通/困难约 10.1/16.6/22.1”与代码一致（3% → 12/18/24 → 过 10 甲 → 10.1/16.6/22.1）。

### 2.3 攻击 / 技能表

调度：`DemiurgeCombatGoal` 每 tick 一次；招式结束（age ≥ duration）后 `globalCooldown = 8`，另一招可用就换招；每招冷却 = 时长 + 附加（冲撞/吼叫/横扫 +22、激光 +46、石柱 +64）(DemiurgeCombatGoal.java:14,66-83,171-176)。

| 技能 | 名义伤害（config 基础 × 难度倍率） | 范围 / 半径 | 前摇 → 生效 → 总时长（tick） | 冷却 | 触发条件 | 附带效果 | 位置 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 冲撞 `CHARGE` | `demiurge.chargeDamage = 7.0` | 自身 5.1 宽全身扫掠 + 各段体节，膨胀 0.65；有效距离约 17 格 | 0–19 → 20–43 → 64；前 5 tick 加速到 0.8 格/tick，之后 10 tick 减速滑行 | 64 + 22 | 距离 ≤ 17 且方位在 70° 锥内 | 撞飞石柱；生效期暂停身体推挤 | DemiurgeAttack.java:8,47-52；DemiurgeStatueEntity.java:714-743；DemiurgeCombatGoal.java:105-107 |
| 激光 `LASER` | `demiurge.laserDamage = 6.0` / 每 10 tick 一拍 | 最长 40 格，方块与石柱截断，双眼各一条 | 0–23 → 24–83 → 100 | 100 + 46 | 距离 ≤ 40 且 105° 锥内 | `malum:silenced` 100 tick（不叠加） | DemiurgeAttack.java:9,20-22；DemiurgeStatueEntity.java:744-746,833-845 |
| 吼叫 `ROAR` | `demiurge.rainDamage = 8.0` / 每滴 | 头部中心半径 **10** 的圆盘，每 tick 8 滴，0.9 格/tick 下落，单滴寿命 30 | 0–19 → 20–89 → 110 | 110 + 22 | 当前目标（仅玩家）在头部 10 格水平内 | 收招后同一落区继续每 tick 4 滴，持续 **1200 tick**（60 s） | DemiurgeAttack.java:10,26；DemiurgeStatueEntity.java:60-64,753-772,847-865；DemiurgeTearEntity.java:19-20 |
| 蛇尾横扫 `SWEEP` | `demiurge.sweepDamage = 10.0` | 中后段蛇身，振幅 4.0 格（随断尾缩到 ≥35%），扫掠框膨胀 0.35/0.2/0.35 | 0–15 → 16–27 → 50 | 50 + 22 | 目标在可挥动骨节附近，有一侧可达 | 击退 0.35；碰到已升起石柱则使其强化并停摆 | DemiurgeAttack.java:14；DemiurgeSweep.java:15-36；DemiurgeStatueEntity.java:775-831 |
| 伪造物 `FALSE_CREATION` | 升起 `demiurge.pillarRiseDamage = 8.0`；普通爆 `pillarExplosionDamage = 8.0`；强化爆 `pillarEmpoweredDamage = 12.0` | 内环 3 根（半径 3.2–4.2）、外环 3 根（6.6–8.0）；普通爆半径 5.0、强化爆 5.5 | 0–13 → 第 14 tick 生成 → 48 | 48 + 64 | 距离 ≤ 36 且有可用布阵 | 石柱生命 `pillarHealth = 24`；升起击退 + 竖直弹起 0.85 | DemiurgeAttack.java:18；DemiurgePillarEntity.java:60-87；MaledictConfig.java:79-89 |
| 石柱被撞飞 | 落点/飞行中命中用 `pillarEmpoweredDamage = 12.0` | 1.7 格/tick 飞行 30 tick | — | — | 冲撞碰到可发射石柱 | 途中破坏方块、撞玩家立即强化爆 | DemiurgePillarEntity.java:77-78,441-484 |
| 石柱时序 | 预警 16 → 升起 8 → 待机 30 → 爆炸；强化引信 5 tick | — | 合计 54+ tick | — | — | 升起时每 tick 判定一次伤害 | DemiurgePillarEntity.java:60-63 |

**动作时长总表**：冲撞 64（0–19 / 20–43 / 44–63）、激光 100（0–23 / 24–83 / 84–99）、吼叫 110（0–19 / 20–89 / 90–109）、横扫 50（0–15 / 16–27 / 28–49）、石柱 48（0–13 / 14 生成 / 40–47）(DemiurgeAttack.java:7-18)。

**选招权重**（可调但不在 config 里）：冲撞 1.7（近）/ 1.2（远）、激光 0.8 / 1.6、吼叫 2.4、横扫首次强制、石柱首次强制否则 4.0；断尾 ≥4/≥8/≥11 时石柱再 ×1.5 / ×1.4 / ×1.25，冲撞 ×0.65，吼叫 ×1.1；首次使用 ×1.6；上一招权重归零 (DemiurgeCombatGoal.java:105-161)。

**移动 / 寻路**：每 5 tick 检查站位；缓弯转向半径与拆墙由 `DemiurgeMoveControl` 管；连续 6 次受阻 → 8 tick 前爬解缠；连续 8 次拆不掉 → 丢路径；24 tick 无位移 → 丢路径 (DemiurgeMoveControl.java:10-11,42-61；DemiurgeStatueEntity.java:499-519)。

---

## 三、腐枯凋灵（Weeping Wither）

### 3.1 基础数值

| 项 | 值 | 来源类型 | 位置 |
| --- | --- | --- | --- |
| 实体注册尺寸 | 1.08 × 4.2，渲染缩放 1.2 | 常量 | MaledictEntities.java:40；WeepingWitherEntity.java:50 |
| 生命池 | 400（**不随难度变化**） | 属性 | WeepingWitherEntity.java:106 |
| 护甲 | 8（固定属性；原版半血“护甲”是箭矢免疫，不是护甲值） | 属性 | WeepingWitherEntity.java:107 |
| 移速 | 原版 0.6，`travel` 每 tick ×1.1 → 等效 0.66 | 原版 + 常量 | WeepingWitherEntity.java:497-499 |
| `FOLLOW_RANGE` | 40（原版） | 原版 | 未覆盖，见「不确定项」 |
| 侧头视野/索敌 | 半径 20，排除亡灵与不可攻击目标 | 常量 | WeepingWitherEntity.java:74-76 |
| 祭坛活动范围 | 拴绳 24 格；自身出圈 → 归位；目标出圈 → 放目标；回到 20 格内解除归位 | 常量 | WeepingWitherEntity.java:209,212-218,312-317 |
| 免伤 / 回血 | 首次致死 → 1 HP + 重聚 60 tick → 80 HP + 强度 7 爆炸 | 常量 | WeepingWitherCombat.java:7；WeepingWitherEntity.java:578-601 |

**难度表**（不缩放生命与伤害；难度只改凋零时长、侧头行为与齐射危险弹）

| 世界难度 | 生命池 | 直击伤害倍率 | 附加凋零时长 | 其它 | 位置 |
| --- | ---: | --- | ---: | --- | --- |
| PEACEFUL / EASY | 400 | ×1 | 0（不施加） | 侧头不空放 | WeepingWitherSkull.java:78-81；WeepingWitherEntity.java:393 |
| NORMAL | 400 | ×1 | 300 tick（15 s） | 侧头空放阈值 15 | 同上 |
| HARD | 400 | ×1 | 1200 tick（60 s） | 半血 + 困难时骨刺改环形 | WeepingWitherSkillController.java:343-346 |
| 半血强化 | 不变 | 不变 | 不变 | 主头危险弹 15%、侧头 10%、空闲阈值 8、齐射固定 2 枚危险弹、云雾必试 | WeepingWitherEntity.java:392,435-437；WeepingWitherSkillController.java:350-358 |

### 3.2 防御机制

| 机制 | 数值 / 公式 | 位置 |
| --- | --- | --- |
| 单次命中上限 | `min(伤害, 最大生命 × 0.05)` | WeepingWitherEntity.java:513,543 |
| 绝对值 | **20**（400 × 5%） | 同上 |
| 二次截断 | `actuallyHurt` 再取一次 `min(finalAmount, max × 0.05)` | WeepingWitherEntity.java:543-544 |
| 护甲 / 魔法减免路径 | 走原版 `WitherBoss.hurt` → `BossDamage.resolve`（护甲 / 魔法吸收 / Forge 事件） | WeepingWitherEntity.java:539-540；BossDamage.java:33-45 |
| ChangeLib 前置 | `CombatProtection.incoming` 在 `actuallyHurt` 里先过一遍 | WeepingWitherEntity.java:533 |
| 无敌阶段 · 重聚 | 60 tick（`reformationTicks`），期间 `hurt` 返回 false、`heal` 直接返回、停止移动与攻击 | WeepingWitherCombat.java:7,30；WeepingWitherEntity.java:504-505,558-561,303-309 |
| 无敌阶段 · 出生 | 原版 `makeInvulnerable()` = 220 tick，期间每 10 tick 回 10 HP，结束后强度 7 爆炸 | WeepingWitherSummoning.java:40；WeepingWitherEntity.java:344-353 |
| 半血箭矢免疫 | 生命 ≤ 50% 后免疫所有 `AbstractArrow` 直击 | 原版 `WitherBoss.isPowered` / `hurt`，见「不确定项」 |
| 回血 · 脱战 | 无目标、且无活着的 `lastHurtByMob` 时每 tick 回 5% 最大生命 = **20**；重构期间 `heal` 直接返回 | BossRecovery.java:28,43-52；WeepingWitherEntity.java:294,558-561 |
| 回血 · 战斗记忆（工作区新增） | 最近 100 tick 内有结算伤害 / 复活完成时也不算脱战 | BossRecovery.java:33,58-68；WeepingWitherEntity.java:546,600 |
| 回血 · 原版自然 | 每 20 tick 回 1 HP；**最终阶段关闭** | WeepingWitherEntity.java:361-362 |
| 回血 · 增殖 | 命中“已有凋零”的有效目标回 3 HP，整只实体共用 40 tick 冷却 | WeepingWitherCombat.java:8,19-24；WeepingWitherEntity.java:573-575 |
| 回血 · 击杀 | 凋灵之首直接击杀目标回 5 HP | WeepingWitherSkull.java:75 |
| 重组 / 复活 | 首次致死 → 1 HP + 60 tick 重聚 → 80 HP + 爆炸；仅一次，最终阶段不再重聚 | WeepingWitherEntity.java:578-601 |
| 同 tick 多通道合并 | 同 tick 清零 `invulnerableTime`；各通道各自结算后只扣超过本批最大值的差额 | WeepingWitherEntity.java:502-527,544-554 |
| 死亡拒绝 | `CombatProtection.allowDeath` 不放行时直接吞掉死亡 | WeepingWitherEntity.java:580-581 |

### 3.3 攻击 / 技能表

调度：`WeepingWitherSkillController.tick` 由 `aiStep` 驱动；全局间隔 40 tick（最终阶段 30）；优先级 **D 脉冲 > E 召唤 > A 骨刺 > C 齐射**；D 可抢占 A/C 未释放的蓄力；C 施法期间抑制普通主头/侧头射击 (WeepingWitherSkillController.java:44-45,108-133,156-158)。

| 技能 | 名义伤害 | 范围 / 半径 | 前摇 → 命中帧 → 总时长（tick） | 冷却（常规 / 最终） | 触发条件 / 阶段 | 附带效果 | 位置 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 主头普通弹 | 直击 12；目标已有凋零 14 | 弹道直射，600 tick 后无爆炸消失 | — | 常规 40（原版 `RangedAttackGoal`）；最终 32 | 目标存活、有视线、出生无敌结束 | 命中后施加凋零（NORMAL 300 / HARD 1200 tick）；击杀回 5 HP | WeepingWitherCombat.java:13-17；WeepingWitherSkull.java:21,69-82；WeepingWitherEntity.java:365-383 |
| 侧头普通弹 | 同上 | 索敌 20 格，命中判定 900 格²(30 格) | — | 10–19 tick；瞄准射击 40–59 tick；最终阶段 ×0.8 | 侧头目标有效 | 半血后 10% 概率换成危险弹 | WeepingWitherEntity.java:391-428 |
| 侧头空放弹 | 同上 | 自身 ±10 / ±5 / ±10 随机方向 | — | 每次检查间隔 10–19 tick | NORMAL/HARD 且空闲计数 > 15（半血 8） | 必为危险弹 | WeepingWitherEntity.java:397-404 |
| A 骸骨迸裂 | 原版尖牙 **6.0** 魔法伤害 | 直线 5–7 根；环形 12 槽留两处相邻 60° 缺口 = 8 根（半径 3.0） | 16 tick 蓄力 → 释放即生成（`setWarmup(-7)`） | 280 / 200 | 距离 4–16、有站立地面、目标不在持续飞行 | 环形布局：最终阶段恒开，半血+困难也开 | WeepingWitherSkillController.java:34-35,290-302,398-484；EvokerFangs（原版）6.0F |
| B 腐血增殖 | 云雾内凋零 I 100 tick（无瞬间伤害） | 半径 2.5，等待 10 tick，总 200 tick；命中点 4 格内已有云雾则跳过 | 危险弹命中方块即触发 | 120 / 80 | 危险弹撞方块；半血前 25% 概率、半血后必试 | 深紫龙息粒子 | WeepingWitherSkillController.java:138-150,714-735 |
| C 幽骸齐射 | 每枚按上面两种直击伤害 | 9 枚，瞄准点随机 ±1.5 / ±1 / ±1.5，直线不追踪 | 16 蓄力，第 16 / 24 / 32 tick 各一轮（每轮 3 枚） | 400 / 300 | 距离 10–28 且有视线 | 半血后固定抽 2 枚危险弹；期间停射 | WeepingWitherSkillController.java:36-37,46-49,304-312,495-513 |
| D 腐朽脉冲 | **4.0** 魔法伤害 | 半径 **7**；击退强度 **1.5** | 12 tick 蓄力 → 释放 | 240 / 180 | 目标 5 格内持续 ≥10 tick；最高优先级 | 径向击退；自身召唤物免疫 | WeepingWitherSkillController.java:38-39,54-56,269-275,515-555 |
| E 残骸回响 | 无（召唤） | 自身 4–7 格地面，2 只 | 10 tick 召唤动作 | 600 / 480 | 战斗开始 8 s 后、目标 ≤20 格、存活召唤物 <2、有地面 | 召唤骷髅，寿命 600 tick | WeepingWitherSkillController.java:40-41,51-53,277-288,320-324 |
| 半血一次性召唤 | 无 | 4–7 格地面，3 只（受总数上限 5 限制） | 立即 | 仅一次 | 首次 ≤50% 生命 | 寿命 0（永久） | WeepingWitherSkillController.java:57,173-184 |
| 出生爆炸 | 原版爆炸强度 **7.0** | 以眼睛为中心 | 出生无敌结束时 | 一次 | `makeInvulnerable` | 1023 音效 | WeepingWitherEntity.java:385-389 |
| 拆方块 | — | 3×4×3 区域 | 受击后 20 tick 延迟 | — | 受击且 mobGriefing | 原版强度 | WeepingWitherEntity.java:475-487,520-524 |

**AI 节奏**：主头/侧头沿用原版凋灵时序，最终阶段整体提速（主头 40→32、侧头间隔 ×0.8）；技能冷却与全局间隔并行递减；读档保留剩余冷却与正在进行的施法 (WeepingWitherSkillController.java:742-804)。

---

## 四、召唤物：腐枯凋灵骷髅

| 项 | 值 | 位置 |
| --- | --- | --- |
| 实体 id | `maledict:weeping_wither_skeleton` | MaledictEntities.java:49-52 |
| 尺寸 | 0.7 × 2.4 | MaledictEntities.java:52 |
| 生命 | 26 | WeepingWitherSkeletonEntity.java:62 |
| 基础攻击属性 | 5.0（`finalizeSpawn` 里强行写回 5，压掉原版的 4） | WeepingWitherSkeletonEntity.java:63,90 |
| 实际攻击 | 5 + 原版石剑 +3 = **8**（石剑由原版凋灵骷髅装备流程给） | WeepingWitherSkeletonEntity.java:60-66；石剑数值属原版 |
| 移速 | 0.27 | WeepingWitherSkeletonEntity.java:64 |
| 护甲 | 2 | WeepingWitherSkeletonEntity.java:65 |
| 攻击方式 | 原版凋灵骷髅近战 + 命中附加凋零（原版时长），无独立技能树 | WeepingWitherSkeletonEntity.java:55-66；原版 `WitherSkeleton` |
| 目标规则 · 跟随召唤者 | 每 10 tick 重读一次召唤者目标；出生时直接继承 | WeepingWitherSkeletonEntity.java:44,180-188,199-200 |
| 目标规则 · 召唤者查找 | 先查加载实体，未加载则在 128 格内搜同 UUID | WeepingWitherSkeletonEntity.java:48,157-175 |
| 目标规则 · 永不敌对 | 任何凋灵（`WitherBoss`）、同批召唤物、自己 → `isWeepingAlly` 拦截 `setTarget` / `canAttack` / `isAlliedTo` | WeepingWitherSkeletonEntity.java:126-155 |
| 目标规则 · 友伤不成仇恨 | 被凋灵的弹、尖牙、脉冲蹭到也不转火（原版 `HurtByTargetGoal` 被覆盖） | WeepingWitherSkeletonEntity.java:135-145 |
| 目标规则 · 命令生成体 | 保留原版四个扫描（玩家/铁傀儡/海龟/猪灵），不设寿命、走原版战利品表 | WeepingWitherSkeletonEntity.java:72-81,204-215 |
| 生命周期 · E 版 | 600 tick（30 s），到时 `discard()`；不推进区块卸载时间 | WeepingWitherSkillController.java:52；WeepingWitherSkeletonEntity.java:195-198 |
| 生命周期 · 半血版 | 寿命 0 = 永久，随存档保留 | WeepingWitherSkillController.java:183 |
| 容量 | 总数上限 5；E 要求现存 < 2 | WeepingWitherSkillController.java:53,285 |
| 掉落 | 召唤版本无掉落、无装备、无经验 | WeepingWitherSkeletonEntity.java:204-209 |
| 清理 | Boss 最终死亡时清掉 128 格内归属自己的召唤物 | WeepingWitherSkillController.java:160-162,567-573 |

---

## 五、数值总表（可直接改的数字）

> 位置列最后一段是文件，`:` 后是行号。**加粗**表示 config 项（改 `config/maledict-common.toml`）。

### 第一化身

| 数值 | 当前值 | 位置 |
| --- | --- | --- |
| 阶段一预热 | 20 tick | FirstVicissitudeBossEntity.java:73 |
| 基础攻击槽间隔 | 30 tick | FirstVicissitudeBossEntity.java:76 |
| 转场时长 | 60 tick | FirstVicissitudeBossEntity.java:74 |
| 死亡序列 | 80 tick | FirstVicissitudeBossEntity.java:75 |
| 空场重置 | 100 tick | FirstVicissitudeBossEntity.java:89 |
| 齐射冷却 | 70 tick | FirstVicissitudeBossEntity.java:77 |
| 胸技冷却 | 90 tick | FirstVicissitudeBossEntity.java:78 |
| 环技冷却 | 80 tick | FirstVicissitudeBossEntity.java:79 |
| 横斩冷却 | 20 tick | FirstVicissitudeBossEntity.java:80 |
| 竖劈冷却 | 30 tick | FirstVicissitudeBossEntity.java:81 |
| 重击冷却 | 120 tick | FirstVicissitudeBossEntity.java:82 |
| 冲刺冷却 | 160 tick | FirstVicissitudeBossEntity.java:83 |
| 掷镰冷却 | 120 tick | FirstVicissitudeBossEntity.java:84 |
| 兜底齐射冷却 | 60 tick | FirstVicissitudeBossEntity.java:85 |
| 近战触发距离 | 5.75 格 | FirstVicissitudeBossEntity.java:92 |
| 掷镰区间 | 6–24 格 | FirstVicissitudeBossEntity.java:98-99 |
| 冲刺区间 / 距离 | 8–20 格 / 10 格 | FirstVicissitudeBossEntity.java:100-102 |
| 不还手距离 | 64 格 | FirstVicissitudeBossEntity.java:103 |
| 非追踪弹上限 | 48 | FirstVicissitudeBossEntity.java:90 |
| 追踪球上限 | 2 | FirstVicissitudeBossEntity.java:91 |
| 阶段一偏好距离 / 悬浮高度 | 7.0 / 2.5 格 | FirstVicissitudeBossEntity.java:179-180 |
| 二阶段稳定距离 / 悬浮 | 2.25 / 1.35 格 | FirstVicissitudeBossEntity.java:181-182 |
| 飞行速度上限（阶段一/二） | 0.38 / 0.52 | FirstVicissitudeBossEntity.java:183-184 |
| 飞行转向混合 | 0.2 | FirstVicissitudeBossEntity.java:185 |
| 拆方块间隔 | 10 tick | FirstVicissitudeBossEntity.java:187 |
| 卡住判定 | 40 tick / 0.25 格 | FirstVicissitudeBossEntity.java:188-189 |
| 脱困冷却 | 100 tick | FirstVicissitudeBossEntity.java:190 |
| 经验 | 1000 | FirstVicissitudeBossEntity.java:305 |
| 基础攻击属性 | 8.0 | FirstVicissitudeBossEntity.java:310 |
| 护甲 | 15.0 | FirstVicissitudeBossEntity.java:312 |
| `ATTACK_DAMAGE` 武器词条（COMPLETE / EXTREME） | 3.0 / 9.0 | FirstVicissitudeBossEntity.java:2944 |
| 扇形弹数 / 角度步长 | 5 枚 / 12° | FirstVicissitudeBossEntity.java:1368-1369 |
| 扇射弹速 / 寿命 | 0.45 格/tick / 80 tick | FirstVicissitudeBossEntity.java:1376 |
| 齐射波数 / 波间隔 | 3 波 / 8 tick | FirstVicissitudeBossEntity.java:1404-1406 |
| 兜底弹数 / 弹速 / 寿命 | 3 枚 / 0.65 / 120 tick | FirstVicissitudeBossEntity.java:1430-1433 |
| 弹体伤害倍率（二阶段） | 0.75 × D | FirstVicissitudeBossEntity.java:1446 |
| 胸技半径 | 2.0 格 | FirstVicissitudeBossEntity.java:2359 |
| 环技内外半径 / 缺口 | 3.0 / 6.0 格 / 60° | FirstVicissitudeBossEntity.java:2369 |
| 近战倍率（横/竖/重） | 1.0 / 1.25 / 1.75 | FirstVicissitudeBossEntity.java:1596-1603 |
| 冲刺推进 / 时长 | 10 格 / 12 tick | FirstVicissitudeBossEntity.java:1675-1680 |
| 非玩家伤害倍率 | 0.5 | FirstVicissitudeBossEntity.java:1894 |
| 嘲弄阈值（e⁻²） | 0.135335 | FirstVicissitudeBossEntity.java:1900 |
| 嘲弄冷却 | 200 tick | FirstVicissitudeBossEntity.java:1905 |
| 命中门间隔 | 20 tick | VicissitudeBossEntity.java:280-282 |
| 击杀尝试窗口 | 100 tick | VicissitudeBossEntity.java:287-289 |
| 难度生命池（SIMPLE/DIFFICULT/COMPLETE/EXTREME） | 500 / 750 / 1000 / 1500 | FirstVicissitudeBossEntity.java:3201-3204 |
| 阶段一时长（同上顺序） | 1800 / 1400 / 1000 / 750 tick | FirstVicissitudeBossEntity.java:3201-3204 |
| 每击上限比例（SIMPLE/DIFFICULT/COMPLETE/EXTREME） | 0.05 / 0.03 / 0.01 / 0.01 | FirstVicissitudeBossEntity.java:2416-2420 |
| 部位权重（躯干/头/翼根/外翼） | 1.0 / 1.25 / 0.75 / 0.5 | VicissitudeRig.java:74-80 |
| 请求允许额度倍数 | 1.25 | VicissitudeBossEntity.java:45 |
| 剑带内沿 / 半宽 / 容差 | 0.6 / 1.25 / 0.01 格 | VicissitudeRig.java:898,905,911 |
| **engagementRange** | **96.0** | MaledictConfig.java:147 |
| **damageRange** | **48.0** | MaledictConfig.java:155 |
| **adaptationLevel** | **2** | MaledictConfig.java:167 |

### 巨匠造物主

| 数值 | 当前值 | 位置 |
| --- | --- | --- |
| 生命池（easy/normal/hard） | 400 / 600 / 800 | DemiurgeDifficulty.java:14-20 |
| 护甲 | 10 | DemiurgeStatueEntity.java:127 |
| 移速 | 0.30 | DemiurgeStatueEntity.java:52 |
| `FOLLOW_RANGE` | 48 | DemiurgeStatueEntity.java:128 |
| 每击上限比例 | 0.03 | DemiurgeStatueEntity.java:154 |
| 冲撞命中膨胀 | 0.65 格 | DemiurgeStatueEntity.java:56 |
| 环境雨持续 | 1200 tick | DemiurgeStatueEntity.java:60 |
| 环境雨密度 | 4 滴/tick | DemiurgeStatueEntity.java:64 |
| 死亡序列 | 80 tick | DemiurgeStatueEntity.java:68 |
| 觉醒时长 / 吼叫刻 | 100 / 64 tick | DemiurgeAwakening.java:7-8 |
| 站立→地面过渡 | 40 tick | DemiurgeSlither.java:7 |
| 蛇行周期 / 每周期格数 / 站高 | 60 tick / 8 格 / 6.4 | DemiurgeSlither.java:8-10 |
| 招式时长表 | 64 / 100 / 110 / 50 / 48 | DemiurgeAttack.java:7-18 |
| 激光间隔 / 射程 / 沉默 | 10 tick / 40 格 / 100 tick | DemiurgeAttack.java:20-22 |
| 泪雨半径 | 10 格 | DemiurgeAttack.java:26 |
| 冲撞速度峰值 | 0.8 格/tick | DemiurgeAttack.java:50 |
| 收招空档 | 8 tick | DemiurgeCombatGoal.java:14 |
| 站位检查间隔 | 5 tick | DemiurgeCombatGoal.java:15 |
| 石柱重选间隔 | 10 tick | DemiurgeCombatGoal.java:16 |
| 保持距离 | 12 格 | DemiurgeCombatGoal.java:17 |
| 冲撞触发距离 / 锥角 | 17 格 / 70° | DemiurgeCombatGoal.java:21,25 |
| 激光锥角 | 105° | DemiurgeCombatGoal.java:26 |
| 招式冷却附加（冲/吼/扫/激/柱） | 22 / 22 / 22 / 46 / 64 | DemiurgeCombatGoal.java:171-176 |
| 横扫时序 | 16 / 28 / 50 tick | DemiurgeSweep.java:15-17 |
| 横扫振幅 / 最短缩放 | 4.0 格 / 35% | DemiurgeSweep.java:25,29 |
| 横扫判定膨胀 | 0.35 / 0.2 / 0.35 | DemiurgeSweep.java:36 |
| 石柱时序（预警/升起/待机/强化引信） | 16 / 8 / 30 / 5 tick | DemiurgePillarEntity.java:60-63 |
| 石柱爆炸半径（普通/强化） | 5.0 / 5.5 格 | DemiurgePillarEntity.java:70-71 |
| 石柱击飞速度 / 时长 | 1.7 格/tick / 30 tick | DemiurgePillarEntity.java:77-78 |
| 石柱数量 / 半径 | 内 3 根 3.2–4.2、外 3 根 6.6–8.0 | DemiurgePillarEntity.java:84-87 |
| 泪滴寿命 / 下落速度 | 30 tick / 0.9 格/tick | DemiurgeTearEntity.java:19-20 |
| 解缠阈值 | 6 次受阻 / 8 tick | DemiurgeMoveControl.java:10-11 |
| **chargeDamage** | **7.0** | MaledictConfig.java:72 |
| **laserDamage** | **6.0** | MaledictConfig.java:74 |
| **rainDamage** | **8.0** | MaledictConfig.java:76 |
| **sweepDamage** | **10.0** | MaledictConfig.java:78 |
| **pillarRiseDamage** | **8.0** | MaledictConfig.java:80 |
| **pillarExplosionDamage** | **8.0** | MaledictConfig.java:82 |
| **pillarEmpoweredDamage** | **12.0** | MaledictConfig.java:84 |
| **pillarHealth** | **24** | MaledictConfig.java:89 |
| **easy / normal / hard DamageMultiplier** | **0.5 / 1.0 / 1.5** | MaledictConfig.java:90-92 |

### 腐枯凋灵

| 数值 | 当前值 | 位置 |
| --- | --- | --- |
| 生命 / 护甲 | 400 / 8 | WeepingWitherEntity.java:106-107 |
| 位移加成 | ×1.1 | WeepingWitherEntity.java:497 |
| 每击上限比例 | 0.05（= 20） | WeepingWitherEntity.java:513,543 |
| 重聚时长 | 60 tick | WeepingWitherCombat.java:7 |
| 重聚后生命 | 80 | WeepingWitherEntity.java:598 |
| 增殖冷却 / 回血 | 40 tick / 3 HP | WeepingWitherCombat.java:8；WeepingWitherEntity.java:575 |
| 直击伤害公式 | `8×1.25 (+min(2, 4)) + 2` = 12 / 14 | WeepingWitherCombat.java:13-17 |
| 击杀回血 | 5 HP | WeepingWitherSkull.java:75 |
| 自然回血 | 1 HP / 20 tick（最终阶段关闭） | WeepingWitherEntity.java:361-362 |
| 出生无敌 / 回血 | 220 tick / 10 HP per 10 tick | WeepingWitherSummoning.java:40；WeepingWitherEntity.java:350-351 |
| 出生爆炸强度 | 7.0 | WeepingWitherEntity.java:386 |
| 主头冷却（常规/最终） | 40（原版）/ 32 | WeepingWitherEntity.java:381 |
| 主头危险弹概率 | 0.001 / 半血 0.15 | WeepingWitherEntity.java:440-442 |
| 侧头间隔 | 10–19 / 瞄准 40–59；最终 ×0.8 | WeepingWitherEntity.java:396,410,425-428 |
| 侧头空闲阈值 | 15 / 半血 8 | WeepingWitherEntity.java:397 |
| 侧头瞄准危险弹概率 | 0.10 | WeepingWitherEntity.java:409 |
| 侧头索敌半径 | 20 格 | WeepingWitherEntity.java:74,415 |
| 祭坛拴绳 / 解除 | 24 / 20 格 | WeepingWitherEntity.java:209,314 |
| 归位寻路间隔 | 10 tick | WeepingWitherEntity.java:698 |
| 骷髅弹寿命 | 600 tick | WeepingWitherSkull.java:21 |
| 凋零时长（困难/普通/简单） | 1200 / 300 / 0 tick | WeepingWitherSkull.java:78-79 |
| 全局间隔（常规/最终） | 40 / 30 tick | WeepingWitherSkillController.java:44-45 |
| 骨刺冷却（常规/最终） | 280 / 200 tick | WeepingWitherSkillController.java:34-35 |
| 齐射冷却 | 400 / 300 tick | WeepingWitherSkillController.java:36-37 |
| 脉冲冷却 | 240 / 180 tick | WeepingWitherSkillController.java:38-39 |
| 召唤冷却 | 600 / 480 tick | WeepingWitherSkillController.java:40-41 |
| 云雾冷却 | 120 / 80 tick | WeepingWitherSkillController.java:42-43 |
| 蓄力时长（骨刺/齐射/脉冲/召唤） | 16 / 16 / 12 / 10 tick | WeepingWitherSkillController.java:46-51 |
| 齐射轮间隔 / 总时长 | 8 / 32 tick | WeepingWitherSkillController.java:48-49 |
| 召唤寿命（E 版） | 600 tick | WeepingWitherSkillController.java:52 |
| 召唤总上限 / 半血波次 | 5 / 3 只 | WeepingWitherSkillController.java:53,57 |
| 脉冲半径 / 伤害 / 击退 | 7 格 / 4.0 / 1.5 | WeepingWitherSkillController.java:54-56 |
| 战斗开始到可召唤 | 160 tick | WeepingWitherSkillController.java:58 |
| 半血前齐射危险弹概率 | 0.001 | WeepingWitherSkillController.java:62 |
| 骨刺触发距离 | 4–16 格 | WeepingWitherSkillController.java:297 |
| 尖牙根数 | 5–7（直线）/ 8（环形） | WeepingWitherSkillController.java:437,418-420 |
| 环形缺口 | 12 槽去掉 4 槽 = 两处相邻 60° | WeepingWitherSkillController.java:418-421 |
| 齐射触发距离 | 10–28 格 | WeepingWitherSkillController.java:311 |
| 脉冲触发 | 5 格内持续 10 tick | WeepingWitherSkillController.java:195-199,270 |
| 召唤触发半径 / 现存上限 | 20 格 / 2 只 | WeepingWitherSkillController.java:283-285 |
| 云雾半径 / 持续 / 等待 / 凋零 | 2.5 / 200 / 10 / 100 tick | WeepingWitherSkillController.java:725-730 |
| 云雾相邻判定 | 4 格 | WeepingWitherSkillController.java:715 |
| 半血前云雾概率 | 0.25 | WeepingWitherSkillController.java:143 |
| 召唤骷髅属性 | 26 / 5 / 0.27 / 2 | WeepingWitherSkeletonEntity.java:62-65 |
| 召唤骷髅跟目标间隔 / 搜主半径 | 10 tick / 128 格 | WeepingWitherSkeletonEntity.java:44,48 |

---

## 六、不确定项

1. **SIMPLE / DIFFICULT 的武器攻击加成（第一化身）**。代码只把武器 `MAINHAND` 属性烘焙进实体（`FirstVicissitudeBossEntity.java:335-358`），基础 8.0 之上加多少取决于 Malum 的染魂钢镰刀与救赎之锋物品数据，**本仓库里没有该数值**。两份文档互相矛盾：`01_SPEC.md:39-40` 记 +5 / +9（D = 13 / 17），`06_DAMAGE_AND_DPS.md:14` 记 SIMPLE 实际是 11（+3）。COMPLETE / EXTREME 可确定是 11.0 / 17.0（`FirstVicissitudeBossEntity.java:2944`）。
2. **原版继承值没有仓库行号**：腐枯凋灵的 `MOVEMENT_SPEED 0.6`、`FLYING_SPEED 0.6`、`FOLLOW_RANGE 40`、出生无敌 220 tick、半血箭矢免疫、`EvokerFangs` 的 6.0 点魔法伤害，都在 Minecraft 1.20.1 本体（`WitherBoss.java` / `EvokerFangs.java`），不在 `src/main` 里。上文按原版行为记录，未给行号。
3. **召唤骷髅的“石剑 +3”**：石剑由原版 `WitherSkeleton.finalizeSpawn` 装备，+3 来自原版 `SwordItem` 的 `ATTACK_DAMAGE` 修饰符，仓库里只有“基础攻击写回 5”（`WeepingWitherSkeletonEntity.java:90`）。
4. **`CombatProtection` 内部**：`bindOwned` 的额度语义、`defenseGrade` 的实际判定、`guard` 的在途请求处理都在 ChangeLib（`org.brahypno.changelib.HealthHelper.CombatProtection`），仓库里只有调用点。本文只记录调用点给出的数值（grade、额度），未展开库内部实现。
5. **`DamageProbe.lighter/medium/finalDamageMethod` 的实际扣血比例**：属于 ChangeLib，仓库里只有选择逻辑（`FirstVicissitudeBossEntity.java:1643-1646`、`DemiurgeDifficulty.java:51-56`）。
6. **巨匠断尾档位的具体节数**：`DemiurgeBodyLength.cutFor` 的阈值表本文只转述设计文档（`demiurge-combat.md:43-48`：>75% 0 节、≤75% 4 节、≤50% 8 节、≤25% 11 节），`DemiurgeBodyLength.java` 未逐一展开核对到行号。
7. **`docs/design/first-vicissitude/06_DAMAGE_AND_DPS.md` 的 DPS 表**：其模拟器输入已过时（该文 13 行自述“不能直接拿来调平衡”），本文不引用其 DPS 结论，只引用可被代码验证的时序与倍率。
8. **腐枯凋灵侧头“空放”阈值**：`idleSideShots[side]++ > idleThreshold` 是**严格大于**（`WeepingWitherEntity.java:399`），所以常规档实际是第 17 次之后，而非文档写的 15；半血档是第 9 次之后。文档 `weeping-wither.md:95` 写“侧头空闲计数阈值 8”，与代码的 `>` 语义差一。
