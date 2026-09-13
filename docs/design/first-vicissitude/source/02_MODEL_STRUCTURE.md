# 02 — 模型结构与骨骼规格

## 1. 技术基线

当前默认：
- Minecraft Forge 1.20.x
- 暂不使用 GeckoLib
- 优先 `HierarchicalModel` / `ModelPart`
- 一个基础模型服务两个阶段
- 阶段差异通过 rotation、position、visibility、遮挡关系和 Render Layer 实现

不要一开始维护两套完全独立模型。

---

## 2. 推荐总层级

```text
root
│
├─ body_root
│  ├─ torso
│  │  ├─ chest_shell
│  │  ├─ chest_ring
│  │  ├─ chest_void_anchor
│  │  ├─ left_shoulder
│  │  └─ right_shoulder
│  │
│  └─ lower_root
│     ├─ spine_tail_1
│     │  └─ spine_tail_2
│     │     └─ spine_tail_3
│     ├─ lower_fragment_left
│     ├─ lower_fragment_right
│     ├─ cloth_fragment_left
│     └─ cloth_fragment_right
│
├─ head_root
│  ├─ head_core
│  ├─ head_shell_left
│  ├─ head_shell_right
│  ├─ head_shell_top
│  ├─ crown_fragment_1
│  ├─ crown_fragment_2
│  └─ halo_root
│     ├─ halo_fragment_1
│     ├─ halo_fragment_2
│     ├─ halo_fragment_3
│     └─ halo_fragment_4
│
├─ arm_left
│  ├─ upper_arm_left
│  └─ forearm_left
│     └─ hand_left
│
├─ arm_right
│  ├─ upper_arm_right
│  └─ forearm_right
│     └─ hand_right
│
├─ wing_left_root
│  ├─ wing_bone_upper_l
│  │  └─ wing_bone_outer_l
│  ├─ wing_bone_lower_l
│  ├─ primary_feather_l_1
│  ├─ primary_feather_l_2
│  ├─ primary_feather_l_3
│  ├─ primary_feather_l_4
│  ├─ broken_feather_l_1
│  └─ broken_feather_l_2
│
└─ wing_right_root
   ├─ wing_bone_upper_r
   │  └─ wing_bone_outer_r
   ├─ wing_bone_lower_r
   ├─ primary_feather_r_1
   ├─ primary_feather_r_2
   ├─ primary_feather_r_3
   ├─ primary_feather_r_4
   ├─ broken_feather_r_1
   └─ broken_feather_r_2
```

名称可以调整，但独立动画职责不要被合并。

---

## 3. 头部

### head_core

建议视觉尺寸约：
- `6 × 7 × 6 px` 左右

不需要做成普通方块脑袋。

目标：
- 尖锐
- 星芒
- 多面
- 中央收束

### head_shell

至少拆：
- left
- right
- top

这样支持：
- 第一阶段封闭
- 第二阶段错位/张开
- 死亡进一步崩解

---

## 4. 头后命运环

建议：
- 3–5 个主要环片
- 独立 ModelPart
- 挂在 `halo_root`

需要支持：
- 缓慢整体转动
- 各片微小错位
- 阶段变化
- 死亡失稳

不要把整环烘焙成一个固定 group。

---

## 5. 胸腔

### 真几何空洞

胸腔中央要真正缺失几何，而不是画黑色圆。

建模时：
- 外壳主动避开中心
- 让玩家从部分角度能看穿
- 用额外 Render Layer/粒子加强深度

### chest_ring

可拆：
- left
- right
- bottom
- optional top fragment

环不能完整闭合。

---

## 6. 双臂

至少需要：
- upper arm
- forearm
- hand

手臂比标准 humanoid 稍长约 10–20%。

右手：
- 主武器手
- 第二阶段单手持镰刀
- 必须有稳定 item transform anchor

左手：
- 施法
- 指向
- 张开
- 命运线
- 辅助动作

左右手都要有足够自由度。

---

## 7. 翅膀

单侧建议：
- 2–3 个骨架段
- 4 个左右主要大羽片
- 1–3 个破损羽片

推荐：
- 8–12 个可动画部件 / 单翼

不建议：
- 每根羽毛独立
- 30+ bone 单侧
- 用大量细小 cube 堆复杂度

Minecraft 中更应依靠：
- 大 silhouette
- 关键羽片
- texture
- emissive
- 动画遮挡

---

## 8. 下半身

中央主残躯至少：

```text
spine_tail_1
└─ spine_tail_2
   └─ spine_tail_3
```

用于形成低频延迟摆动。

两侧独立碎片：
- lower_fragment_left/right
- cloth_fragment_left/right

允许未来扩展额外：
- bone shard
- feather shard
- relic shard

但不要做成腿。

---

## 9. Anchor 预留

建议模型中设置逻辑锚点：

```text
head_effect_anchor
chest_effect_anchor

left_hand_effect_anchor
right_hand_effect_anchor

left_wing_attack_anchor_1
left_wing_attack_anchor_2
right_wing_attack_anchor_1
right_wing_attack_anchor_2

left_wing_tip_anchor
right_wing_tip_anchor

scythe_hand_anchor
```

这些可以是零尺寸或辅助 bone。

用途：
- projectile spawn
- particle origin
- beam origin
- fate-line origin
- held item transform
- scythe throw origin

不要在 Entity 里硬编码大量固定世界偏移。

---

## 10. UV 与贴图

最低建议：
- 128×128

更推荐：
- 256×256

重要部位不要完全镜像 UV：
- 左右翼
- head shell
- chest
- halo

普通手臂和部分骨结构可适度镜像。

原因：
> Boss 的“失败实验体”和“不对称命运”需要真实落到材质差异上。

---

## 11. Emissive 预留

推荐仅用于：
- head core
- chest void 深处
- halo 局部
- wing attack anchors / wing tips
- 阶段转换裂隙

不要全身描边发光。
不要整副翅膀大面积发光。

---

## 12. 复杂度建议

整体目标：
- 35–55 个真正需要独立动画的 ModelPart

大致预算：
- 头部 + 环：8–10
- 躯干 + 下身：8–10
- 双臂：6–8
- 双翼：16–22

Cube 数可以更多，但不必全部拥有独立 bone。

---

## 13. Blockbench 工作顺序

### 第一轮：Silhouette Blockout

只做：
- 躯干
- 头
- 双臂
- 主翼骨
- 4–6 根主羽
- 中央下半身

先验证：
- 4+ 格主体高度
- 6.5–8 格翼展
- 远距离 silhouette

### 第二轮：结构

加入：
- head shell
- halo
- chest void
- chest ring
- bone wing details
- lower fragments

### 第三轮：双阶段 Pose

必须用同一个模型摆出：
- Phase 1
- Phase 2

无粒子、无特效时也应明显不同。

### 第四轮：Texture

结构确认后再正式绘制纹理。

---

## 14. Pivot 要求

尤其注意：
- shoulder
- elbow
- hand
- wing root
- wing joint 1
- wing joint 2
- spine tail segments

Pivot 必须位于真实关节附近。

不要为了静态好看把 pivot 放在 cube 中心。
