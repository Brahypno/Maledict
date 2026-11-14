# 扬弃逐光（Aufheben Lightward）六件套

## 组成与计数

| 套件位 | 物品 |
| --- | --- |
| 护甲 ×4 | 扬弃逐光头盔 / 胸甲 / 护腿 / 靴子 |
| 剑位 | 神侵恶刃 |
| 斧位 | 熔坠之斧、恋人之斧（同属一个斧位，两只手各拿一把斧仍只算一件） |

- 只有真正穿在四个护甲栏、或拿在主手 / 副手的套件才计数；背包、箱子、地上不算。
- 4 护甲 + 剑 + 斧 = **6/6**。
- 等级就是当前计数的件数；2、3、6 级的效果尚未设计，**不写效果、不显示效果行**。

## 各等级效果

| 等级 | 效果 |
| --- | --- |
| 4 | 所有已穿戴 / 手持的套装装备上**已有**的附魔按真实等级 **+3** 参与计算 |
| 5 | 主手上的套装武器解锁「补满」：轻档探针从 `mediumDamageMethod(..., false)` 改为 `(..., true)` |
| 6 | 未定，不显示 |

「已有」是硬条件：真实等级为 0 的附魔不会被 +3 凭空生成；真实 NBT 等级也从不改写。
有效等级允许超过附魔自身的 `getMaxLevel()`。

## 实现口径

### 有效等级

`LightwardArmorItem`、`IncursusBladeItem`、`LoverAxeItem` 覆写 Forge 的两个入口：

- `getEnchantmentLevel(ItemStack, Enchantment)`
- `getAllEnchantments(ItemStack)`

两者都读 `EnchantmentHelper.getTagEnchantmentLevel` / `deserializeEnchantments` 的真实 NBT，
再在套装条件成立时统一 +3，保证口径一致、不会重复累加。读取路径之外的附魔逻辑
（直接读 NBT、自己缓存等级等）不在增强范围内。

这两个入口没有持有者参数，因此由 `LightwardSet` 在查询时按 `ItemStack` 实例反查主人：
服务端扫描在线玩家，客户端扫描 `LocalPlayer`。不写 NBT、不维护会过期的运行时标记，
物品离开槽位的同一刻自然失效。

### 提示文字

提示文字全部写在物品自己的 `appendHoverText` 里，不使用 tooltip 事件：

- 四件护甲默认只在名字下面挂一行 `套装效果（x/6）`。
- 按住 Shift 才展开：扬弃逐光套装组成（护甲四件 + 两把武器）、4 / 5 级效果。
- 效果只用颜色区分：亮蓝紫 `#9B7BFF` 表示已激活，暗灰表示未激活；不写“已激活 / 未激活”文字。
- 不额外加“按 Shift 查看”的提示行。
- 武器不显示套装文字。
- 套装物品用 `getDefaultTooltipHideFlags` 藏掉原版附魔行，再由 `appendHoverText` 按 NBT 顺序补回；
  生效时显示 `锋利 IV（I+III）`（英文 `Sharpness IV (I+III)`），未生效时保持原版行。
- 原版 `enchantment.level.N` 只翻到 10，有效等级更高时由本模组自己拼罗马数字。

### 5 级补满

`IncursusBladeAttack` 在挥砍开始时算一次 `LightwardSet.unlocksWeaponTopUp`，
把 `compensateDamage` 传给主伤害、魔法补打和细雪通道。只有主手近战挥砍走这条；
投掷、飞升等固定档位的技能伤害不因此改变。恋人斧的 `LoverAxeItem.attack` 也用同一个开关：
主手且件数 ≥5 时补满，否则退回 `mediumDamageMethod(..., false)`（不补满）。

## 暂缺与待定

- 护甲材质数值暂按下界合金一档。四件护甲均已接入 16×16 物品图标与穿戴模型。
- 没有合成 / 获取配方，暂时只从创造模式物品栏取得。
- 2、3、6 级效果与套装获取方式未定。

## 穿戴模型

- 头盔采用「未戴冠者」的独立模型。银灰额带承托三道向后、向内收止的冠脊。冠脊与低矮的后托之间是下沉的铅灰顶面，两侧银白折边勾出受冠的空位。
- 面甲沿中轴隆起，窄眼槽下方由护颊向内收束。额带、护颊与外壳以冷银白为底，暗铅色压在冠位、眼槽和甲片接缝里。额带和太阳穴折边带有折辉铅铋共晶的青金折光，深紫只绘在凹入的眼下泪槽正面。各片随头部转动。
- 胸甲使用恶念要塞的原始穿戴模型，在倾斜胸甲表面绘制金色中心。肩甲下缘与护臂横边以一像素宽的渐变金色表现鎏金。
- 头盔、护腿和靴子的穿戴贴图与 16×16 图标采用胸甲的冷白银与深黑配色。头盔两侧和大腿甲以成对黑色嵌片分区，靴面用对称黑带连接侧面与后跟。
- 护腿沿用魂染钢的腰片和倾斜大腿甲。腰片中央为紫青折辉，两侧和大腿甲上角点缀渐变鎏金，银灰折边向下转为深铅灰。穿戴图集保留原 UV 与透明度，甲片跟随腿部姿态。
- 靴子沿用魂染钢的厚鞋底与斜置外侧甲片。靴口和外侧甲片带一线鎏金，鞋面以紫、蓝、青折辉衔接冷银灰折面，深铅灰鞋底压住重心。
- 头盔的 `assets/maledict/models/armor/aufheben_lightward_helmet.geometry.json` 同时供游戏烘焙和离线导出。其余部位在运行时委托 Malum 的 `createBodyLayer`，离线导出使用 `tools/lightward-model/reference_geometry.json` 中的 Malum 6646111 几何记录。`import_reference.py` 可从本地反编译源码更新该记录。
- 运行时为 `client/equipment/lightward/LightwardArmorModel.java`；模型注册与烘焙集中在 `MaledictClientSetup`，物品通过 Forge `IClientItemExtensions` 接入，姿态直接跟随原版护甲渲染器。
- `art/aufheben-lightward/model/` 包含组合工程与四件护甲各自的 `.bbmodel`，均内嵌贴图，另有 `.blend`、三视角预览及原版同机位对照。纹样修改应同步到像素处理源，重建时会更新这些导出文件。
- 重建：`& 'C:\Program Files\Blender Foundation\Blender 5.2\blender.exe' --background --factory-startup --python tools/lightward-model/build_reference_models.py`。
