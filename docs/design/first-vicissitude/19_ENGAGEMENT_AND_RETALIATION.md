# 19 — 仇恨、参战者与 UUID 清理

2026-09-20 用户实测反馈（第十五轮补充）：a0d9c23 更新了无常的形态与攻击方式之后，
「看起来有些没 hook 上」——在游戏里「它打监守者没动画」。随后用户给出要求：

1. 它会对受击还手，**即使对方不是参战者，这是对的**；但「没有动画」不对。
2. **宠物、召唤物这些有主人的，应该把主人加入参战者。**
3. 也要考虑**玩家拖着它、周围召唤一堆僵尸攻击它**的情况。

用户在看过第一版实现后明确：**「我的需求和原版的仇恨系统差不了太多」**，
否掉了"创造/旁观出手也会被追打"，并追问**「隔着墙打它会反击吗？难道会什么也不干吗？」**，
同时要求**「记得正确清理 UUID」**。本文记录按这些要求落地之后的最终规则。

## 一、现场取证：那不是没 hook，是「没有对手」

翻当时那份存档（`run/saves/新的世界 (1)`，20:57:54 存盘）里那只无常的 NBT：

| 键 | 值 | 说明 |
| --- | --- | --- |
| `VicissitudeStage` | 1 | 停在 PHASE_ONE |
| `PhaseOneTicks` / `PhaseOneDuration` | 460 / 1400 | 困难档一阶段只走了 1/3 |
| `VicissitudeDifficulty` | `difficult` | 困难档 |
| `ActionSequence` / `BaseSlotIndex` / `SpecialRotator` | 17 / 9 / 5 | **它真的放了 17 个动作** |
| `PhaseOneTargets` | 空 | 存盘时一个参战者都没有 |
| `PhaseOneAnnouncements` | 1 条 | 有过一名玩家参战 |

同一次会话的聊天记录里有一句「Dev被切成了两半」，那是 Malum
`death.attack.scythe_sweep` 的死亡消息，而无常的胸/环伤害用的正是
`DamageTypeRegistry.SCYTHE_SWEEP`——**它出手过、动画链路是通的，玩家死在它的技能下**。

卡住的链条是：参战者只能靠"先打它一下"进来（`onIncomingAttack` 里
`phaseOneTargets.add` 是唯一入口），玩家阵亡后被 `forgiveDeadPlayers`（默认开）移出名单，
随后切到创造模式又被完全忽略，名单清空 → `currentPhaseOneTarget()` 返回 null →
`tickPhaseOneCombat()` 直接 return → **零动作 = 零动画**；`phaseOneTicks` 到点后它还会
自己走进二阶段，而二阶段只认玩家，之后就永久是一只雕像。

结论：形态与攻击都挂上了，缺的是**「谁算对手、没有对手时怎么办」这套规则**。

## 二、与原版仇恨系统的对应

| 原版 | 无常 |
| --- | --- |
| `HurtByTargetGoal`：谁打我我打谁（`lastHurtByMob`），**不检查视线** | `onIncomingAttack` 把攻击者写进参战名单，**隔着墙打它也照样结仇还手**；参战名单就是它可以打的人 |
| 驯服生物的攻击算在主人头上（`lastHurtByPlayer`） | `ownerOf`（`OwnableEntity#getOwnerUUID`）把主人一起写进名单；连非玩家主人也算，比原版更宽 |
| 创造与旁观不是目标 | `isIgnoredPlayer` 在**入口**挡掉：他们出手不算数、不结仇、也不会被追打 |
| 怪物优先盯玩家（`NearestAttackableTargetGoal`） | 选目标玩家优先：一阶段轮换与二阶段挑选都先看玩家，再轮到宠物/召唤物/别的生物 |
| 挨打就醒，不需要先看你一眼 | `DORMANT` 的像被任何人打中（含隔墙）都会进入一阶段 |

只剩两条属于本遭遇战自己的规则（不是原版）：

1. **交战半径**：目标超出 `firstVicissitude.engagementRange`（默认 12 格）就不追，原位悬停。
2. **没有对手就回到未参战**：见第三节第 4 条。

## 三、具体改动

### 1. 受击即结仇，不挑视线（创造与旁观除外）

- `onIncomingAttack` 不再有任何视线判断：**打它的人一律进名单**，
  守者、僵尸、宠物、召唤物都算；隔着墙、隔着地形打中它，它一样会转过来还手。
- **创造与旁观玩家的例外保持不变**：入口就挡掉，出手不算数、不会被追打
  （两个 `register*Participant` 的第一道判断就是 `isIgnoredPlayer`）。
  想在创造模式下看它动手，请让手下的生物去打它。
- 「不扫伤围观玩家」这条规则靠入口保证：名单只由有效攻击者构成，没出过手的旁观者永远进不来。

### 2. 宠物与召唤物的归属：主人一起参战

- 新增 `ownerOf`：按 `OwnableEntity#getOwnerUUID` 解析主人（不用 `getOwner()`，那个只认玩家，
  而主人也可能是别的生物）。宠物或召唤物打过来时，**它自己和主人都进名单**；
  第一次还手优先主人（`preferredTarget`）——这一条比原版更进一步：原版只把主人记成
  `lastHurtByPlayer`，目标仍然是那只宠物。
- 主人若是创造/旁观，则照上面的例外被挡掉，只剩宠物参战。

### 3. 二阶段也认非玩家参战者

- `phaseTwoPlayers` 改名 `phaseTwoParticipants`（存档键仍是 `PhaseTwoPlayers`，旧档兼容）：
  一阶段名单在转场时**整体**带过来，宠物、召唤物、别的生物都能一路打到二阶段；
  `isValidCombatParticipant` 不再要求 `ServerPlayer`。
- **选目标玩家优先**：先在没有被无视的玩家参战者里挑血最多的，没有才轮到非玩家。
  这就是「玩家拖着它、周围一堆僵尸」的答案——僵尸各自参战、都会被招呼，
  但只要玩家还在名单里，无常盯的仍然是人。一阶段轮换用同一优先级
  （`orderedPhaseOneTargets`：玩家排前、其余排后）。
- 转场时创造/旁观玩家不带走，也不没收饰品。

### 4. 没有对手就回到「未参战」

- `EMPTY_ENCOUNTER_RESET_TICKS = 100`：一阶段连续 5 秒场上没有活着的对手，
  阶段回到 `DORMANT`、解除阶段时长锁、计时清零（真生命、难度、武器、掉落都不动），
  同时把名单与死亡记录一起清空。
- 没有对手包括：名单为空（实测那种：唯一参战者阵亡并被宽恕）、只剩创造/旁观玩家
  （打到一半切了模式）、剩下的都已经死掉或不在场。
- 效果：不再出现"空转阶段计时、自己走进二阶段变成雕像"；重新打它一次就能从头再打一场。
  二阶段不做这个回退（升级形态是永久的），但任何新的攻击者都会重新参战。

### 5. UUID 清理

每 10 tick（`forgetDeadParticipants`）与回退时（`tickEmptyEncounter`）各清一次，
规则统一为「**不在场就退出名单**」：解不开 UUID（离线、区块没加载、已经不在世界上）、
不在这个维度、已经死了，都算不在场；玩家额外走原版死亡计数规则
（`forgiveDeadPlayers` 决定阵亡后是原谅还是继续记仇）。

| 持有者 | 内容 | 清理时机 |
| --- | --- | --- |
| `phaseOneTargets` | 一阶段参战者 | 死亡/离线/换维度/被宽恕/区块与实体都没了 → 移出；回退未参战时整表清空 |
| `phaseTwoParticipants` | 二阶段参战者 | 同上；转场时整表重建；回退时清空 |
| `targetPlayerDeaths` / `phaseTwoPlayerDeaths` | 玩家阵亡计数 | 随对应名单条目一起删除；回退时清空 |
| `announcedPlayerDeaths` | 每名玩家每命播一次的开场白 | 随一阶段名单条目一起删除；保留键的数量上限是打过它的玩家数 |
| `roundDamagedTargets` / `dashHitTargets` | 本轮已命中的目标 | 每次起手/每次冲刺清空 |
| `confiscated` | 没收但还没还的饰品 | 交付成功后移出；Boss 真死时整体转入世界账本 |
| `VicissitudeVitalityLedger`（世界级） | 每只无常的真生命 | **故意不清理**：死掉的 UUID 留着，旧实体副本才无法复活（见 `VicissitudeVitalityTest`） |
| `VicissitudeCurioLedger`（世界级） | 待交付饰品 | 交付后移出；离线玩家的饰品由登录/重生/克隆钩子继续送 |

离线玩家同样退出名单：饰品返还走账本 + 登录钩子（`VicissitudeCurioReturns`），
不依赖这份名单，所以这里可以放心清干净，不留悬空 UUID。

## 四、实现位置

- `common/entity/FirstVicissitudeBossEntity`：
  `onIncomingAttack`、`ownerOf`、`registerPhaseOneParticipant`、`registerPhaseTwoParticipant`、
  `preferredTarget`、`completeTransition`、`isValidCombatParticipant`、
  `selectBalancedPhaseTwoTarget`/`healthiestPhaseTwoTarget`、`orderedPhaseOneTargets`、
  `isIgnoredPlayer`、`isValidPhaseOneTarget`、`forgetDeadParticipants`/`updateParticipant`/
  `dropAbsentParticipant`、`tickEmptyEncounter`/`hasPossibleOpponent`、
  `EMPTY_ENCOUNTER_RESET_TICKS`。
- 存档格式未变（`PhaseOneTargets`、`PhaseTwoPlayers` 都是 UUID 列表，非玩家同样是 UUID）。

## 五、验证

| 命令 | 结果 |
| --- | --- |
| `.\gradlew compileJava test --offline` | BUILD SUCCESSFUL；7 个测试类全绿 |
| `.\gradlew runData --offline` | 本轮不动掉落表与资源，无产出变化 |

游戏内未验证（本轮改动全部依赖真实实体与 AI，没有可离线断言的部分）：

1. 守者/僵尸从一阶段一路打到二阶段（转场后不再变雕像）——本轮针对"没动画"的主修。
2. 隔着墙打它 → 一样会结仇、转过来还手，不会站着不动。
3. 宠物/召唤物打它 → 主人被追；主人跑远只剩宠物时，目标回到宠物。
4. 创造/旁观玩家出手 → 不算参战、不会被追打；站在旁边也不会被扫到。
5. 玩家拖着它 + 周围一群僵尸 → 僵尸各自参战，但只要玩家还在名单里，目标仍是玩家。
6. 唯一参战者阵亡并被宽恕、离线、或切开创造 → 5 秒后回到未参战，名单与死亡记录同时清空。
