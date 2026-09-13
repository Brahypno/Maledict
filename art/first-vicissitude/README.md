# first-vicissitude 美术交付

本目录保存无常（`maledict:first_vicissitude`）的可编辑美术源和离线预览。所有产物都由
`tools/RigArtGenerator.java` 从游戏内同一份骨架数据生成，不存在“手工贴图和模型对不上”的可能。

## 文件

- `first_vicissitude.bbmodel`：Blockbench 工程（modded_entity 格式）。层级、pivot、cube 尺寸、
  每个面的 UV 与贴图引用齐全，可直接编辑。工程内坐标是 y 向上；游戏 Java 模型使用原版
  `y 向下` 约定，两者关系是 `javaY = -blockbenchY`，实体原点在 `y = 0`，16 单位 = 1 格。
- `preview/phase_one_{front,side,back}.png`、`preview/phase_two_{front,side,back}.png`：
  离线正交投影预览，使用与游戏相同的姿态求解结果。**这不是游戏内截图**，日夜、粒子开关、
  实机光照仍需要在客户端补拍。
- `../src/main/resources/assets/maledict/textures/entity/first_vicissitude.png`：基础贴图（256×256）。
- `../src/main/resources/assets/maledict/textures/entity/first_vicissitude_emissive.png`：
  同 UV 的自发光层，只有头核、能量与部分环片被绘制。
- `../src/main/resources/assets/maledict/textures/item/curio_return_token.png`：凭证图标。

## 重新生成

```powershell
javac -encoding UTF-8 -d build/rig-tool -sourcepath src/main/java art/first-vicissitude/tools/RigArtGenerator.java
java -cp build/rig-tool RigArtGenerator
```

工具会同时打印 cube 数量；如果新增 cube 导致 256×256 排不下，会直接抛错而不是悄悄错位。

## 修改造型的正确流程

1. 改 `src/main/java/org/brahypno/maledict/rig/VicissitudeRigData.java`（关节 pivot、cube、材质）。
2. 需要动作/姿态时改 `rig/VicissitudeRig.java`（阶段、idle、动作、受伤、死亡曲线）。
3. 重新运行上面的工具，贴图、bbmodel 与预览会一起更新。
4. 受击分段与释放锚点也在同一份数据里，改完不需要另外同步服务端几何。
