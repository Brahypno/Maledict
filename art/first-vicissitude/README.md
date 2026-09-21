# first-vicissitude 美术交付

2026-09-13 按用户要求改用 Blender 重做。造型源为 `tools/build_blender.py` 和
`first_vicissitude.blend`；游戏网格、Blockbench 网格和翼部受击包围盒由同一个 Blender 导出器输出。
旧 `RigArtGenerator.java` 是早期方块预览工具，不再用于这版资产。

## 文件

- `first_vicissitude.blend`：186 个可编辑网格，3,572 个三角面，65 个层级关节/锚点。身体与肘腕、握拳形态修订见设计文档 17；翼部沿用 15。
  贴图内嵌；时间轴第 1 / 41 / 81 帧分别为一阶段、二阶段、死亡露核检查姿态。
  使用 Empty 父子关节做刚性绑定，没有蒙皮依赖。Blender 坐标 = `(javaX, javaZ, -javaY)`，
  实体原点对应 Blender Z = -24；16 单位 = 1 格。
- `first_vicissitude.bbmodel`：Blockbench **free mesh** 工程，包含对应网格、关节、UV 和贴图。
  本轮采用真实收尖、曲面残环和羽片缺口，故从仅支持 cube 的 modded_entity 格式改为 free mesh。
  运行时仍使用原版 HierarchicalModel/ModelPart 动画，不增加 GeckoLib。
- `preview/phase_one_{front,side,back}.png`、`preview/phase_two_{front,side,back}.png`：
  Blender 正交预览，使用 Java 导出的相同姿态。`preview/blender` 另有主视角、灰模三视图、
  胸部/头部细节、死亡露核和夜间灯光检查。**这些不是游戏内截图**。
- `../../src/main/resources/assets/maledict/models/entity/first_vicissitude.mesh.json`：游戏实际加载的网格。
- `../../src/main/resources/assets/maledict/textures/entity/first_vicissitude.png`：基础贴图（256×256）。
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

已通过 `compileJava --offline`，以及 1,483,776 次实际翼部顶点在各动作、两阶段、四朝向和收翼状态下的
服务端包围盒检查。缩小后的胸洞 12 条采样射线全部贯通，头核在一阶段正面采样中被外壳遮蔽。
游戏实机光照、资源重载、三类武器握持和实战性能尚未在客户端验收。

## 用户反馈修订

最新翼部：按用户对减面版的反馈，改为实心关节骨架、开放骨叉和带浅折脊的扇形羽面；四种原创羽材质用透明缺口、羽轴与斜向羽枝表达细节。羽面正背错层，右翼局部缺羽，复用原共享骨架与 cutout 双面渲染。面数为 1,792，最新翼部检查为 159,744 次；详见 `docs/design/first-vicissitude/15_WING_STRUCTURE_AND_FEATHERS.md`。`preview/blender/wing_front_detail.png` 和 `wing_back_detail.png` 为新增细节视角。下段 1,488 面为先前减面稿记录。

2026-09-20：在现有造型源上改为更清晰的棱面骨架、分段断环、有厚度的片状羽毛与成组像素材质，保留原主体轮廓和关节。按用户补充的参考面数要求，最终收敛到 1,488 三角面；移除重复羽层与细骨条，不使用全局塌陷破坏胸洞或关节。重新导出所有资产；最新翼部检查覆盖 84,480 次顶点/动作/阶段/朝向/收翼组合。详见 `docs/design/first-vicissitude/13_COMBAT_AND_STYLE_REFINEMENT.md` 和 `14_REFERENCE_GEOMETRY_BUDGET.md`。两阶段主视角、三视图、灰模与细节图均通过现有 Blender 工具生成，不是游戏截图。下列条目为历次修改记录，羽片数量与几何细节以本段及 13 为准。

- 头后断环的待机及施法旋转改绕模型 Z 轴（环面法线），不再绕垂直 Y 轴翻转；有独立环面检查。
- 每侧翼增加至 10 主羽、10 前覆羽、10 后层羽、7 内侧次羽，另有上缘骨片与破羽。
  三股翼骨、分叉肋骨和前后错层形成侧向体积；四组主羽关节移到各组羽根。
- 加厚肩胸、上臂、前臂和三节残躯，补上颈部、肩胛到翼根、胸部到下身的连接。
- 移除分指和拇指，使用块状手与护手背板。
- 胸前取消等粗圆截面金属弧，改用不等宽骨质残弧、错位黑紫底片、断续紫色星轨与非对称裂光。
  中央保留缺失与透空。
- 最新头部方向按用户澄清采用末地水晶式分层内核/外壳：八片多面裂壳、断开的菱形包围框、
  偏置星芒；不用完整立方体，也不恢复早期零散长尖片拼成的人脸轮廓。
- 胸部以有棱面的实心胸肩、侧壁、腹部围住较小空洞，取消矩形胸甲与空洞占满躯干的结构。
- 前臂护甲向腕部收窄，腕环与较小的手掌连续衔接，保留简化手部而不增加手指。
- 修复贴图输出时重复线性化造成的灰暗；PNG 按 sRGB 色板直接输出，Blender 重新加载同一 PNG。
  骨白、蓝灰羽片和黑紫外壳使用明确分区与明暗层次，而非提高全身自发光。
