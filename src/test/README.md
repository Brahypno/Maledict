# 测试

游戏测试与 JUnit 单元测试各自最多保留 50 项，当前各 50 项。新增用例达到上限时，替换或删除
低价值用例；不靠过滤、禁用或把旧测试藏进一个入口来压低报告数量。

所有测试在 `src/test`，项目根目录显式执行：

```powershell
.\gradlew.bat test
.\gradlew.bat test --tests '*DamageAdaptationTest'   # 只跑一个类
```

测试包跟随生产代码：`common/raven`、`common/vicissitude`（含 `rig`、`rite`、`curio`）、
`common/weapon`（`incursus`、`lover`、`remembrance`）、`common/rune`、`common/curio`、
`common/wisdom` 等。跨功能伤害适应算法在 `common/combat`；客户端测试跟随各自的功能包。

`test` 先用已有 `runGameTestServer` 配置启动真实 Forge 服务端，运行同一目录中标注 `@GameTest` 的实体场景；
随后执行 JUnit 逻辑用例。没有独立 GameTest 目录或 source set，测试内容不会打入正式包。
`--tests` 只过滤 JUnit 用例；游戏场景仍会执行。游戏报告保存在 `build/tests/game-environment.log`，
不会被随后初始化 Minecraft 日志的 JUnit 用例覆盖。

`check` 与 `build` 不依赖 `test`，不会自动跑测试。JUnit HTML 报告在 `build/reports/tests/test/index.html`，
首次运行需要下载测试依赖，缓存齐全后可以加 `--offline`。

JUnit 公式断言只证明被调用的生产函数满足断言；实体场景另在真实 Forge 世界中验证执行链。
JUnit 只执行一次；Forge 场景直接验证实体和事件流程，不再嵌套启动 JUnit 重跑同一组断言。

保留各功能的代表性场景，删去重复组合、简单常数与低价值边界用例：

| 范围 | 保留的重点 |
| --- | --- |
| 伤害适应 | 两消息与三消息轮换、最近命中顺序、淘汰清账、同 tick 去重与倍率复用、NBT 往返 |
| 无常 Boss | 旁观者与创造模式、1500 血量和旧账本、虚拟武器重载、实际适应存档、部位命中、挥砍高度隔离、世界脱困 |
| 血量与饰品 | 存档保留伤口、取消伤害与死亡、嵌套伤害、三次强制击杀、护甲与吸收后的批次扣血、原槽返还、依赖栏位、放箱失败重试 |
| 其他世界交互 | 属性转换、倒吊者传送与反弹、女祭司延迟伤害与灵魂转移、祈唤支付与退款、渡鸦繁殖与火中逃生、维度与配对井口、矿物掉落合成、武器与献祭 |
| 客户端与纯逻辑 | 姿态与矩阵缓存、网格边界、选曲、微光迟滞、提示框极小尺寸、延迟池、渡鸦行为及献祭进度 |

Curios 场景使用真实 capability、装备事件与栏位属性。FakePlayer 不会自动更新装备，测试显式投递
`LivingTickEvent` 并等待栏位更新。渡鸦台阶繁殖使用人工搭建的独立场地，不代表复现用户存档。
服务端测试不验证实际画面、客户端同步、操作手感或游戏内帧率。
