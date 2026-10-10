# 客户端物品 Tooltip 背景

Minecraft 1.20.1 / Forge 47.4.23。使用 `RenderTooltipEvent.Color`，无额外 Mixin、网络包或服务端逻辑。
此事件发生在原版背景绘制前，提供物品、最终位置、实际字体和已经换行的组件。
因此不用重新推算鼠标位置，也不会因后续取消 `Pre` 而留下孤立的装饰。

## 接入物品

监听客户端注册事件 `RegisterTooltipBackgroundsEvent`，每条注册同时声明物品规则和整套组合。
事件在客户端初始化的主线程任务中向所有模组的 MOD 总线分发一次；处理器内不需要 `enqueueWork`。

```java
@Mod.EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MyTooltipBackgrounds {
    @SubscribeEvent
    public static void register(RegisterTooltipBackgroundsEvent event) {
        var bladeFrame = new TooltipBackground(
                new ResourceLocation("maledict", "textures/gui/tooltip/incursus_blade.png"),
                24, 24,
                TooltipFrameLayout.Insets.all(8),
                TooltipFrameLayout.Insets.all(8),
                TooltipBackground.Mode.DECORATE);
        event.register(MaledictItems.INCURSUS_BLADE.get(), bladeFrame);
    }
}
```

以上为接入示例，其中 Incursus Blade 的贴图路径是示例路径。
实际注册见 `MaledictTooltipBackgrounds`：启蒙之年已绑定到紫金羽翼框组合，采用 `DECORATE` 模式，
左右连续羽翼、简洁的上下边以及独立的顶部中央羽饰；四角没有外突羽毛。
图稿和打包说明位于 `art/tooltip-backgrounds/`。
一套 `TooltipBackground` 就是贴图、切片尺寸、外扩尺寸和模式的组合，可复用于多个物品。
需要按标签、NBT 或一组物品选择时，使用 `event.register(Predicate<ItemStack>, TooltipBackground)`。
需要在悬停时动态选择组合时，使用 `event.register(Item, Function<ItemStack, TooltipBackground>)`
或 `event.register(Predicate<ItemStack>, Function<ItemStack, TooltipBackground>)`：

```java
event.register(MaledictItems.INCURSUS_BLADE.get(), stack ->
        stack.hasTag() && stack.getTag().getBoolean("custom_frame") ? ornateFrame : plainFrame);
```

`ornateFrame` / `plainFrame` 为自行声明的 `TooltipBackground` 组合。
匹配规则与组合选择函数每次悬停重新执行，无需再次注册。
后注册的匹配项优先；选择函数返回 `null` 时继续查找更早的注册。
没有匹配项和没有关联物品的纯文字 tooltip 保持原样。
不要在公共初始化或物品类中引用这些客户端类。

## 贴图与模式

PNG 路径位于 `src/main/resources/assets/<namespace>/textures/...`，可以由资源包替换，按 F3+T 重载。
`textureWidth` / `textureHeight` 必须填写整张 PNG 的像素尺寸。
`border` 按 **左、上、右、下** 定义九宫格切片宽度；四角保持尺寸，边缘和中心拉伸。
例如 24×24、四边 8 的图会切成九块 8×8。
透明像素可用于翅膀、尖角、花纹等非矩形轮廓。
默认 `EdgeMode.STRETCH` 拉伸边缘；构造组合时最后传入 `TooltipBackground.EdgeMode.TILE`，
则边缘按原始长度平铺，末块裁切，四角保持尺寸、中心仍拉伸。
启蒙之年采用拉伸，左右完整的羽翼主体随提示框高度适配，肩部与末端保持固定尺寸。

组合可通过最后一个参数设置 `TooltipBackground.TopDecoration`：
`new TopDecoration(texture, textureWidth, textureHeight, width, height, overlap)`。
这是独立贴图，按固定 `width` / `height` 绘制于原版黑底的顶部中央；`overlap` 是基座进入黑底的像素数。
顶部装饰不随 tooltip 宽度拉伸、不会平铺，可用于居中徽记、羽翼或宝石。

- `DECORATE`：只画周围八块，保留原版底色和边框。中心块不绘制。
- `REPLACE`：画完整九块，将原版底色和边框变为透明。文字和图片组件仍由原版绘制。

`outset` 是相对原版最外侧轮廓的额外延伸，顺序也为 **左、上、右、下**。
原版轮廓以文字区域向外延伸 4 像素计算。
装饰模式把 `outset` 设为与 `border` 一样，八块贴图就位于原版背景外侧。
替换模式也可采用同样设置，中心恰好覆盖原版背景；也可以减小外扩，让框体进入原有 padding。
四边可分别设置，例如 `new Insets(12, 6, 20, 10)`。

贴图绘制于 z=399，原版背景与内容位于 z=400；透明混合已开启。
`Color` 的最终坐标自动跟随原版屏幕边缘翻转和其他模组的 `Pre` 定位／字体修改。
外扩装饰不参与原版定位，靠近屏幕边缘时可能被屏幕裁切；外扩不宜过大。
同样修改 `Color` 的其他模组可能覆盖颜色设置，需在游戏中检查实际组合。

## 验证

单元测试覆盖单组件高度、文字加图片组件、八块装饰的覆盖范围、九块替换、非对称切片和小尺寸框。
游戏内检查：目标／非目标物品、Shift 展开、F3+H、不同 GUI 缩放、屏幕四角、透明纹理、两种模式。
把模式切成 `REPLACE` 时应看到自定义中心底图，文字和内容图片位于贴图上方。

Forge 源码依据：
[RenderTooltipEvent](https://github.com/MinecraftForge/MinecraftForge/blob/1.20.x/src/main/java/net/minecraftforge/client/event/RenderTooltipEvent.java)、
[GuiGraphics patch](https://github.com/MinecraftForge/MinecraftForge/blob/1.20.x/patches/minecraft/net/minecraft/client/gui/GuiGraphics.java.patch)。
具体调用顺序已核对本地 47.4.23 sources，而不只依赖分支最新代码。
