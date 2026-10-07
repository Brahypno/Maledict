# 渡鸦声音

原始录音：[Common Raven.ogg](https://commons.wikimedia.org/wiki/File:Common_Raven.ogg)，
G. McGrane 于 1996 年在美国缅因州阿卡迪亚国家公园录制的真实渡鸦叫声。
作者本人通过 **PD-self** 声明释放到全球公有领域；在不能直接弃权的地区，声明允许
任何人无条件用于任何用途（法律要求的条件除外）。允许修改、商用和再分发，没有署名义务。
页面核对与下载日期：2026-10-05。不是将网站的 CC0 元数据许可当作录音许可。

- 来源固定版本：https://commons.wikimedia.org/w/index.php?title=File:Common_Raven.ogg&oldid=866605535
- 原始下载：https://upload.wikimedia.org/wikipedia/commons/c/c3/Common_Raven.ogg
- 本地原始录音：`source/common-raven.ogg`（22.7265 秒，196941 字节）
- SHA-1：`5510722e428280528775b45c28f6f38c281fc761`

`../tools/make_raven_sfx.py` 截取原始录音的 0.30–1.05、2.05–2.80、12.15–13.05 秒，
用 120 Hz 高通减弱低频轰鸣，分别归一化至 −3 dBFS，淡入 10 毫秒、淡出 60 毫秒，
转换成 44.1 kHz 单声道 Ogg Vorbis（q5）。没有叠加音乐、人声或其他模组素材。

在同一录音上细化用途：受伤声使用前两个完整叫声，分别以 1.12 / 1.08 倍采样速度
得到较短、较尖的反应；死亡声使用第一、第三个叫声，以 0.78 / 0.82 倍速度降低音高、
拉长尾音，并淡出 220 / 250 毫秒。这是游戏用途的加工，并非动物真实受伤或死亡的录音。
脚步（0.16 秒）和振翅（0.30 秒）为脚本生成的原创拟音：脚步是短促爪尖敲击与摩擦，
振翅是带包络的滤波噪声羽毛扫动。随机种子固定，每种两个变化，无额外来源素材。
脚步峰值 −12 dBFS，振翅 −9 dBFS，死亡声 −4 dBFS，叫声及受伤声 −3 dBFS。

重建（需要 numpy，以及已有 ffmpeg 或 imageio-ffmpeg）：

```powershell
python art/raven/tools/make_raven_sfx.py --ffmpeg C:/Users/Jingfan/Downloads/ffmpeg-9.0.2-essentials_build/bin/ffmpeg.exe
```

游戏声音事件为 `maledict:entity.raven.ambient/hurt/death/step/fly`（斜线表示五个独立事件）。
日常鸣叫沿用原版生物随机计时与幼鸟音高，间隔参数为 240 tick、音量为 0.7。
受伤与死亡由原版伤害流程播放，避免多段伤害每次都另播一声；地面脚步由原版移动接口触发，
音量 0.15。飞行中实际移动或悬停时播放振翅，普通飞行每 12 tick、悬停每 8 tick，
音量 0.15，落地、入水、进入熔岩或死亡时停止。运动音不会取代叫声。
中英文字幕通过 `MaledictLanguage` 和 `runData` 生成，许可记录随 `CREDITS.md` 打包。
