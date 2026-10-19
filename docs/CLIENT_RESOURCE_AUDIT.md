# 1.21.1 客户端资源复查

## 已修正

- 两种语言各 16 条饰品效果键从 `malum.gui.curio.effect.*` 改为新版
  `ComponentHelper.positiveEffect` / `negativeEffect` 实际使用的 `malum.effect.*`。
  同时补齐方尖碑组件和四种弹射物实体的名称、无常仪式的效果提示；启蒙说明改为已确认的每级 3 Spell Charges。
- `SpiritInfusionPage.fromId` 在 1.8.2 中会自动添加 `spirit_infusion/`。
  Bleeding Eye 内两张 Hangman's Fool 配方页现在传入 `maledict:hangman_fool` 和
  `maledict:hangman_fool_stage_2`，对应实际生成的两个配方，不再拼出双重目录。
- Malum 已删除 `textures/vfx/rite/infernal.png` 和 `arcane.png`。
  Sacrifice 借用 `RITE_OF_THE_BURNING_FERVOR` 的原生图标；Vicissitude 借用 `UNDIRECTED_RITE` 的原生图标。
  节点和仪式正文页继续走原生 `RiteEntryObject` / `SpiritRiteTextPage` 绘制。
- 旧 `forge:scythe` 改为 Malum 1.8.2 提供的 `SCYTHES_COMMON`，即 `c:tools/scythe`。
  生成器已删除旧标签输出；`malum:scythe` 分类保持存在。

## Codex 条目与重叠

核对了 Malum 1.8.2 主地图注册源码，包括 `addBundledRiteEntry` 产生的主节点。
Maledict 在 Arcana 注册 3 个地图节点，在 Void 注册 8 个；没有重复的条目 ID。
「图腾仪式：续」位于原饱食符文的位置 `(-17,8)`，内部包含饱食、衰朽、兽群、熟成四枚符文的说明与配方。
四枚符文不再注册独立地图节点，消除汇总入口与四个单独入口的内容重复。
Spirit Arrows 从与 `aqueous_rites` 冲突的 `(2,14)` 移到 `(2,12)`。

调整后，本模组节点与目标版原生主节点没有同坐标项。此处核对的是注册数据，不是截图比对。

## 汇总页布局

「精魂箭」左页为分段简介与底部八支箭的横向展示，右页是两列四行目录。
「图腾符文：续」左页合并原有两段介绍，右页是两列两行目录。
这两个汇总条目不再使用侧边 references，因此不会产生超出书框的长串书签。
点击目录使用原生条目打开逻辑，子条目保留说明、配方和返回目录的路径。
这些说明页采用无阴影、无多重描边的深褐色正文；没有修改前置模组的字体或全局渲染。

排版检查增加了精魂箭底部展示区域的 12 行限制，以及合并后的符文介绍检查。
本次网格调整通过数据生成、编译、182 个 JUnit 与中英文排版检查。
客户端启动未获允许，因此未执行截图、实机点击和返回路径的自动检查。

## 日志警告来源

只记录与 Maledict 自身代码、资源有关的日志问题；前置模组自身的 warning / error 不在本次修复范围内。

| 日志 | 来源与处理 |
| --- | --- |
| `Legacy Tags detected` | 本仓库的 `forge:scythe`，已迁移生成器与输出。没有关闭警告配置。 |
| `Failed to load texture: .../rite/infernal.png` | 本仓库仍引用已移除纹理，已修正，并同步修正 `arcane.png`。 |
| 缺失 Goety 属性的转换警告 | 本仓库的 27 条 Goety 联动数据已核对属性 ID；当前未安装 Goety，原生解析器会警告并跳过缺失属性。保留正确的联动数据，不为消除未安装联动的提示而删除规则。 |

## 验证

`LanguageCoverageTest` 对实际符文与 Hangman 提示组件逐层提取翻译键，分别核对生成的中英文资源，
同时检查注册名称、仪式效果说明，以及目标模组 jar 中图标的实际存在性。
另外核对两张 Hangman 配方页经原生目录拼接后的目标资源，并检查直接写入 `Component.translatable` 的本模组键。
语言文件由 `runData` 更新，页面排版使用现有 `check-lang-layout.mjs` 检查。

本次 `runData`、184 个 JUnit、102 个 GameTest 和排版检查均通过；真实服务端加载日志没有
`Legacy Tags detected`。没有重新生成成品 jar，也没有重启正在使用的开发客户端；运行中的客户端需重启以加载 Java 改动。
