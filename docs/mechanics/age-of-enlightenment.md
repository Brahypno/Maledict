# 启蒙时代（Age of Enlightenment）

配置：`ageOfEnlightenment.meleeDamageMultiplier`，默认 **2.0**。实现：`AgeOfEnlightenmentEvents#onMeleeHurt`。

## 它是什么

一个**普通的近战伤害倍率**：在 `LivingHurtEvent` 里，当伤害来源的**直接实体是玩家**、且该玩家有效果时，
把护甲前的数值乘以配置倍率（默认翻倍）。

## 它不是什么

- **不是强制暴击**。旧实现用 `CriticalHitEvent`（`ALLOW` + 1.5×）强行暴击，已经去掉。
- 只作用于近战：奥术通道、箭、爆炸的直接实体是别人（或没有），保持各自的数值。
- 横扫伤害走同一个 `player_attack` 来源，所以和挥砍一起翻倍。
- 真实暴击（下落攻击）仍然先 ×1.5，之后再被这个倍率翻倍。

## 一起去掉的东西

换成倍率后，也失去了强制暴击带来的暴击粒子与音效；同时，强制暴击原本会**压制镰刀横扫**，这条副作用也一并消失。
