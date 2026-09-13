# 无常的命运 Boss：Agent 工作包

目标：为 Minecraft Forge 1.20.x 中的高难度 Boss 提供统一的模型、阶段、动画与实现约束。

当前默认技术路线：
- Forge 1.20.x
- 暂不引入 GeckoLib
- 优先使用原版 `ModelPart` / `HierarchicalModel`
- 两阶段尽量复用同一个模型，通过姿态、部件显隐、遮挡关系和渲染效果区分
- 镰刀已有对应物品，不建进 Boss 模型
- 第二阶段镰刀可以被**投出**，此前“透出”是笔误，不存在“镰刀逐渐透出/显现”的设计要求

建议 Agent 阅读顺序：
1. `01_CONCEPT_AND_VISUAL.md`
2. `02_MODEL_STRUCTURE.md`
3. `03_PHASES_AND_ANIMATION.md`
4. `04_RENDER_AND_EFFECTS.md`
5. `05_AGENT_CONSTRAINTS.md`

## 一句话定义

这是一个由神圣遗骸制造出的失败实验体。它未能完成原本应执行的命运，却因“无常的命运”再次被以不愿接受的方式唤醒，并被迫以扭曲形式继续执行原本的职责。

它既是命运的牺牲品，也是命运的执行者。
