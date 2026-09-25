# 无常（First Vicissitude）

`maledict:first_vicissitude` 的设计与实现文档。

## 四份正典

| 文档 | 回答什么问题 | 什么时候读 |
| --- | --- | --- |
| [01 设计与规格](01_SPEC.md) | 这个 Boss 是什么、有什么技能、数值多少、掉落什么、客户端边界在哪 | 改主题、技能、数值、掉落、饰品策略时 |
| [02 动作、时序与判定](02_ANIMATION_AND_HITBOX.md) | 动作的时间轴怎么定义、伤害判定体从哪来、姿态怎么收尾 | 改动作系数、命中帧、攻击距离、判定形状时 |
| [03 工程与验证](03_ENGINEERING_AND_VERIFICATION.md) | 实现到哪一步、跑过什么命令、**还有哪些从未验证** | 接手这个 Boss、准备验收、怀疑某个功能是否可靠时 |
| 本文件 | 导航 | |

`02` 是唯一同时描述**代码意图和实测数字**的一篇：它第 6 节的表由 `tools/rig-probe/` 生成，
改动作后必须重跑并更新。

## 其他目录

| 路径 | 内容 |
| --- | --- |
| `archive/` | 31 篇逐轮历史记录（00–28）。保留了当时的数值和取舍，**不要当作当前规格**。 |
| `source/` | 用户提供的原始工作包，用于溯源。与正典冲突时以正典为准。 |
| `references/` | 用户提供的原型参考图，只作设计参考，不作为游戏纹理或宣传素材导出。 |

## 三条硬规则

1. **改动作就重跑实测。** 动 `VicissitudeRig` 的动作系数、`Action` 的时长或命中帧、
   或者 `BLADE_*` 参数之后，跑 `tools/rig-probe/`，把新数字写回
   [02 第 7 节](02_ANIMATION_AND_HITBOX.md)，并让 `VicissitudeRigTest` 全绿。
2. **不要再引入写死的近战半径。** 近战距离由刀身几何和命中帧姿态决定；
   想改威胁范围就改刀身长度或手臂姿态。见 [02 第 1 节](02_ANIMATION_AND_HITBOX.md)。
3. **「未验证」是一种状态，不是懒惰。** [03 第 5 节](03_ENGINEERING_AND_VERIFICATION.md)
   列的是真的没测过的东西。不要因为代码看起来对就把它从那一节删掉——
   删掉它的唯一方式是进游戏测过。

## 不要做（踩过的坑）

**不要把范围判定做成精密测量。** 近战只需要三件事说对：**够高**、**站对距离**、
**在正确的 tick**。这三样之外的几何——刀身水平面上的横向位置、目标落在刀身上的哪一点——
都是**范围**，由一条有宽度的判定带吸收，不是要算到毫米的量。

[02 §4.4](02_ANIMATION_AND_HITBOX.md) 有完整的分界表。简单说：想让判定更准之前，
先问"这一项错了会导致打不中吗"。不会的话，它就该是范围的一部分，
加精度只会多出需要实机校准的参数——这正是这一轮返工的原因。
原版近战本身就是锥形范围判定，玩家看到刀刃离身体还有一段就掉血，那是设计如此。

## 改什么东西要看哪里

| 想改… | 先看 | 涉及文件 |
| --- | --- | --- |
| 技能伤害、冷却、触发距离 | [01 §4](01_SPEC.md) | `FirstVicissitudeBossEntity`、`VicissitudeRig.Action` |
| 挥砍幅度、收势手感 | [02 §3、§4](02_ANIMATION_AND_HITBOX.md) | `VicissitudeRig.applyAction` |
| 近战打到多远 | [02 §1、§6](02_ANIMATION_AND_HITBOX.md) | `VicissitudeRig.BLADE_*` |
| 难度、血量、时长 | [01 §1](01_SPEC.md) | `BossDifficulty` |
| 掉落 | [01 §8](01_SPEC.md) | `MaledictEntityLoot` + `runData` |
| 模型、贴图、UV | [01 §3](01_SPEC.md) | `art/first-vicissitude/` + `rig/VicissitudeRigData` |
| 特效、粒子、震动 | [01 §3](01_SPEC.md) | `client/vfx/FirstVicissitudeEffects` |
| 饰品没收与返还 | [01 §9](01_SPEC.md) | `VicissitudeCurioReturns`、`VicissitudeCurioLedger` |
