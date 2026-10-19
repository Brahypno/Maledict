# 1.21.1 NeoForge 移植记录

目标分支为 `1.21.1-neo`，仓库为 `Brahypno/Maledict`。
运行环境：Java 21、NeoForge 21.1.262、Malum 1.8.2、Lodestone 1.8.2、Curios 9.5.1。
ChangeLib 使用本机 Maven 中的 `org.brahypno:changelib-1.21:21.1.5-SNAPSHOT`。

## 已确认的行为选择

| 功能 | 1.21.1 选择 |
| --- | --- |
| Age of Enlightenment 近战倍率、半血触发 | LivingDamageEvent.Pre；半血读取扣血前血量 |
| Transcend Omen | 跟随新版转换数据；三项 Aegis 属性仅在值为 0 时忽略，正值仍限制并消耗；份额算法不变 |
| Descent、Melancholia 适应倍率 | Pre，护甲后、黄心前 |
| Melancholia DelayedVitalsPool | Pre 中先消耗黄心，只将余下伤害入池 |
| Rune of the Fallen / CorruptionState | Pre 收账，黄心完全吸收的攻击仍可触发 |
| Aftertaste | LivingDamageEvent.Post |
| Bliss 正常 SharedFate 倍率 | Incoming，保留护甲前增伤 |
| Bliss 强制命中 | 只响应被取消的 Incoming；Pre 归零不补打 |
| Incursus Blade 熟练度、抗性、黄心收益 | Pre |
| Incursus MAGIC_DAMAGE | 新 Lodestone 的 Post 行为，按蓄力缩放，跳过已死亡目标；未进入原生事件时保留补打 |
| Infernal Looting | GetEnchantmentLevelEvent，所有游戏查询计入，存储附魔不变 |
| 增强投掷 / Ascension | isEnhanced，只认 Necklace of the Narrow Edge |
| Incursus 黄心容量 | 主手提供 MAX_ABSORPTION +1000，卸下时接受原生截断；仅隐藏这条 modifier 的物品提示，刀自身收益与积累上限不变 |
| Age of Enlightenment 法杖容量 | 每级 CHARGE_CAPACITY +1，即 3 个 Spell Charges |
| Light Orb、震屏 | 使用新版追踪与震屏算法 |
| 出井表现 | 新版 rejection 60 tick 与接触条件 |
| 配对井 | 原生 WeepingWellStructurePiece；保留配对、安全落点与建筑避让 |
| 旧 hexing smoke / ritual circle | 使用新版原生特效，不恢复旧粒子实现 |

原 Malum tablet 材料改为：runewood / soulwood tablet → 4 对应 log；
tainted rock tablet → 4 tainted rock；void tablet → 4 null slate。
地下 BLIGHTED_SOIL 改用 BLIGHTED_EARTH。
工作台按材料使用 WOODEN / STONE / VOID 音效，Maledict 自有 malignant pewter tablet 使用 GENERIC。

Recipe ID 与配方 advancement 使用新版 builder 原生规则，不加旧 ID 兼容包装。
新版 builder 自动加类型前缀，调用时传入 basename；目前 Hangman Fool、Rune of Bliss 等最终 recipe ID 与旧版一致。
原版数据目录 `recipes` / `advancements` / `loot_tables` / `structures` 改为对应单数。
`src/generated` 只通过 `runData` 更新。

## 手写 data 联动配置

核对旧项目 47 个手写 data 文件，均有迁入或对应的新路径。以下配置不能仅按原版目录改名处理：

| 联动 | 当前路径与处理 |
| --- | --- |
| Better Combat | `data/maledict/weapon_attributes/incursus_blade.json` 保持；新版仍加载该目录，保留 scythe 父预设与两次攻击。angle=0 在新旧合并规则中均代表继承父值。 |
| Epic Fight | 移到 `data/maledict/capabilities/weapons/incursus_blade.json`；greatsword、common、armor_negation、impact、max_strikes 均受目标加载器支持。 |
| Apothic Enchanting | `enchanting_stats/` 保持；eterna / maxEterna 按新尺度翻倍：Mnemonic 20 / 60，Brilliant 10 / 40。Quanta、Arcana、Clues 保持，移除无效 rectification: 0。 |
| Goety | `malignant_conversion_data/goety/` 保持；27 个来源属性 ID 均与 Goety 3 注册表对应，Malum 1.8.2 加载器仍支持现有字段与比例。 |

核对依据为官方版本分支的实际加载器和 schema：

- [Better Combat 1.21.1 WeaponRegistry](https://github.com/ZsoltMolnarrr/BetterCombat/blob/1.21.1/common/src/main/java/net/bettercombat/logic/WeaponRegistry.java)
- [Better Combat 1.21.1 合并规则](https://github.com/ZsoltMolnarrr/BetterCombat/blob/1.21.1/common/src/main/java/net/bettercombat/api/WeaponAttributesHelper.java)
- [Epic Fight 1.21.1 ItemCapabilityReloadListener](https://github.com/Antikythera-Studios/epicfight/blob/1.21.1/src/main/java/yesman/epicfight/api/data/reloader/ItemCapabilityReloadListener.java)
- [Apothic Enchanting 1.21 EnchantingStatRegistry](https://github.com/Shadows-of-Fire/Apothic-Enchanting/blob/1.21/src/main/java/dev/shadowsoffire/apothic_enchanting/table/EnchantingStatRegistry.java)
- [旧 Apotheosis Eterna 尺度](https://github.com/Shadows-of-Fire/Apotheosis/blob/1.20/src/main/java/dev/shadowsoffire/apotheosis/ench/table/EnchantingStatRegistry.java)
- [Goety 3 ModAttributes](https://github.com/Vivideru/Goety-3/blob/main/src/main/java/com/Polarice3/Goety/init/ModAttributes.java)

这些联动完成了源码和资源格式核对；默认回归环境没有装入 Better Combat、Epic Fight、Apothic Enchanting 或 Goety，
因此不将默认环境测试结果视为这四个模组同时安装后的实机验证。

## 验证入口

```powershell
.\gradlew.bat runData
.\gradlew.bat test
.\gradlew.bat runGameTestServer
.\gradlew.bat build
```

旧测试必要性、合并项目、测试夹具及覆盖边界见 `src/test/README.md`。
正式包不包含测试类、测试结构或测试世界预设。
测试世界支持真实维度、装备、伤害与方块链路；客户端渲染、音频和联机握手需分别验证。

2026-10-18 验证结果：`runData`、`build`、182 个 JUnit、102 个 GameTest 全部通过；
中英法典排版检查通过。客户端成功完成资源图集、Boss 网格和着色器加载后正常关闭，
未将启动检查等同于全部游戏内视觉、音频和联机操作验收。
成品为 `build/libs/maledict-0.30-SNAPSHOT.jar`，已内嵌 ChangeLib；检查确认不含测试资源。
