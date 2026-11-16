# Boss 的身体接触

适用于第一化身与巨匠造物主（`VicissitudeBossEntity` 及其子类）。

## 结论

Boss **不会被推动**，玩家推着它走、把它撞开都不行。玩家贴在 Boss 身上时，仍会被 Boss 自己的
`pushEntities()` 分开；翅膀那一下（`Player#push`，水平 0.06）是故意保留的位移。

## 为什么需要专门处理

原版 `LivingEntity#aiStep` 结尾调用 `pushEntities()`，把每一对**可推动**的活体互相挤开：`EntitySelector#pushableBy`
先筛候选，`Entity#push` 再各自判断，然后把挤开量**直接加到 `deltaMovement`**，每 tick、每个重叠实体各一次。
`LivingEntity#isPushable` 对所有活体都是 true，而第一化身的转向是与上一 tick 速度**混合**（`FLIGHT_STEERING = 0.2`）
而不是覆盖它——两件事叠加，玩家靠在 Boss 身上就能以约 0.18 格/tick 把它推着走。

所以 `VicissitudeBossEntity`：

- `isPushable()` 恒为 `false`；
- 三参数 `push` 直接空实现。

两个都要做：冲撞类攻击（末影龙、劫掠兽、疣猪冲撞、监守者音波、移动的矿车）和其他模组会**直接调用 `push`**，
根本不会先问 `isPushable`。

原则：**只有 Boss 自己的移动代码可以改它的速度。**
