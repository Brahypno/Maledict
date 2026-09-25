# 测试示例

测试代码位于 `src/test/java`，目前有十个类：

| 类 | 覆盖 |
| --- | --- |
| `common/entity/VicissitudeVitalityTest` | 无常 Boss 的真生命、击杀计数与世界存档 |
| `common/entity/DamageAdaptationTest` | 无常的适应效果：按伤害消息记账与自然指数递减（纯数值） |
| `common/curio/HalfHealthTest` | 启蒙之年的半血判定（纯数值） |
| `common/curio/EnlightenmentLevelTest` | 启蒙之年护符 NBT 上的等级读取（纯数值） |
| `common/effect/BlessedRegenerationTest` | 生命祝福的额外自然回血量（纯数值） |
| `common/effect/RipeningBonusTest` | 熟成之赐的经验加成取整（纯数值） |
| `rig/VicissitudeRigTest` | 无常骨架的动作曲线、收尾连续性、刀刃可达范围与高度、手部间距 |
| `rig/VicissitudeFeatherShedTest` | 转场落羽的错时曲线与二阶段碰撞组 |
| `data/FirstVicissitudeSpiritDataTest` | 无常交给 Malum 的精魂表（手写资源，八种各 6 + 幽影 1） |
| `common/item/IncursusBladeBlinkTest` | 无常之刃物品栏眨眼的纯换算规律 |

每个 `@Test` 方法对应一个完整场景，使用 JUnit 5 的断言检查结果。
`VicissitudeRigTest` 里那几条「动作最后一帧必须回到静息」「刀刃可达范围必须落在起手距离内」
「手不能进入躯干」的断言，是 `docs/design/first-vicissitude/02_ANIMATION_AND_HITBOX.md`
那份规格的可执行版本；改动作之前先看那一篇。

## 运行

在项目根目录执行：

```powershell
.\gradlew.bat test
```

Gradle 会自动发现并运行 `src/test/java` 下的 JUnit 5 测试。首次运行需要下载测试依赖，缓存齐全后可加 `--offline`。
HTML 报告位于 `build/reports/tests/test/index.html`，日志位于 `build/tests`。
临时存档由 JUnit 的 `@TempDir` 创建和自动清理。
本项目已取消 `check` 对 `test` 的依赖；`check` 和 `build` 不会自动运行测试，需要显式执行 `test`。

只运行一个测试类：

```powershell
.\gradlew.bat test --tests '*VicissitudeVitalityTest'
```

## 无常 Boss 的可运行示例

| 方法 | 操作及预期 |
| --- | --- |
| `thirdDistinctKillBypasses` | 同一 tick 连续尝试 21 次只计一次；后续两个 tick 各尝试一次，第三次真生命归零。 |
| `expiredSequenceRestarts` | 相邻尝试间隔为 100 tick 时继续累计，超过 100 tick 则重新从一次开始。 |
| `ordinaryWoundsDoNotResetTheSequence` | 受伤产生扣血和冷却；窗口内的受伤、治疗不会清除已经累计的 kill 次数。 |
| `persistenceKeepsWoundsAndDeadIdentities` | 写入真实 `.dat` 文件后重载，伤势、最大生命、冷却和 kill 次数保持；不同 UUID 分开存储，死亡记录仍为零。 |
| `entityNbtCannotRewriteTheLedgerOrBoundCapability` | 修改 Capability 导出的 NBT 不影响已绑定副本及世界记录；入场绑定时以世界记录覆盖伪造的加载值。 |

这些例子直接验证状态、文件存储和 Capability 镜像的序列化策略，不启动 Minecraft 服务端。

## 启蒙之年的半血判定

`HalfHealthTest` 只测 `HalfHealth` 这一个纯数值函数，不启动 Minecraft。它钉住的是那条
tooltip 的字面规则「攻击半血生物时触发魂息虚空」——与监视者项链的「攻击满血生物」相对：

| 方法 | 操作及预期 |
| --- | --- |
| `aWoundedCreatureCounts` | 40/100 算半血生物。 |
| `landingExactlyOnTheLineCounts` | 50/100 触发：恰好半血算在内。 |
| `aHealthyCreatureDoesNotCount` | 50.1/100、100/100 都不触发。 |
| `aWoundedCreatureCountsAgainAndAgain` | 50、49、1 都成立：只看出手时的血量。 |
| `aDeadCreatureDoesNotCount` | 0 与负血量都不触发。 |
| `aTargetWithNoMaximumHealthDoesNotCount` | 上限为 0 时不触发。 |
| `theLineFollowsTheMaximumHealth` | 上限 10 时线在 5，上限 500 时线在 250。 |

限流那道 `SpiritVoidCooldownCapability`（100 tick，照着监视者项链配的）没有单测：
它是 capability 上的一个自减计数器，真值只能靠游戏内验证。

## 启蒙之年的饰品等级

`EnlightenmentLevelTest` 同样不启动 Minecraft，只测「护符 NBT 上那个 `EnlightenmentLevel`
怎么读」。这个数同时当击杀给出的启蒙之年与黑暗年代的 `amplifier` 用——也就是
「取决于饰品的 NBT，fallback 0 级」这条规则：

| 方法 | 操作及预期 |
| --- | --- |
| `aStackWithoutNbtFallsBackToZeroLevel` | 没有 NBT 的护符读作 0 级。 |
| `aStackWithoutTheKeyFallsBackToZeroLevel` | 有 NBT 但没有这个键，同样 0 级（普通途径拿到的护符就是这样）。 |
| `anExplicitLevelIsRead` | 写 1、2、9 就读出 1、2、9。 |
| `levelZeroReadsAsZero` | 显式写 0 与没有键是同一个结果。 |
| `aNegativeLevelFallsBackToZero` | 负数夹回 0：等级乘数是 `amount * (amplifier + 1)`，负等级会把效果反过来。 |
| `aKeyOfAnotherTypeFallsBackToZero` | 键写成字符串时当没写处理，不抛异常。 |

黑暗年代的传染（`AgeOfEnlightenmentEvents#onDarknessBearerHurt`）没有单测：它要真事件、
真实体和真世界。那一段的规则是「受击者身上有黑暗年代 + 伤害源身上有启蒙之年 →
黑暗年代跳到受击者附近最近的一名敌人，等级取伤害源的启蒙之年等级」，只能在游戏内验证。

## 无常的精魂表

`FirstVicissitudeSpiritDataTest` 读的是手写资源
`src/main/resources/data/maledict/spirit_data/entity/first_vicissitude.json`——Malum 的逐实体
精魂掉落数据，提尔锋按它算额外伤害。它不是 runData 产物，写错键名或数量不会有编译错误，
只会在游戏里静静地少掉奖励，所以用测试钉住本轮需求的字面值：

| 方法 | 操作及预期 |
| --- | --- |
| `theBossIsRegisteredUnderItsOwnRegistryName` | `registry_name` 是 `maledict:first_vicissitude`。 |
| `thePrimaryTypeIsARealSpirit` | `primary_type` 必须是九种真精魂之一，不能是 Malum 认不出的拼写。 |
| `eightSpiritsAtSixEachPlusOneUmbral` | 八种精魂各 6 枚，外加幽影 1 枚，不重不漏。 |
| `theTotalSpiritCountIsFortyNine` | 合计 49 点灵魂强度（提尔锋那条公式的输入，即 98 点额外伤害）。 |
| `umbralIsHandedOutExactlyOnce` | 幽影只有一枚：它是第十六轮追加的第九种，不跟着八种凑 6。 |

资源是否真的被 Malum 加载（`spirit_data` 目录、主键、命名空间）只能进游戏验证。

## 无常的适应效果

`DamageAdaptationTest` 只测 `DamageAdaptation` 这个纯记账类，不启动 Minecraft。
需求是「记录 damage message，已记录的 message 再次命中时伤害指数递减；默认适应二、
可配置」，测试钉住的是换算与淘汰规则（适应只按次数，不带时限）：

| 方法 | 操作及预期 |
| --- | --- |
| `theFirstHitOfAMessageIsFullDamage` | 没见过的消息第一下全额，并就此记上一笔。 |
| `aRecordedMessageDecaysOnTheNaturalExponential` | 同一消息第 2/3/4 下依次是 e⁻¹、e⁻²、e⁻³。 |
| `eachMessageKeepsItsOwnCount` | 适应二是两格各自记账：另一种消息第一次来仍是全额。 |
| `aThirdMessageReplacesTheOldestRecord` | 两格满了，第三条消息顶掉最早记下的那条；三条轮换一直全额。 |
| `zeroAdaptationDisablesTheEffect` | 适应几为 0 或负数等于关掉：永远全额，也什么都不记。 |
| `aMissingMessageIsNeverAdapted` | null / 空串不减伤也不记账。 |
| `clearingForgetsEveryRecord` | 清账之后同一消息重新从全额开始（回到未参战时用）。 |
| `snapshotAndRestoreKeepTheRecords` | 存档往返后次数与先后顺序都还在。 |
| `theSnapshotIsAReadOnlyCopy` | 快照不可写，改它不影响内部账目。 |
| `restoringDropsUnusableEntries` | 次数非正、消息为空的行一律丢掉，不把记过的消息洗成全额。 |

「配置读多少、什么时候清账、减伤落在哪一档」在实体那一层（`modifyIncomingDamage`、
`tickEmptyEncounter`、NBT `DamageAdaptation`），只能进游戏验证。

增加场景时，在 `src/test/java` 中新增测试类，或在已有类中增加 `@Test` 方法；不需要修改 `build.gradle`。

## 具体实体接入后的游戏内验证示例

下列片段中的 `boss` 指服务端世界里已加入世界、真实生命大于零的 `VicissitudeBossEntity` 实例。
这是后续实体集成测试的示例，当前 Gradle 任务不会执行这些片段。

### 外部 setter 和强制死亡不得生效

```java
float before = boss.getHealth();
for (int i = 0; i < 10; i++) {
    boss.setHealth(0.0F);
    boss.die(boss.damageSources().genericKill());
}
if (boss.getHealth() != before || !boss.isAlive()) {
    throw new AssertionError("直接 setHealth/die 不应扣血或杀死 Boss");
}
var mirror = boss.getCapability(VicissitudeVitalityCapability.CAPABILITY)
        .orElseThrow(() -> new AssertionError("缺少 Capability"));
if (mirror.killAttempts() != 0) {
    throw new AssertionError("直接 setHealth/die 不应累计 kill 次数");
}
```

此例应使用尚未收到任何 kill 尝试的新 Boss。

### 三次 kill 击穿

对一个新 Boss，在服务端连续三个不同 tick 分别调用一次 `boss.kill()`：

1. 第一个 tick：仍存活，计数为 1。
2. 第二个 tick：仍存活，计数为 2。
3. 第三个 tick：`getHealth()` 为 0，开始正常死亡流程。

不要用同一 tick 内的三次调用代替，因为同 tick 调用会去重。

### 入场同步与 NBT 防反写

1. 在首次加入世界前增加最大生命属性的修饰符，入场后确认真生命包含额外加成。
2. 受伤后记录 `getHealth()`，再修改最大生命属性，确认真实生命不变。
3. 用 `saveWithoutId` 取得 NBT 副本，将其中 `Health` 和 `ForgeCaps` 中的 `Current` 改为零，再调用 `load`，确认真生命与 kill 次数不变。
4. 正常保存世界并重新加载，确认同一 UUID 恢复原来的伤势与最大生命记录。
