# 无常：设计与规格

本文档是无常（`maledict:first_vicissitude`）的正典规格：主题、模型、战斗、掉落、客户端边界和饰品策略。
动作时序与近战判定单独成篇，见 [02 动作、时序与判定](02_ANIMATION_AND_HITBOX.md)；
实现与验证状态见 [03 工程与验证](03_ENGINEERING_AND_VERIFICATION.md)。

> 本文记录**当前**规格。数值以代码为准；文档与代码不一致时先改代码或先改这里，不留两份说法。

---

## 1. 主题与阶段

神圣遗骸制造的失败实验体，被未完成的命运重新唤醒。情绪是冷静、无奈、不得已——不是神本身，也不是恶魔或传统的骷髅死神。

必须始终可辨认的特征：非人晶体头、无腿、头后断环、真正贯通的胸腔、骨羽混合双翼、三节中央残躯及其周围碎片。
第一阶段是高位圣像，第二阶段是前倾的执行者；死亡时揭示拼接结构，最后熄灭而不是爆炸。
主环与躯干属于同一具身体，`ModelPart` 只是动画关节，不代表独立实体或可拾取物。

| 项目 | 值 |
| --- | --- |
| 主体高度 | 70 模型单位 = 4.375 格（不含环、翼尖与特效） |
| 身体碰撞 | 1.6 × 4.375 格 |
| 翼展 | 常态 116 单位 = 7.25 格，最大 128 单位 = 8 格 |
| 头核 | 约 6×7×6 单位，无眼鼻口，死亡时才完整展示 |
| 主环 | 4 个不等长断片，缺口永久存在 |
| 换算 | 16 单位 = 1 格 |

色板：深黑紫 `#19151F`、灰紫 `#49404F`、冷骨白 `#B8B8C4`、冷白 `#E6EDF5`、暗紫能量 `#51436D`。

### 难度表

数值集中在服务端的 `BossDifficulty` 枚举，不散落魔法数字。

| 难度 | 一阶段 | tick | 真生命上限 | 二阶段武器 | 对玩家伤害档 |
| --- | --- | ---: | ---: | --- | --- |
| SIMPLE | 90 秒 | 1800 | 500 | 染魂钢镰刀（+5 攻击伤害） | light |
| DIFFICULT | 70 秒 | 1400 | 750 | 救赎之锋（+9 攻击伤害） | light |
| COMPLETE | 50 秒 | 1000 | 1000 | Incursus Blade（各项等级 3，+3） | medium |
| EXTREME | 37.5 秒 | 750 | 1500 | Incursus Blade（各项等级 9，+9） | medium |

20 tick 预热计入该时长；60 tick 转场另计。
血量在该实体加入世界、真生命账本捕获上限**之前**以 `MAX_HEALTH` 固定修饰符写入；
账本捕获之后改难度不会改变本场血量。

属性注册：`ATTACK_DAMAGE` 基础 8、护甲 15、击退抗性 1.0、`FOLLOW_RANGE` 96。
持械时由 `applyWeaponAttributes()` 从武器自身的属性表读出并烘焙进实体，
每 tick `suppressHeldItemAttributes()` 清掉手上物品的临时修饰符，所以加成不会算两次，
缴械或换装也不改变数值。一阶段不持械，伤害就是 8。

---

## 2. 阶段与转场

状态机：`DORMANT → PHASE_ONE → TRANSITION → PHASE_TWO → DYING`。
首个有效攻击者触发计时；未参战时保持第一阶段姿态但不推进阶段计时；死亡可打断任意状态，
`DYING` 不可回到存活。

### 转场（`TRANSITION_TICKS = 60`）

| tick | 内容 |
| --- | --- |
| 0–14 | 停止攻击并悬停 |
| 15–29 | 收翼，头壳与环错位 |
| 30–44 | 羽片下压，暴露骨架 |
| 45 | 装备武器 |
| 45–59 | 混合到二阶段姿态 |
| 60 | 开放二阶段 AI |

即二阶段在本难度一阶段时长走完后再经过 60 tick 开始。
转场期间免伤、停止破坏方块与阻挡实体攻击、不再生成一阶段球，并清除旧攻击球与场地预兆。
公告、没收 Curios、应用免没收 tag 都在转场完成点**一次**执行，不能每 tick 重复副作用。

### 死亡（`DEATH_TICKS = 80`）

| tick | 内容 |
| --- | --- |
| 0–15 | 悬停，断环停止 |
| 16–35 | 胸环失效，头壳崩开，露出重构痕迹 |
| 36–59 | 翼根失力，碎片脱离（此处触发 12 tick / 0.18 震动） |
| 60–79 | 核心熄灭（只做弱光收束，不加震动） |
| 80 | 服务端移除 |

掉落由一次性死亡事务在**真实死亡时**发放，不是动画末尾。死亡保存/重载维持归零与移除，不重新奖励。

### 空场回退

一阶段名单连续 100 tick（`EMPTY_ENCOUNTER_RESET_TICKS`）没有活着的对手 →
回到 `DORMANT`、解除阶段时长锁、计时清零（真生命、难度、武器、掉落不动），
名单与死亡记录一并清空。二阶段不做这个回退。

### 公告

全部走动作栏文字，冷白 `#E6EDF5` 加粗，用原版动作栏（60 tick 后淡出）。

| 键 | 时机 |
| --- | --- |
| `message.maledict.first_vicissitude.attack` | 一阶段开场白，每名玩家每命一次 |
| `message.maledict.first_vicissitude.phase_two` | 转场完成点一次 |
| `message.maledict.first_vicissitude.unstick` | 脱困瞬移 |
| `message.maledict.first_vicissitude.curio_return_pending` | 饰品待返还，带剩余件数 |
| `message.maledict.first_vicissitude.curio_return_complete` | 饰品全部归还 |

### 同步与存档

服务端同步 stage、action、actionStartGameTime、actionSequence、activeSide、weaponState（含 weapon tier）。
持续状态用 `SynchedEntityData` 表达，不能只发一次动画事件；客户端读同步 stage，
不读服务端 `phaseOneTicks` 推断阶段，晚加入也能从开始时间还原姿态。

NBT 键：`VicissitudeStage`、`PhaseOneTicks`、`PhaseOneDuration`、`VicissitudeDifficulty`、
`ActionSequence`、`BaseSlotIndex`、`SpecialRotator`、`PhaseOneTargets`、`PhaseOneAnnouncements`、
`PhaseTwoPlayers`、`PhaseTwoStarted`、`DamageAdaptation`。
`PhaseTwoStarted=true` 映射 `PHASE_TWO`；旧档缺时长字段时按 `PhaseOneTicks/6000` 的已完成比例换算到新时长；
旧档不重播没收，也不重复发武器。

### 姿态基线

一阶段：躯干近直立、头下 8°、双臂张开约 20°、高位 V 翼、无武器。
二阶段：躯干前倾约 15°、低头约 15°、翼根比一阶段降低约 25°、右肩前移。
待机上下浮动振幅约 0.08 格、周期 80 tick；羽片微动 ≤3°；
胸环与头后断环反向同速旋转各 0.6°/tick；受击 6 tick 小幅偏移。

---

## 3. 模型、贴图与特效

### 当前交付

| 项目 | 值 |
| --- | ---: |
| 网格部件 | 185 |
| 三角面（完整） | 3,450 |
| 二阶段可见（羽片隐藏） | 3,150 |
| 关节与锚点 | 65 |
| 图集 | 512×512（293 个区域，占用 112,832 像素） |

可编辑源是 `art/first-vicissitude/first_vicissitude.blend`，交付件是同目录的
`first_vicissitude.bbmodel`（Blockbench free mesh）。运行时网格、Blockbench 网格与翼部受击包围盒
由同一个 Blender 导出器输出；连续动画由 Java 侧 `ModelPart` 在运行时求解，
Blender 时间轴只保留一阶段、二阶段、死亡三个检查姿态。

### 关节层级

单一来源是 `rig/VicissitudeRigData.java` 的 `Joint` 枚举，父在前、子在后，
构造参数是相对父枢轴的偏移。以下是实际层级（不是示意图）：

```text
ROOT
└─ BODY
   ├─ TORSO ── CHEST_SHELL_LEFT / _RIGHT / _BACK
   │        ├─ CHEST_RING_LEFT / _RIGHT / _BOTTOM
   │        └─ CHEST_EFFECT_ANCHOR
   ├─ HEAD_ROOT ── HEAD_CORE / HEAD_SHELL_LEFT / _RIGHT / _TOP / HEAD_EFFECT_ANCHOR
   │            └─ HALO_ROOT ── HALO_FRAGMENT_1 … _4
   ├─ ARM_LEFT  ── UPPER_ARM_LEFT ── FOREARM_LEFT ── HAND_LEFT ── LEFT_HAND_EFFECT_ANCHOR
   ├─ ARM_RIGHT ── UPPER_ARM_RIGHT ── FOREARM_RIGHT ── HAND_RIGHT ── SCYTHE_HAND_ANCHOR
   ├─ WING_LEFT_ROOT  ── _UPPER ── _OUTER ── _LOWER ── { _FEATHERS ── _FEATHER_1…4 / _BROKEN_1…2,
   │                                                    _ATTACK_ANCHOR, _TIP }
   ├─ WING_RIGHT_ROOT ── （同上镜像）
   └─ LOWER_ROOT ── SPINE_TAIL_1 ── _2 ── _3
                 └─ LOWER_FRAGMENT_LEFT / _RIGHT
                 └─ CLOTH_FRAGMENT_LEFT / _RIGHT
```

约定：原点取实体底部中央；坐标轴与缩放换算见 `VicissitudeRigData` 的类注释。
`MODEL_ORIGIN_Y = 24`，即模型 y = 24 是实体原点（脚底），高度 h 格对应 `y = 24 - 16h`。
`SCYTHE_HAND_ANCHOR` 是镰刀的唯一挂点，`*_EFFECT_ANCHOR` 与 `WING_*_ATTACK_ANCHOR`
是特效与弹体发射点的唯一来源——不得在实体各处散落固定偏移。

### 形体要点

- **躯干与胸洞**：失败圣像、骨质承重、紫色残甲。胸甲、胸侧和腹甲为宽阔内面、边缘倒角和实心侧壁；
  胸侧两层短肋骨覆片环绕空腔而不填满胸口；肩部是有平面与厚度的六边形护片，连接颈部、胸部与翼根。
  力量感由粗壮肩骨、肱骨粗细变化、肘节和前臂双骨承担，深色残甲只覆盖肩背与前臂外侧。
  胸腔通透以射线检查保证：中央 12/12 条射线贯通，胸洞内缘 32 个方向射线全部贯通。
- **拳**：实体掌背、三组凸出指节与下方回扣指尖，浅指缝止于共用掌体，内侧拇指与指面形成高低差，
  不增加独立指骨。镰柄锚点与手部骨架不变，拳体围绕已有握持中心布置。
- **胸环与背环**：模型空间环心 `(0, -9)`；三个活动环片共用半径 5，底环截面半宽 0.54；
  固定骨质边座半径 6.4、半宽 0.65；环底实际半径约 4.46–5.54。
  实体共 11 段——4 段固定环座、3 段转动胸环、4 段背环；固定环座不跟随胸环旋转。
  胸环保留三段缺口与反向旋转，背环为四段不等长断环。弧段约 15°，断口保留截面宽度。
  银轨、凹槽、卡扣和横向刻印已移入贴图，不再有实体装饰。
- **头**：末地水晶式分层晶核与开裂外壳，八片壳面各有独立关节归属，顶部与左右壳尖为不对称轮廓；
  逐片绘制晶面交界、亮棱和矿物裂口；不加五官。头核正面采样可见比例约 1.3%，绝大部分被外壳遮住。
- **一阶段翼（羽翼）**：每侧八枚主羽、五枚内侧次羽、八枚正面覆羽、四枚背面覆羽；
  主羽沿翼端扇形展开，覆羽遮住相邻主羽根部形成连续翼面。每羽 6 个三角面，
  运行时用 `entityCutoutNoCull`，背面不会因剔除消失。左翼完整，右翼两组主羽缩短并采用破损像素。
- **二阶段翼（骨翼）**：每侧由同一个肩后翼根分出四条不等长镰形主翼，宽度沿长度变化，末端回弯收尖；
  五条较短侧钩贴着主翼生长，主翼之间留长弧形间隙。主翼用 10–12 段控制弧线，侧钩 6 段，
  截面保留三角厚度，不进一步薄片化。
- **下身**：`LOWER_ROOT → SPINE_TAIL_1…3`，无腿。`CLOTH_FRAGMENT_*` 是祭衣残幅，
  由下腹两侧向后包绕、中间敞开，每幅横向四个折面、纵向四段、共 32 三角面，左右长度不同；
  上缘收回腹侧形成衔接，两条折脊提供真实受光变化。不新增上下摆关节或布料物理。
  腹桥与三块腹部残节的外轮廓保持，内面增加浅凸起与内收边，全模型仅增加 106 三角面。
- **转场（模型侧）**：一阶段骨刃保持 45% 伸展量藏于羽层附近，转场第 8–44 tick 平滑伸展到完整形态。
  羽片按翼根到外侧错开脱落，背层稍后；单片持续 24 tick，末尾 7 tick 缩小消失。
  落羽使用原网格、不增加实体或服务端粒子，随实体渲染坐标运动，不是独立的世界物理对象。

### 贴图与 UV

单张 512×512 图集加同 UV 的自发光层 `first_vicissitude_emissive.png`
（只有头核、能量与部分环片被绘制）。左右翼、胸、头壳和主环不完全镜像。

分配规则：

1. 不为 UV 增加几何；导出时用每个面角的 UV 表达接缝，空间坐标与关节不变。
2. 类方块部件按实际长宽高做盒式展开，六个方向有可区分的面区。
3. 多面骨条按截面边长累计 U、沿轴线长度累计 V；首尾接缝展开，端盖独立。
4. 长翼按弧长而非包围盒归一化；正背可分别绘制，厚度带低密度。
5. 长翼 UV 按截面中心之间的累计距离分配，而不是按曲线参数等分，弯曲处的像素密度因此更均匀。
6. 先绘制一侧，镜像后在 UV 与局部受光关系上校对。左右臂共用一套主 UV，左右翼同样优先复用。
7. 拆分单位是「可绘制表面」（可独立绘制、方向明确的一组相邻表面），不是对象数量：
   同一网格可以有多个 UV 岛，不同网格也可以有经过设计的共同 UV。

透明与双面：羽片透明像素只有 0/1；薄面与短分叉远端用不剔除背面渲染显示两侧；
祭衣的参差底边、侧边豁口与撕裂用 alpha cutout，保留现有双面渲染，不叠透明渲染层。

当前尚未替换的旧稿：一阶段羽片与部分附属残片仍沿用旧占位贴图。

上半身骨面使用九级冷灰/灰紫色阶及半级过渡，亮面向根部遮蔽和侧面半影渐变；
下腹主面使用 48×64 独立图稿，祭衣使用 64×128；仅下半身区域提高四倍线性密度。
骨脊、曲线凹槽、边缘亮面和接触半影按新表面密度绘制，不放大旧色块。
祭衣明暗对应实际折面，并保留细边饰与纵向断环纹章，破损边沿用更密像素细化。
旧图集的 263 个区域保持原像素与原密度；上半身、双臂、圆环的造型和纹理采样保持。
所有 UV 与 Blockbench 尺寸同步适配 512 图集，下半身重新分配独立表面区域。

### 几何预算与取舍

预算口径以项目实际导出的三角面数为准，不用某个参考模组的平均值硬卡面数。
「换截面」不记作「删除隐藏面」；减面百分比也不是帧率提升的承诺。

| 部位 | 做法 | 三角面变化 |
| --- | --- | --- |
| 胸环与固定座 | 环截面六顶点→四顶点，去掉两条倒角带 | 1,192 → 800 → 380 |
| 背环与附件 | 同上 | 852 → 588 → 176 |
| 羽片 | 三条纵向顶点轨→两条，折脊改由贴图表达 | 每片 12 → 6；合计 600 → 300 |
| 骨翼与翼根 | 三截面顶点约束简化，采样偏差阈值 0.30 单位，UV 按弧长重分配 | 988 → 808 → 648 |
| 躯干连接 | 六个连接网格由六边截面改为四边截面 | 216 → 136 |
| 十个短分叉 | 远端删第三条隆起骨脊与对应背侧面 | 320 → 160 |
| 祭衣残幅 | 实心尖片 → 宽薄残幅 → 恢复布料体量 | 每片 12 → 6 → 12 |

可以移入贴图的：骨脊亮线、浅凹槽、接缝、划痕、崩边、局部阴影、银轨、卡扣、横向刻印、羽片折脊。
不能移入贴图的：外轮廓、真实孔洞、关节活动、必要的侧面厚度。
薄片两侧共用纹理的代价已有记录——背面比实体版更亮，沿片面方向观察时远端失去厚度，
纹理补不回这项几何损失，因此该方法没有扩展到大块主翼。

### 特效接入

复用项目已接入的 Lodestone / Malum 接口，`applyAndCache` 缓存 RenderType，尽量不复制 Malum 贴图。

| 能力 | 接口 | 用在哪 |
| --- | --- | --- |
| 可调粒子 | Lodestone `WorldParticleBuilder` | 翼尖蓄光、释放碎芒、死亡熄灭 |
| 灵魂光点与柔光 | Malum `SpiritLightSpecs.spiritLightSpecs` / `spiritBloom` | 头核局部光、翼根与羽片依次点亮 |
| 旋转光点 | `SpiritLightSpecs.rotatingLightSpecs` | 头后断环失稳时的少量绕行光点 |
| 历史拖尾 | Lodestone `TrailPointBuilder` + Malum `RenderUtils.renderEntityTrail` | 飞镰、冲刺残迹、追踪球曲线 |
| 线段/光带 | `VFXBuilders.WorldVFXBuilder.renderBeam` | 左手到飞镰的细线 |
| 地面法阵 | Malum `ParticleRegistry.RITUAL_CIRCLE` / `CIRCLE` + `DirectionalBehaviorComponent(new Vec3(0,1,0))` | 胸标记的整圆符文、环裁定的断环符文 |
| 斩击方向 | Malum `ParticleHelper.SlashParticleEffectBuilder` | 横斩、竖劈、重击各自的斩弧 |
| 屏幕震动 | Lodestone `ScreenshakeHandler` + `PositionedScreenshakeInstance` | 转场、重击命中、死亡翼根失效 |

关键约束：

- **地面法阵必须用 `LUMITRANSPARENT` 且颜色为 `SIGIL_BLACK`**。Malum 的符文贴图是全不透明的白字黑底
  （alpha 恒为 255），普通 `TRANSPARENT` 会拍出一整块黑方块；该着色器先用贴图自身亮度替换贴图 alpha、
  再乘顶点色。想换颜色只改 `SIGIL_BLACK`，不要改渲染类型。
- **不要再用 `RenderType.lightning()` 画地面预兆**：那是原版闪电/天气的渲染类型（加色、全亮、
  写天气目标），任何颜色都会显成发白发黄的一圈。
- 预兆符文全部 `enableForcedSpawn()`，最低粒子档位下仍然显示；模型本身不依赖粒子开关。
- 一阶段扇射提前 12 tick 蓄力；胸标记触发后约 8 tick 淡出。
- 环裁定用 12 个固定槽位、每 tick 复活一枚、单枚寿命 14 tick；
  槽位角度与符文尺寸都按 60° 安全缺口内缩，不能画满圆把安全区盖掉。
- 飞镰与追踪球每客户端 tick 最多采一个历史点，保存 12–20 tick 的短历史；帧渲染只插值，
  不能让高 FPS 客户端尾迹更长。
- builder 是可变对象，复用时重置颜色/alpha/UV，避免跨实体串色。

震动只在三个一次性事件触发，经 `VicissitudeEffectPacket` 按
`entityId + eventId + actionSequence` 去重（记忆 128 条），晚加入不补播：

| 事件 | 时长 | 峰值 |
| --- | ---: | ---: |
| 一阶段转场第 1 tick | 16 | 0.25 |
| 重击命中 | 12 | 0.30 |
| 死亡 36–59「翼根失效」 | 12 | 0.18 |

全强度距离 8 格、最大 24 格。客户端倍率 `firstVicissitude.screenshakeIntensity`（0–1，0 关闭）。

---

## 4. 技能规格

共用：20 tick = 1 秒；**D = 自身 `ATTACK_DAMAGE` 属性值**（一阶段 8，二阶段含武器加成）。
所有攻击有唯一 `actionSequence`，服务端只执行一次释放/命中事件。
追踪目标死亡或换维度即失效，不自动换目标；弹体只打本战斗的有效参与者。

每个动作的时长、命中帧、命中窗口与刀身判定见 [02](02_ANIMATION_AND_HITBOX.md)；
下面只列调度、弹道和伤害。

### 瞄准承诺

前摇前段只朝该目标有限速转向，`aimLockTick` 之后锁住朝向与弹道瞄点直到收势结束。

| 动作 | 停止跟踪 tick | 释放 tick | 定向前固定窗口 |
| --- | ---: | ---: | ---: |
| 横斩 | 4 | 8 | 4 |
| 竖劈 | 8 | 14 | 6 |
| 重击 | 14 | 24 | 10 |
| 冲刺 | 14 | 20 | 6 |
| 翼尖扇射 | 8 | 12 | 4 |
| 三波齐射 | 14 | 20 / 28 / 36 | 首波前 6，后两波沿用瞄点 |
| 飞镰与普通远程 | 10 | 16 | 6 |
| 胸 / 环 | 10 | 30 / 40 | 保留独立地面预兆 |

两阶段施法与近战都制动悬停，收势不继续贴身滑行；冲刺按独立位移执行。
无动作时一阶段绕行、二阶段持镰接近。

### 一阶段

| 技能 | 触发与时序 | 生成与轨迹 | 命中 |
| --- | --- | --- | --- |
| 翼尖扇射 `WING_RANGED` | 首轮第 20 tick；基础攻击槽每 30 tick；提前 12 tick 翼尖亮起 | 左右交替，一次 5 球；水平 −24/−12/0/12/24°、0.45 格/tick、不追踪 | 压至最多 1 血；寿命 80 tick |
| 命运追踪球 | 每第二个扇射槽（`fanCount % 2 == 1`），替代中心那枚 | 1 球追踪、穿地形、最多 200 tick，其余 4 球直线 | 同样压至最多 1 血 |
| `WING_BARRAGE` 羽片齐射 | 冷却 70 tick；20 tick 蓄力 → 每 8 tick 一波共 3 波 → 收势 | 每波左右翼共 8 球，交错扇形，不追加追踪球 | 每球压至最多 1 血；同波同目标去重；波间留可通行缺口 |
| `CAST_FROM_CHEST` 命运标记 | 冷却 90 tick；30 前摇 / 20 收势 | 目标脚下半径 2 格区域，第 10 tick 后位置固定 | 1D 普通伤害；落点最多向下 8 格搜索，判定高度为落点向上 2 格 |
| `CAST_FROM_HALO` 星轨裁定 | 冷却 80 tick；40 前摇 / 20 收势 | 内半径 3、外半径 6 格的断环，单次判定 | 1D 普通伤害；中心、环外与 60° 缺口安全 |

**一阶段所有弹只压血、伤害为 0**；一阶段对玩家的普通伤害只有胸与环两处，恒用 light 档。

调度：特殊技能按 羽片齐射 → 胸 → 环 轮换；只有偶数基础槽才尝试特殊技能，
其余时用基础扇射。冷却从开始算，空目标不推进选择；特殊技能占用期间跳过基础槽，
不补发积压弹幕。任意时刻只有一个主动作。
上限：每 Boss 同时非追踪球 ≤48（`MAX_NON_HOMING_BOLTS`）、追踪球 ≤2（`MAX_HOMING_ORBS`）；
达到上限直接跳过生成，不延迟补发。

### 二阶段

| 技能 | 使用条件 | 时序 | 判定 |
| --- | --- | --- | --- |
| `SLASH_HORIZONTAL` | 有武器、≤5.75 格 | 8 前摇 / 命中 / 11 收势；冷却 20 | 刀刃扫掠，1D |
| `SLASH_VERTICAL` | 近战动作每第三次 | 14 / 命中 / 15；冷却 30 | 刀刃扫掠，1.25D |
| `HEAVY_ATTACK` | 同上距离开、冷却 120 tick、优先于普通斩 | 24 / 命中 / 25 | 刀刃扫掠，1.75D |
| `DASH` | 8–20 格、冷却 160 tick | 20 前摇 / 最多 12 tick 冲刺 / 收势 | 锁定前摇结束方向，最多 10 格；扫掠体积每目标一次 1D；实心障碍停止，不穿墙 |
| `SCYTHE_THROW` | 6–24 格、持武器、无飞行镰刀、冷却 120 tick | 16 蓄力 / 释放 / 12 放手，接回 10 tick 恢复 | 去程 1D、回程无伤害；空手不斩击 |
| `RANGED_FALLBACK` | >24 格，或 ≥6 格且镰刀在飞 | 冷却 60 tick；16 前摇 / 收势 | 翼尖 3 枚非追踪球，−10/0/10°、0.65 格/tick、寿命 120 tick，每枚 0.75D |

调度：>24 格远程并接近；6–24 格优先 投掷 → 持械且视线畅通的冲刺 → 远程特殊轮换 → 普通远程；
近距离重击可用则重击，否则横斩/竖劈轮换；>64 格只接近不发射。
飞镰离手期间以约 8 格距离侧移；接镰恢复排在当前动作之后；飞镰期间不发动持械冲刺；
投镰冷却不在起手时清零。同一轮弹幕同一目标最多接受一次普通球伤害。

---

## 5. 伤害与承伤

### 探针阶梯

玩家目标走 ChangeLib 的 `DamageProbe`：light = `lighterDamageMethod`（首次生效即止，
护甲/附魔/伤害上限照常参与）；medium = `mediumDamageMethod`（持续补足到指定数值真正扣掉）。

难度表只管**二阶段**：SIMPLE / DIFFICULT 用 light，COMPLETE / EXTREME 用 medium。
**一阶段对玩家的伤害恒用 `lighterDamageMethod`**——一阶段的伤害都发生在压血之后，
medium/final 会让护甲形同虚设。非玩家目标始终走普通 `hurt`，不用探针。

### 修正顺序

`modifyIncomingDamage` 依次为：**非玩家 ×0.5 → 适应 → 距离衰减**，三者独立相乘。

- 非玩家减伤：`getEntity()` 或 `getDirectEntity()` 是玩家（本人近战、玩家射出的弹体、
  玩家点燃的爆炸）算玩家的账；其余来源固定 ×0.5（`NON_PLAYER_DAMAGE_MULTIPLIER`）。
- 距离衰减：`firstVicissitude.damageRange`（默认 48 格，范围 4–128）内全额，
  往外线性衰减，**1.5×（默认 72 格）处为 0**；无活体攻击者的伤害不参与。
- 一阶段免疫、转场、死亡与 `source.getEntity() == null` 的无来源伤害在 `hurt` 里被
  `isDamageImmune` 挡掉，走不到这里。

### 适应效果

按 `DamageSource#getMsgId()`（damage message，如 `player`/`arrow`/`scythe_sweep`/`mob`）记账。
窗口容量 = `firstVicissitude.adaptationLevel`（「适应几」，默认 2，范围 0–16，**0 关闭**）；
已记录的消息第 n 次命中吃 **e⁻⁽ⁿ⁻¹⁾**（第 2 次 ≈0.368、第 3 次 ≈0.135、第 4 次 ≈0.050）。
只按次数、不设时限，唯一清账时机是回到未参战。

顺序是**先记录、再判定**：命中的消息先记进窗口（已在窗内则前移到最新），
然后才决定倍率——所以刚记进来的那条全额。记满后顶掉**最久未命中**的一条，其计数一并作废。
读不出 message（null 或空串）时既不减伤也不占格；同一种 message 只占一格。

三条推论（`AdaptationTest` 钉住）：适应 1 与适应 2 都无法减伤 A→B→C 三消息轮换
（每次记进来时上一条刚被挤出）；两条轮换在适应 2 下从第三下起递减；
开发用镰刀一次挥砍落三条消息（`scythe_sweep`/`voodoo`/`freeze`），默认适应 2 下永远全额。

NBT 写 `DamageAdaptation` 列表（`Message` + `Hits`，顺序即最新在前）；
重载恢复时每条算「刚挨过一次」，次数不跟存档走。类：`common/entity/DamageAdaptation`。

### 压血

一阶段扇射弹、齐射弹、追踪球命中只把目标压到 **1 血**，弹体本身伤害为 0。
压血之后**没有**喘息窗口：只按原本的攻击槽与前摇走，胸/环也不作废重起。
二阶段同款扇射/齐射弹是 `attackDamage × 0.75` 的普通伤害。

### 生命账本与击杀计数

- 真生命走账本 + capability：`VicissitudeVitality`（current / maximum / gateOpensAt /
  killAttempts / lastKillTick），世界级 `VicissitudeVitalityLedger`。
  死掉的 UUID **故意不清理**，这样旧实体副本无法复活。
- 每击上限 = 难度基准比例 × 最大生命 × 部位权重。
  基准比例：SIMPLE **0.10**、DIFFICULT 0.05、COMPLETE / EXTREME 0.01。
- 命中门：每次有效命中把 `nextHit` 重算为 `now + 20` tick；窗口内落下的后续伤害
  × `1 - 剩余/20`，窗口内的命中同样把窗口重新推到 `now + 20`；
  同一 tick 内的重复命中直接返回 false。这层与适应互不影响。
- 击杀计数：`killAttempts` 最多 3 次、窗口 100 tick、同 tick 重复只算一次；
  第 3 次把生命归零即真死。直接 `setHealth` 不是另一条权威。
- 掉落与经验只在真死时结算一次；死亡动画、存档重载与 probe 都不重复结算。

---

## 6. 参战、仇恨与目标

- **受击即结仇，不挑视线**：`onIncomingAttack` 没有任何视线判断，打它的人一律进名单
  （守者、僵尸、宠物、召唤物、别的生物都算），隔着墙打中它一样会转过来还手。
  `DORMANT` 被任何人打中都会进入一阶段。
- **创造与旁观例外**：`isIgnoredPlayer` 在入口挡掉——他们出手不算数、不结仇、也不会被追打。
- **宠物与召唤物算在主人头上**：`ownerOf` 按 `OwnableEntity#getOwnerUUID` 解析；
  宠物或召唤物打过来时它自己和主人都进名单，第一次还手优先主人。
  主人若是创造/旁观，则只剩宠物参战。
- **与原版 `HurtByTargetGoal` 对齐**：谁打我我打谁、不检查视线；驯服生物的攻击算在主人头上；
  创造与旁观不是目标；怪物优先盯玩家。只剩两条自有规则——交战半径与空场回退。
- **交战半径**：`firstVicissitude.engagementRange`（默认 96 格，范围 4–256；`FOLLOW_RANGE` 96）。
  超出就不追，原位悬停。
- **二阶段选目标**：`phaseTwoParticipants`（存档键仍是 `PhaseTwoPlayers`）在转场时整体接管一阶段名单，
  宠物、召唤物和别的生物都能一路打到二阶段。`selectBalancedPhaseTwoTarget` 先在没有被无视的玩家参战者里挑
  **血最多的**，没有才轮到非玩家；一阶段轮换用同一优先级。

| 持有者 | 内容 | 清理时机 |
| --- | --- | --- |
| `phaseOneTargets` | 一阶段参战者 | 死亡/离线/换维度/被宽恕/区块与实体都没了 → 移出；回退未参战时整表清空 |
| `phaseTwoParticipants` | 二阶段参战者 | 同上；转场时整表重建 |
| `targetPlayerDeaths` / `phaseTwoPlayerDeaths` | 玩家阵亡计数 | 随对应名单条目删除 |
| `announcedPlayerDeaths` | 每名玩家每命一次的开场白 | 随一阶段名单条目删除 |
| `roundDamagedTargets` / `dashHitTargets` | 本轮已命中的目标 | 每次起手 / 每次冲刺清空 |
| `confiscated` | 没收但还没还的饰品 | 交付成功后移出；Boss 真死时整体转入世界账本 |
| `VicissitudeVitalityLedger`（世界级） | 每只无常的真生命 | 故意不清理 |
| `VicissitudeCurioLedger`（世界级） | 待交付饰品 | 交付后移出；离线玩家由登录/重生/克隆钩子继续送 |

名单清理每 10 tick 一次（`forgetDeadParticipants`）与回退时一次，规则统一为「**不在场就退出名单**」：
解不开 UUID（离线、区块没加载、不在世界上）、不在这个维度、已经死了；
玩家额外走原版死亡计数规则（`forgiveDeadPlayers` 决定阵亡后是原谅还是继续记仇）。
离线玩家同样退出名单——饰品返还走账本加登录/重生/克隆钩子，不依赖名单。

---

## 7. 碰撞、部位与脱困

### 部位权重

躯干与环 1.0、头外壳 1.25、翼根 0.75、外翼 0.5。作用点是**单次命中上限**
（`Segment#capWeight()`）：容易打中的部位允许吃下更多，而不是每一击先被乘一遍——
两处都乘会把同一个优势算两次。范围伤害无法定位部位时按躯干 1.0；
同时命中多段只取一次有效命中，弹体取最近接触点，范围攻击取覆盖部位的最高倍率但不累加。

### 翼与阻挡

每翼 3 个粗分段受击区域，随服务端权威翼姿态更新，覆盖骨羽主体、不填满羽片之间全部空隙。
命中转发同一个 Boss 的 hurt / 真生命流程，不设翼生命或断翼玩法。
一阶段免伤仍适用，但命中翼仍能登记攻击者开始战斗。

双翼也阻挡玩家和地形。服务端检查身体位移及翼段旋转的**扫掠范围**（不只查本 tick 终点）；
撞到实心方块先停止受阻运动并在允许关节范围内收翼，再绕行或尝试破坏；
破坏只检查实际受阻段，不按整个最大翼展矩形清场。玩家被翼扫到有限推离（水平 0.06），
不造成未经定义的接触伤害；玩家被墙夹住时限制翼运动，不把玩家推入墙。

方块破坏权限沿用现有检查（`mobGriefing`、方块自身许可、Forge 销毁事件），
不写领地绕过，不做模组特判；被任何规则拒绝或破坏失败且卡住时走脱困瞬移。
碰撞时每 10 tick 检查拆方块。

### 身体接触

无常**不接受任何碰撞冲量**：`isPushable()` 恒为 false，三参 `push` 覆写为空实现。两者都要，
因为原版有两套入口——`pushEntities` 靠 `EntitySelector#pushableBy` 先筛「可推」候选，
而撞击类攻击（末影龙、劫掠兽、疣猪兽冲撞、监守者音爆、行进中的矿车）与部分模组直接调三参 `push`，
根本不看 `isPushable`。位置与速度只由它自己的移动代码改动（`steerToward`、冲刺的 `setPos`、脱困瞬移）。

玩家贴进身体包围盒时仍走原版分离：Boss 自己的 `pushEntities` 每 tick 把玩家水平推开 0.05，
玩家那一侧因为 Boss 不可推而完全不参与。翼部推离（水平 0.06）是独立的另一条，不受影响；
武器的击退本来就由 `KNOCKBACK_RESISTANCE = 1.0` 吃掉，与这条无关。

### 脱困瞬移

触发条件：有移动目标、连续 40 tick 位移不足 0.25 格，且扫掠范围有阻挡或破坏被拒绝/失败。
正常悬停、攻击定身、转场、死亡不算受困。

搜索以实体为中心水平半径 4/8/12/16 格、垂直 ±8 格，优先近点，每次最多检查 64 个候选，
只查已加载区块、不为搜索强载世界；失败 40 tick 后重试，成功后冷却 100 tick。
落点检查当前及短期展开后的身体与双翼所有碰撞段，必须在世界边界与建筑高度内，
与实心方块及玩家不重叠；飞行 Boss 不要求脚下有地面。
无有效点则留在原位继续受限重试，不塞入墙中、不删除实体。

成功时服务端原子更新位置与碰撞段并清除速度，保留 UUID、真生命、阶段、目标和饰品记录；
中断当前攻击进入 recovery、取消未发生的释放、不重放命中；已有飞镰转入回收或按 token 超时收回。
瞬移只为脱困，不是无预兆追击技能。

### 范围维护

身体碰撞 1.6×4.375 格；广相碰撞覆盖最大翼展，细分段随动作变化；
实体主身体碰撞、渲染裁剪范围与分段受击范围分别维护；
视锥裁剪包含最大翼展与主环，飞行镰刀独立裁剪。
不能只扩大 `renderer.scale` 而留下玩家大小的隐形命中箱。

---

## 8. 掉落与交付

### 常规掉落表

四张表内容相同，四个表 ID 保留：
`maledict:entities/first_vicissitude`（SIMPLE，也是实体默认路径）、
`..._difficult`、`..._complete`、`..._extreme`。

| 奖励 | 数量 | 条目 |
| --- | ---: | --- |
| 深岩珍金块 | ×2 | `malum:block_of_cthonic_gold` |
| 虚无板石 | ×6 | `malum:null_slate` |
| 虚空盐 | ×6 | `malum:void_salts` |

三池都是 `rolls 1`、固定数量、必掉，不吃抢夺（表里没有 `looting_enchant`），
不要求玩家击杀，受 `doMobLoot` 约束。数量只由 `MaledictEntityLoot` 的三个常量决定：
`CTHONIC_GOLD_BLOCKS = 2.0F`、`NULL_SLATE = 6.0F`、`VOID_SALTS = 6.0F`。
`src/generated` 下的表是 runData 产物，禁止直接编辑。

经验不进表，由原版流程按 `xpReward` 发放，当前 1000。

### 启蒙之年

`maledict:age_of_enlightenment`，**仅玩家击杀**才发。
覆写 `FirstVicissitudeBossEntity#dropCustomDeathLoot`，与下界之星同一条路
（`spawnAtLocation` + `setExtendedLifetime`，不自然消失）。
判定只看原版 `recentlyHit`（`lastHurtByPlayerTime > 0`，即最后 100 tick 内有玩家，
含玩家射出的弹体与玩家驯服的宠物造成的伤害）。

等级跟难度走 I–IV，NBT 键名是 `Maldict:EnlightenmentLevel`（原样拼写），
写进去的是 amplifier，`0` 就是游戏内 I 级；等级由 `BossDifficulty#enlightenmentLevel()` 唯一决定，
写等级只有 `AgeOfEnlightenmentItem#create(int)` 一个入口。

### 精魂表

`src/main/resources/data/maledict/spirit_data/entity/first_vicissitude.json`，手写资源，不是 runData 产物。
`registry_name` = `maledict:first_vicissitude`，`primary_type` = `eldritch`；
八种精魂各 6 加幽影 1，合计 **49** 点灵魂强度 → 提尔锋额外伤害 **98**（`totalSpirits × 2`）。

精魂只在 Malum 认定的「灵魂暴露」状态击杀才爆出（需要带 `malum:soul_hunter_weapon`
标签的武器）；无此表时 Malum 会套 `DEFAULT_BOSS_SPIRIT_DATA`（邪术 2 点）。

### 召唤仪式

四个灵组合唯一的仪式，档次直接决定召唤难度；不改动 Malum 任何现有仪式。

| 难度 | 仪式 ID | 配方 |
| --- | --- | --- |
| SIMPLE | `vicissitude_rite` | 奥术 ×3（3 柱） |
| DIFFICULT | `greater_vicissitude_rite` | 奥术 ×4（4 柱） |
| COMPLETE | `eldritch_vicissitude_rite` | 邪术 ×1 + 奥术 ×3（4 柱） |
| EXTREME | `greater_eldritch_vicissitude_rite` | 邪术 ×2 + 奥术 ×3（5 柱） |

自下而上摆放，邪术只出现在后两档。**幽影精魂放不上图腾柱是 Malum 写死的**
（`MalumLogBLock#createTotemPole` 对 `UMBRAL_SPIRIT` 直接返回 false），
且点击面为 UP/DOWN 直接失败——必须右键原木侧面；可用柱灵只有八种。
`SummoningRite#poleSpirit` 会拦掉含幽影的配方并打 warning。

召唤时读配置、64 格内已有同类型实体则跳过、按 3/4/6/2/8 高度找无碰撞落点
（全堵时放最低候选点交给脱困）、朝向取图腾朝向、播 `malum:soul_shatter` 与灵魂火粒子。
配置：`summoningRite`（默认 true，关掉恢复 Malum 原版行为）、
`summoningRiteEntity`（默认 `maledict:first_vicissitude`）。

---

## 9. 饰品没收与返还

### 没收范围

只没收**实际装备的 Curio**，只在 COMPLETE / EXTREME 的转场完成点执行；
不扩展到装饰槽、背包或其他难度。

免没收 item tag：`maledict:vicissitude_confiscation_immune`
（`common/MaledictTags.java` 的 `VICISSITUDE_CONFISCATION_IMMUNE`）。
每次实际没收前检查 `stack.is(tag)`，命中则留在原槽、不入队列、不触发摘下回调。
标签由 `data/MaledictItemTags.java` 生成，默认是空标签；整合包可以用数据包扩充：

```json
{"replace": false, "values": [{"id": "examplemod:protected_curio", "required": false}]}
```

数据包重载只影响下一次没收，不撤销已经发生的没收。

### 选定方案：自动返还 + 凭证兜底

| 方案 | 成本 | 缺点 |
| --- | --- | --- |
| 事件触发自动返还 | 每次重生/死亡/登录只处理该 UUID | 要处理满背包与槽位已被占用 |
| 必须右键凭证 | 仍要执行同样的 N 件返还，只把工作推迟 | 占格、多一步操作、丢失还要补发 |
| **自动返还 + 凭证兜底** | 兼有两套逻辑 | 满背包不会吞物品，这是选它的唯一理由 |

实现要点：

1. 返还顺序是**原合法空槽 → 其他合法空 Curios 槽 → 背包**；不挤掉现有装备
   （要求槽位为空且 `isItemValid`），不跳过 Curios 验证，完整保留附魔与自定义 NBT。
2. 背包不足的部分留在服务端持久记录，不丢地上、不删除，并向玩家显示剩余件数。
3. 只按 UUID 定向检查：登录、重生、克隆、死亡四个事件，另有一条只含在线待返还 UUID 的
   低频队列，每 20 tick（`QUEUE_INTERVAL_TICKS`）检查一次，
   同一 owner 两次尝试至少间隔 100 tick（`ATTEMPT_COOLDOWN_TICKS`）。无人待返还时不扫描。
4. 成功后只从持久记录扣除已交付条目（`VicissitudeCurioLedger#replace`），
   部分交付不会复制或删除整叠。
5. 同一 stack 要么在活着的 Boss 手里、要么在账本里，绝不两边都可返还；
   Boss 真死时队列整体转入世界账本。
6. 防止返还自身触发槽位事件递归重入：同一玩家一次处理中只合并下一次检查。

兜底凭证 `maledict:curio_return_token` 只在主人 UUID 上关联世界级记录，
不把饰品 NBT 塞进可复制物品；**全部交付完成才消耗凭证**。
发放时保证持有者最多一枚；连凭证都放不下时账本保留余额、之后重试。

| 类 / 文件 | 作用 |
| --- | --- |
| `common/MaledictTags.java` | 免没收标签 |
| `common/curio/VicissitudeCurioReturns.java` | `confiscate` / `retain` / `deliverAll` / `deliver` / `equip` / `claim` / `issueToken` / `consumeTokens` + 四个事件订阅 + 20 tick 队列 |
| `common/curio/VicissitudeCurioLedger.java` | 世界级 `SavedData`，`FILE_ID = "maledict_vicissitude_curios"` |
| `common/item/CurioReturnTokenItem.java` | 凭证物品，右键调用 `VicissitudeCurioReturns.claim` |
| `common/entity/FirstVicissitudeBossEntity.java` | 持有 `confiscated`，Boss 真死时把剩余饰品移交账本 |

**没有配置键**：饰品没收与返还不可通过配置开关，唯一可调项是数据包标签。

---

## 10. 客户端表现边界

- **职责划分**：Entity/Goal 管服务端阶段与命中；Model 管骨骼与姿势；Renderer 管材质、
  视锥范围和持物 layer；emissive layer 只画少量冷白核心与裂缝；effect layer 管命运线、星轨；
  独立 Projectile 管投掷轨迹与命中。
  **禁止把飞出去的镰刀藏在 Boss 模型里用 visibility 假装弹体。**
  所有纯视觉代码只在客户端运行（`@OnlyIn(Dist.CLIENT)`），不能被 common 静态初始化引用。
- **投掷物所有权**：持握 → 去程 → 回程 → 接住 → 持握；同一 Boss 最多一个有效投掷 token。
  只在二阶段、主手有武器、当前没有飞行镰刀且看见有效目标时选择；6–24 格优先投掷，冷却 120 tick。
  初速 0.8 格/tick；出行最多 20 tick 或 16 格，命中方块即开始返回；总寿命最多 100 tick。
  去程只对当前锁定目标命中一次，伤害取 Boss 攻击属性、使用 `SCYTHE_SWEEP`，
  不自动附加玩家 Incursus 的整套主动技能；回程无伤害。
  返回距离手部小于 1 格则归还武器并进入 recover。
  Boss 死亡则取消并销毁 Boss 专用武器副本，不掉落；Boss 武器不让玩家捡取。
- **渲染与实际手持分离**（D11）：右手画的是按同步难度档位在客户端重建的 ItemStack
  （`getDisplayWeapon` / `DATA_WEAPON_TIER`），缴械、换装或删除手上物品都不会让 Boss 看起来空手。
  手上仍维持一把同档武器（空手立即补、类型不符每 20 tick 纠正一次），
  飞行镰刀状态不被该纠正触碰。三种武器分别校准握持与飞行显示尺寸。
- **预留稳定语义事件**：战斗开始、阶段转换开始/完成、攻击蓄力/释放、投掷/返回、受伤、
  死亡开始/完成、结束跟踪。客户端的声音/音乐/屏幕表现消费同一 stage/action 信息，
  以 Boss UUID 区分实例，不从 renderer 每帧反复播放。
- 全屏滤镜、自定义 shader、新音乐当前不制作，但保留接入位置。
