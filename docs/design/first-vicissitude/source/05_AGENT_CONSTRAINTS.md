# 05 — Agent 硬约束与验收标准

## 1. 不得自行改变的设计

Agent 不应自行重新设计 Boss。

以下为硬约束：

1. Forge 1.20.x。
2. 当前阶段不强制 GeckoLib。
3. Boss 是神圣遗骸制造的失败实验体。
4. 它是命运的牺牲品，同时也是命运的执行者。
5. Boss 比 Warden 更大。
6. 没有腿。
7. 头不是人头。
8. 头后存在断裂主命运环。
9. 胸部中心是真实几何空洞。
10. 双翼是骨翼 + 羽翼混合。
11. Phase 1 羽翼更完整且高位展开。
12. Phase 1 不持武器。
13. Phase 1 主要远程攻击视觉来源是翅膀/翼尖/羽片。
14. Phase 2 右翼/整体翼位降低，骨翼暴露增加。
15. Phase 2 右手单持已有镰刀 Item。
16. 镰刀可以被**投出**。
17. “透出”是笔误，不存在必须做镰刀透明显现动画的要求。
18. 左手保留施法、牵引、命运线等功能。
19. 下半身是中央残躯/脊柱 + 周围碎片。
20. 两阶段优先复用同一个模型。
21. 死亡动画负责揭示失败实验体/拼接重构的痕迹。
22. Boss 的情绪不是愤怒，而是冷静、无奈、不得已。
23. 配色为黑、深紫、冷灰白，最高亮度为冷白。
24. 避免恶魔化、血肉化、传统死神化。

---

## 2. 技术原则

优先：
- 清晰 bone hierarchy
- 合理 pivot
- anchor 驱动攻击起点
- Model / Renderer / Entity / Effect 职责分离
- procedural animation 可维护
- 少量 visibility 切换
- 通过 pose 和遮挡关系做阶段差异

避免：
- 两阶段完全两套模型
- Entity 中大量硬编码模型空间偏移
- 将所有粒子/线条/光环写死进 Model
- 大量每 tick mesh rebuild
- 镰刀作为 Boss model 固定 geometry
- 用 model visibility 假装镰刀 projectile

---

## 3. 第一版实现优先级

建议 Agent 依次完成：

### P0 — 静态骨架
- root
- torso
- head
- arms
- wings
- lower body
- anchors

### P1 — 两阶段基础 Pose
- Phase 1 圣像姿态
- Phase 2 执行者姿态

### P2 — Item 持握
- 右手镰刀 transform
- Phase 2 equip/unequip 状态
- 投出时右手空置

### P3 — 基础 procedural animation
- hover
- wing idle
- halo motion
- lower-body follow-through
- look / hurt

### P4 — 攻击动作
- wing ranged attack
- scythe melee
- scythe throw

### P5 — Renderer
- emissive
- fate lines
- star tracks

### P6 — death
- halo collapse
- shell separation
- wing failure
- inner relic reveal

---

## 4. 静态验收

完成模型后，不开粒子和攻击效果，做以下检查：

### 检查 A：Silhouette
材质全部换成灰色后，远距离是否仍能辨认出：
- 巨大骨羽翼
- 非人头部
- 无腿
- 高位悬浮圣骸

### 检查 B：阶段差异
同一模型仅改变 pose 和少量部件状态时：
- Phase 1 应读作“降临 / 宣告 / 圣像”
- Phase 2 应读作“执行 / 追猎 / 被迫亲自动手”

### 检查 C：主题
第一印象是否是：
> 神圣但有问题的遗骸

而不是：
> 恶魔 / 普通天使 / 传统死神 / 幽灵法师

### 检查 D：镰刀
- Phase 2 默认右手单持
- 投出后右手空置自然
- 镰刀 projectile 独立于 Boss model

---

## 5. 需要本地 Codex 进一步确认的事项

在读取真实项目后，Agent 应进一步分析：

1. 当前 Minecraft/Forge 精确小版本。
2. 现有实体模型基类与 Renderer 体系。
3. 是否已有通用动画 helper。
4. 是否已有 projectile renderer 可复用。
5. 镰刀 Item 当前实现、模型、transform 和投掷兼容性。
6. 是否已有 emissive layer 工具。
7. 是否已有 particle/beam/line renderer。
8. 是否已有 model-part anchor 到 world-space 的辅助方法。
9. Boss AI / phase framework 是否已有通用实现。
10. death animation 是否受当前 Entity removal 逻辑限制。

Agent 应基于项目真实代码适配，而不是预设某个不存在的框架。
