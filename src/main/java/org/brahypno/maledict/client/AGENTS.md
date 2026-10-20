# 客户端代码组织约定

本文件供代码代理使用。目录和类名应直接表达职责，不另建面向读者的 README 导航。

- 按功能归属组织代码；`client` 根目录只放跨功能的客户端注册入口 `MaledictClientSetup`。
- MOD 总线上的实体与图层、饰品、HUD、颜色、物品粒子和后处理器注册集中在 `MaledictClientSetup`。
  保留各自的事件阶段与 `enqueueWork` 时机；Curios 渲染器必须在 `AddLayers` 冻结注册表前绑定。
- `codex` 放 Malum 法典条目与页面配置。
- `item` 放跨功能的物品模型谓词注册及其辅助逻辑 `MaledictItemProperties`。
- `weapon/incursus` 放神侵恶刃输入、提示与斩击特效；`weapon/remembrance` 放箭矢渲染与拉弓视野。
- `curio/enlightenment` 放启蒙之年的饰品模型与渲染；`curio/hangman` 放倒吊者的陷阱渲染与反射特效。
- `raven` 放渡鸦模型、实体与羽毛渲染、玩家肩部图层。
- `vitals` 放延迟生命池的客户端同步与红心显示。
- `infrared` 放微光视觉的状态、曲线、眼槽开关与后处理。
- `realm` 放枯败领域的维度视觉与客户端事件；`wisdom` 放智慧牺牲的黑屏状态与绘制。
- `tooltip` 放提示框背景注册、布局、绘制与颜色。
- `vicissitude` 放无常 Boss 的客户端 tick 驱动与血条；其 `model`、`render`、`music`、`vfx`
  子目录分别放模型与网格、实体与图层渲染、音乐、战斗与召唤特效。
- 运行期间的 FORGE 总线事件处理跟随功能目录，不与 MOD 总线注册混在同一个订阅类中。
- 只合并职责紧密相关的类。独立的模型、动画计算、音乐状态和后处理器继续分开。
- 测试跟随被测类的包结构；移动包内辅助类时同步移动测试，并更新游戏环境测试中通过字符串加载的类名。
- 移动源码时同步更新调用方、工具脚本和当前设计文档中的路径；保留归档记录里的历史路径。
