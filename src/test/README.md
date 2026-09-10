# Vicissitude 测试示例

测试代码位于 `java/org/brahypno/maledict/common/entity/VicissitudeVitalityTest.java`。
每个 `@Test` 方法对应一个完整场景，使用 JUnit 5 的断言检查结果。

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

## 可运行示例

| 方法 | 操作及预期 |
| --- | --- |
| `thirdDistinctKillBypasses` | 同一 tick 连续尝试 21 次只计一次；后续两个 tick 各尝试一次，第三次真生命归零。 |
| `expiredSequenceRestarts` | 相邻尝试间隔为 100 tick 时继续累计，超过 100 tick 则重新从一次开始。 |
| `ordinaryWoundsDoNotResetTheSequence` | 受伤产生扣血和冷却；窗口内的受伤、治疗不会清除已经累计的 kill 次数。 |
| `persistenceKeepsWoundsAndDeadIdentities` | 写入真实 `.dat` 文件后重载，伤势、最大生命、冷却和 kill 次数保持；不同 UUID 分开存储，死亡记录仍为零。 |
| `entityNbtCannotRewriteTheLedgerOrBoundCapability` | 修改 Capability 导出的 NBT 不影响已绑定副本及世界记录；入场绑定时以世界记录覆盖伪造的加载值。 |

这些例子直接验证状态、文件存储和 Capability 镜像的序列化策略，不启动 Minecraft 服务端。
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
