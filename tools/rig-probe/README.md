# tools/rig-probe

无常骨架的离线实测程序。**不依赖 Minecraft**：`rig/` 下三个源文件是纯数学，
所以这个程序可以直接 `javac` 出来跑，用来核对动作幅度、刀身可达范围、
单 tick 位移和手部离躯干的间距。

它是 `docs/design/first-vicissitude/02_ANIMATION_AND_HITBOX.md` 第 6 节那张表的来源。
改了 `VicissitudeRig` 的动作系数、`Action` 的时长/命中帧、或者 `BLADE_*` 参数之后，
必须重跑并把新数字写回那张表。

## 运行

在仓库根目录执行，需要 Java 17：

```powershell
$out = "build/rig-probe-out"
javac -encoding UTF-8 -d $out `
  src/main/java/org/brahypno/maledict/rig/VicissitudeRigData.java `
  src/main/java/org/brahypno/maledict/rig/VicissitudeRig.java `
  src/main/java/org/brahypno/maledict/rig/VicissitudeMeshGeometry.java `
  tools/rig-probe/RigProbe.java
java -cp $out RigProbe
```

## 输出怎么读

| 列 | 含义 |
| --- | --- |
| `maxEdge` | 命中窗口内，刀身（握点或刀尖）离实体中心最远的水平距离，单位格 |
| `impactF` | 命中帧刀尖的前伸距离，单位格 |
| `stepMax` | 动作内任意关节相邻两 tick 的最大位移，用来发现抖动 |
| `stepEnd` | 最后一帧渲染帧相对静息姿态的最大偏差，用来发现收尾跳变 |
| `minBody` | 挥动的手离头核/胸壳的最近距离，用来发现穿模 |

末尾还会打印 DASH 副手在最后几帧的收敛过程。

注意：程序里的 idle 时钟取常数 `1000`。待机层是按世界时钟连续变化的，
让它跟着采样 tick 变会把两套动画混进同一个测量里。

## 运行时诊断开关

离线表算不出「Entity 实际站在哪、目标实际在哪」，所以实体侧另有一个开关：

```powershell
.\gradlew.bat runClient -Dmaledict.meleeTrace=true
```

打开后会逐 tick 打印近战动作的 Boss 位置与朝向、刀身在世界坐标下的握点与刀尖、
到目标的体积距离、命中与否，以及被参战名单挡掉的实体。
用它一次就能分清是**站位**、**高度**还是**容差**的问题。