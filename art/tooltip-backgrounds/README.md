# 启蒙之年 Tooltip 羽翼框

当前图稿使用内置 `image_gen` 编辑，提示词在
`age-of-enlightenment/wing-redesign-prompt.json` 和 `wing-refine-prompt.json`。
边框原图为 `age-of-enlightenment/wing-master.png`，中央羽饰仍为 `crest-master.png`。
旧版原图留作记录；打包脚本只读取当前边框和中央羽饰。

左右各是一段贴着黑底的完整羽翼：肩部与覆羽相连，宽长飞羽向下收拢。
移除四角外突的羽毛束，上下边保持简洁，让两侧羽翼与中央羽饰成为主体。
主色为深紫、紫晶和亮紫，少量金色用于固定件和内边。
顶部独立羽饰固定大小并居中，宽度不随 tooltip 拉伸。

运行 `make_tooltip_background.py` 将原图采样为以下资源：

- `src/main/resources/assets/maledict/textures/gui/tooltip/age_of_enlightenment.png`：96×144 RGBA，九宫格切片左／右 32，上／下 28。
- `src/main/resources/assets/maledict/textures/gui/tooltip/age_of_enlightenment_crest.png`：72×24 RGBA，羽饰基座进入黑底 2 像素。

按采样图内边测量，注册外扩为左 25、上 22、右 25、下 27 GUI 像素，使翼根和窄边贴住原版黑底。
采用 `STRETCH`，两侧翼身整体随提示框高度适配，肩部与末端保持固定尺寸，
不将单片羽毛沿边反复平铺。

`MaledictTooltipBackgrounds` 通过 `RegisterTooltipBackgroundsEvent` 注册启蒙之年组合。
保持 `DECORATE` 模式和原版黑底，其他物品不受此注册影响。

脚本输出 `build/tooltip-background-review/age_of_enlightenment-preview.png`，
以相同切片、外扩和中央羽饰坐标展示宽／窄 tooltip。
这是离线布局预览，字体与游戏渲染可能不同；运行时截图才是最终视觉依据。
