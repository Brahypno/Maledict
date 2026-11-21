# 1.21.1 待移植功能清单

本文件记录 `Maledict`（1.20.1 / Forge 47.4.23）与 `Maledict-1.21`（1.21.1 / NeoForge 21.1.262）之间
**仍然缺失的功能组**，供后续逐组移植时对账。包结构对齐的结论见文末「包结构对齐」。

核对方法：两侧 `src/main/java` 目录树逐目录对照，类名逐项清点，再用关键字在 1.21 全项目（含
`src/main/resources` 与 `src/generated`）反查确认「确实没有」而不是「换了名字」。清点时已排除以下
误判：`Ectoplasm` / `Reminiscence` 在 1.21 改由数据驱动附魔实现（`MaledictEnchantments` +
`data/maledict/enchantment` + 语言键），仍有功能，不算缺失。

## 一、待移植功能组

| # | 功能组 | 1.20.1 源 | 1.21 现状 | 备注 |
| --- | --- | --- | --- | --- |
| 1 | Demiurge（巨型 Boss、柱体/雕像/泪滴实体、巡逻与缠斗 AI） | `common/entity/demiurge`(21)、`client/entity/demiurge`(9) | 完全缺失 | 依赖共享的 `common/combat/{BossDamage,BossRecovery}`（1.21 也没有）；模型/贴图/音效随组迁移 |
| 2 | Weeping Wither（凋灵系 Boss、骷髅、头骨、獠牙） | `common/entity/wither`(11)、`client/entity/weepingWither`(7) | 完全缺失 | 含 `BlightedWitherSkullBlock` 与 `OssuaryFangs*`；资源目录 `textures/block/blighted_wither_skull` 也缺 |
| 3 | Soulbinding（缚魂仪式：数据、击杀、卷轴、音效、提示） | `common/ritual/soulbinding`(10)、`mixin/SoulWardHandlerMixin` | 完全缺失 | 1.21 已有 `common/ritual/{invocation,wisdom}`，本组落到 `common/ritual/soulbinding` 即可 |
| 4 | Refulgence（辉耀战斗子系统） | `common/combat/refulgence`(7) | 完全缺失 | 挂 `common/combat/refulgence` |
| 5 | Lumenfall（光坠战斗子系统 + 客户端表现） | `common/combat/lumenfall`(5)、`client/combat/lumenfall`(1) | 完全缺失 | 1.21 已建好 `client/combat/vitals`，`client/combat/lumenfall` 需新建 |
| 6 | Lightward 护甲（套装、材质、客户端扩展与状态） | `common/item/equipment/lightward`(4)、`client/item/equipment/lightward`(3) | 完全缺失 | 需要 `client/item/equipment/` 新目录 |
| 7 | Incursus Rift（裂隙斩击：几何、实例、着色器、笔划图案、网络包） | `client/item/weapon/incursus/rift`(13 + `patterns` 3)、`common/item/weapon/incursus/rift/RiftMode`、`network/IncursusRiftSkillPacket`、`mixin/HiddenBladeNecklaceRiftMixin` | 完全缺失（1.21 只有 `IncursusBladeBlink`，与 Rift 不是同一机制） | 体量最大的一组，含 shader 与 `RiftLegacySlashes` 兼容层 |
| 8 | Diplopia 附魔 + 回旋镰刀 | `common/enchantment/{DiplopiaEnchantment,DiplopiaScytheBoomerangEntity,DiplopiaThrows}`、`client/item/weapon/scythe/DiplopiaScytheBoomerangRenderer` | 完全缺失 | 新增 `client/item/weapon/scythe` |
| 9 | Baron 法杖 | `common/item/weapon/baron`(2) | 完全缺失 | 小体量，可与 8 合并一次提交 |
| 10 | Emperor in the Eggshell | `common/item/curio/eggshell/EmperorInTheEggshellItem` | 完全缺失 | 新增 `common/item/curio/eggshell` |
| 11 | Inverted Reliquary（倒置圣物匣结构、祭坛） | `common/realm/reliquary`(4)、`data/MaledictReliquaryTemplates` | 完全缺失 | 新增 `common/realm/reliquary`；模板生成进 `runData` |
| 12 | Lightfall 法典（法典 UI、虚空/奥术页、打开包） | `common/item/codex/LightfallCodexItem`、`client/item/codex/{LightfallCodex,LightfallCodexScreen,LightfallArcanaScreen,LightfallVoidScreen,SoulwoodSpiritRiteRecipePage}`、`network/OpenLightfallCodexPacket` | 1.21 只有 `client/item/codex` 的 3 个类（入口索引、词条、可读文本页） | 这是 1.21 现存 `client/item/codex` 的上位替代，移植时先看能否复用现有页面基类 |
| 13 | Maid 兼容（羁绊数据、语音、客户端） | `compat/maid`(5)、`compat/maid/client`(1)、`network/MaidVoicePacket` | 完全缺失，1.21 无 `compat/` 包 | 软依赖编译，`compileOnly` |
| 14 | Tetra 兼容（材料与改良） | `compat/tetra/TetraCompat` | 完全缺失 | 数据侧 `data/tetra/{materials,improvements}` 同样缺 |

合计约 130 个 java 文件。**建议顺序**：先补共享基础（`BossDamage` / `BossRecovery` / `MaledictClock` /
`MaledictProbeDamage` / `MaledictDamageTypes`），再按 1 → 2（Boss 组依赖共享基础），然后 3 → 5（战斗子系统），
最后 6 → 14（物品、结构、兼容层）。

## 二、核心辅助类，需逐个核实后再决定

以下类在 1.20.1 存在、1.21 找不到同名类，但功能可能已被别处吸收，**移植前先确认是否真的缺**：

`MaledictAbilities`、`MaledictClock`、`MaledictDamageTypes`、`MaledictProbeDamage`、`MaledictParticles`、
`MaledictStructures`、`MaledictSpiritData`、`StatedSpawnEggItem`、`VicissitudeSpawnEvents`、
`ModularAbilityTooltipEvents`、`LivingEntityHurtSoundAccess`、`PurpleSoulFireParticle`。

1.20.1 有、1.21 没有的 mixin（8 个）：`HiddenBladeNecklaceRiftMixin`、`HiddenTagRegistryMixin`、
`LivingEntityHurtSoundAccess`、`LodestoneAttributeEventHandlerMixin`、`ReboundEnchantmentMixin`、
`SoulWardHandlerMixin`、`TouchOfDarknessHandlerMixin`、`VoidConduitBlockEntityMixin`。
其中与 Lodestone / Malum 事件时序相关的几个 **不要照搬**：1.21 侧已经改用 `LivingDamageEvent.Pre/Post`
与 `LivingHurtEvent` 组合实现（见 `AGENTS.md` 的「已确认机制」，以及 `docs/PORT_1.21.1.md` 的行为选择表），
照搬 mixin 会与现有实现冲突。按 `AGENTS.md` 的要求，mixin 仍是最后手段。

## 三、每个功能组的移植流程

1. 从 1.20.1 分支（同级目录 `../Maledict`）读源码，确认它在 1.20.1 的实现语义与调用点。
2. 按 1.21 API 改写：`DamageTypeRegistry` → `MalumDamageTypes`、`DeferredHolder` 泛型、`LevelReader#hasChunkAt`
   等方法的 1.21 签名。签名以参考文件或反编译产物为准，不凭记忆写。
3. 注册表与资源：`registry/` 里的 `DeferredRegister`、`data/` 下的生成器、`src/main/resources/assets|data`
   的贴图/模型/音效/语言键，一并补齐。`src/generated` **只能**通过 `runData` 更新。
4. 测试：仓库上限是 50 个 GameTest + 50 个 JUnit，加新用例前先替换低价值用例，不要用过滤器绕过。
5. 验证：`compileTestJava` → `test` → 需要数据时 `runData`。不要为了验证追加 `build`。

## 四、包结构对齐（本轮已完成）

`1.21.1-neo` 的源码包已按 1.20.1 的功能域收拢，同一功能在两条分支上落到相同的相对位置，
后续 cherry-pick 与对照可以直接按路径找文件：

- `common/combat/{DamageAdaptation, vitals}`（原 `common/vitals` 与 `common/entity/vicissitude/DamageAdaptation`）
- `common/entity/vicissitude/{curio, rig, rite}`（原 `common/curio/vicissitude`、顶层 `rig`、`common/rite`）
- `common/item/curio/{enlightenment, hangman, priestess, rune}`（原 `common/curio/*` 与 `common/item/rune`）
- `common/item/weapon/{incursus, lover, remembrance}`（原 `common/item/weapon/{axe,bow}`、`common/item/arrow`）
- `common/ritual/{invocation, wisdom}`（原 `common/invocation`、`common/rite` 的智慧献祭部分）
- `common/realm`（并入 `common/block` 里的 `RealmGroundcoverBlock`、`WitherlightMushroomBlock`）
- `client/entity/{raven, vicissitude/{model,music,render,vfx}}`（原 `client/raven`、`client/vicissitude`）
- `client/item/{codex, curio/*, weapon/*}`（原 `client/codex`、`client/item/priestess` 等）
- `client/combat/vitals`（原 `client/vitals`）

刻意保留的差异，以及理由：

| 项 | 1.21 现状 | 理由 |
| --- | --- | --- |
| `registry/`、`network/{clientbound,serverbound}`、`data/{client,loot,recipe,tags,worldgen}`、`mixin/`、`config/` | 保留 1.21 的细分 | 基础设施包，1.20.1 是扁平结构，细分更适合 1.21 NeoForge 的注册/网络规模 |
| `common/ritual/OneShotRiteEffect` | 放在 `common/ritual` 根 | 1.20.1 无对应类；它是 `SpiritRiteEffect` 的通用封装，被召唤与献祭两类仪式共用 |
| `common/item/weapon/incursus/MaledictItemTiers` | 从 `registry/` 移入 | 跟随 1.20.1 位置，保证路径对齐 |
