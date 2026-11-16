# 猎首之刃的奥术与霜冻通道

猎首之刃在物理挥砍之外有两条**独立结算**的加伤通道。它们各自有来源、各自吃减免，也都**不受攻击强度缩放**。

## 奥术通道（`can_trigger_magic_damage`）

- 刀刃的奥术灌注（`MAGIC_DAMAGE` 属性）**本身不造成伤害**，是 Lodestone 在**受害者的
  `LivingHurtEvent` 里**兑现的：来源类型在 `forge:can_trigger_magic_damage` 里（Malum 把
  `malum:scythe_melee` 与 `malum:scythe_sweep` 放进去）时，把 `invulnerableTime` 清零后追加一跳
  `minecraft:magic`，数值等于该属性；直接实体为 null，攻击者是造成者。
- `forge:ignores_magic_attack_cooldown_scalar` 为空，所以这一跳**从不按攻击强度缩放**。
- Maledict 对流内武器接管了这条属性：`MaledictAttributes` 把 Lodestone 的 `MAGIC_DAMAGE` 注册进
  ChangeLib 的 `AttributeDamageRegistry`，`LodestoneAttributeEventHandlerMixin` 在「主手属于
  `maledict:lodestone_magic_takeover`」且「原来源在 `CAN_TRIGGER_MAGIC` 但不在 `IS_MAGIC`」时取消
  Lodestone 自己那次结算。接管后的请求保持 Lodestone 的形状：source `minecraft:magic`、直接实体 null、
  攻击者为造成者。旧版 `markMeleeHurtEvent` 的补偿已经删掉。

## 霜冻通道（`powder_snow_damage`）

- `maledict:powder_snow_damage` 是注册过的属性；刀刃的 NBT 数值喂一个主手 `ADDITION` 修饰符，
  ChangeLib 的 `AttributeDamageRegistry` 在主探测之后派发一跳独立的 `minecraft:freeze`。
- 属性请求按注册顺序派出：**先 freeze，再 magic**；旧的显式 `voodoo` 探测已删除。
- ChangeLib 的 `ProbeScope` 只要 `totalDealt() > 0` 就记一笔，不要求观测到 `hurt` 路径（完全覆写
  `hurt` 的实体、或以 setHealth/NBT 结清的探测都算），所以这一跳不依赖原版 `hurt` 事件。
- 注册使用 `AttributeAmountReaders.itemBacked()`：飞行中的镰刀也能把物品修饰符与攻击者属性复合，
  不要求物品握在主手。

## 一次挥砍只开一轮

Forge 的多部位 Boss 会把本体和每个动画部件暴露成独立实体。`IncursusBladeAttack.uniqueLivingTargets`
把近战与 Ascension 的目标列表收敛到「活体根实体」（每个根取最近的一个命中箱），所以一次挥砍仍然只开一次
主探测、只产生一轮霜冻/奥术。回旋镖只命中单个部位，不需要收敛。

## 阶（`DamageTier`）

`DamageTier` 在 `IncursusBladeEffects` 的 `DamageRequestEvent` 上设置请求的基础 `priority`/`strength`
（NORMAL），`BlissRuneEvents` 再提升一级（LOW）。统一的 `DamageRequestEvent` 也覆盖探测请求，
`attribute() != null` 用来区分「属性请求」和普通攻击请求。
