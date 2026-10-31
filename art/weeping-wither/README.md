# 腐枯凋灵：渗泣型外观

依据 [原始外观设计](source/腐枯凋灵_渗泣型外观设计.md)，并采用本次确认的两项约束：

- 直接修改原版凋灵模型与贴图。保留方块结构、三头和三道肋骨，只小幅调整姿态、侧头间距及躯干厚度。
- 出生闪烁改成灰紫，眼窝与裂纹使用局部自发光。按后续实机反馈，半血护盾使用明确的紫色，
  身体增加紫色分支裂纹，部分裂纹和交汇点的饱和度更高。

## 模型与贴图来源

`WeepingWitherModel` 继承 `WitherBossModel`，直接使用 Minecraft 1.20.1 的 `ModelLayers.WITHER`；
护盾使用原版 `WITHER_ARMOR` 膨胀模型并应用相同调整。全部九个原版方块及其 64×64 UV 保留，
没有另造近似模型。肩梁宽度 ×1.12，肋骨宽度 ×1.16、厚度 ×1.30，尾骨厚度 ×1.15；
两侧头各向外移 1.0 模型单位、下移 1.3 单位并略微外倾，主头前压。
侧头略微收窄，左右比例轻微不对称，保持原版六单位侧头与八单位主头的大小关系。
保持原来的实体 1.2 倍缩放和独立侧头索敌动画。

`source/wither.png`、`wither_invulnerable.png`、`wither_armor.png` 提取自本机 Minecraft 1.20.1
`client.jar` 的 `assets/minecraft/textures/entity/wither/`，属于 Mojang 的原版资源，不能视为本项目原创。

`source/weeping-wither-imagegen.png` 是第一版内置 imagegen 编辑结果，提示词见
[imagegen-prompt.txt](source/imagegen-prompt.txt)。用户反馈第一版改动过小、区分度不足，
第二版以 [首版贴图](source/weeping-wither-v1.png) 为底稿继续编辑，产物为
`source/weeping-wither-imagegen-v2.png`，提示词见 [imagegen-prompt-v2.txt](source/imagegen-prompt-v2.txt)。
重建工具将生成结果对齐回原版 64×64 网格，再按原 UV 岛合成风化表面、黑色空眼窝、长泣痕与胸裂。
额骨、颧骨、肩梁与肋骨的灰白磨损覆盖更广，保留原版底图的斑驳块面和眼口位置。
常态贴图改变原图 1566 个不透明像素中的 1182 个，全部透明区域保持一致。
第一版对照保存在 [comparison-v1.png](preview/comparison-v1.png)。

正面与侧面裂纹编辑来自 `source/weeping-wither-imagegen-v3.png`，以第二版实际贴图为底稿，
提示词见 [imagegen-prompt-v3.txt](source/imagegen-prompt-v3.txt)。重建时将裂纹分别注册到
头骨正面与侧面、肩梁、三道肋骨、胸骨和尾骨的 UV 上；保留焦黑、灰白底材。
大部分裂纹为深紫，少量交汇点和短段为高饱和紫晶色。第二版对照保存在
[comparison-v2.png](preview/comparison-v2.png)。

背面补全来自 `source/weeping-wither-imagegen-v4.png`，以第三版实际贴图为底稿，
提示词见 [imagegen-prompt-v4.txt](source/imagegen-prompt-v4.txt)。裂纹覆盖三颗头的后部、
肩梁背面、三道肋骨背面、脊柱与尾骨背面，部分支线延伸到相邻侧面。
背面同样使用深紫与局部高饱和紫晶色，并写入发光遮罩、出生闪烁与重聚贴图。

眼窝底色与小范围自发光分离；泣痕本身不发光。分支裂纹使用独立的紫色发光遮罩。
拒死时使用眼光熄灭、裂纹加深的贴图，头部进一步垂下，裂纹光增强并脉动。
出生闪烁沿用原版时序；下半血护盾沿用原版滚动纹理与生效条件。
护盾同时使用紫色贴图与偏紫顶点色，降低绿色通道，避免叠加在灰白骨质上后失去紫色。
重聚期间保留下半血的紫色护盾，与裂纹和紫雾一同显示；实际箭矢免疫规则不变。
原版普通凋灵的资源、渲染器和行为不变。

## 凋灵之首投射物

`WeepingWitherSkullRenderer` 使用原版 `ModelLayers.WITHER_SKULL` 单方块模型与飞行朝向。
纹理重建工具把已确认的主头 UV 像素原样移到原版投射物使用的 `(0, 35)` 贴图区，
因此前面、侧面和背面均有与 Boss 一致的骨质和裂纹。普通弹具有眼窝与裂纹的局部自发光；
蓄力弹增加紫色底色并增强局部光，不沿用原版蓝色外观。
[投射物预览](preview/projectiles.png) 包含普通弹正面、侧面、蓄力弹侧面与普通弹背面。
用户确认直击最终固定增加 1♥（2 HP）：无凋零目标 12 HP，已有凋零目标 14 HP。
普通弹和蓄力弹均在存活 600 tick 后直接移除、不爆炸；存活时长随实体保存，重载不重置。

## 粒子

保留原版凋灵烟雾，额外稀疏加入受重力影响的灰烬和暗紫灰尘，受击增加少量灰烬与骨粉。
`WeepingWitherEffects` 在客户端 `LivingTickEvent` 中观察现有同步重聚标记，
进入时在胸裂闪出紫光，期间持续出现向胸腔内下落收拢的紫色碎光和雾团，结束时散开一圈紫色残光。
这些重聚粒子显式使用全亮照明，并使用强制生成，避免洞穴无光和粒子数量设置令效果消失。
烟雾使用延后透明度淘汰，确保淡入首帧为零透明度时不会直接被丢弃。
过渡记录仅存在客户端弱引用集合中，不增加同步字段、服务端粒子包或存档数据。

## 重建与验证

在项目根目录用 Java 17 运行：

```powershell
java art/weeping-wither/tools/WeepingWitherTextures.java
.\gradlew.bat -I art/weeping-wither/tools/preview.init.gradle weepingWitherPreview --offline
```

第一条生成实体 PNG 及贴图对照，并检查透明区域不变；第二条编译生产代码并调用实际烘焙模型，
核对九个原版方块及 UV 后输出 [模型对照](preview/comparison.png)，包含半血护盾的材质预览。
护盾预览使用实际贴图、顶点色与 UV 滚动位置，软件近似计算叠加混合；它不是游戏截图。
同时输出 [正面、侧面、背面与侧后方](preview/all-sides-v4.png)，用于检查当前贴图实际覆盖。
补全前的四视图保存在 [all-sides-v3.png](preview/all-sides-v3.png)。
本次资源不属于 `src/generated`，不需要 `runData`。

实际游戏仍需检查 `/summon maledict:weeping_wither` 的移动护盾、出生闪烁、受击粒子及首次致死重聚。
可用原版凋灵作同场对比；出生阶段可通过召唤 NBT `Invul:220` 查看。
软件预览仅近似叠加发光遮罩，不能验证游戏着色器、透明混合与粒子动态。
