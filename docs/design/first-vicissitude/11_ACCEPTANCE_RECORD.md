# 11 — M1–M7 实现与验收记录

记录日期：实现轮次（D6 定为「自动返还＋凭证兜底」之后）。
本页只记录实际做了什么、实际跑了什么命令，以及**没有**验证的部分。没有运行过的游戏场景一律
记「未验证」，不以代码存在替代游戏内结论。

## 结论摘要

2026-09-13 Blender 重做补充：用户否定旧方块模型精度后，已交付 `.blend`、free mesh `.bbmodel`、
256×256 base/emissive 及游戏 mesh JSON。反馈修订后为 380 个网格、23,244 个三角面，保留 65 个层级关节/锚点；
胸腔背板改为两侧残片，断环为四段不等长真弧线，双翼改为正面展开的分层骨羽，头部为遮住内核的分离冠壳。
渲染接回原版 ModelPart 姿态及持物/特效父链；服务端翼部包围盒从 Blender 网格导出。
`compileJava --offline` 和 1,483,776 次翼顶点/动作/阶段/朝向/收翼组合检查通过。
本次反馈修订：断环改为环面内旋转；翼部增加三股骨架及前中后三层羽片；加厚躯干与自然连接；
分指改为块状手；胸环改为破碎骨质残弧、错位黑紫底片和断续星轨/裂光。环面旋转检查通过。
后续澄清：头部采用末地水晶式内核、多面裂壳与断菱形框，保留尖锐星芒，不采用完整立方体头。
胸肩、侧壁、腹部补成有棱面的实体，缩小中央空洞；手掌减小并以收窄前臂/腕环衔接。
修复贴图重复线性化引起的灰暗输出，PNG 与 Blender 预览共用相同 sRGB 资产。
两阶段四视角、灰模三视图、头胸细节、死亡露核和夜间 Blender 预览见 `art/first-vicissitude/preview/blender`。
这些是离线检查，游戏实机灯光、三类武器握持、资源重载和帧率仍待客户端验收。
以下原 M2/M3 数量和早期 cube 工具描述仅记录旧版，不代表当前美术交付；复现步骤以美术目录 README 为准。

| 里程碑 | 状态 | 说明 |
| --- | --- | --- |
| M1 状态与接口 | 代码完成待验证 | stage/action/序列/起始时间/持械状态全部走 SynchedEntityData；难度时长集中在 `BossDifficulty` 表；免没收 tag 与账本已接入 |
| M2 灰模及双姿态 | 代码完成待验证 | Java 模型 52 个 cube、55 个关节（含零尺寸锚点）；分段受击、翼阻挡与脱困瞬移已实现；离线预览见 `art/first-vicissitude/preview` |
| M3 正式材质及持物 | 代码完成待验证 | 256×256 base/emissive 贴图由离线工具从同一份 rig 数据生成；右手挂点渲染真实武器；bbmodel 已交付 |
| M4 动画与攻击接入 | 代码完成待验证 | 一阶段扇射/追踪球/羽片齐射/胸标记/环裁定，二阶段横斩/竖劈/重击/冲刺/投掷/远程兜底全部落地；释放帧单次判定 |
| M5 投掷闭环 | 代码完成待验证 | Boss 专用弹体持有 ItemStack、token 唯一、保存加载、死亡取消并不掉落 |
| M6 效果与死亡 | 代码完成待验证 | Lodestone 粒子/地面预警/斩击、位置震动包、80 tick 死亡序列 |
| M7 集成验收 | 部分完成 | `compileJava`、`test`、`runData` 已执行；游戏内场景未验证 |

## 实际改动文件

新增：

- `rig/VicissitudeRigData.java`、`rig/VicissitudeRig.java`：骨架、cube、UV 排布、姿态与正向运动学（不含任何 Minecraft 类型）。
- `common/entity/VicissitudeBossStage.java`、`common/entity/VicissitudeSpiritBoltEntity.java`、`common/entity/VicissitudeScytheProjectileEntity.java`。
- `common/curio/VicissitudeCurioLedger.java`、`common/curio/VicissitudeCurioReturns.java`、`common/item/CurioReturnTokenItem.java`、`common/MaledictTags.java`。
- `client/model/FirstVicissitudeBossModel.java`、`client/vfx/FirstVicissitudeEffects.java`、`client/vfx/FirstVicissitudeClientEvents.java`、`client/VicissitudeSpiritBoltRenderer.java`、`client/VicissitudeScytheRenderer.java`。
- `network/VicissitudeEffectPacket.java`。
- `art/first-vicissitude/tools/RigArtGenerator.java`、`art/first-vicissitude/first_vicissitude.bbmodel`、`art/first-vicissitude/preview/*.png`。
- 资源：`assets/maledict/textures/entity/first_vicissitude.png`、`_emissive.png`、`assets/maledict/textures/item/curio_return_token.png`。

修改：

- `common/entity/FirstVicissitudeBossEntity.java`（重写：阶段机、全部技能、分段倍率、翼碰撞、脱困、投掷、死亡）。
- `common/entity/VicissitudeBossEntity.java`（有限扩展：`modifyIncomingDamage`、`getDeathDurationTicks`、`onDeathTick`、`onFinalDeath`；真生命与一次性掉落事务不变）。
- `common/entity/VicissitudeLightOrbEntity.java`（`isOwnedBy`，供追踪球上限统计）。
- `client/FirstVicissitudeBossRenderer.java`（真实模型 + emissive + 持物 + 特效层 + 放宽视锥）。
- `client/MaledictEntityRenderers.java`（layers 与新实体渲染器）、`registry/MaledictEntities.java`（命中箱 1.6×4.375、两个新弹体）、`registry/MaledictItems.java`、`registry/MaledictCreativeTabs.java`、`config/MaledictConfig.java`（客户端震动倍率）、`data/MaledictItemTags.java`、`data/MaledictItemModels.java`、`data/MaledictLanguage.java`、`network/MaledictNetwork.java`、`Maledict.java`。

## 与规格条目的对应

- 状态契约（03）：`DORMANT → PHASE_ONE → TRANSITION → PHASE_TWO → DYING`；死亡可打断任意状态；转场 60 tick、0–14 悬停、15–29 收翼错位、30–44 羽片下压、45 装备、45–59 混合、60 开放 AI。
- 难度时长（03/D9）：SIMPLE/DIFFICULT/COMPLETE/EXTREME = 3600/2800/2000/1500；首次有效攻击锁定；旧档按 `PhaseOneTicks/6000` 比例换算到新时长；`PhaseTwoStarted=true` 映射 PHASE_TWO。
- 攻击规格（07）：扇射 5 球 -24/-12/0/12/24°、0.45 格/tick、寿命 80；每第三个基础槽用追踪球替换中心球；羽片齐射 3 波×8 球、间隔 8、中心留缺口；胸标记半径 2、第 10 tick 锁定、1D；环裁定内 3 外 6、60° 缺口、1D；横斩 100°/5 格/1D；竖劈 5×1.5/1.25D；重击 120°/1.75D；冲刺最多 10 格、遇实心停止；投掷 6–24 格、去程 1D、回程无伤害；二阶段远程 3 球 0.75D、寿命 120；>64 格不发射。
- 弹体上限（07）：非追踪球≤48、追踪球≤2，超限跳过不补发。
- 部位倍率（08）：躯干/环 1.0、头外壳 1.25、翼根 0.75、外翼 0.5；范围攻击取覆盖部位最高值不累加；同一次攻击同一目标只转发一次（`roundDamagedTargets`）。
- 翼物理（08）：服务端按姿态计算分段体积，扫掠范围撞到实心方块即停止受阻运动并收翼；玩家被扫到有限推离，被墙夹住时改为限制翼运动。
- 脱困（08/D8）：有目标时 40 tick 位移 < 0.25 格且存在阻挡才触发；水平半径 4/8/12/16、垂直 ±8、最多 64 个候选、只查已加载区块；成功冷却 100 tick；不删除实体、不塞进墙里。不做任何领地模组特判。
- 投掷闭环（04）：直线去程→追踪返回；位移/寿命上限 20 tick、16 格、100 tick；回程无伤害；距离手部 <1 格接住；Boss 死亡销毁专用副本且不掉落；飞行中改难度延迟到回收后换装。
- 饰品（09/D6）：`maledict:vicissitude_confiscation_immune` 物品标签；没收前检查并跳过；世界级 `maledict_vicissitude_curios` 账本持有「尚未交付」的条目；登录/重生/克隆/死亡与每 20 tick 的在线队列定向尝试；原槽→任意合法空 Curio 槽→背包；**交付失败不删除、不掉地**，并发放 `maledict:curio_return_token` 凭证兜底，全部交付才消耗凭证。
- 震动（08）：只在三个一次性事件上触发，全部走 VicissitudeEffectPacket 并由客户端按
  key 去重：一阶段转场第 1 tick（16 tick / 0.25）、重击命中（8 tick / 0.15）、
  死亡 36–59「翼根失效」（12 tick / 0.18）。全强度距离 8 格、最大 24 格，客户端倍率
  irstVicissitude.screenshakeIntensity 0–1（0 关闭）。死亡第 60 tick 的核心熄灭只做弱光收束，
  不再补一次震动；不随每个球的命中震动。
- 特效（10）：翼尖蓄光、释放碎芒、地面预警分段几何（含 60° 缺口）、斩击 builder（横斩用 `setSlashAngle`、竖劈用 `setVerticalSlashAngle`）、死亡熄灭粒子、位置震动（重击 8/0.15、转场 16/0.25、死亡 12/0.18，全强度 8 格、最大 24 格，客户端 0–1 倍率）。
- 事件契约（08/10）：一次性表现走 `VicissitudeEffectPacket`（UUID→实体 id、事件类型、actionSequence、位置），客户端按 key 去重，晚加入不补播。

## 命令与结果

| 命令 | 结果 |
| --- | --- |
| `.\gradlew compileJava --offline` | BUILD SUCCESSFUL |
| `.\gradlew test --offline` | BUILD SUCCESSFUL；`VicissitudeVitalityTest` 5 项、0 失败 0 错误（既有测试，本次未新增） |
| `.\gradlew runData --offline` | BUILD SUCCESSFUL；新增 data/maledict/tags/items/vicissitude_confiscation_immune.json（空标签）、models/item/curio_return_token.json，并更新 zh_cn/en_us |
| `javac art/first-vicissitude/tools/RigArtGenerator.java` + `java RigArtGenerator` | 生成 base/emissive 贴图、token 图标、bbmodel、6 张预览图与一份尺寸报告 |

## 用户反馈修正（实现轮次后）

1. **翼碰撞体积量纲错误**：segmentVolumes 的翼段半径按「模型单位」传入了「格」的参数，
   导致每侧翼的判定盒向四周膨胀 5 格（约 10 格厚），玩家隔着很远就被判定为与翼重叠。
   现已改为模型单位并在内部换算；同时把每侧从 2 段细化为 3 段（翼根 / 外翼骨 / 羽片扇面），
   实测包围盒贴合网格：一阶段翼段 y 2.44–4.34、|x| ≤ 3.36、厚度 0.5 格，二阶段 y 2.48–3.51、
   |x| ≤ 3.68，均与模型 cube 位置一致（见 uild/rig-tool/DumpVolumes 输出）。
2. **推挤手感**：改为只在玩家包围盒与真实翼段相交时生效，且只施加很短的推力
   （Player#push，水平 0.06），不再服务端瞬移玩家、不再每 tick 强制 hurtMarked
   重同步。玩家贴墙时改为让翼收拢，不把玩家挤进方块。

3. **持械位置错误**：WeaponLayer 只对挂点自身调用了一次 ModelPart#translateAndRotate，
   而该方法只应用该部件的**局部**变换，不含父链，于是武器被画在模型原点（身体中间）。
   现有 FirstVicissitudeBossModel#poseStackTo 从肩→上臂→前臂→手→握点依次应用整条父链，
   并把模型的 y 下 / z 后坐标系用 180° x 旋转换算到物品渲染的 y 上坐标系（不用负缩放，
   避免物品面片翻面）。握点实测位置：一阶段 (-1.24, 1.07, 0) 格、二阶段 (-0.98, 0.99, 0.27) 格
   （相对实体原点，x 为实体左侧），即垂下的右手处。武器显示尺寸按三件武器分别校准
   （镰刀 1.6、剑 1.15），并加外向 28° 倾斜以免横在躯干前。

4. **朝向抖动**：原版 LookControl + 寻路会与直控位移争夺 yaw，30–40°/tick 的转向速率让 180° 转身
   在 4–5 tick 内完成，看起来就是"突兀切换朝向 / 背对玩家飞行"。现在由实体自己接管朝向：
   	ickFacing() 在 super.tick() 之后以 12°/tick（出招时 5°/tick）限速转向当前目标，并同时写入
   yRot / yHeadRot / yBodyRot，模型不再有头部滞后。目标为空时保持当前朝向不空转。
5. **持械角度**：按反馈在屏幕平面内逆时针补 60°（Axis.ZP.rotationDegrees(-28 → 32)），倾斜与位置未动。
6. **动作力量感与连续性**：所有攻击改用新的 strike() 曲线——蓄力缓入到释放帧正好 1.0，随后 2 tick
   过冲 1.28、4 tick 回落保持、再跟随收势；同时整体加大躯干扭转与手臂摆幅（横斩躯干 62°、
   竖劈手臂 172°、重击手臂 208°）。客户端新增姿态混合：每帧向目标姿态插值，出招窗口 0.9、
   其余 0.45、静止/死亡 0.3，动作切换与收招不再瞬跳；特效锚点统一读混合后的姿态。

7. **武器朝向（多轮迭代后定基线）**：最终回到原版基准，并把微调做成可调配置。
   已核对：item/handheld 的 	hirdperson_righthand 为 rotation [0,-90,55]，
   ItemTransform 用 Quaternionf.rotationXYZ（即 R = Rx·Ry·Rz）合成，刀身方向在该渲染帧里为
   (0, 0.574, -0.819)；原版 ItemInHandLayer 再补 Rx(-90)·Ry(180)（其 translate 0.625 正好是
   肩到手肘到手心的距离，因为原版锚点在**肩**，而本模型锚点已在手，故不重复该位移），
   得到手臂坐标系里的刀身 = (0, 0.819, -0.574)，即**沿手臂向下并前倾约 35°**，与原版生物持剑一致。
   用户实测确认**这套原版基准即为正确姿态**，因此没有保留任何朝向配置项：曾短暂加入的
   weaponPitchDegrees / weaponTiltDegrees / weaponSpinDegrees 三个客户端旋钮已删除，
   FirstVicissitudeBossRenderer 现在只应用上述原版基准（外加按武器校准的显示尺寸）。
   客户端配置里只剩 irstVicissitude.screenshakeIntensity。
：确认上一轮方向反了，屏幕平面内改为 Axis.ZP.rotationDegrees(-88)（即从 -28 再向另一侧 60°）。
8. **复活不清仇恨 / 主动攻击距离过大**：新增每 10 tick 的 orgetDeadParticipants()——以「死亡次数 =
   一条命」为身份，参战者在死亡、换维度或死亡次数变化时从 phaseOneTargets / phaseTwoPlayers
   移除并清空当前目标，符合原版死亡宽恕；离线玩家保留记录，以免影响饰品账本。
   宽恕行为对齐原版 NeutralMob#playerDied：只有 forgiveDeadPlayers 为 true 时才遗忘
   （判据同样按 UUID），为 false 时像原版愤怒生物一样继续追击复活后的同一玩家（仅把记录里的
   死亡次数刷新到当前这条命）；换维度一律结束交战；死亡实体在任何设置下都不作为活动目标。
   没有实现 universalAnger：那会让 Boss 攻击未参战的旁观者，与 07 的「只攻击本战斗有效参与者」冲突。
   同时加入可配置交战半径 firstVicissitude.engagementRange（默认 12 格，4–64）：超出半径不再选取
   或保留目标、不再跨场接近，仅回到悬停位；一阶段环绕半径由 12 收到 9 格、FOLLOW_RANGE 128 → 32。

9. **一阶段射弹打向远方**：扇射/齐射/远程兜底的弹道原来只取水平朝向（directionFromYaw），
   从 3–4 格高的翼尖平飞出去，正好从站地球的头顶掠过，看起来就是"射向远方"。
   现在每发都从各自翼锚点三维瞄准目标躯干中心（imPoint + imedDirection），再叠加
   原有的水平扩散角（±12/±24°、羽片齐射随距离加大、远程 ±10°），所以既有落在目标身上的，
   也有打在附近地面的，扩散仍可侧移躲开。
10. **一阶段时长减半**：按用户要求 1800/1400/1000/750（90/70/50/37.5 秒），03、06、README 同步。
    已锁定 PhaseOneDuration 的存档不受影响（设计上改配置不改变进行中的战斗）。
11. **定场词不可见 → 动作栏 + 定制 Component**：一阶段首击台词在重写实体时丢了调用
    （只剩 lang key），已补回 nnounceFirstAttack（DORMANT → PHASE_ONE 那一次，
    按死亡次数做到每人每命一次）。两句都走原版动作栏 displayClientMessage(..., true)，
    内容用 nnouncement(key) 包一层 Style：冷白 #E6EDF5 + 加粗，时长沿用原版动作栏
    （60 tick 后淡出），不需要自定义计时。
    中途曾实现过一个自定义 GUI overlay（自带淡入淡出与持续时间），按"用定制 Component 就够"
    的意见已全部撤回：VicissitudeBannerOverlay、MaledictClientTicks 两个类删除，
    VicissitudeEffectPacket 也回到原来 4 个字段、不带文案负载。
    注意 Style 能改颜色/加粗/斜体甚至字体，但**改不了字号**——动作栏只有一档固定字号；
    若以后确实要更大字号，只能走 overlay 或 mixin，届时再加。

## 用户反馈修正（第二轮）

12. **死亡侧倒**：这是原版 LivingEntityRenderer#setupRotations 的死亡翻滚（deathTime 前 20 tick
    绕 Z 轴转 90°），对 80 tick 的自定义死亡序列不合适。渲染器已覆写 setupRotations 为空实现，
    只保留模型自己的塌陷动画；该实体不会游泳也不会自动旋转攻击，没有其它副作用。

## 用户反馈修正（第三轮）

13. **一阶段节奏加快**：基础攻击槽 40 → 30 tick，羽片齐射/胸标记/环裁定冷却 160/200/240 →
    110/130/160。没有继续压缩的原因是同屏压血球上限是 48（07 规定），弹体寿命 80 tick：
    30 tick 一次 5 球时同屏约 13 球，留出齐射爆发的余量；再快就会频繁触发上限而"跳波"。
14. **看向发射方向**：蓄力阶段（release 之前）转向速率从 5°/tick 提到 28°/tick，让身体在释放前
    对准射击线；每波出手的瞬间再调用 aceTowards(aim) 精确对齐 yRot / yHeadRot / yBodyRot，
    所以弹道和朝向现在出自同一个点。释放后回到 5°/tick，避免收招时甩头。
15. **压血逻辑加固 + 视觉区分**：逐行核对后，一阶段的扇射与羽片齐射确实传的是
    pressHealth = true（只有二阶段的远程兜底是普通伤害，胸/环地面技能按 07 本来就是 1D 普通伤害）。
    为避免任何同步/存档因素让压血失效，弹体现在按"释放标记 **或** 拥有者当前仍处于一阶段"判定，
    一阶段出手的球不可能变成普通伤害。同时把两类球的样子分开：压血球是大的慢脉冲双层光核，
    普通伤害球是小的快闪单核（07 要求"不能只靠颜色区分"）。

16. **胸标记与环裁定看不见（真 bug）**：预警几何画在了错误的坐标系里。渲染层拿到 pose stack 时
    已经处于**实体变换**中（Ry(180 - yaw) * scale(-1,-1,1) * translate(0,-1.501,0)，见
    LivingEntityRenderer.render 的调用顺序），而我直接用它当世界坐标平移，结果落点变成
    实体位置 + 旋转后的绝对坐标，跑到几十格开外（多数情况在地下或空中）。现在先把世界点换算进
    该坐标系（	oLayerSpace，含 y 取反与 +1.501）再绘制，并用 uild/rig-tool/DumpMarker
    做了往返验证：0/37/90/-143 度四种朝向、三个采样点误差均为 0。同时修正了缺口角度：
    绘制用世界角、判定也用世界角（tan2(dz, dx)），修好之前两者还差一个实体朝向的旋转。
    可读性同时加强：外缘加了一圈更亮的骨白描边环、整体抬高 0.03 格避免与地面 z-fighting、
    alpha 提到 0.5–0.9。

17. **地面技能判定与显示对齐（补充）**：候选实体是用方形包围盒筛的，判定时没有再做径向检查，
    于是站在"画出来的圈外一点点"（方盒对角最多 2.83 / 8.5 格）也会被打到。现在胸标记按
    dist ≤ 半径、环裁定按 内半径 ≤ dist ≤ 外半径 判定，和画出来的几何完全一致
    （07 要求"视觉范围使用与判定相同的中心/半径"）。

## 用户反馈修正（第四轮）

18. **一阶段特殊技能更常见**：羽片齐射/胸标记/环裁定冷却 110/130/160 → **70/90/80**；
    选择方式由"能放就放特殊"改成**基础槽交替**——偶数槽才尝试特殊技能，奇数槽留给扇射，
    这样三个特殊大约每 6 个槽（约 9 秒）各出现一次，同时保证 07 要求的"一阶段以翼/羽球为主要攻击来源"。
    简单模式一阶段 90 秒内每个特殊技能大致能看到 4–6 次。
19. **二阶段复用三个远程技能**：	ryRangedSpecialSkill 现在也从中距离（6–24 格）与远距离（>24 格）
    分支调用（优先级：投掷 > 特殊 > 冲刺 > 远程兜底）。同一套动作在二阶段**不具备压血能力**：
    spawnBolt 依据拥有者当前阶段决定效果，二阶段一律走普通伤害（每球 0.75D，且同一动作对同一目标
    只结算一次）；胸标记与环裁定本来就是 1D 普通伤害，无需改动。
20. **一阶段重新有追踪压血球**：追踪球改为**每两个扇射槽**一枚（原为每三个），仍是压血到 1、
    仍在二阶段完全不出（二阶段球只做普通伤害，与 07 一致）。

## 用户反馈修正（第五轮）

21. **一阶段行动漫无目的 / 施法不朝向玩家**：
    - 悬停高度 5 → **2.5** 格（原来停在玩家视线上方 5 格，显得高高在上且飘）、环绕半径 9 → **7** 格；
    - 环绕角速度 0.08 → **0.03** rad/tick（绕一圈从约 4 秒放慢到约 12 秒），从"乱飘"变成有意图的走位；
    - **施法期间定点**：动作开始到 release+4 tick 之间把水平速度衰减到 0.5 倍并停住，
      所以蓄力—释放是被看到"停下来对准你"，而不是边飞边打；
    - 朝向仍由 	ickFacing 接管（蓄力 28°/tick、释放瞬间精确对齐、收招 5°/tick）。
22. **地面预警（胸标记 / 环裁定）彻底看不见**：不再画在实体渲染层里。实体层要经过
    Ry(180-yaw)·scale(-1,-1,1)·translate(0,-1.501,0)，而且**实体被视锥裁掉时整层都不画**
    —— 预警在玩家脚下、Boss 在屏幕外时正好会丢。现在改为世界坐标的独立渲染 pass：
    RenderLevelStageEvent#AFTER_TRANSLUCENT_BLOCKS 里用相机相对坐标绘制，候选列表每客户端
    tick 维护一次（96 格内、激活中的 Boss）。角度仍用世界角（与判定同源），坐标不再经过任何
    实体变换，因此也不依赖朝向。相机相对坐标已在 uild/rig-tool/DumpMarker 验证过变换公式。

## 用户反馈修正（第六轮）

23. **二阶段不转身 / 背对玩家后撤**：转向速率跟不上。二阶段站位距离 2.25 格，玩家以正常
    行走速度绕圈时，相对转角速度约 **5.5°/tick**，而原来动作期间只给 **5°/tick** ——
    刚好差一点点，于是身体永远追不上，表现为"怎么绕它都不转、打同一个方向"。
    现在 跟踪 12 → **20°/tick**、动作期间 5 → **14°/tick**、蓄力 28 → **30°/tick**；
    另外当目标偏角超过 **90°** 时额外乘 **1.8 倍** 追赶，保证没有任何绕圈速度能超过转身速度。
    这套数值的依据直接来自 v/r 估算，不是凭感觉调的。
24. **确认地面预警已在渲染**（用户报告"偶尔能看到白光"）：世界坐标系渲染生效，白光应当是
    胸标记（半径 2 实心圆盘）或环裁定（内 3 外 6 断环）的冷白加色几何。

## 用户反馈修正（第七轮）

25. **二阶段完全不转身（站定观察）**：	ickFacing 只在 getTarget() 有效时工作，而目标来自
    参战名单；一旦玩家因为阵亡被 orgiveDeadPlayers 宽恕剔除（默认 true，而一阶段压血 + 地面
    技能很容易致死），二阶段就"没有目标"——身体不转、也不做近战。现在补齐两点：
    - 没有战斗目标时，仍会转向**交战半径内最近的玩家**（纯表现，不把对方变成目标，
      因此不影响死亡宽恕）；
    - ireBarrageWave 不再接受空目标：没有活目标就丢弃该波，避免"朝原方向空放"的观感。
    另外若确实是被宽恕剔除，重新打它一次即可重新参战（这是宽恕规则本身的效果）。

## 用户反馈修正（第八轮）—— 模型朝向的根因

26. **模型根本不跟随实体朝向（真 bug，由上一轮"去掉死亡侧倒"引入）**：原版把**身体 yaw** 放在
    LivingEntityRenderer#setupRotations 里（javap 实读：Axis.YP.rotationDegrees(180.0F - rotationYaw)
    在该方法开头，死亡翻滚紧随其后），而我在去掉死亡侧倒时把整个方法覆写成空实现 ——
    于是**yaw 一起被删掉了**：实体自己照常转身（弹道、命中、翼判定都正确，因为它们读的是
    getYRot()），但客户端模型永远朝着同一个方向渲染。
    这解释了此前一连串"观感"问题：二阶段绕到背后它"不转身"、一阶段"释放技能时不看玩家"、
    以及更早的"武器像是换到了另一只手臂"。
    现在改为**只保留 yaw、只跳过死亡翻滚**：
    poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - rotationYaw));
    另外 	ickFacing 的无目标回退（转向最近玩家）与"无目标不放齐射波"也一并保留 ——
    它们在目标缺失时仍然是对的，只是不再是这次症状的原因。

## 用户反馈修正（第九轮）

27. **未开战不追踪玩家**：	ickFacing 在 DORMANT（已召唤、尚未被首次有效攻击）与死亡状态直接
    返回，保持出生朝向，不再东张西望。开战后（PHASE_ONE / TRANSITION / PHASE_TWO）才转向。
28. **多目标时的视觉角度**：不再"盯着当前攻击目标"来回甩头。朝向改为**按距离加权的多目标合成方向**
    ——遍历参战名单里所有在同一交战半径内、存活且非旁观/创造的成员，取单位方向按
    1/(1+0.15d) 加权求和（近者主导但不忽略其他人），视线高度同样加权平均。这样两三个人从不同
    方向围上来时，身体会稳定地朝人群合力的方向，而不是每 10 tick 随目标轮换猛地掉头。
    攻击仍然在**释放帧**对齐到真正的攻击对象：扇射/齐射/远程在出手瞬间 aceTowards(aim)，
    近战（横斩/竖劈/重击）在结算前 aceTowards(swingTarget.position())，
    所以"看到它朝我挥刀"和"这一刀打的是我"始终一致。

## 用户反馈修正（第十轮）

用户更新了 Blender 模型（380 网格、23,244 三角面，关节名不变），随后提出四项修订：

29. **胸口的环与头后的环反向旋转**：胸环三段弧是三个各自带枢轴的关节（局部枢轴 (10,5)、( -10,5)、(0,9)），
    单关节自转只会让碎片原地打转。新增 `VicissitudeRig.spinChestRing`：以 TORSO 局部 (0,1)（即网格实测的环心，
    xy 半径约 5.2、深度由网格自身给出）为心，把三条弧的枢轴沿同一个圆一起推移并附加同角度自转，
    于是整环刚性旋转。idle 中光环 +0.6°/tick、胸环 **-0.6°/tick**（同速反向）。
    离线检查 `RigMeshCheck` 新增：三条弧到环心的距离在 0–240 tick 内恒定（刚性），
    且"胸环转角 − 光环转角 = 2·tick·0.6"在 NONE 与 CAST_FROM_CHEST 下都成立（互为反向同速）。
30. **渲染与实际手持物品分离**（应对"缴械"类能力）：
    - 新增同步字段 `DATA_WEAPON_TIER`；`FirstVicissitudeBossEntity#getDisplayWeapon` 在客户端按档位用
      `createWeaponForDifficulty` 重建 ItemStack（带缓存，只在档位变化时重建），渲染层不再读 `getMainHandItem()`；
    - 手上仍会维持同档武器：空手当 tick 立即补，类型不符每 20 tick 纠正一次，飞行镰刀（weaponState=2）不被触碰；
      `cancelFlyingScythe` 在保管副本丢失时也用代码副本补回；
    - **物品带来的属性写在实体的 attribute 里**：`applyWeaponAttributes()` 在装备二阶段武器时，从
      `createWeaponForDifficulty(...).getAttributeModifiers(MAINHAND)` **读出武器自身的属性表**，为每一条生成
      一个固定修饰符（自有 UUID）加到实体上——攻击伤害、攻击速度等一个不漏，代码里不出现任何手写数值；
      同时 `suppressHeldItemAttributes()` 每 tick 清掉手上物品的临时修饰符，所以同一份加成不会被算两次。
      `attackDamage()` 回到原来的 `getAttributeValue(ATTACK_DAMAGE)`，攻击方式与原版一致。
      （本例实际结果：基础 8 + 染魂钢镰刀 5 / 救赎之锋 9 / Incursus Blade 等级 3 → 3、等级 9 → 9，
      即 13 / 17 / 11 / 17；一阶段不持械仍是 8。这些数字来自武器定义，不在本仓库里写死。）
31. **对玩家的伤害分档**：`hurtParticipant` 是所有技能（近战、冲刺、胸/环判定、投掷弹体、二阶段球）唯一的伤害入口。
    玩家目标走 ChangeLib：SIMPLE/DIFFICULT 用 `DamageProbe.lighterDamageMethod`，COMPLETE/EXTREME 用
    `mediumDamageMethod`；非玩家仍走普通 `hurt`。每个技能仍保留"每轮每目标一次"与 `invulnerableTime=0`。
32. **血量与防御**：`BossDifficulty` 增加 `maxHealth`(500/750/1000/1500) 与 `damagePress`（伤害本身仍来自实体属性）；
    `applyDifficultyAttributes` 在该实体加入世界、真生命账本捕获上限之前写入 `MAX_HEALTH` 固定修饰符，
    入世后锁定（与阶段时长一致，中途改难度不改本场血量）；属性注册补 15 点护甲与显式击退抗性 1.0。
    直接从存档载入二阶段时，`onAddedToWorld` 也会补一次武器属性烘焙，避免第一 tick 掉回基础值。
33. **翅膀与手臂的次要动态**（第四轮"右臂攻击时左臂一动不动"的收尾）：
    - 所有攻击取释放曲线**延迟 4 tick** 的副本，得到 `trail = swing - swingLag`；
    - 另一只手做反向配重：横斩时左臂蓄力抬起、释放甩出；竖劈/重击时反向驱动；胸施法双手合拢再张开；
      环施法右手抬起、左手低位张开；投掷/冲刺/收招/远程兜底同样补齐；
    - 双翼按身体 yaw 或俯仰的落后量拖拽（左右**同号**，是整体滞后），并在释放帧加一次开合；
    - 下段残躯与布片按 `trail` 延迟跟随；
    - idle 增加：双臂各自独立慢漂移（1.9 rad 相位差）、翼根 160 tick 呼吸且左右不完全同步、
      二阶段左臂静止位更低（对应设计"左手独立"）。

### 本轮命令与结果

| 命令 | 结果 |
| --- | --- |
| `.\gradlew compileJava --offline` | BUILD SUCCESSFUL |
| `javac src/main/java/org/brahypno/maledict/rig/*.java art/first-vicissitude/tools/RigMeshCheck.java` + `java RigMeshCheck` | 三条 PASS：光环保持面内、胸环刚性且与光环反向同速、1,483,776 次翼顶点/动作/阶段/朝向/收翼组合检查（与上一轮同数，未回退） |

本轮同样**未在游戏内验证**：反向旋转的观感、四种难度下的血量/伤害手感、DamageProbe 在玩家身上的实际表现
（尤其 COMPLETE/EXTREME 的补足伤害是否会显得"护甲无效"）、缴械模组下的补装行为，都需要客户端实测。

## 用户反馈修正（第十一轮）

34. **跟随距离又偏短**：真正决定"跟多远"的不是 `FOLLOW_RANGE` 属性，而是 `firstVicissitude.engagementRange`
    ——第六轮按"主动攻击距离别那么大"把它设成 12 格，于是玩家一跑出 12 格，Boss 就直接放弃目标、原地悬停。
    这和 07 写的"超过 64 格仅接近不发射"自相矛盾：12 格时那条规则根本触发不到。现在：
    - `engagementRange` 默认 12 → **96** 格，可调范围 4–64 → **4–256**；
    - `Attributes.FOLLOW_RANGE` 32 → **96**，与本战斗实际使用的距离一致（该属性对本实体只作对外一致用，
      追击由 CombatGoal 自己走）；
    - 一阶段补上和二阶段同款的**超距不发射**门：距离 > 64 格时只接近不开火。一阶段弹体寿命只够飞约 36 格，
      从半场外开火只会生成打不到的空弹。
    死亡宽恕、旁观/创造忽略、换维度结束交战这些不受影响，所以第六轮"别跨场追复活玩家"的诉求仍然成立
    ——那条现在由 `forgiveDeadPlayers` 负责，而不是由 12 格的半径负责。
35. **羽翼乱翻（第十轮次要动态的实现错误）**：`dragWingsYaw/Pitch` 的实参已经是**角度**，
    但我当时又乘了一遍 16–24 的"每单位角度"系数，于是翼根在释放帧被甩到 **-254°**（横斩）、
    竖劈 **-252°**、重击 **-432°**、环施法 **-120°** —— 正是用户看到的"绕横轴和躯干纵轴乱翻、幅度大又快"。
    现在改为取身体转角的 **30%** 并封顶 **±10°**（`WING_DRAG_FRACTION` / `WING_DRAG_CAP`），
    翼根 / 外翼 / 羽片按 1.0 / 0.6 / 0.4 递减，峰值分别不超过 10° / 6° / 4°。
    新增离线检查 `RigMeshCheck#checkWingComposure`：扫 3060 组动作/阶段/收翼/时钟组合，
    逐关节逐轴比对"作者姿态包络 + 约 15%"的上限（实测峰值：翼根 10.1/61.5/56.4、外翼 3.7/49.0/18.2、
    下翼 0/46.0/0、羽片 2.6/4.0/0），任何再次乘错倍数都会当场断言失败。
36. **区块加载时崩溃：Modifier is already applied on this attribute!**（用户实测报错，
    栈顶 `applyWeaponAttributes` → `AttributeInstance.addPermanentModifier` → `AttributeMap.addModifier`）
    ——固定修饰符是**实体存档的一部分**（`LivingEntity` 会把 attributes 写进 NBT），
    从磁盘重载时属性表里已经有我写的那些 UUID，而我用来避免叠加的 `appliedWeaponAttributes`
    是运行期列表、重载后是空的，于是"移除旧的"什么都没移除，接着又加同一个 UUID，原版直接抛异常。
    现在把"先清除再写入"改成**按槽位 UUID 无条件清除**（`weaponModifierId(slot)`，
    `removeModifier` 对不存在的 id 是空操作），运行期列表只作为额外保险。
    同一类问题已连带复查：`applyDifficultyAttributes` 本来就是先 `removeModifier` 再 `add`，不受影响；
    全仓库只有这两处会写固定修饰符。

## 未验证项（明确不声称通过）

1. 客户端与专服双人场景、晚加入不串阶段、走出/走入跟踪范围。
2. 翼分段命中、翼阻挡玩家与地形、脱困瞬移全部场景（mobGriefing 开/关、可破坏/不可破坏方块、狭窄洞穴、无可用落点、未加载邻区、飞镰期间脱困）。
3. 存档：转场中、二阶段空手、弹体飞行中、死亡过程保存重载不重复装备/没收/命中/掉落。
4. 外观：模型与贴图在游戏内的实际观感、三武器持握尺寸、日夜与低粒子设置下的可读性；`art/first-vicissitude/preview` 只是离线正交投影预览，不是游戏截图。
5. Lodestone/Malum 具体效果的游戏内表现（粒子数量、震动强度单位、斩击方向是否与武器一致）。
6. Curio 返还的性能数据（1/8 名玩家、每人 8/32 件的耗时）与第三方装备回调成本。

## 备注

- `art/first-vicissitude/preview/phase_*` 之外没有游戏截图；设计文档要求的「灰模正/侧/背、两阶段对照」目前由离线渲染代替，需要在能启动客户端的环境补拍。
- Curio 返还的凭证分支按用户答复实现；若后续改为纯自动返还，删除 `CurioReturnTokenItem` 与 `issueToken/consumeTokens` 即可，账本与交付逻辑无需变动。
