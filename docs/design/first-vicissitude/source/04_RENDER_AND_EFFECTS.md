# 04 — 渲染与效果边界

## 1. 原则

Boss Model 只负责：

> “它是什么形状”。

不要把所有视觉效果塞进 `BossModel`。

建议职责拆分：

```text
BossEntity
    阶段、战斗状态、AI、攻击逻辑

BossModel
    骨骼、姿态、procedural animation

BossRenderer
    主体材质

BossEmissiveLayer
    冷白/暗紫发光

BossFateEffectLayer
    命运线、星轨、额外环效果

Scythe Item / Projectile
    已有镰刀物品与投掷逻辑
```

---

## 2. 不应直接建模的效果

以下内容不建议成为固定 ModelPart：

- 大量命运丝线
- 大型星轨
- 魔法阵
- 弹幕轨迹
- 长距离射线
- 大面积半透明黑紫能量
- 投出后的镰刀本体
- 大规模阶段转换粒子

原因：
- 透明排序问题
- 动态表现差
- 难以维护
- 会把 Model 类变成效果容器

---

## 3. 命运线

推荐作为：
- Renderer layer
- particle
- procedural geometry
- line renderer

起点来自模型 anchor：
- 左手
- chest
- halo
- wing tips
- scythe / scythe projectile

---

## 4. 星轨

星轨主要承担：
- 弹幕预兆
- 镰刀投出轨迹
- 场地技能
- halo 特殊技能

不要直接做成固定大圆。

---

## 5. Emissive

推荐极少使用。

区域：
- head core
- chest void 深处
- halo 局部
- wing attack point
- 裂痕
- 某些技能激活位置

最高亮度为：
- 冷白

暗紫：
- 过渡
- 外围
- 能量残留

---

## 6. 镰刀投出

重要：

“投出”是 throw，不是 transparency/reveal。

镰刀已有 Item。

Phase 2 可从右手投出。

建议：
- Boss 手部只负责起始 transform
- 释放后生成独立 projectile/entity
- 飞行中使用已有镰刀材质/模型
- 可带旋转
- 可加入环形或星轨路径
- 可加入命运线连接
- 返回逻辑以后单独设计

不要：
- 把第二把镰刀藏在 Boss model
- 让镰刀用模型 visibility 假装飞出去

---

## 7. 胸部空洞

真正几何空洞优先。

Renderer 可额外加强：
- 深色 interior
- emissive center
- fog-like darkness
- subtle parallax

但不要用纯黑贴图冒充全部结构。

---

## 8. Halo

基础 halo 可以属于模型。

更大范围的：
- 光圈
- 星轨
- 动态第二层环
- 技能特效

应该属于 Render Layer / effect。

---

## 9. 性能意识

大型 Boss 特效应避免：
- 每 tick 大量重建 Mesh
- 每 tick 生成大量临时对象
- 无限制粒子
- 每片羽毛都跑复杂独立逻辑
- 动态效果与实际不可见状态仍持续高开销

后续 Codex 分析项目时应优先检查：
- RenderType 创建是否缓存
- VertexConsumer/Buffer 获取是否合理
- projectile/effect tick 是否有不必要搜索
- Boss 不在视野中时是否仍做昂贵视觉计算
