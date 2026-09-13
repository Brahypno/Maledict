# 10 — Lodestone / Malum 特效接入清单

2026-09-13 核对。本页将已安装依赖的特效能力映射到 Boss 已确定技能，不另行扩展战斗机制。依据为本地 mapped JAR、javap 方法签名和项目既有调用；不是套用其他版本的网上示例。签名核实不等于游戏效果已经验证，也不自动证明 helper 可从任意线程/逻辑侧调用。

## 已核实的 API 与用途

| 能力 | 本地接口 | 本 Boss 的使用位置 |
| --- | --- | --- |
| 可调粒子 | Lodestone `WorldParticleBuilder`：`setColorData`、`setScaleData`、`setTransparencyData`、`setSpinData`、`setMotion`、`setLifetime`、`setLifeDelay` | 翼尖蓄光、释放碎芒、死亡时由冷白转暗紫并缩小消散 |
| 灵魂光点与柔光 | Malum `SpiritLightSpecs.spiritLightSpecs` 返回 `ParticleEffectSpawner`，有 `ColorParticleData` 重载；`spiritBloom` 返回粒子 builder | 头核局部光、翼根/羽片依次激活、镰刀接回闪光；可用自定义色板而非强行套整套精魂色 |
| 旋转光点 | `SpiritLightSpecs.rotatingLightSpecs`，支持 builder consumer | 头后断环短暂失稳时的少量绕行光点；不是另做一圈永久完整光环 |
| 历史拖尾 | Lodestone `TrailPointBuilder.create`、`addTrailPoint(Vec3)`、`tickTrailPoints`、`getTrailPoints`；Malum `RenderUtils.renderEntityTrail` | 镰刀飞行、冲刺残迹、追踪球曲线；尾宽和颜色随历史位置衰减 |
| 灵魂材质 VFX | `SpiritBasedWorldVFXBuilder.create(MalumSpiritType)`、`setRenderType`、`setAlpha`，继承 WorldVFXBuilder | 延用本项目光球 umbral/eldritch 风格，控制亮度以匹配黑紫圣骸 |
| 线段/光带 | `VFXBuilders.WorldVFXBuilder.renderBeam(Matrix4f, Vec3, Vec3, float)` 与更多重载 | 左手到飞镰的细命运线、标记指向线；末尾 float 的具体几何尺度按方法体及游戏校准 |
| 面片/曲线几何 | `WorldVFXBuilder.renderQuad`、`renderTrail`、`setUV`、`setColor`、`setAlpha` | 胸标记的边界、主环技能的带缺口地面预兆、释放瞬间的薄弧面 |
| 斩击方向 | Malum `ParticleHelper.SlashParticleEffectBuilder`：`setSlashAngle`、`setVerticalSlashAngle`、`setMirrored`、`setSpiritType`、`spawnSlashingParticle(Level,Vec3,Vec3)` | 横斩、竖劈、重击分别绑定正确的方向和服务端释放位置；不随机镜像导致视觉与真实挥刀相反 |
| 空间/形状粒子分布 | `WorldParticleBuilder.spawnLine`、`createCircle`、`repeatCircle`、`surroundVoxelShape` | 少量路径碎光、局部环片火花或破坏成功后沿实际方块形状的碎屑；不是用于决定碰撞/伤害 |
| 震动与缓动 | 已核实 PositionedScreenshakeInstance、ScreenshakeHandler；Easing 类型存在 | 08 的重击、转场、死亡短震动；粒子数据的具体 easing setter 在实施时核对，不猜方法签名 |

关键包：Lodestone 的 `team.lodestar.lodestone.systems.particle.builder`、`systems.rendering`、`systems.rendering.trail`；Malum 的 `com.sammy.malum.client`、`core.helpers`、`visual_effects`。

`renderSphere` 虽然存在，本设计不需要用它填满胸部。`ScreenParticleEffects` 和 Lodestone 屏幕粒子也存在，且本项目 `MaledictScreenParticles` 已用于物品 UI；不能据此把物品图标粒子当成现成的全屏 Boss 后处理接口。未来 HUD/物品表现可参考，首版不额外加入遮挡瞄准的全屏粒子。

## 每个动作的具体表现

| 场景 | 主体表现 | 辅助效果与结束条件 |
| --- | --- | --- |
| idle / hover | 模型姿态和少量 emissive 为主 | 头核偶发光点，避免常驻全翼烟雾；环慢转来自模型 |
| 一阶段普通扇射 | 活动翼片从翼根向翼尖依次亮起，提前 12 tick 蓄力 | 释放短星芒，非追踪球用直线短尾；所有一阶段球均压血，不暗示普通伤害 |
| 一阶段追踪球 | 双层旋转光核＋明显曲线尾迹 | 与直线球形状/运动区分；不能只靠颜色区分；沿用已存在的光球渲染样例 |
| wing barrage | 每波按左右翼顺序闪亮，模型羽片轻微展开 | 每波仅在真正发射锚点出光，不给每枚球永久挂多套粒子 |
| 胸标记 | 胸残环轻微偏移，地面区域有清晰边界和倒计时式收束 | 视觉范围使用与判定相同的中心/半径；边界在低粒子设置下仍存在，触发后约 8 tick 淡出 |
| 主环裁定 | 头后断片短暂错位，地面显示内外环与 60° 安全缺口 | 用分段几何保留缺口；不能直接 repeatCircle 画满圆把安全区盖掉 |
| 横斩 / 竖劈 / 重击 | 粒子斩弧与实际镰刀方向一致 | 释放帧一次产生，重击比普通斩弧更宽更短；不是更大范围伤害 |
| scythe throw / recover | 飞镰独立 Item 渲染，历史轨迹逐渐消失 | 左手到飞镰的细线仅在牵引/返回关键段显示；接回一次短光，空手状态由同步状态决定 |
| dash / charge | 先明确方向预兆，再产生短身后残迹 | 轨迹不延伸成新的攻击区域；碰撞停止或 recovery 时停止采样，旧点自然消失 |
| 二阶段远程 | 翼尖蓄力＋三枚短尾球 | 与一阶段压血球区分核形/闪烁节奏，保持同一色系，不更改普通伤害规则 |
| phase transition | 羽片失序→骨架暴露，局部裂光由胸环到头壳 | 数次短光而非全屏爆闪；原一阶段预警/尾迹随清理事件结束 |
| 脱困瞬移 | 旧位置短暂散开碎芒，新位置短光收束 | 不在两点间连一条跨墙长拖尾；瞬移时清空自身位置历史，飞镰按生命周期规则处理 |
| death | 模型逐段失效和内部拼接暴露 | 光点离开连接处后缩小熄灭；最后结束所有跟随发射器，不用大爆炸代替遗骸失效 |

## 统一接入契约

1. common 侧动作/事件提供 Boss UUID、actionSequence、事件类型、时间、锚点或世界位置以及必要参数。持续效果由同步状态重建；一次性闪光/斩弧/震动只消费一次。只同步事件，不为每个装饰粒子发网络包。
2. 客户端用一个 Boss 特效入口组合 builder 和 trail；Model 不直接散落网络/粒子逻辑。建议位置 `client/vfx/FirstVicissitudeEffects.java`，需要状态时按实体实例保存，不能静态共享一组尾迹。
3. Malum 斩击 builder 关联 networked ParticleEffectType；实施前核对所选效果的发送/消费侧，采用“Malum helper 发一次”或“本模组同步后客户端生成”其中一条路径，避免双方各生成一遍。已有 `IncursusBladeEnchantments` 是调用样例，不直接复用其造成伤害的主动技能入口。
4. 模型锚点只负责客户端精确附着；服务端预兆中心、攻击朝向和半径仍来自 02/07 的权威几何。显示地面环不是 renderer 自行重新选目标。
5. 色彩数据使用 01 的冷白/暗紫层次。SpiritBasedWorldVFXBuilder 可用于风格一致的既有光球；若封装带来的色彩/透明规则与 Boss 目标冲突，使用普通 WorldVFXBuilder 加本色板，不强迫每种效果走同一个封装。

## 数量、缓存与显示

- 继承 04 每 Boss 120 个存活装饰粒子、常态新增最多 8/tick、关键事件最多额外 40 的初始预算；双层 bloom 与 helper 内部产生的粒子都计数，不能把一次 helper 调用当作一粒。必要时直接配置底层 builder。
- 粒子总预算覆盖本 Boss 的所有弹体。达到预算优先减少外围光点和次要尾迹；不得删掉真实弹体显示、技能范围边界或安全缺口。
- 飞镰/追踪球每客户端 tick 最多采一个历史点，首版保存 12–20 tick 的短历史；静止不反复加相同点。每次添加前更新/老化历史，帧渲染只插值，不能让高 FPS 客户端尾迹更长。
- 胸/主环预兆用预计算分段几何，初始约 48 段，段位/UV 可以复用，动态只变中心/朝向/淡出参数。地形贴合只在生成/位置最终锁定时采样，不每帧搜索整个圆盘；复杂地形保证显示面对应实际可伤害地面。
- 使用项目已有 applyAndCache RenderType 模式；builder 是可变对象，复用时重置颜色/alpha/UV，避免跨实体串色。不要长时间缓存绑定帧缓冲的 VertexConsumer。
- 光晕是视觉柔光/叠加，不等同真实动态照明；强光层受深度和距离控制，不能无条件隔墙透视 Boss。低粒子设置也能通过模型、实体和几何预兆辨认攻击。

## 实施与验收

M3 完成基础材质/有限发光；M4 的每个实际技能同时接入最低可读预兆，不能等 M6 才让攻击能被看懂；M5 完成飞镰尾迹/牵引；M6 完成分层光效、斩弧修饰、转场/死亡和震动润色。

验收：正常/最少粒子、日夜、墙后遮挡、两个 Boss 同屏、多人晚加入、重复事件、低/高帧率、瞬移前后尾迹、离开跟踪范围和死亡清理。检查画出的安全区与伤害范围一致、无持久尾迹/发射器泄漏、攻击读得清、不因粒子数量掩盖模型主题。

此次仅核对 API 并细化规格，不声称渲染已实现或帧率已经测得。实现者可以在同等表现下选择这些已存在的 helper，无需每个工具重新向用户提问。
