# 测试

所有测试在 `src/test`，项目根目录显式执行：

```powershell
.\gradlew.bat test
.\gradlew.bat test --tests '*DamageAdaptationTest'   # 只跑一个类
```

`test` 使用 ModDevGradle 的 NeoForge JUnit 集成执行逻辑用例；`runGameTestServer` 单独启动
真实 NeoForge 服务端，运行同一目录中标注 `@GameTest` 的实体场景：

```powershell
.\gradlew.bat runGameTestServer
.\gradlew.bat test runGameTestServer
```

`--tests` 只过滤 JUnit。游戏场景日志在 `build/tests/game/logs/latest.log`，JUnit HTML 报告在
`build/reports/tests/test/index.html`。正常 client/server/data 运行配置只加载 main；GameTest 配置将
main 与 test 合并加载。测试类、测试标签及测试世界预设不会打入正式包。
`check` / `build` 使用 Gradle 默认的 JUnit 检查；GameTest 需显式执行。缓存齐全后可加 `--offline`。

GameTestServer 1.21.1 从 FLAT 世界预设构建维度，测试预设加入与生产资源一致的 Blighted Realm。
两个几何/脱困场景使用靠近原点的独立场地，避免新测试服务器随机选择数千万格外的坐标导致
原有 float 几何精度不足；它们不验证远坐标精度。
FakePlayer 不会自行更新装备，Curios 夹具显式投递原生 EntityTickEvent.Pre 和 Post。
跨维度测试只替换无真实连接的发送器和频道查询，传送、维度加载和配对井仍走真实实现；不覆盖网络握手。
SavedData 文件往返等待 NeoForge 的异步 I/O 完成后读取。

迁移时已检查旧测试的必要性：

- 保留适应窗口/批次、NBT 持久化、返还账本、AI 边界、几何和动画计算，以及真实伤害/装备/仪式场景；
  单元规则与实际调用链有不同覆盖范围。
- 合并 Sacrifice 时间表的常量断言为完整倒计时验证，保留首次/末次脉冲、间隔、次数和总伤害约束。
- 移除 RavenCombat 中重复的无状态调用，合并 Incursus 档位的短测试；边界断言均保留。
- 将 FadeEnvelope 测试中“再次淡入”的误导名称改为实际覆盖的“淡入立即转淡出”。
- 删除在 GameTest 里再次启动同一批 JUnit 的包装场景。新的 JUnit 集成本身已加载模组，重复执行
  不增加覆盖；真实实体场景仍保留。
- 保留 tooltip 注册事件的公共可见性/模组总线断言，这是本模组事件的注册约束。
- 移除火球旧向量加速度断言，保留实际转向、速度与新版标量加速度；新算法已确认。
- 新增持刀提供容量、实际伤害获得黄心、刀自身积累上限、卸下后原生截断的整条链路测试。

JUnit 公式断言只证明生产函数满足断言；实体场景另在真实 NeoForge 世界中验证执行链。
本次无常体系检视逐项检查了测试的覆盖边界：

| 测试 | JUnit 实际验证 | 单元断言不能证明的行为 |
| --- | --- | --- |
| DamageAdaptationTest | 已确认的窗口、计数、淘汰及批次序列 | Boss 是否在正确时机调用；Incursus 多通道和探针追加 |
| DamageAdaptationStorageTest | 生产 NBT 编解码保留顺序和计数 | 实体卸载、重载后继续受伤 |
| VicissitudeVitalityTest | 不可变状态、SavedData 文件和 attachment mirror | 实体附加 attachment、世界账本回读、事件与死亡流程 |
| VicissitudeUnstickSearchTest | 生产搜索找到远侧出口，失败最多检查 64 点 | 世界碰撞、未加载区块、传送后 AI 恢复 |
| VicissitudeRig/SwingBand/HitGeometryTest | 姿态采样、朝向和几何交点 | 实体实际站位、视线、命中帧、上下层目标、玩家和投射物命中 |
| VicissitudeTotemConsumptionTest | 生产选取函数选出上方日志高度 | 真实图腾捕获、方块删除、存档恢复与一次性消耗 |
| VicissitudeSummoningTest | 进度与视觉曲线的边界和连续性 | 实体下降、图腾消耗、迟加入玩家的同步 |
| VicissitudeFeatherShedTest | 羽翼曲线及静态网格边界 | 渲染层与实体碰撞是否使用同一姿态 |
| VicissitudeRenderOptimizationTest | 矩阵缓存与祖先变换等价、发光掩码选择 | 实际渲染两层及 GPU 输出 |
| VicissitudeThemeTest | 输入 Participant 后的选曲规则 | 实体到 Participant 的转换、客户端同步与实际播放 |
| FirstVicissitudeSpiritDataTest | 精魂 JSON 内容 | Malum 加载该资源、真实死亡时结算精魂 |

修正了旧测试中超出其覆盖范围的名称和注释，并移除了渲染测试里不参与计算的重复 layer 循环。
已加入真实服务端场景：伤害事件取消、双向嵌套伤害、最终限伤、取消死亡、致命追加命中、
真实护甲与吸收后的批次扣血、真实实体 NBT 与卸载重载、三次强制击杀、虚拟武器恢复与重载、
真实手持武器与虚拟武器攻击强度对照、实际适应存档、饰品死亡交接、投射物碰撞部位与玩家视线部位、
真实挥砍的高度隔离、世界碰撞中的脱困，以及图腾消耗存档恢复与被替换方块保护。
饰品场景使用真实 Curios capability、装备校验事件、EntityTickEvent.Post 和原生栏位属性：
256 件不同 NBT 饰品装满七类栏位；护符增加头饰栏、头饰再增加腰带栏的两层依赖；
世界时间推进但装备尚未 tick 时，提供栏位的饰品仍会阻止依赖项过早回退到背包；
原槽被占用时先恢复其他条目并持续重试；禁止改放其他空 Curios 槽；背包最后一格被领取凭证占用；
校验事件递归领取并占用目标槽；多人满背包时在 Boss 位置附近分箱；放箱失败、账本 NBT 恢复后再次交付。
FakePlayer 的 tick 本身不更新装备，场景显式投递原生 EntityTickEvent.Post 给实际 Curios 订阅者，
并跨游戏 tick 等待栏位更新；没有用测试实现代替 Curios。账本 NBT 往返只验证序列化，不声称模拟完整服务器重启。
真实 Boss 的 hurt 链按已确认的批次规则验证返还：同 tick 最终伤害 6、14、14 共扣 14 血，只释放一件；
等待实际伤害冷却后，下一批才释放提供头饰栏位的护符，再经 Curios 装备更新将此前待返还的头饰放回 head[4]。
另覆盖 LivingDamageEvent.Pre 内嵌套的外层 2 / 内层 10 与外层 10 / 内层 2、最终伤害取消、真实吸收完全挡伤，
以及 Curios 装备校验回调触发更大伤害的重入；同时检查真实血量、Boss 剩余持有量、返还账本和原槽位。
部分场景通过 LivingDamageEvent.Pre 明确设置最终通道伤害，以隔离批次规则；这些断言不证明输入伤害到最终伤害的换算正确。
实际区块卸载、重载、删除回调验证所有权交接。
FakePlayer 没有网络握手，离线交接场景直接调用本模组的真实登录订阅方法，不覆盖客户端连接过程。
这些场景验证服务端执行链；实际视觉、客户端同步与玩家操作手感仍需客户端验证。
渡鸦繁殖测试使用独立封闭场地，通过真实 `BabyEntitySpawnEvent` 追踪指定父母的后代，并检查双方繁殖冷却。
台阶场景只是人工搭建的一级台阶，不代表加载或完整复现用户存档。
繁殖场景不允许无关伤害；失败信息包含血量和最近一次伤害来源，测试前后会清理本批次的渡鸦。
新极难 Boss 的场景检查真实初始血量 1500、身体百分比限伤实际扣 15 血、服务器血条显示 1485/1500、
实体卸载重载保留 1485/1500、治疗可回到 1500；已有 900/1024 的世界账本仍保持原值。
