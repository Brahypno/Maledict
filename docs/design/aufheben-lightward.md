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

- 护甲材质数值暂按下界合金一档；贴图只有占位图与透明的护甲层，正式美术待补。
- 没有合成 / 获取配方，暂时只从创造模式物品栏取得。
- 2、3、6 级效果与套装获取方式未定。
