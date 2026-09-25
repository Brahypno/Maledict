# 02 — 模型、资产与锚点接口

最新交付为 215 部件 / 5,552 三角面，二阶段可见 4,952 面，见 [20](20_PAINTED_MASS_AND_FEATHER_SHEDDING.md)。胸环中心从运行时骨架导出，供洞缘和环片共同使用；以下较早交付数量保留为历史。

上一阶段资产为 186 部件 / 3,572 三角面，历史对照、肢体与握拳修订见 [17](17_BODY_MASS_AND_HISTORY_REVIEW.md)。16 的 2,188 面和下方 1,792 面记录均为历史阶段。

2026-09-20：保留本页骨架与锚点接口；最新骨翼/羽翼修订为 128 部件 / 1,792 三角面，见 [15](15_WING_STRUCTURE_AND_FEATHERS.md)。参考统计见 [14](14_REFERENCE_GEOMETRY_BUDGET.md)，行为修订见 [13](13_COMBAT_AND_STYLE_REFINEMENT.md)。

采用 Forge 1.20.1 原版 HierarchicalModel/ModelPart，一套基础模型服务两阶段，建议不新增 GeckoLib。原稿层级的手臂和头部分居 root 下会增加躯干联动工作；采用下列实际父子关系。

2026-09-13 用户要求使用 Blender 提升造型精度：现采用 Blender 多边形网格绑定下述原版关节，
由 `VicissitudeBlenderMesh` 提交三角面，保留 ModelPart 动画及锚点；Blockbench 交付改为 free mesh 格式，
并增加可编辑 `.blend`。具体资产和复现步骤见 `art/first-vicissitude/README.md`。

```text
root
└─ body_root
   ├─ torso（容器，不用实心 cube 填胸口）
   │  ├─ chest_shell_left/right/back_fragments
   │  ├─ chest_ring_left/right/bottom
   │  └─ chest_effect_anchor
   ├─ head_root
   │  ├─ head_core / head_effect_anchor
   │  ├─ head_shell_left/right/top / crown_fragments
   │  └─ halo_root → halo_fragment_1..4
   ├─ arm_left → upper_arm_left → forearm_left → hand_left → left_hand_effect_anchor
   ├─ arm_right → upper_arm_right → forearm_right → hand_right → scythe_hand_anchor
   ├─ wing_left_root → upper_l → outer_l → feathers_l / attack_anchors_l / tip_l
   ├─ wing_right_root → upper_r → outer_r → feathers_r / attack_anchors_r / tip_r
   └─ lower_root → spine_tail_1 → spine_tail_2 → spine_tail_3
                → lower_fragment_left/right / cloth_fragment_left/right
```

箭头表示父子关系；lower_root 的碎片与 spine_tail_1 并列。每翼另加 lower bone、约 4 主羽和 2 破羽；羽片应挂在负责带动它的翼骨上。肩、肘、腕、翼关节和脊柱 pivot 放在真实连接位置。

预算为 35–55 个真正独立驱动的活动部件，纯容器与零尺寸锚点另计。不能把预算误读为所有 ModelPart 的总数上限。

## 实施输出（建议路径）

- `src/main/java/org/brahypno/maledict/client/model/FirstVicissitudeBossModel.java`：网格、默认姿态、动画混合、锚点访问。
- 既有 `client/FirstVicissitudeBossRenderer.java`：替换临时 PlayerModel，并登记新的 layer definition。
- `src/main/resources/assets/maledict/textures/entity/first_vicissitude.png` 与 `_emissive.png`：256×256，同 UV；左右翼、胸、头壳和主环不完全镜像。
- 已确认交付 `art/first-vicissitude/first_vicissitude.bbmodel`，保存可编辑层级、pivot、UV 和贴图引用；游戏侧 Java 模型与其保持对应，不依赖运行时读取 bbmodel。所有三种武器与阶段预览应可复现。bbmodel 不放进运行时资源。
- 需要生成的 item model、语言、codex 等改对应 data generator，再运行 runData，禁止直接改 `src/generated`。

## 坐标与 pose

模型 bind pose、世界坐标和 renderer 的变换由一处文档/常量明确：原点取实体底部中央，模型尺度仅应用一次；原版模型坐标到实体局部坐标的轴向翻转由 helper 处理。绑定层在肩→臂→腕完整父链变换后绘制 Item，使用第三人称右手显示上下文，再加每种武器握柄微调。

每帧先 resetPose，再基础阶段插值、动作、look、hover 和小幅 follow-through，最后计算锚点。优先级：死亡 > 转场 > 攻击 > 受伤细动 > idle。受伤不覆盖释放帧，死亡不叠加飞行摆动。模型实例跨实体复用，不能把上一只 Boss 的显隐/角度留给下一只。

## 服务端与客户端锚点

- common 侧建立不引用 client 类的 `FirstVicissitudeAttackGeometry`（建议名），统一模型单位换算、攻击 pose 的关键位置和释放偏移。
- 服务端由动作 ID、动作 tick、朝向和实体位置得到发射点；客户端 ModelPart 锚点仅用于持物、线条和粒子。
- 主翼攻击点左右各至少 1 个，翼尖、胸、头、左右手保留独立接口。不得在 Entity 各处散落固定偏移。
- 渲染使用 partialTick 插值，伤害使用服务端 tick；旋转 0/90/180/270 度与俯仰时，发射点均须贴近翼尖或手部。建议释放误差不超过 0.25 格。
- 胸/环技能与全套翼、镰刀动作均须接入实际战斗，详见 07；锚点不能代替技能完成。
