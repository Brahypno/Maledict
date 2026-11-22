# 神侵恶刃：统一裂隙斩击渲染

四种斩击共用同一套刀痕材质，只换布局：普通挥砍的八瓣水平腰环原样保留，飞腾旋斩、强化上挑与
Malum「隐藏锋刃项链」的连斩从 Malum 的原版刀光改画成同一套星空裂隙。改造当时**只改渲染**——
伤害计算、ChangeLib 探针、饰品冷却、`WICKED_INTENT`、`HiddenBladeDelayedImpactEntity`、附魔与
攻击判定全部不动。后续的「范围同步」（`docs/mechanics/incursus-swing-range.md`）把普通挥砍的腰环
改成按伤害半径绘制，并把命中判法的几何换成同一个球；伤害数值、探针、冷却与附魔仍未改动。

## 数据模型

一次技能特效由一个 `IncursusRiftInstance` 记录一批 `RiftStroke`。每道刀痕在创建时定死相对锚点位移、
两条正交基向量 `axisU`/`axisV`、初速度、半边长、出生 tick、寿命、`fadeStart`、着色种子与批次序号。
顶点每帧由 `age + partialTick` 重新求值，不做逐帧累加，帧率不会拖出形变；不生成任何需要独立 tick 的实体。

- 普通挥砍的实例额外记一个 `radius`：服务端这一次挥砍的伤害半径。花瓣位置与刀痕半边都由它推出
  （`IncursusRiftGeometry#petalDistance` / `#halfExtentForRadius`），所以画的圈就是能打到的圈。
  技能模式的 `radius` 为 0，它们按逐道刀痕画。
- 普通挥砍保留原有的 0.98 摩擦积分和整环遮罩帧。技能按 Lodestone 的出生顺序插值，出生帧停在生成点，
  首次移动后以设定速度推进（摩擦系数 1、重力 0）。每道刀光按 Lodestone 的 tick 年龄选择贴图帧。
- 同一遮罩帧的顶点写完并提交后再切换缓冲。技能刀光沿用 Malum 的场景深度遮挡，普通挥砍读取
  `respectWorldDepth` 配置。
- 朝向由 Malum `PointyDirectionalParticleBehavior` 的四元数换算成两条基向量：`axisU` 就是单位方向向量，
  `axisV` 随偏航、俯仰与 `spinOffset` 转动。`spin = 0` 时四边形平躺（旧的八瓣花），
  `spin = ±90°` 时立起来成为纵斩。旧 `emitCorner` 的 `(cos yaw, 0, -sin yaw)`/`(sin yaw, 0, cos yaw)`
  正是水平方向、`spin = 0` 的特例，所以普通挥砍逐顶点不变。
- 锚点：普通挥砍 `FOLLOW_OWNER`（每帧取挥砍者本地插值腰部位置）；飞腾与隐藏锋刃 `FIXED_WORLD`
  （Malum 那三种本来就是触发时在世界里一次性生成、随后各自飞走的粒子群）。

## 四种布局（数量为硬约束）

| 模式 | 数量 | 布局与节奏 |
| --- | --- | --- |
| 普通挥砍 `MELEE_RING` | 8 | 严格 45° 等分，花瓣中心末帧圆 = 本次挥砍的伤害半径（上限 6 格）；开合走原版摩擦积分并归一化，起手 `0.117R`、12 tick 收尾到 `R`，刀痕半边 = `petalSize × R / 4.27` |
| 飞腾旋斩 `ASCENSION_SPIN` | 48 = 3×16 | 圆周角 `(i·0.33 + j)/16·2π`，半径 1.75，尺寸 2.5–3，速度 0.2–0.4 加 0.8 上飘，寿命 `3+i`，延迟 `i + j/4` |
| 强化上挑 `ASCENSION_UPPERCUT` | 12 = 6×2 | 前移 `2 - i·0.6`、升高 `i·0.4`，`spinOffset = 1.57 ± 0.25 + 3.14`，寿命 `3+i`，延迟 `i/2` |
| 隐藏锋刃 `HIDDEN_BLADE_FLURRY` | 300 = 3×100 | 位置为 `direction` 三分量各抽 −1.5…4.5；角度 `baseAngle ± (0.314–3.14 随机再乘 ±1)`；寿命 `2+i`，延迟 `(j 偶数 ? 2 : 0) + i + j/6`，最后一道约 24 tick 消失 |

随机数由报文种子在客户端重建（`RandomSource.create(seed)`），抽样范围、奇偶分支与调用顺序照抄原版；
保证"公式与分布一致、同种子跨客户端一致"，不与某次 Malum 世界 RNG 逐点相同。着色种子另用 `hash01` 派生，
不插进原随机流。

## 网络与入口

- `IncursusSlashPacket`（消息 id 7，协议 9 → 10）携带挥砍者 id、锚点、**伤害半径**与种子。半径是
  客户端画图的唯一尺寸来源；服务端已封顶 6 格，客户端再过一次同一个闸。
- `IncursusRiftSkillPacket`（消息 id 11，协议 8 → 9）携带模式、施法者 id、锚点、方向、`baseAngle`、
  `mirrored`、种子。`direction` **不归一化**：强化上挑的纵向间距直接乘它的长度。
- 飞腾：`IncursusBladeEnchantments#performAscensionAttack` 自己发报文，`TRACKING_ENTITY_AND_SELF` 广播。
  原版在创建粒子构建器时抽的那一次 `mirrored` 仍照抽（强化分支同样抽一次再丢弃），世界随机流消耗顺序不变。
- 隐藏锋刃：`HiddenBladeNecklaceRiftMixin` 在 Malum `lambda$hurtEvent$1` 里 `@Redirect`
  `SlashParticleEffectBuilder#spawnForwardSlashingParticle(Entity, Vec3)` 这**一次**发送，落点偏移公式逐行照抄，
  `builder.slashAngle`/`isMirrored` 原样沿用；未持神侵恶刃时原样转调。注册在公共 `mixins` 数组，
  独立服务器也要能加载。不重写 Malum 的 `hurtEvent`——连斩实体、冷却、音效与 `cancel` 都留在 Malum 手里。
  广播用效果中心 96 格内的 `NEAR`，范围内的施法者也会收到。
- 客户端：`IncursusRiftClientManager` 按模式挑 `RiftStrokePattern`，实例寿命取所有刀痕
  `startTick + lifetimeTicks` 的最大值，再画完最后一次移动的插值帧（普通挥砍默认 12 tick，连斩约 25 tick）。

## 降级

Shader 不可用时采用蓝黑色普通着色。刀痕贴图缺失或配置关闭时，普通挥砍回到旧粒子腰环（整朵按
`半径 / 4.27` 等比缩放，末态环仍落在伤害半径上），三种技能模式由 `RiftLegacySlashes`
按同一批刀痕逐道重建 Malum 原版刀光（位置、朝向、自旋、尺寸、运动、延迟、寿命全部取自刀痕，
数量与原版一致）。服务端不再产生这些粒子，所以降级必须在客户端完成。

## 测试

`RiftStrokeGeometryTest` 覆盖方向基向量、三种技能的刀光数量与时序、固定种子，以及上挑前排刀光的出生位置、
首次移动和遮罩帧。普通挥砍的几何由 `IncursusRiftGeometryTest` 覆盖：末帧花瓣中心落在伤害半径上、
开合单调、退化配置（漂移速度 0、半径 0/NaN）不产出奇怪顶点、刀痕半边随半径等比缩放。
范围本身（半径公式、封顶、NaN 归零、碰撞箱最近点判法）由 `IncursusSwingReachTest` 覆盖。
