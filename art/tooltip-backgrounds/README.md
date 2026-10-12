# 启蒙之年 Tooltip 羽翼框

当前图稿使用内置 `image_gen` 编辑，羽片节奏、明暗层级、冠饰弧线与金属细节的提示词在
`age-of-enlightenment/rhythm-prompts.json`。
此前结构调整与长羽构图的提示词保存在 `structured-prompts.json` 和 `golden-prompts.json`。
边框原图为 `age-of-enlightenment/wing-rhythm.png`，中央羽饰为 `crest-rhythm.png`。
旧版原图留作记录；打包脚本只读取当前边框和中央羽饰。

左右羽翼集中在上角：弯曲翼肩带动覆羽与错开的飞羽，根部藏在覆羽下。
保留长羽版覆盖范围，最下方翼尖仍在采样图 y=82；末端羽片逐渐变窄，尖端错落收束。
根部覆羽较密，前景主羽较亮，后层和中下段使用较暗、较低饱和度的紫色，让属性文字更突出。
少量羽片缺口和较轻的内部分界表现层叠；中下段框架保留可伸缩的双层直线框边。
金色弯箍连接翼根，下方钩形扣件使用相同的弯曲、倒角和明暗处理。
顶部中央羽饰的外侧羽片向下弯，与略向上扬的侧翼肩部呼应，仍固定大小并居中。

运行 `make_tooltip_background.py` 将原图采样为以下资源：

- `src/main/resources/assets/maledict/textures/gui/tooltip/age_of_enlightenment.png`：96×144 RGBA，九宫格切片左／右 36，上 88、下 28。
- `src/main/resources/assets/maledict/textures/gui/tooltip/age_of_enlightenment_crest.png`：72×24 RGBA，羽饰基座进入黑底 2 像素。

按采样图内边测量，注册外扩为左 25、上 22、右 25、下 22 GUI 像素，使框边贴住原版黑底。
采用 `STRETCH`，完整羽翼都位于固定的上角，切片线落在羽翼结束之后，只有直线框边伸缩。
上下切片合计 116 像素，内容高度至少 64 像素时羽翼保持原尺寸；更短的框按 88:28 分配上下切片，
并同步缩小上下外扩，使框边继续贴住原版黑底。预览脚本使用与 Java 相同的布局规则。

`MaledictTooltipBackgrounds` 通过 `RegisterTooltipBackgroundsEvent` 注册启蒙之年组合。
保持 `DECORATE` 模式和原版黑底，其他物品不受此注册影响。

脚本输出 `build/tooltip-background-review/age_of_enlightenment-preview.png`，
以相同切片、外扩和中央羽饰坐标展示宽／窄及长／短 tooltip，并展示蓝紫色属性文字。
这是离线布局预览，字体与游戏渲染可能不同；运行时截图才是最终视觉依据。
