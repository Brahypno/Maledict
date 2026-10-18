# 超越预兆 — Transcend Omen

属性 ID：`maledict:transcend_omen`，默认 0，范围 0–1，服务端计算、属性同步到客户端。
本次提供属性与转化机制，尚不绑定任何物品或效果。可用 `/attribute @s maledict:transcend_omen base set 0.5` 调试。

## 已确认的模型

令 M 为减少前的 Malum 恶念转化，R 为超越预兆。正向强度为 `max(M−R, 0)`，反向强度为 `max(R−M, 0)`。
`TranscendOmenUtil.setAmount(entity, sourceId, amount)` 设置一个来源的超越预兆临时加成，
同时减少实体实际的恶念转化属性，并返回未被抵消的反向余量。同一来源复用 ResourceLocation；传入 0 移除该来源。
超越预兆属性本身保留完整加成，不额外减少。移除时撤掉对应修饰，恢复原来的恶念转化；不修改属性底值。

handler 先检查实际恶念转化：不为零则清除、跳过反向；归零后也**只使用余量**，而非全部超越预兆。
例如 M=0.5、R=0.5 时，实际恶念虽已归零，反向余量仍为 0；R=0.75 时只有 0.25 用于反向。
正向由 Malum 原处理器按已经减少的恶念属性执行，未使用 `MalignantConversionHandlerMixin`，
也不在 Malum 的防御增益或来源惩罚之后添加相反修饰来补偿。

反向读取 Malum 已加载的 `malignant_conversion_data`，不维护第二份配方。
每条路线使用完整目标属性值，忽略正向的 `ignore_base_value` 和 `consumptionRatio`：

`base = min(各有效目标当前值 / 对应目标倍率)`

来源属性必须存在，且清除预兆旧增益、旧扣除及已失效的正向修饰后，当前值必须不同于属性的注册默认值。
只有默认值的来源整条路线不参与分流，也不触发目标扣除；装备撤下、属性回到默认值后同样退出。
判断比较 `Attribute#getDefaultValue()`，不是实体当前底值；命令或成长机制改变底值也可以激活路线。
倍率非正数或非有限值的目标被跳过；
1.21.1 已确认例外：Malignant Aegis Capacity、Malignant Aegis Recovery Rate、Malignant Aegis Recovery Gain
当前值为 0（或实体没有该属性）时，跳过该目标；既不限制底数，也不参与消耗。
这三项有正值时仍按原判据参与限制与消耗。其余目标必须全部存在，并产生正的有限候选值。
没有有效目标的路线不参与；不能通过仅有零值 Aegis 目标的路线凭空获得收益。
先采集所有路线，再应用任何修饰，保证来源同时也是另一条路线的目标时也只读取一次干净基线。

各有效路线等权，份额为 `max(1 / 有效路线数, minimumShare)`。
配置 `transcendOmen.minimumShare` 默认 0.01，可后续调整；允许保底使总份额超过 100%，
不再使用 `maximumFloorBudget` 限制总预算。
例如有效路线 4 条时各 25%，29 条时各约 3.448%，100 条时各 1%，101 条时仍各 1%（合计 101%）。
这表示路线份额至少 1%，实际增益仍须乘以路线底数与反向强度。

默认熟练度 1、默认护盾容量 0、护甲 20、第二目标 0.25、反向强度 50% 的确认序列：
两来源均默认 → 无路线、不扣防御；熟练度装备 +1 → 熟练度 2.5、护甲 10；
再装备护盾容量 +1 → 熟练度 2.25、容量 1.25、护甲仍 10；
撤下熟练度装备 → 熟练度恢复 1、容量 1.5；再撤下容量装备 → 容量恢复 0、护甲恢复 20。
预兆自身旧加成不能让已回到默认值的来源继续参与。

**用户确认采用扣除方案 2，覆盖原下载规格中的“不扣防御”条款：**

- 收益为 `base × 反向强度 × 路线份额`，以来源专属稳定 ResourceLocation 的临时 ADD_VALUE 修饰实现。
- 每个参与有效路线的目标属性统一扣除完整反向比例，以临时 ADD_MULTIPLIED_TOTAL 修饰实现。
- 同一目标即使参与多条路线也只扣一次；扣除不随短板或分流份额减小。
- 不参与任何有效路线的属性不扣除。

例如：一条魔法熟练度路线的护甲倍率为 10、第二防御属性倍率为 0.25，完整护甲 20、第二属性 0.25，
则底数为 1。只有一条路线、反向强度 50% 时，熟练度获得 +0.5，护甲留下 10，第二属性留下 0.125。
若还有第二条完全相同目标的有效路线，各路线获得 +0.25，防御仍留下 10 和 0.125。

当前 Lodestone 的 `magic_resistance` 默认值和下限均为 1，原下载规格的总魔抗 0.25 不可取到。
上述数值测试用允许该数值的击退抗性作为第二目标；用户已确认保留依赖属性的上下限。
目标超过上限时，收益和扣除都按封顶后的当前值计算，例如原始击退抗性 4、当前值 1，扣除 50% 后为 0.5。

固定 M=0.5，依次设置 R=0、0.25、0.5、0.75，结果应为正向 50%、正向 25%、无转化、反向 25%。
跨越零点时清除旧方向的全部修饰；撤掉超越预兆时恢复原有属性。

## 重算与验证

只在服务端修改属性。使用原生 `EntityTickEvent.Pre`：HIGHEST 优先级在 Malum 的 NORMAL 处理器之前
移除自身旧增益和扣除、调用 util 更新恶念减少量与反向余量，LOWEST 在 Malum 结算后计算反向。
后续装备或药效改变恶念转化时也会重算，不需要改写 Malum 的方法。
缓存检查来源、目标及两种转化属性的底值与每个修饰的 ResourceLocation、数额、操作类型，
因此即使 100% 扣除让目标当前值一直为零，装备变化仍能触发重算。
缓存命中保留原修饰；数据重载会清除已删除路线的增益和惩罚，包括 Malum 的旧正向修饰。
若装备等处理器在 HIGHEST 与 LOWEST 之间修改两种转化属性的底值或修饰，本 tick 不应用旧强度的反向结果，
并使缓存失效，下一 tick 重新抵消与结算。检查原始底值和修饰，因此恶念被完全抵消为零时也能发现变化。

`TranscendOmenTest` 在真实 NeoForge 服务端通过 EntityTickEvent.Pre 验证上述序列、100% 正向抑制与抵消、
默认来源排除与装备激活/撤下、共享目标扣除、保底超过总预算及配置重算、连续 tick、满额扣除下装备变化、
事件中途属性变化、数据表替换与清空、非法倍率、交叉路线及属性封顶。
保底测试使用两条真实属性路线、临时 60% 保底验证总份额 120%；不声称加载了 101 条整合包路线。
数据重载测试修改 Malum 的已加载规则表，不模拟资源包解析或客户端连接。

## Malum 1.8.2 原生路线

继续读取加载后的数据，不覆盖 Malum 的正向规则。新版以护甲与恶念庇护代替旧版部分魔抗/韧性路线：

| 反向收益来源 | 原生目标与倍率 |
| --- | --- |
| Magic Proficiency | Armor / 10，Malignant Aegis Capacity / 5 |
| Soul Ward Capacity | Armor / .5，Malignant Aegis Capacity / .25 |
| Soul Ward Integrity | Armor / 10，Malignant Aegis Capacity / 5 |
| Soul Ward Recovery Rate | Armor / 5，Malignant Aegis Capacity / 2.5，Malignant Aegis Recovery Rate / 1 |
| Soul Ward Recovery Gain | Armor / 5，Malignant Aegis Capacity / 2.5，Malignant Aegis Recovery Gain / 1 |

按上面的零值例外过滤后取最小值。正向转化本身会产生 Aegis，而超越预兆先抵消正向；
若零值 Aegis 仍是硬要求，普通“护甲换法术”会被整条阻断，因此仅对上述三项作零值豁免。
来源仍需有外部提升，且不同于注册默认值；默认来源不参与分配。

真实加载规则的回归场景选取前四条路线，Recovery Gain 留在默认值。起始护甲 20，
反向余量 1，各路线份额 1/4，Aegis Recovery Rate 为 1：

| 原 Aegis Capacity | Proficiency 增量 | Ward Capacity 增量 | Recovery Rate 增量 | Integrity 增量 |
| --- | --- | --- | --- | --- |
| 0 | .5 | 10 | .25 | .5 |
| 2 | .1 | 2 | .2 | .1 |

两种情况护甲均按完整反向比例扣到 0；第二行 Aegis Capacity 也扣到 0。
正值 Aegis Recovery Rate 参与扣除；不在新版路线中的护甲韧性与 Magic Resistance 保持原值。
`loadedMalumRulesIgnoreZeroAegisAndConsumePositiveAegis` 验证这两行及移除预兆后的恢复。
`allThreeAegisInputsIgnoreOnlyZeroValues` 单独验证三个 Aegis 输入全为零和各自有正值时的限制与消耗。
