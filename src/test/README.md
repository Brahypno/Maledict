# 测试

测试按被测功能组织，与 `src/main/java` 的包结构对应：

```text
java/org/brahypno/maledict/
  common/entity/raven/          渡鸦 AI、繁殖、战斗与移动
  common/entity/vicissitude/    伤害适应、真实生命、存档与 Boss 场景
  common/item/weapon/           incursus/ 神侵恶刃；axe/ 恋人斧
  common/curio/                 hangman/ 投射物与装备；rune/ 共命倍率；vicissitude/ 饰品返还
  common/attribute/             属性换算与真实伤害链
  common/realm/                 跨维度传送、井配对与矿石
  common/rite/, common/effect/, common/vitals/
  client/                       布局、视觉曲线与音乐状态
  rig/                          骨架、命中几何与动画连续性
resources/                      GameTest 场地、测试世界预设与 Curios 标签
```

在项目根目录执行：

```powershell
.\gradlew.bat test
.\gradlew.bat test --tests '*DamageAdaptationTest'
.\gradlew.bat runGameTestServer
.\gradlew.bat build
```

`test` 使用 ModDevGradle 的 NeoForge JUnit 集成，验证状态序列、计算边界与存档往返。
`build` / `check` 包含 JUnit；`runGameTestServer` 需单独运行，在真实服务端验证装备、事件、
伤害、实体 AI、传送和卸载重载。`--tests` 只过滤 JUnit。缓存齐全时可加 `--offline`。

JUnit 报告在 `build/reports/tests/test/index.html`；GameTest 日志在
`build/tests/game/logs/latest.log`。测试类和测试资源不会打入正式 jar。

正常 client/server/data 配置只加载 main；GameTest 配置合并加载 main 与 test。
测试预设为 Blighted Realm 添加维度。FakePlayer 的 Curios 场景显式推进装备事件；
网络替身只替换没有真实连接的发送端，实体、装备、传送与存档仍调用生产实现。

保留能定位实际故障的状态序列、几何约束和游戏场景。已移除单独复述枚举、简单比较和乘法、
固定精魂 JSON 数值，以及只演示事件总线投递的测试。公式断言不代表实际执行链已验证，
服务端场景也不代表客户端画面与操作手感已验证。
