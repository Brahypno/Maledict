# 00 — 代码基线与适配结论

下列路径相对仓库根目录。这里记录“当前做了什么”，不代表用户批准所有现有行为，也不代表已经游戏内验证。

## 真实项目

| 项目 | 已核实内容 | 依据 |
| --- | --- | --- |
| 平台 | Minecraft 1.20.1、Forge 47.4.23、Java 17、official 1.20.1 mappings | `gradle.properties`、`build.gradle` |
| 依赖 | Malum Curse 文件 6646111；Lodestone 文件 6213794；Curios 文件 6418456；ChangeLib | `build.gradle` |
| Boss | 注册 ID `maledict:first_vicissitude`，当前碰撞 0.6 × 1.8 格 | `registry/MaledictEntities.java` |
| 模型 | slim PlayerModel、默认玩家皮肤、缩放 0.9375；没有正式 Boss Model 或持物 layer | `client/FirstVicissitudeBossRenderer.java` |
| 模型工具 | 本项目未发现 ModelPart 锚点转换、通用骨骼动画或 Boss emissive helper | `src/main/java` 检索；不推断依赖中也没有 |
| 第一阶段 | 有效攻击者触发计时，6000 tick；20 tick 预热，此后每 40 tick 一个光球；轮换目标，飞行绕行 | `common/entity/FirstVicissitudeBossEntity.java` |
| 光球 | 当前从眼高附近发射；命中将目标生命压到最多 1；不是普通伤害弹 | `fireLightOrb`、`VicissitudeLightOrbEntity.collect` |
| 转阶段 | 当前没有转场状态；到时立即装备、公告、注册目标，并按难度没收 Curios | `beginPhaseTwo` |
| 第二阶段 | 优先选生命较高玩家，接近并直接伤害；攻击成功后冷却 20 tick；攻击属性为 8 | `PhaseTwoCombatGoal`、`performPhaseTwoAttack` |
| 难度武器 | SIMPLE 染魂钢镰刀；DIFFICULT 救赎之锋；COMPLETE/EXTREME Incursus Blade，各项等级 3/9 | `equipPhaseTwoWeapon` |
| 环境 | 第二阶段攻击阻挡实体；碰撞时每 10 tick 检查拆方块，服从 mobGriefing 与 Forge 事件 | `destroyBlockingEntities/Blocks` |
| 同步 | 当前 phaseOneTicks/phaseTwoStarted 为服务端字段，没有 Boss 动作同步协议；不能让 renderer 直接依赖其客户端值 | `FirstVicissitudeBossEntity` |
| 生命/死亡 | 真生命有账本和 capability；受伤、kill、掉落受基类保护；final tickDeath 调用原版流程 | `VicissitudeBossEntity`、`VicissitudeVitality*` |
| 测试 | 已有真生命 JUnit 场景；build/check 不自动运行 test | `src/test/README.md`、`build.gradle` |

Java 路径前缀均为 `src/main/java/org/brahypno/maledict/`。

## Malum 本地核对

核对的是 Gradle 本地缓存的 mapped JAR，不是一个另行检出的 Malum 源码仓库。缓存来源可按 `curse/maven/malum-484064/6646111_mapped_official_1.20.1` 定位，不把个人绝对路径写进构建配置。

- JAR 包含 `ScytheBoomerangEntity`、`AbstractScytheProjectileEntity`、`ScytheBoomerangEntityRenderer`，以及镰刀 handheld/gui 模型、纹理和声音。
- javap 输出显示抽象弹体有 `setData(Entity,float,float,int,int)`、damage/magicDamage、slot、returnTimer；回旋镰刀有 `flyBack(Entity)` 和 enhanced 状态。
- 第二轮已通过沙箱外只读 javap 核对 `ReboundEnchantment.throwScythe` 方法体：要求 Player/ServerPlayer，读取玩家物品栏槽位与攻击/魔法属性，生成回旋镰刀，调用 setData/setItem/shootFromRotation，禁用玩家槽位并记录使用统计。普通/增强初速分别 1.75/3.0；增强伤害乘 1.5。这些是 Malum 现状，不自动成为 Boss 平衡数值。
- `pickupScythe` 明确接收 ServerPlayer，不能把 Boss 强转玩家调用原方法。可参考生成、ItemStack、轨迹与声音实现，但必须提供 Boss 自身的持握/返回协议。先前沙箱内 javap 的访问错误已通过这次检查解决；没有宣称所有 Malum 方法均审计完毕。
- 本项目 `IncursusScytheBoomerangEntity` 继承 Malum 回旋镰刀，命中时会暂换主人主手并加入伤害通道。不能直接假定它满足 Boss 独立物品所有权和返回协议。
- 已有 `VicissitudeLightOrbRenderer` 使用 `SpiritBasedWorldVFXBuilder`、`RenderUtils.renderEntityTrail`、`FloatingItemEntityRenderer.renderSpiritGlimmer`，RenderType 使用 `applyAndCache`。这是可用的 Malum/Lodestone 特效接入样例。
- 第三轮通过本地 Lodestone JAR 与 javap 核实 ScreenshakeHandler、PositionedScreenshakeInstance 和强度/缓动接口，接入位置及首版参数见 08。本地基线中的 6000 tick 是旧代码事实，目标时长已经改为 03 的难度表。

## 原稿必须修正的缺口

2026-09-13 追加核对：WorldParticleBuilder、WorldVFXBuilder、TrailPointBuilder、SpiritLightSpecs、SpiritBasedWorldVFXBuilder、RenderUtils、SlashParticleEffectBuilder 的本地签名已核实；本项目光球、Incursus 斩击和物品 UI 粒子已有调用可参考。用途与适配限制记录在 10，不把方法签名检查表述为游戏验证。

1. 模型动画开始之前先建立阶段/动作同步，否则远端玩家看不到正确二阶段。
2. 模型锚点仅在客户端存在；弹体由服务端的共享几何定义生成。
3. “已有镰刀”实际是四难度三种物品，不应写死单一 item 或尺寸。
4. 第一阶段高位飞行和第二阶段高度使用实体原点；扩大模型后必须一起校准原点、眼高、攻击距离与悬停高度。
5. 死亡动画不能由子类 override final tickDeath；需要基类有限扩展，保持真生命和一次性掉落事务。
6. 原稿技能清单混合“模型能摆出的动作”和“必须实现的技能”。必须先确定范围。
7. 原稿未定义投掷命中、返回、空手时行为、保存加载以及 Boss 死亡后弹体处理。
8. 原稿没有量化动作时序和美术交付边界；不能据此声称几乎无需后续提问。
