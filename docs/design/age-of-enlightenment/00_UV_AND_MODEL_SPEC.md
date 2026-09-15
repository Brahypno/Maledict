# 启蒙之年（Age of Enlightenment）面具饰品

护符类饰品（Curios `charm` 槽），戴上后在佩戴者头部渲染一张面具。**目前只做渲染部分**，
实际效果待定。

## 0. 分工：你画 8×8，对齐和厚度由生成器算

- **美术交付**：一张 **8×8** 脸部贴图，方向与原版皮肤的脸区域一致（正面视角，图左 = 玩家右脸）。
  眼洞/口部镂空就是涂透明像素。
- **自动处理**：UV 对齐、厚度挤出、拼出完整的 64×64 模型贴图。

## 1. 几何与对齐

原版头是 `8×8×8`，且 **1 texel = 1 模型单位**，脸铺满模型 `x ∈ [-4,4]`、`y ∈ [-8,0]`。
要让画与原版脸 **1:1 对齐**，盒子边界必须落在**整数**上：

```java
addBox(-5F, -9F, -5F, 10F, 10F, 5F)   // x -5~5, y -9~1, z -5~0
```

| 轴 | 范围 | 相对头部 |
| --- | --- | --- |
| X | -5 ~ 5（宽 10） | 每边留 1，避免面具侧面与头侧面共面（z-fighting） |
| Y | -9 ~ 1（高 10） | 上下各留 1，同理避开头顶/下巴 |
| Z | -5 ~ 0（深 5） | 脸前留 1；后端停在 z=0，即耳朵前 |

正脸因此是 **10×10 texel**，中间的 8×8 覆盖模型 `x -4~4`、`y -8~0` —— 与原版脸逐 texel 对应，
透明像素挖出的眼洞必然对得上玩家的眼睛。外面那 1 圈是自动复制出来的边缘。

> 曾经用过 9×9（余量 0.5）：texel 边界落在半整数上，与原版整数网格错开半格，眼洞怎么涂都差半像素。
> **余量必须是整数。**

## 2. 厚度：从 8×8 自动挤出

`MaskTextureBuilder` 按下面的规则生成，不需要画任何一格厚度：

| 部位 | 来源 | 处理 |
| --- | --- | --- |
| 正脸 `north` 10×10 | 8×8 居中 + 四周各复制 1 texel | 原样 |
| 左右侧壁 `west`/`east` 5×10 | 正脸最外一列沿深度平涂 5 格 | RGB ×0.72 |
| 顶/底 `up`/`down` 10×5 | 正脸最外一行沿深度平涂 5 格 | RGB ×0.84 |
| 内衬 `south` 10×10 | 不透明像素的平均色 | RGB ×0.32 |

**透明会传染**：没画的地方扩展出来也是透明，所以不规则形状的面具不会被强行补出一圈实体边。

已知小瑕疵：透明像素仍会写深度，眼洞后面的粒子偶尔会被裁掉；只有大面积镂空才看得出来。

## 3. 贴图布局（64×64，本体 `texOffs(0,0)`）

盒子展开规则（`u/v` = texOffs，`w/h/d` = 盒子的 X/Y/Z 尺寸）：

```text
第一行（高 d）：[u+d, v] 上面(w×d)      [u+d+w, v] 下面(w×d)
第二行（高 h）：[u, v+d] -X(d×h)  [u+d] 正面(w×h)  [u+d+w] +X(d×h)  [u+d+w+d] 背面(w×h)
```

| 区域 | 面 | 坐标 x,y | 尺寸 | 内容 |
| --- | --- | --- | --- | --- |
| `north` | 正脸（-Z） | 5, 5 | 10×10 | 中间 8×8（6,6 起）= 美术交付 |
| `west` | 戴者右侧（-X） | 0, 5 | 5×10 | 自动 |
| `east` | 戴者左侧（+X） | 15, 5 | 5×10 | 自动 |
| `up` | 顶面（-Y） | 5, 0 | 10×5 | 自动 |
| `down` | 底面（+Y） | 15, 0 | 10×5 | 自动 |
| `south` | 内面（+Z，贴着头） | 20, 5 | 10×10 | 自动（内衬） |

可选外扩壳：同尺寸 + `CubeDeformation(0.35)`，@ `texOffs(0, 17)`，坐标整体 y 加 17。
用于描边/金属包边/发光边，不做就整块留空（**当前未做**）。

## 4. 文件清单

**资源**

| 路径 | 说明 |
| --- | --- |
| `assets/maledict/textures/curio/age_of_enlightenment.png` | 模型贴图，64×64，生成产物 |
| `assets/maledict/textures/item/age_of_enlightenment.png` | 物品栏图标，8×8 **占位** |

**美术源文件**

| 路径 | 说明 |
| --- | --- |
| `art/age-of-enlightenment/source/mask_face_pixilart_source.png` | Pixilart 原稿（64×64，画在 UV 模板上） |
| `art/age-of-enlightenment/source/age_of_enlightenment_face_8x8.png` | 从原稿裁出的 8×8 脸部贴图 |
| `art/age-of-enlightenment/uv_template_64.png` | UV 模板（纯色块） |
| `art/age-of-enlightenment/preview/` | 放大预览图 |

**代码**

| 路径 | 说明 |
| --- | --- |
| `common/item/AgeOfEnlightenmentItem.java` | 物品（`ICurioItem`，`stacksTo(1)`，RARE） |
| `client/model/AgeOfEnlightenmentModel.java` | 面具模型，层名 `maledict:age_of_enlightenment` |
| `client/AgeOfEnlightenmentCurioRenderer.java` | `ICurioRenderer`，`followHeadRotations` + `entityTranslucent` |
| `client/MaledictCurioRenderers.java` | `CuriosRendererRegistry.register` 绑定 |
| `client/MaledictEntityRenderers.java` | 注册 layer definition + `AddLayers` 烘焙 |
| `data/MaledictItemModels.java` | 物品模型（`item/generated`） |
| `data/MaledictItemTags.java` | `curios:charm` 槽标签 |
| `data/MaledictLanguage.java` | `启蒙之年` / `Age of Enlightenment` |
| `registry/MaledictItems.java`、`MaledictCreativeTabs.java` | 注册与创造模式标签页 |

数据生成产物一律走 `runData`，不要手改 `src/generated`。

## 5. 工具

```powershell
javac -encoding UTF-8 -d build/mask-tool art/age-of-enlightenment/tools/*.java

# 8x8 -> 64x64 模型贴图（含自动厚度）
java -cp build/mask-tool MaskTextureBuilder <face8x8.png> <out64.png>
java -cp build/mask-tool MaskTextureBuilder --demo          # 用示例图跑一遍

java -cp build/mask-tool MaskUvTemplate                     # 重新生成 UV 参考图
java -cp build/mask-tool CropPng <in.png> <out.png> x y w h # 裁切
java -cp build/mask-tool UpscalePng <in.png> <out.png> [倍数] # 放大预览
```

改几何时，`MaskTextureBuilder` / `MaskUvTemplate` 里的 `BOX_*`、`TEX_OFF_*`
必须与 `AgeOfEnlightenmentModel` 的 `addBox` 和 `TEXTURE_*` 同步。

## 6. 待办

- [ ] 玩法效果（佩戴加成、触发条件）—— 用户说明后再做
- [ ] 美术确认：8×8 只画了上半部分，下半部分（下颌/牙）是否要重画
- [ ] 可选外扩壳（描边/金属沿/发光）—— 需要美术出图
- [ ] 物品图标目前是 8×8 占位，正式图标建议 16×16
