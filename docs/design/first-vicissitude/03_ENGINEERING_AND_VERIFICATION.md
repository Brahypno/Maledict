# 无常：工程与验证

本文档记录无常的实现状态、跑过的命令、每一轮改了什么，以及**从未验证的事项**。
规格见 [01](01_SPEC.md)，动作与判定见 [02](02_ANIMATION_AND_HITBOX.md)。

> 一句话状态：规格已闭环，M1–M7 已按规格实现，编译、显式 `test` 与 `runData` 通过，
> **游戏内场景全部未验证**。已有代码不等于已验收。

---

## 1. 里程碑状态

| 里程碑 | 状态 | 离线验证方式 | 未验证 |
| --- | --- | --- | --- |
| M1 状态与接口 | 代码完成待验证 | stage/action/序列/起始时间/持械状态全部走 `SynchedEntityData`；难度时长集中在 `BossDifficulty`；免没收 tag 与账本已接入 | 客户端与专服的同步表现 |
| M2 灰模及双姿态 | 代码完成待验证 | Java 模型、分段受击、翼阻挡与脱困瞬移；离线预览见 `art/first-vicissitude/preview` | 实机姿态与碰撞 |
| M3 正式材质及持物 | 代码完成待验证 | 256×256 base/emissive 由离线工具从同一份 rig 数据生成；右手挂点渲染真实武器 | 实机握持 |
| M4 动画与攻击接入 | 代码完成待验证 | 一阶段与二阶段全部技能落地；近战按刀刃命中窗口逐 tick 判定 | 实机动画与命中 |
| M5 投掷闭环 | 代码完成待验证 | Boss 专用弹体持有 ItemStack、token 唯一、保存加载、死亡取消并不掉落 | 实机投掷与接住 |
| M6 效果与死亡 | 代码完成待验证 | Lodestone 粒子、地面预警、斩击、位置震动包、80 tick 死亡序列 | 实机特效 |
| M7 集成验收 | 部分完成 | `compileJava`、`test`、`runData` 已执行 | 游戏内场景全部未验证 |

早期 M2/M3 记录里的「52 个 cube、55 个关节」只代表旧方块预览工具，不是当前交付。
当前美术交付是 185 网格 / 3,344 三角面 / 65 关节与锚点，二阶段可见 3,044 面。

---

## 2. 跑过的命令与结果

| 命令 | 结果 |
| --- | --- |
| `.\gradlew compileJava --offline` | BUILD SUCCESSFUL（实现轮、第十轮、第十三轮、各资产轮） |
| `.\gradlew test --offline` | BUILD SUCCESSFUL；测试数随轮次从 5 项增长到 54 项，始终 0 失败 |
| `.\gradlew runData --offline` | BUILD SUCCESSFUL（实现轮、召唤仪式轮、掉落表轮）；不动生成器时不跑 |
| `javac` + `java RigArtGenerator` | 生成 base/emissive 贴图、凭证图标、bbmodel、预览图与尺寸报告 |
| `javac` + `java RigMeshCheck` | 三条 PASS：光环保持面内、胸环刚性且与光环反向同速、翼顶点组合检查 |
| `javac` + `java RigPoseExporter` | 从真实运行时骨架导出胸环中心 |
| `javac` + `java tools/rig-probe/RigProbe.java` | 见 [02 第 6 节](02_ANIMATION_AND_HITBOX.md)的实测表 |

离线检查的报数（无独立命令行记录，由 `RigMeshCheck` / Blender / rig-tool 产出）：

- Blender：12/12 胸洞射线贯通、头核正面遮挡、胸肩体积、贴图对比、UV 与法线。
- `RigMeshCheck`：最多 1,483,776 次翼顶点 × 动作 × 阶段 × 朝向 × 收翼组合检查；
  23,736 次实际胸环顶点、半径约 4.46–5.54；
  `checkWingComposure` 3,060 组动作/阶段/收翼/时钟组合。
- `DumpVolumes`：翼段包围盒（一阶段 y 2.44–4.34、|x| ≤ 3.36、厚 0.5 格；
  二阶段 y 2.48–3.51、|x| ≤ 3.68）。
- `DumpMarker`：地面标记坐标系往返验证，0/37/90/−143 度四种朝向、三个采样点误差均为 0。

以上都是**数值验证**，不代表游戏内的观感和帧率。

---

## 3. 测试资产

| 类 | 覆盖 | `@Test` |
| --- | --- | ---: |
| `common/entity/VicissitudeVitalityTest` | 真生命、击杀计数与世界存档 | 5 |
| `common/entity/DamageAdaptationTest` | 适应效果：按 damage message 记账与自然指数递减 | 15 |
| `common/curio/HalfHealthTest` | 启蒙之年的半血判定 | 7 |
| `common/curio/EnlightenmentLevelTest` | 护符 NBT 上的等级读取 | 6 |
| `common/effect/BlessedRegenerationTest` | 生命祝福的额外自然回血量 | 7 |
| `common/effect/RipeningBonusTest` | 熟成之赐的经验加成取整 | 8 |
| `rig/VicissitudeRigTest` | 动作曲线、姿态连续性、刀刃可达范围与高度、手部间距 | 12 |
| `rig/VicissitudeFeatherShedTest` | 转场落羽的错时曲线与二阶段碰撞组 | 3 |
| `data/FirstVicissitudeSpiritDataTest` | Malum 精魂表（手写资源） | 5 |
| `common/item/IncursusBladeBlinkTest` | 物品栏眨眼的纯换算规律 | 4 |

钉住的关键规则：

- `VicissitudeVitalityTest`——同 tick 连续 21 次只计一次；相邻尝试间隔 100 tick 继续累计，
  超过则重新从一次开始；窗口内受伤/治疗不清 kill 计数；写真实 `.dat` 后重载保伤势、
  最大生命、冷却与 kill 次数，不同 UUID 分开存；改 Capability 导出的 NBT 不影响已绑定副本。
- `DamageAdaptationTest`——首击全额；第 2/3/4 下 e⁻¹/e⁻²/e⁻³；分 message 各记；
  记满顶掉最久未命中的一条；适应 0 或负数关闭且什么都不记；null/空串不减伤不记账；
  清账回全额；存档往返保序；坏行丢弃。
- `HalfHealthTest`——40/100 算半血、50/100 恰好半血触发、50.1/100 与 100/100 不触发；
  只看出手时血量；上限 0 不触发；线随上限走。
- `EnlightenmentLevelTest`——无 NBT 或无键读作 0 级；显式 0 与无键同；负数夹回 0；
  键类型不符当没写且不抛异常。
- `VicissitudeRigTest`——见 [02](02_ANIMATION_AND_HITBOX.md)：
  wind 在最后一帧归零、strike 峰值恰在命中帧、命中窗口包含命中帧且落在动作内、
  刀刃可达范围在 [4.0, 5.75]、任意 tick 手离躯干 ≥0.75 格、
  动作最后一帧离静息 ≤0.45 格、线段距离在端点处截断。
- `FirstVicissitudeSpiritDataTest`——`registry_name` 正确、`primary_type` 是九种真精魂之一、
  八种各 6 加幽影 1 不重不漏、合计 49 点、幽影只发一枚。

### 运行方式

```powershell
.\gradlew.bat test                                  # 全部
.\gradlew.bat test --tests '*VicissitudeRigTest'    # 单个类
```

首次运行需要下载测试依赖，缓存齐全后可加 `--offline`。
HTML 报告在 `build/reports/tests/test/index.html`，日志在 `build/tests`。
临时存档由 JUnit `@TempDir` 创建和清理。
**本项目已取消 `check` 对 `test` 的依赖**：`check` 与 `build` 不会自动跑测试，必须显式执行 `test`。

---

## 4. 逐轮改动（文件级）

### 实现轮（M1–M7）

新增：`rig/VicissitudeRigData.java`、`rig/VicissitudeRig.java`、`rig/VicissitudeMeshGeometry.java`；
`common/entity/VicissitudeBossEntity.java`、`common/entity/FirstVicissitudeBossEntity.java`、
`common/entity/VicissitudeBossStage.java`、`common/entity/VicissitudeSpiritBoltEntity.java`、
`common/entity/VicissitudeScytheProjectileEntity.java`、`common/entity/VicissitudeLightOrbEntity.java`；
`common/curio/VicissitudeCurioLedger.java`、`common/curio/VicissitudeCurioReturns.java`、
`common/item/CurioReturnTokenItem.java`、`common/MaledictTags.java`；
`client/model/FirstVicissitudeBossModel.java`、`client/model/VicissitudeBlenderMesh.java`、
`client/FirstVicissitudeBossRenderer.java`、`client/vfx/FirstVicissitudeEffects.java`、
`client/vfx/FirstVicissitudeClientEvents.java`、`client/VicissitudeSpiritBoltRenderer.java`、
`client/VicissitudeScytheRenderer.java`、`client/VicissitudeLightOrbRenderer.java`；
`network/VicissitudeEffectPacket.java`；`common/rite/VicissitudeRiteType.java`、`common/rite/SummoningRite.java`；
资产 `assets/maledict/textures/entity/first_vicissitude.png`、`_emissive.png`、
`assets/maledict/textures/item/curio_return_token.png`、`models/entity/first_vicissitude.mesh.json`。

修改：`registry/MaledictEntities.java`、`registry/MaledictItems.java`、`registry/MaledictCreativeTabs.java`、
`client/MaledictEntityRenderers.java`、`config/MaledictConfig.java`、`data/MaledictItemTags.java`、
`data/MaledictItemModels.java`、`data/MaledictEntityLoot.java`、`data/MaledictLanguage.java`、
`client/MaledictCodexEntries.java`、`network/MaledictNetwork.java`、`Maledict.java`。

### 用户反馈修正（按主题归并，同一主题只留结论）

**判定与几何**

1. **翼判定体积单位错**（`segmentVolumes`）：翼段半径把「格」当「模型单位」传入，
   每侧判定盒膨胀约 5 格。改为内部换算模型单位，每侧由 2 段改为 3 段（翼根 / 外翼骨 / 羽片扇面）。
2. **推挤手感**：只在玩家包围盒与真实翼段相交时生效，只施加 `Player#push`（水平 0.06）；
   不再服务端瞬移玩家、不再每 tick 强制 `hurtMarked`；贴墙时让翼收拢。
3. **翼乱翻**（`dragWingsYaw/Pitch`）：实参已是角度却又乘 16–24 的系数，
   翼根在释放帧被甩到 −254°（横斩）/−252°（竖劈）/−432°（重击）/−120°（环施法）。
   改为取身体转角的 60% 并封顶 ±30°，翼根/外翼/羽片按 1.0/0.6/0.4 递减。
4. **近战判定跟随刀身**：横斩、竖劈、重击不再用「半径 + 扇形角」，
   改为在命中窗口内逐 tick 用刀刃线段做胶囊判定；刀身长度与握点偏移成为唯一可达范围来源。
   详见 [02](02_ANIMATION_AND_HITBOX.md)。
5. **近战完全打不中**：判定改成真实三维线段之后暴露出一串问题，全部表现为「它靠近了、攻击了，但打不中」：
   ① Boss 二阶段巡航高度让刀刃扫过世界高度 2.0–3.7 格，站着玩家（碰撞箱到 1.8）整个人在刀下方，
   **任何水平距离都不可能命中**——旧的「半径 + 扇形角」只看水平分量，所以从没暴露过；
   ② 站位用错了数：起手距离 5.75 被当成站位距离，而刀身前伸只有 4.2–4.5 格，
   于是 Boss 站得比目标该在的位置更远，刀落在目标身前；
   ③ 镰刀握在右手，整个刀身偏在正对方向右侧约 0.75 格，身体正对目标时刀刃从**肩膀旁边**扫过去，
   转身只能把偏移减半（刀刃平面沿目标滑动，不是压上去）；
   ④ 横斩被写成齐胸横扫、刀身在 y≈2.2 扫过，即使站到地面也仍高过站着玩家的头。
   修法：按动作俯冲（`meleeDiveY` + `Action.bladeHeightAboveFeet()`）、
   按刀身自己的前伸量站位（`Action.bladeForwardReach()`）、横斩压低手臂并放平刀身
   （`bladeLoweringDegrees()` 34°、`bladeTrailDegrees()` 28° → 62°）、
   判定量到受害者**包围盒**而不是中心点并留出 `BLADE_HIT_RADIUS = 0.60` 的范围。
   ③ 那 0.75 格的横向偏移**有意不修**——判定是一条带不是一条激光，
   目标不必落在刀身中线上；理由和"哪些数该算准、哪些该留成范围"的分界见
   [02 §4.4](02_ANIMATION_AND_HITBOX.md)。
6. **动作收尾跳变**：`wind` 通道原本 latch 到动作结束，最后一帧所有蓄力系数仍全额生效、
   下一帧一次性归零。改为在 `duration - 1` 之前回落，并给大挥砍加收势手臂保护。
   详见 [02](02_ANIMATION_AND_HITBOX.md)。

**瞄准与朝向**

7. **朝向接管**（`tickFacing`）：原版 LookControl 加寻路与直控位移争 yaw（30–40°/tick，
   180° 转身只要 4–5 tick）。改为 `super.tick()` 之后自管：跟踪 20°/tick、
   动作期与蓄力期分别限速、目标偏角 >90° 额外 ×1.8；DORMANT 与死亡不转向；
   多目标改为按 `1/(1+0.15d)` 加权的合成方向并加权平均视线高度。
8. **模型 yaw 根因**（`FirstVicissitudeBossRenderer#setupRotations`）：
   原版身体 yaw 就在 `setupRotations` 里，此前为去掉死亡侧倒把整个方法覆写成空实现，
   yaw 一起被删——实体照常转身，客户端模型却永远朝同一方向。
   现在只保留 yaw、只跳过死亡翻滚。
9. **射弹瞄准**（`aimPoint` / `aimedDirection`）：原来只取水平朝向，
   从 3–4 格高的翼尖平飞过站地球头顶。改为每发从各自翼锚点三维瞄准目标躯干中心，
   再叠加水平扩散。

**武器与伤害**

10. **持械位置**（`poseStackTo`）：WeaponLayer 原本只对挂点自身做 `translateAndRotate`
   （不含父链），武器被画在模型原点。改为走肩→上臂→前臂→手→握点整条父链，
   并用 180° x 旋转换算到物品渲染坐标系（不用负缩放）。
11. **武器朝向**：中间试过若干屏幕平面内旋转，最终确认原版基准即正确——
    `item/handheld` 的 `thirdperson_righthand` 就是 rotation [0,−90,55]，
    `ItemInHandLayer` 再补 Rx(−90)·Ry(180)；其 translate 0.625 是肩到手心的距离，
    本模型锚点已在手上，不重复位移。三个客户端旋转旋钮已删除。
12. **渲染与实际手持分离**（D11）：新增同步字段 `DATA_WEAPON_TIER`，
    `getDisplayWeapon` 在客户端按档位重建 ItemStack；手上仍维持同档武器
    （空手立即补、类型不符每 20 tick 纠正、飞行镰刀不被触碰）。
    `applyWeaponAttributes()` 从武器自身属性表读出并烘焙成固定修饰符，
    `suppressHeldItemAttributes()` 每 tick 清掉手上临时修饰符。
13. **区块加载崩溃**（`Modifier is already applied on this attribute!`）：
    固定修饰符是实体存档的一部分，重载后属性表已有这些 UUID 而运行期列表为空。
    改为按槽位 UUID 无条件 `removeModifier`（对不存在的 id 是空操作），运行期列表只作保险。
14. **玩家伤害分档**（`hurtParticipant`，所有技能的唯一伤害入口）：
    SIMPLE/DIFFICULT 用 light、COMPLETE/EXTREME 用 medium，非玩家走普通 `hurt`，
    每轮每目标一次并清 `invulnerableTime`。

**节奏与技能**

15. **一阶段时长**：180/140/100/75 秒 → 1800/1400/1000/750 tick。
    已锁定 `PhaseOneDuration` 的存档不受影响。
16. **一阶段节奏**：基础槽 40 → 30 tick；羽片齐射/胸/环冷却 160/200/240 → 110/130/160 → **70/90/80**；
    选择方式改为「偶数基础槽才尝试特殊技能」；追踪球每两个扇射槽一枚。
    不再压缩的原因是同屏压血球上限 48、弹体寿命 80 tick。
17. **压血**：改为「释放标记**或**拥有者当前仍处于一阶段」判定；
    压血球是大的慢脉冲双层光核，普通伤害球是小的快闪单核（不能只靠颜色区分）。
18. **悬停与环绕**：悬停高度 5 → 2.5 格、环绕半径 9 → 7 格、环绕角速度 0.08 → 0.03 rad/tick；
    动作开始到 release+4 tick 之间水平速度衰减到 0.5 倍并停住。

**地面预兆（三次修正，最后一次是重做）**

18. ① 坐标系：渲染层拿到的 pose stack 已在实体变换中，直接当世界坐标会跑到几十格开外；
    改为统一用世界角 `atan2(dz, dx)`。
    ② 判定补径向检查：原来只用方形包围盒筛（对角最多 2.83/8.5 格），
    现在胸标记 `dist ≤ 半径`、环裁定 `内半径 ≤ dist ≤ 外半径`。
    ③ 实体渲染层被视锥裁掉时整层不画，改为世界坐标独立 pass。
    ④ **整段重做**：`RenderType.lightning()` 是原版闪电/天气用的（加色、全亮），
    任何颜色都显成发白发黄；删除几何环与 `MaledictClientRenderEvents#onRenderLevel`，
    改为每客户端 tick 的符文粒子（胸标记整圆、环裁定 12 个固定槽位断环，
    全部 `enableForcedSpawn()`，`SIGIL_BLACK` + `LUMITRANSPARENT`）。

**仇恨、名单与适应**

19. **参战者与 UUID 清理**：起因是用户实测「无常打守者没动画」，
    翻存档确认实体停在 PHASE_ONE、名单为空——动画链路本身是通的，缺的是「谁算对手」。
    对齐原版 `HurtByTargetGoal`：谁打它谁进名单、完全不看视线；
    创造与旁观在入口忽略；宠物与召唤物连主人一起参战且第一次还手优先主人；
    二阶段认任意存活生物且选目标玩家优先。新增「没有对手满 100 tick 回到未参战」。
20. **交战半径**：`engagementRange` 12 → 96 格，`FOLLOW_RANGE` 32 → 96；
    一阶段补上 >64 格只接近不开火的门（一阶段弹体寿命只够约 36 格）。
21. **适应效果**：按 damage message 记账、已记录的消息第 n 次命中吃 e⁻⁽ⁿ⁻¹⁾、
    记满顶掉最久未命中、挂在 `modifyIncomingDamage` 最后一档、NBT 持久化、回到未参战时清账。
    同时**取消压血喘息**：删除 `PRESS_RECOVERY_TICKS`、`onPressLanded`、`pressRecoveryTicks`、
    `dealsPlayerDamage` 与两处弹体回调。
22. **精魂表**：追加幽影精魂 ×1，总数 48 → 49 点，提尔锋额外伤害 96 → 98。

**其他**

23. **定场词**：重写实体时丢了调用（只剩 lang key），补回 DORMANT → PHASE_ONE 那一次，
    按死亡次数做到每人每命一次。中途实现的自定义 GUI overlay 全部撤回，
    删除 `VicissitudeBannerOverlay.java` 与 `MaledictClientTicks.java`。
24. **召唤仪式**：新增四个灵组合唯一的仪式，不改 Malum 任何现有仪式。
25. **胸环旋转**（`spinChestRing`）：三段弧各自带枢轴，单关节自转只会原地打转；
    改为以 TORSO 局部 `(0,1)`（网格实测环心）为心整体刚性旋转。
26. **重击震动**：8 tick / 峰值 0.15 → 12 tick / 峰值 0.30。

### 资产轮（2026-09-13 ~ 2026-09-23）

面数演进，每轮都是完整重新导出：

| 网格 | 三角面 | 二阶段 | 这一轮做了什么 |
| ---: | ---: | ---: | --- |
| 380 | 23,244 | — | 用户否定旧方块模型精度后改用 Blender 重做 |
| 108 | 1,488 | — | 首次减面（减幅约 93.6%），用户反馈粗糙 |
| 128 | 1,792 | — | 重做翼部几何与材质，四格原创羽毛 cutout |
| 168 | 3,312 | — | 恢复环绕肋骨、肩胛、前臂双骨、腕环、横突与环刻轨 |
| 186 | 3,572 | — | 肘轴、护片、腕块、掌体、回扣指节 |
| 217 | 5,246 | — | 粗骨承托与同心残环 |
| 215 | 5,552 | 4,952 | 分叉骨翼、错时落羽、二阶段不再渲染羽片 |
| 217 | 4,416 | 4,116 | 去环截面倒角与羽片折脊 |
| 185 | 3,504 | 3,204 | 六个支撑网格改四边截面 |
| 185 | 3,584 | 3,284 | 移除 30 个装饰网格，环保留 11 段实体 |
| 185 | 3,332 | 3,032 | 祭衣薄片减面 |
| **185** | **3,344** | **3,044** | 祭衣重做为宽幅包绕残衣 |

逐表面 UV：第一轮 50 个网格 / 25 对左右共用；第二批扩展到躯干与完整骨翼（105 网格、44 对镜像复用）；
当前覆盖 116 个网格、44 对左右部件复用。区域数量只用于核对布局，不代表视觉质量。

被用户否决的中间方案：早期 380 网格的方块精度；16×16 单元模板式图集
（技术检查通过但视觉被否）；祭衣的窄挂片版本。

---

## 5. 从未验证的事项

这一节是本文档最重要的部分。以下全部**没有**在客户端或专服上验证过：

1. 客户端与专服双人场景、晚加入不串阶段、走出与走入跟踪范围。
2. 翼分段命中、翼阻挡玩家与地形、脱困瞬移的全部场景
   （`mobGriefing` 开/关、可破坏与不可破坏方块、狭窄洞穴、无可用落点、未加载邻区、飞镰期间的脱困）。
3. 存档：转场中、二阶段空手、弹体飞行中、死亡过程保存重载后不重复装备/没收/命中/掉落。
4. 外观：游戏内的实际观感、三件武器的持握尺寸、日夜与低粒子设置下的可读性。
   `art/first-vicissitude/preview/` 下的图都是离线正交预览，**不是游戏截图**，
   除了这些预览之外没有游戏内截图。
5. Lodestone / Malum 具体效果的游戏内表现（粒子数量、震动强度单位、斩击方向是否与武器一致）。
6. 饰品返还的性能数据（1/8 名玩家、每人 8/32 件的耗时）与第三方装备回调成本。
   **该项至今从未实测。**
7. 四个召唤仪式：柱数与顺序是否与法典配方页一致、召唤位置是否合适、
   会不会与图腾结构卡住、64 格去重够不够、JEI 是否会列出新仪式。
8. 四难度血量与伤害手感、`DamageProbe` 在玩家身上的实际表现
   （尤其 COMPLETE/EXTREME 的补足伤害是否会显得「护甲无效」）、缴械模组下的补装行为。
9. 法阵黑白对比在日夜与不同地表上的观感、最低粒子档位下是否仍读得出判定边界、
   重击震动 0.30 的强度（Lodestone 强度单位按观感校准，未做游戏内确认）。
10. 资产轮：实机动画、光照、碰撞手感、连续落羽的游戏内节奏、实机帧率（未声称提升）。
11. **近战到底够不够得着**：俯冲高度、站位距离、容差和横斩的手臂压低量都是离线几何反推出来的，
    只有进游戏才能确认。特别是横斩的刀尖会降到 Boss 脚底以下约 0.46 格（切入地面），
    这个观感是否可接受没有验证过。
    `BLADE_HIT_RADIUS = 0.60` 是**范围**（见 [02 §4.4](02_ANIMATION_AND_HITBOX.md)），
    意味着刀刃可以在离目标身体约 0.6 格处判定命中——这比原版近战的锥形判定更紧，
    但比"刀刃必须穿过身体"更松。进游戏要确认两点：**该中的时候中**（不要复现"打不中"），
    以及**不该中的时候不中**（站在刀后或刀扫过之后不该掉血）。
    开关：`-Dmaledict.meleeTrace=true` 会逐 tick 打印 Boss 位置、刀身世界坐标、
    到目标的体积距离与命中与否，用来一次定位是站位、高度还是容差的问题。
    参数位置见 [02 第 4 节与第 7 节](02_ANIMATION_AND_HITBOX.md)。
12. **刀身长度与渲染武器是否吻合**：`BLADE_LENGTH_MODEL_UNITS`（当前 44 单位 ≈ 2.75 格）
    是按「命中帧刀尖够到设计上限 5 格」反推的，**没有和实际渲染出来的镰刀刃长核对过**。
    进游戏第一件事就是站在 4–5 格看刀刃落点：刀尖明显超出刀刃就调小，明显够不到就调大；
    改完重跑 `tools/rig-probe/` 并更新 02 第 7 节。
13. 测试层面：`SpiritVoidCooldownCapability`（100 tick 自减计数器）、
    `AgeOfEnlightenmentEvents#onDarknessBearerHurt` 都没有单测；
    Malum 是否真的加载 `spirit_data`（目录、主键、命名空间）只能进游戏验证；
    `DamageAdaptation` 的配置读取、清账时机与减伤档位在实体层，只能进游戏验证。
14. **`rig/VicissitudeRigTest` 的连续性断言是静态采样，不是动画播放**：
    它证明姿态在数学上连续，不证明在实际帧率与插值下看起来连续。

---

## 6. 来源与约定

- 原工作包 `fate_boss_agent_md_pack.zip`，SHA-256
  `0800AB1AB214132F380B9E9F272197278B6E0DC1197155D6CFCD8007F47500E4`。
  原稿保存在 [`source/`](source/README.md) 用于溯源，**不再作为独立执行清单**。
- 逐轮历史记录（含被取代的数值）在 [`archive/`](archive/)，只用于追溯某一轮改了什么。
- 用户提供的原型参考图在 [`references/`](references/README.md)，
  原作不是 Minecraft 模型，只作设计参考，不作为游戏纹理或宣传素材导出。
- 工作约定：冲突以确认记录为准，不得把「建议」或未回复的问题标成已确认；
  影响技能、伤害、时长、碰撞或主题的改动必须同时更新 [01](01_SPEC.md)；
  `src/generated` 只能由 `runData` 更新，禁止手改；
  中文 codex 正文先写，英文从其含义派生，每 13 个可见字符插入一个字面空格。
- 可复现的离线工具：`tools/rig-probe/`（骨架实测）、`art/first-vicissitude/tools/`
  （造型、导出、检查）。
