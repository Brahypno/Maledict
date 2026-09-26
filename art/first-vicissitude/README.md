# first-vicissitude 美术交付

造型源为 `tools/build_blender.py` 与 `first_vicissitude.blend`；游戏网格、Blockbench 网格和翼部受击包围盒由同一个
Blender 导出器输出。旧 `RigArtGenerator.java` 是早期方块预览工具，不再用于这版资产。
造型与贴图的当前规格、预算和逐表面展开规则见 `docs/design/first-vicissitude/`；逐轮历史见该目录的 `archive/`。

## 文件

- `first_vicissitude.blend`：185 个可编辑网格，3,450 个三角面，二阶段可见 3,150 面，65 个层级关节/锚点。
  贴图内嵌；时间轴第 1 / 41 / 81 帧分别为一阶段、二阶段、死亡露核检查姿态。
  使用 Empty 父子关节做刚性绑定，没有蒙皮依赖。Blender 坐标 = `(javaX, javaZ, -javaY)`，
  实体原点对应 Blender Z = -24；16 单位 = 1 格。
- `first_vicissitude.bbmodel`：Blockbench **free mesh** 工程，包含对应网格、关节、UV 和贴图。
  采用真实收尖、曲面残环和羽片缺口，故从仅支持 cube 的 modded_entity 格式改为 free mesh。
  运行时仍使用原版 HierarchicalModel/ModelPart 动画，不增加 GeckoLib。
- `tools/build_blender.py`、`tools/export_blender.py`、`tools/review_blender.py`：重建造型、导出资产、生成检查图。
- `tools/body_surfaces.py`、`tools/wing_surfaces.py`、`tools/ring_surfaces.py`、`tools/relic_surfaces.py`、
  `tools/surface_sample.py`：逐表面图稿的编排与样稿生成；`tools/atlas16.py` 只生成样稿范围以外的旧占位。
- `tools/lower_surfaces.py`：下腹与祭衣的高密度表面绘制及 UV 分配；旧图集原密度保留，新增区域独立绘制。
- [preview/index.html](preview/index.html)：唯一预览入口，仅四张图：一阶段整体、二阶段整体、
  本轮胸背与头环修改前、修改后。均为离线渲染，**不是游戏截图**。
- `build/first-vicissitude-review/`（仓库根目录下）：技术检查产物，包括其他视角、无灯光图、
  UV 线稿、图集布局和验证报告。由工具重建，不放进用户预览目录。
- `../../src/main/resources/assets/maledict/models/entity/first_vicissitude.mesh.json`：游戏实际加载的网格。
- `../../src/main/resources/assets/maledict/textures/entity/first_vicissitude.png`：基础贴图（512×512）。
- `../../src/main/resources/assets/maledict/textures/entity/first_vicissitude_emissive.png`：
  同 UV 的自发光层，只有头核、能量与部分环片被绘制。
- `../../src/main/java/org/brahypno/maledict/rig/VicissitudeMeshGeometry.java`：导出的逐关节翼部包围盒。

## 重新生成

```powershell
javac -encoding UTF-8 -d build/rig-tool src/main/java/org/brahypno/maledict/rig/VicissitudeRigData.java src/main/java/org/brahypno/maledict/rig/VicissitudeRig.java src/main/java/org/brahypno/maledict/rig/VicissitudeMeshGeometry.java art/first-vicissitude/tools/RigPoseExporter.java
java -cp build/rig-tool RigPoseExporter
& 'C:\Program Files\Blender Foundation\Blender 5.2\blender.exe' --background --python art/first-vicissitude/tools/build_blender.py
```

命令在仓库根目录执行，Java 17。`-- --quick` 仅渲染两阶段主视角，`-- --no-render` 仅生成资产。
从脚本重建会替换 `.blend` 中的手工修改；编辑现有工程后应使用下一节的独立导出命令。

## 修改造型的正确流程

在 Blender 编辑网格/UV，保留 `runtime_joint` 属性和关节父子关系，应用几何修改器后保存。
动作和 pivot 的权威来源仍是 Java rig；本工程只预置三个检查姿态，游戏使用完整动作求解器。

```powershell
& 'C:\Program Files\Blender Foundation\Blender 5.2\blender.exe' --background art/first-vicissitude/first_vicissitude.blend --python art/first-vicissitude/tools/export_blender.py
& 'C:\Program Files\Blender Foundation\Blender 5.2\blender.exe' --background art/first-vicissitude/first_vicissitude.blend --python art/first-vicissitude/tools/review_blender.py
```

导出器更新 mesh JSON、bbmodel 和翼部包围盒，不重建造型。贴图修改后应另存到资源目录中的同名 PNG。
检查脚本验证法线、UV、胸洞和头核遮挡，并生成 `build/rig-tool/wing-vertices.csv`，用于独立 Java 检查：

```powershell
javac -encoding UTF-8 -d build/rig-tool src/main/java/org/brahypno/maledict/rig/VicissitudeRigData.java src/main/java/org/brahypno/maledict/rig/VicissitudeRig.java src/main/java/org/brahypno/maledict/rig/VicissitudeMeshGeometry.java art/first-vicissitude/tools/RigMeshCheck.java
java -cp build/rig-tool RigMeshCheck
```

## 已确认的造型基线

这些是用户明确肯定、后续优化不得推翻的方向：

- 主题：失败圣性的残骸——晶体头、真实胸洞与同心断环、无腿悬浮结构，一阶段羽翼转二阶段骨翼、落羽转场。
- 肩臂力量感、翼部伸展轮廓、胸环位置，以及逐表面纹理方向。
- 头后断环的待机与施法旋转绕模型 Z 轴（环面法线），不绕垂直 Y 轴翻转。
- 头部为末地水晶式分层内核/外壳：八片多面裂壳、断开的菱形包围框、偏置星芒；不用完整立方体。
- 胸部以有棱面的实心胸肩、侧壁、腹部围住较小空洞，取消矩形胸甲与空洞占满躯干的结构。
- 块状手与护手背板，不做分指和拇指；前臂护甲向腕部收窄。
- 左右臂与对应左右翼优先复用 UV；同一部件的正背、侧壁和断面按需要独立展开。
  禁止用重复小格或简单放大贴图冒充细节提升。

## 尚未验证

按用户反馈撤去新增的圆形内壁与整圈背缘，恢复之前开放的胸背空洞；完整模型回到 3,450 面。
头环中心由 `(0,-28,9)` 移至 `(0,-32,16)`；椭圆半径由 14.1/15 缩至 11.5/12，
截面半宽 1.2→0.65、半厚 0.85→0.32，保留四段断片与旋转。正背发光刻纹使用已有自发光渲染层。
图集仍为 512×512，共 293 个区域；前胸活动环、双臂、下腹与祭衣的形体保持。
同机位胸背对照见 `preview/index.html`；其他视角和无灯光技术图输出到 `build/first-vicissitude-review/`。

游戏实机光照、资源重载、三类武器握持和实战性能尚未在客户端验收；所有 `preview/` 图都是离线正交预览，
不能代替游戏截图。逐轮验证记录见 `docs/design/first-vicissitude/03_ENGINEERING_AND_VERIFICATION.md`。
