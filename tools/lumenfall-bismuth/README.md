# 沉辉铋锭原生像素材质

`IngotTexture.java` 的字符网格直接定义最终 16×16 像素，不再缩小 AI 大图。
形状与明暗参考用户提供的锭图；银灰色取自初版，虹彩保留我们自己的横向青蓝、淡紫、粉金色带。
所有与透明背景共边的外沿像素形成连续的一像素轮廓：上沿基色为稍暗冷灰 `#9A9DAA`，
下沿基色为深冷灰 `#6A6B78`。左侧通向浅色竖条的 6 像素整段替换为稍暗冷灰 `#9A9DAA`；
右侧短端的 4 像素整段替换为深冷灰 `#6A6B78`，均不使用渐变。
用户红点标出的 14 个内部分界及竖条像素混入 55% 白色，提亮倒角，同时保留原有银灰、粉色和浅金色倾向。
每个像素的 alpha 只能是 0 或 255，透明像素 RGB 为 0。生成时检查尺寸、透明留边及轮廓连通性。

在仓库根目录用 Java 17 运行：

```powershell
& "$env:JAVA_HOME/bin/java.exe" tools/lumenfall-bismuth/IngotTexture.java
```

输出候选材质、透明放大预览和深浅背景预览到 `art/lumenfall-bismuth/`。
加 `--install` 更新 `src/main/resources/assets/maledict/textures/item/lumenfall_bismuth_ingot.png`，
并保留替换前的材质。仅修改贴图，无须运行数据生成或构建。
