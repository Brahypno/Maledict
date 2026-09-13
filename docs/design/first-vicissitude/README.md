# 无常的命运：实现规格与确认记录

状态：**规格已闭环（D6 选自动返还＋凭证兜底），M1–M7 已按规格实现；编译、显式 test 与 runData 通过，游戏内场景未验证。**

本轮实现记录见 [11 验收记录](11_ACCEPTANCE_RECORD.md)。`source/` 保存原稿；实施时阅读本目录修订版，原稿冲突以确认记录为准。不得把“建议”或未回复的问题标成已确认。

## 阅读顺序

1. [代码基线与差异](00_CODE_BASELINE.md)
2. [概念与视觉](01_CONCEPT_AND_VISUAL.md)
3. [模型与资产接口](02_MODEL_STRUCTURE.md)
4. [状态、阶段与动画](03_PHASES_AND_ANIMATION.md)
5. [渲染与投掷](04_RENDER_AND_EFFECTS.md)
6. [实施进度与验收](05_IMPLEMENTATION_PLAN.md)
7. [待确认决策](06_DECISIONS.md)
8. [完整技能规格](07_COMBAT_SPEC.md)
9. [碰撞、返还与扩展](08_COLLISION_AND_RETURNS.md)
10. [Curio 免没收与返还选择](09_CURIO_POLICY.md)
11. [Lodestone / Malum 特效接入清单](10_VFX_INTEGRATION.md)
12. [M1–M7 实现与验收记录](11_ACCEPTANCE_RECORD.md)

第二轮已确认：目标为 first_vicissitude，原稿全部技能须实现，四难度配装保留；新增多球及二阶段普通远程、翼受击、未来声音表现扩展。具体数值第三轮已允许以首版基准采用并后续调整。

随后明确：交付 bbmodel 并自主制作；一阶段所有球压血；双翼物理阻挡玩家和地形并支持部位倍率；受规则限制卡住时就近脱困瞬移，破坏权限可越过部分限制。第三轮已澄清 FTB 仅为例子，不做专门兼容/组合绕过；依靠脱困解决围堵，详见 06/08。

第三轮补充：一阶段按 SIMPLE/DIFFICULT/COMPLETE/EXTREME 采用 180/140/100/75 秒，实施后用户要求统一减半为 90/70/50/37.5 秒；新增免没收 item tag；接入 Lodestone 距离衰减震动。自动返还与凭证的比较见 09，推荐自动返还并保管满背包余额。

## 使用方法

- 先查看 06 已有结论，只关闭剩余返还体验选择；不重复询问已接受的数值。
- 每个问题标记“待确认／已确认”，记录用户答案与日期；删除被否决的备选方案。
- 全部影响范围、战斗、资产交付的决策关闭后，状态才能改为“可实施”。用户的后续实现请求仍是执行起点。
- 实施者可以自主调整类名、辅助方法、UV 排布和已批准范围内的细部造型。影响技能、伤害、时长、碰撞或主题的变化必须更新规格。
- 项目根目录 AGENTS.md 的生成资源规则继续有效。中文 codex 正文先写，再从其含义生成英文；每 13 个可见字符插入一个字面空格。普通设计 Markdown 不需要这些排版空格。

## 来源

- 原包：`fate_boss_agent_md_pack.zip`，SHA-256 `0800AB1AB214132F380B9E9F272197278B6E0DC1197155D6CFCD8007F47500E4`。
- [原稿入口](source/README.md)。保留其文字用于溯源，不再作为独立执行清单。
- 核对日期：2026-09-12；仓库 HEAD `5bcd5ec`，核对包含工作区未提交代码。
- 已存在修改：`FirstVicissitudeBossEntity.java`、`VicissitudeBossEntity.java`、`VicissitudeVitality.java`。本次文档工作不改动这些文件。
