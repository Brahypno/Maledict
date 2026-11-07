# 光的残片贴图

Java 17，无外部依赖。像素源位于 `LightFragmentTexture.java` 的 16×16 字符网格中。
`LightFragmentAnimation.java` 根据该网格生成已确认的动画；中间图和预览保存在忽略追踪的
`art/light-fragment/`，游戏读取 `src/main/resources/assets/maledict/textures/item/` 中的成品。

在仓库根目录依次运行：

```powershell
java tools/light-fragment/LightFragmentTexture.java
java tools/light-fragment/LightFragmentAnimation.java . --install
.\gradlew.bat runData
```

省略 `--install` 只生成候选资源与预览。修改物品模型时改 `MaledictItemModels`，再运行
`runData`，不直接编辑 `src/generated`。

成品为两张 16×1536 RGBA PNG，各有 96 个原生 16×16 帧。普通层与发光层使用相同的
`.png.mcmeta`，每帧 4 ticks，关闭插值，总周期 19.2 秒。

- 石壳每 19.2 秒旋转一圈；两个双像素裂缝作为整体随壳移动，始终共边连接。
- 外围金色残环每 9.6 秒旋转一圈，只用 8 个角度，从石壳前方经过。
- 灵魂每 4.8 秒绕上下轴自转，同时上下各浮动 1 像素。
- 左上光照固定；只有灵魂与石壳裂缝进入自发光层。

生成时检查透明通道、裂缝数量与连接、光核可见性和循环闭合。暗处 GIF 是普通层减亮的
示意，游戏中的实际受光需要在客户端检查。
