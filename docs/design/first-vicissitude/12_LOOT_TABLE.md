# 12 — 无常常规掉落物表

2026-09-16 用户需求：无常（`first_vicissitude`）要有掉落物表——四个难度等级各掉一件等级递增的
启蒙之年饰品，每级再掉两个深岩珍金块。等级档位与「块」的所指由用户当轮确认。

## 掉落规格

| 难度 | 掉落表 ID | 启蒙之年 | 深岩珍金块 |
| --- | --- | --- | --- |
| SIMPLE | `maledict:entities/first_vicissitude` | ×1，`EnlightenmentLevel:0`（游戏内 I 级） | `malum:block_of_cthonic_gold` ×2 |
| DIFFICULT | `maledict:entities/first_vicissitude_difficult` | ×1，`EnlightenmentLevel:1`（II 级） | 同上 |
| COMPLETE | `maledict:entities/first_vicissitude_complete` | ×1，`EnlightenmentLevel:2`（III 级） | 同上 |
| EXTREME | `maledict:entities/first_vicissitude_extreme` | ×1，`EnlightenmentLevel:3`（IV 级） | 同上 |

- NBT 上写的是 amplifier，`0` 就是游戏里显示的一级（见 `EnlightenmentLevel`）；四档由易到难正好
  是 I–IV 级，四个等级都能从这只 Boss 身上拿全——`maledict:age_of_enlightenment` 除创造模式
  物品栏与指令外没有别的获取途径，所以等级 I 不会因为「基础档」而被跳过。
- 「深岩珍金块」取 Malum 的方块物品 `malum:block_of_cthonic_gold`（`ItemRegistry.BLOCK_OF_CTHONIC_GOLD`），
  不是珍金锭；两块即 18 个珍金锭。
- 两个池都是必掉：不要求玩家击杀，也不吃抢夺等级（表里没有 `looting_enchant` 这类函数）；
  与普通生物一样受 `doMobLoot` 游戏规则约束。
- 经验不进表：仍然由 `xpReward = 100` 的原版流程发放。

## 实现位置

- `data/MaledictEntityLoot`：四张表的生成器（`LootTableSubProvider`，注册在 `LootContextParamSets.ENTITY`）。
  没有继承 `EntityLootSubProvider`：那个基类按注册表逐个实体类型要求它有默认表，其他模组的生物也会
  被算进来，而本模组只有这一个实体，且四张表并非都落在默认表 ID 上。
- `common/entity/FirstVicissitudeBossEntity`：`BossDifficulty` 各档带自己的表 ID；实体覆写
  `Mob#getDefaultLootTable` 按当前难度返回（1.20.1 的 `Mob#getLootTable` 是 final，只在实体自带
  显式表时才不经过这里）。
- 每档一张表而不是一张表里放四条条件：难度是服务端权威字段，按难度选表不依赖存档里写没写
  `VicissitudeDifficulty`（旧档缺失时已退到 SIMPLE），不会出现「表在、东西不掉」。
- 发放仍由基类的一次性死亡事务负责：`VicissitudeBossEntity#dropAllDeathLoot` 只在真死时调用一次
  原版流程，死亡动画、存档重载与 probe 都不会重复结算。
- `data/maledict/loot_tables/entities/*.json` 是 runData 产物，禁止直接编辑 `src/generated`。
- 调数值只动两处：`MaledictEntityLoot#enlightenmentLevel`（等级阶梯）与 `CTHONIC_GOLD_BLOCKS`
  （珍金块数量），改完重跑 runData。

## 验证

- 已跑：`compileJava`、`test`、`runData`。产物四张表内容与上表一致：
  启蒙之年分别带 `{EnlightenmentLevel:0..3}`，珍金块 `count 2.0`，两池 `rolls 1.0`。
- 游戏内未验证：四档各实际击杀一次，核对掉落数量、启蒙之年 NBT 等级与效果实际等级；
  用 `/loot` 或战利品查看类模组逐张读表。
