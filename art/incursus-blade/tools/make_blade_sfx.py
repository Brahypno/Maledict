"""生成神侵恶刃（Incursus Blade）五个动作的音效。

和 `art/first-vicissitude/tools/make_boss_bar.py` 是同一套路：**产物由脚本生成**，
改参数重跑即可，不做手工修音。输出到
`src/main/resources/assets/maledict/sounds/item/incursus_blade/`，
事件 id / 字幕在 `MaledictSounds` + `assets/maledict/sounds.json` + `MaledictLanguage`
里（**本脚本只出 .ogg，不碰那三处**）。

## 五个动作与挂点

| 输出 | 动作 | 代码挂点 |
| --- | --- | --- |
| `throw.ogg` | 投掷（Rebound 附魔脱手） | `IncursusBladeEnchantments#throwScythe` |
| `recall.ogg` | 收回（飞镰掉头回手） | `IncursusScytheBoomerangEntity#tick` |
| `ascension.ogg` | 飞腾（Ascension 起跳旋斩） | `IncursusBladeEnchantments#performAscensionAttack` |
| `slash.ogg` | 攻击（每次挥砍） | `IncursusBladeAttack#playSlashEffect` |
| `crit.ogg` | 暴击（一次挥砍至少一个暴击） | `IncursusBladeAttack#playCriticalSound` |

## 素材：全部 CC0 1.0 或公有领域

**没有一个需要署名**（CC0 是弃权，公有领域更不用说）。但仍然逐条记进
`src/main/resources/CREDITS.md`——CC0 徽章的分量取决于上传者有没有权利贴它，
留出处是为了将来有人来主张权利时能一秒拿出证据。原始候选文件在 `sfx-candidates/`
（已 gitignore），下载地址见下面的 `SOURCES`。

**为什么是"合成"而不是"挑一个现成的"**：这几个动作需要的是同一个音色家族的五个成员
（同一把刀刃的脱手、回手、腾空、挥砍、重击），现成的单条音效凑不出这个关系。
所以每条输出都是 2–3 层素材叠出来的：**金属层**（BMacZero / StarNinjas）给"这是同一把刃"，
**气流层**（qubodup / remaxim / artisticdude）给动作方向，**身体层**（BMacZero / gregoryweir）
给暴击的分量。叠层同时解决了"暴击必须是攻击的加强版而不是另一种音色"这个要求。

## 处理链

1. 所有素材统一解码成 44.1 kHz 单声道（**单声道才会随距离衰减**，见 04 §1.5 第 7 条）。
2. 按 `RECIPES` 摆放、叠层、各自给增益。
3. 峰值归一化到每条自己的目标（挥砍最频繁，压得最低，避免听腻）。
4. 头 3 ms / 尾 60–200 ms 淡入淡出，避免爆音。
5. 用 ffmpeg 的 libvorbis 编码成 Ogg Vorbis（**mono**，q5 ≈ 160 kbps，单条约 10–25 KB）。

## 用法

```powershell
pip install numpy soundfile imageio-ffmpeg   # 只需一次
python art/incursus-blade/tools/make_blade_sfx.py
```

`sfx-candidates/` 里缺文件时脚本会按 `SOURCES` 里的地址自动下载。
"""

from __future__ import annotations

import shutil
import subprocess
import sys
import urllib.request
from dataclasses import dataclass
from pathlib import Path

import numpy as np
import soundfile as sf

REPO = Path(__file__).resolve().parents[3]
CANDIDATES = REPO / "sfx-candidates"
OUT_DIR = REPO / "src" / "main" / "resources" / "assets" / "maledict" / "sounds" / "item" / "incursus_blade"

SAMPLE_RATE = 44100


# ------------------------------------------------------------------ 素材

@dataclass(frozen=True)
class Source:
    """一条素材：本地相对路径、下载地址、出处页面、许可、作者。"""

    path: str
    url: str
    page: str
    license: str
    credit: str


SOURCES: dict[str, Source] = {
    # qubodup "Wind, hit, time morph"（CC0）：两条 megaswosh + 一条慢动作拉伸
    "megaswosh1": Source(
        "throw-recall-soar/qubodup_wind-hit-time-morph_x/qubodup-megaswosh1.wav",
        "https://opengameart.org/sites/default/files/qubodup-timehitwind.zip",
        "https://opengameart.org/content/wind-hit-time-morph",
        "CC0 1.0", "qubodup (Iwan Gabovitch)"),
    "slomo1": Source(
        "throw-recall-soar/qubodup_wind-hit-time-morph_x/qubodup-slomo1.wav",
        "https://opengameart.org/sites/default/files/qubodup-timehitwind.zip",
        "https://opengameart.org/content/wind-hit-time-morph",
        "CC0 1.0", "qubodup (Iwan Gabovitch)"),
    # remaxim "3 Melee sounds"（CC0，derived from qubodup's swishes）
    "sword": Source(
        "attack/remaxim_3-melee-sounds_x/melee sounds/sword sound.wav",
        "https://opengameart.org/sites/default/files/melee%20sounds.zip",
        "https://opengameart.org/content/3-melee-sounds",
        "CC0 1.0", "remaxim"),
    # Kenney "50 RPG sound effects"（CC0）
    "knifeSlice": Source(
        "impact/kenney_rpg-sounds-oga-mirror_x/OGG/knifeSlice.ogg",
        "https://opengameart.org/sites/default/files/RPGsounds_Kenney.zip",
        "https://opengameart.org/content/50-rpg-sound-effects",
        "CC0 1.0", "Kenney.nl"),
    # Vehicle "Fantasy Weapons and Apparel SFX Library"（CC0）
    "clash07": Source(
        "new/vehicle_weapons-apparel_x/sfx/sword-knife-clash-07.wav",
        "https://opengameart.org/sites/default/files/weapons-apparel.zip",
        "https://opengameart.org/content/fantasy-weapons-and-apparel-sfx-library",
        "CC0 1.0", "Vehicle (Jan Schupke)"),
    # artisticdude "Swishes Sound Pack"（CC0）：最短最亮的那条，当挥砍的锋刃前缘
    "swish13": Source(
        "attack/artisticdude_swishes_x/swishes/swish-13.wav",
        "https://opengameart.org/sites/default/files/swishes.zip",
        "https://opengameart.org/content/swishes-sound-pack",
        "CC0 1.0", "artisticdude"),
    # BMacZero "Metal Impact Sounds"（CC0）：金属与身体两层
    "bing1": Source(
        "crit/bmaczero_bing1.wav",
        "https://opengameart.org/sites/default/files/bing1.wav",
        "https://opengameart.org/content/metal-impact-sounds",
        "CC0 1.0", "BMacZero (Brian MacIntosh)"),
    "clink1": Source(
        "crit/bmaczero_clink1.wav",
        "https://opengameart.org/sites/default/files/clink1_0.wav",
        "https://opengameart.org/content/metal-impact-sounds",
        "CC0 1.0", "BMacZero (Brian MacIntosh)"),
    "bong1": Source(
        "crit/bmaczero_bong1.wav",
        "https://opengameart.org/sites/default/files/bong1.wav",
        "https://opengameart.org/content/metal-impact-sounds",
        "CC0 1.0", "BMacZero (Brian MacIntosh)"),
    # StarNinjas "20 Sword Sound Effects"（CC0）：两把刀互刮，真金属剪切
    "clash1": Source(
        "crit/starninjas_sword-clashes_x/sword_clash.1.ogg",
        "https://opengameart.org/sites/default/files/sword_clash_-_starninjas_0.zip",
        "https://opengameart.org/content/20-sword-sound-effects-attacks-and-clashes",
        "CC0 1.0", "StarNinjas"),
    # Ogrebane "Teleport Spell"（CC0，和 artisticdude 的 Swishes 同一套 Summoning Wars 素材）
    "teleport": Source(
        "throw-recall-soar/ogrebane_teleport.wav",
        "https://opengameart.org/sites/default/files/teleport.wav",
        "https://opengameart.org/content/teleport-spell",
        "CC0 1.0", "Ogrebane"),
    # JaggedStone "Magic Spell SFX"（CC0）：7 号是唯一一条能量**涨到尾巴**的
    "magical7": Source(
        "throw-recall-soar/jaggedstone_magical_7.ogg",
        "https://opengameart.org/sites/default/files/magical_7_0.ogg",
        "https://opengameart.org/content/magic-spell-sfx",
        "CC0 1.0", "JaggedStone"),
    # remaxim "Short wind sound"（CC0）：取前 1.2 秒那个鼓包，后面的嘶声尾巴不要
    "wind": Source(
        "throw-recall-soar/remaxim_short-wind.wav",
        "https://opengameart.org/sites/default/files/short%20wind%20sound.wav",
        "https://opengameart.org/content/short-wind-sound",
        "CC0 1.0", "remaxim"),
    # gregoryweir "Dull thud"（公有领域，来自 pdsounds.org）
    "thud": Source(
        "impact/dull_thud_pd.ogg",
        "https://upload.wikimedia.org/wikipedia/commons/5/5b/Dull_thud.ogg",
        "https://commons.wikimedia.org/wiki/File:Dull_thud.ogg",
        "Public domain (PD-author)", "gregoryweir"),
}


# ------------------------------------------------------------------ 配方

@dataclass(frozen=True)
class Layer:
    """把 `source` 的 [src_start, src_start + dur) 段，以 `gain` 叠到输出的 `dst_start`。"""

    source: str
    src_start: float
    dst_start: float
    dur: float
    gain: float


@dataclass(frozen=True)
class Recipe:
    peak_db: float          # 归一化目标峰值
    fade_out: float         # 尾部淡出时长（秒）
    layers: tuple[Layer, ...]


RECIPES: dict[str, Recipe] = {
    # 脱手：重型刀刃离手。气流主体是 megaswosh1（峰值在 0.18 s，之后一路掉），
    # 头上压一层很短的 bing1——金属层是"这是同一把刃"的线索，暴击那条用的是同一个音色家族。
    "throw": Recipe(-1.5, 0.12, (
        Layer("megaswosh1", 0.00, 0.00, 0.55, 1.00),
        Layer("bing1", 0.00, 0.00, 0.45, 0.30),
    )),
    # 收回：slomo1 是全套素材里唯一一条**能量一路涨到 0.85 s** 的气流（-51 dB → -13 dB），
    # 正是"刃在往回飞"的形状；bing1 落在 0.66 s 接在隆起上，当入手的一扣
    #（定案时从 clink1 换成它：更长、更清）。
    "recall": Recipe(-1.5, 0.22, (
        Layer("slomo1", 0.00, 0.00, 0.90, 1.00),
        Layer("bing1", 0.00, 0.66, 0.60, 0.85),
    )),
    # 飞腾：三层。teleport 是冷而空的法术鼓包（0.34 s 到顶再衰减）当骨架，
    # 前 1.2 s 的风当"起跳的气"，magical7 那条平缓的合成器微光垫在底下。
    "ascension": Recipe(-1.5, 0.25, (
        Layer("teleport", 0.00, 0.00, 1.35, 1.00),
        Layer("wind", 0.00, 0.00, 1.20, 0.55),
        Layer("magical7", 0.00, 0.00, 1.35, 0.30),
    )),
    # 攻击：每次挥砍都响，所以要短、要干、峰值压得比别的低 1 dB（文件层面先让一档，
    # 代码里的 volume 也只用 1.0）。sword 的瞬态在 0.04 s、0.16 s 就落干净；
    # swish13 是 0.07 s 的极短亮片，只补锋刃前缘；knifeSlice 从 0.10 s 起只取 0.30 s，
    # 落在 0.26–0.56 s，当 "一点尾音"（gain 0.40，听得见但不抢）。
    "slash": Recipe(-2.5, 0.12, (
        Layer("sword", 0.00, 0.00, 0.44, 1.00),
        Layer("swish13", 0.00, 0.00, 0.10, 0.35),
        Layer("knifeSlice", 0.10, 0.26, 0.30, 0.40),
    )),
    # 暴击：金属剪切（跳过 clash1 开头 0.13 s 的静音，让瞬态落在第 0 帧）
    # + 定案时加的 clash07（3.29 s 的真刀相击，取前 0.90 s，尾巴一路拖）
    # + bong1 的低频当胸腔 + thud 的闷击当"打实了"。
    "crit": Recipe(-1.0, 0.20, (
        Layer("clash1", 0.13, 0.00, 0.65, 1.00),
        Layer("clash07", 0.00, 0.00, 0.90, 0.70),
        Layer("bong1", 0.00, 0.00, 0.75, 0.50),
        Layer("thud", 0.00, 0.00, 0.39, 0.60),
    )),
}

FADE_IN = 0.003


# ------------------------------------------------------------------ 工具

def find_ffmpeg() -> str:
    """优先用 PATH 上的 ffmpeg；没有就退回 pip 装出来的那一个。"""
    if (found := shutil.which("ffmpeg")):
        return found
    try:
        import imageio_ffmpeg

        return imageio_ffmpeg.get_ffmpeg_exe()
    except ImportError:
        sys.exit("找不到 ffmpeg：装一个（winget install ffmpeg），或 pip install imageio-ffmpeg。")


FFMPEG = find_ffmpeg()


def ensure_source(key: str) -> Path:
    src = SOURCES[key]
    path = CANDIDATES / src.path
    if not path.exists():
        path.parent.mkdir(parents=True, exist_ok=True)
        print(f"  下载 {key}: {src.url}")
        with urllib.request.urlopen(src.url, timeout=120) as response, path.open("wb") as handle:  # noqa: S310
            shutil.copyfileobj(response, handle)
    return path


def decode(path: Path) -> np.ndarray:
    """一律经 ffmpeg 解成 44.1 kHz 单声道 float32——素材里有 Ogg、WAV 和带 data 流的怪 Ogg。"""
    raw = subprocess.run(
        [FFMPEG, "-v", "error", "-i", str(path), "-ac", "1", "-ar", str(SAMPLE_RATE), "-f", "f32le", "-"],
        check=True, capture_output=True,
    ).stdout
    return np.frombuffer(raw, dtype="<f4").astype(np.float64)


def place(layer: Layer, cache: dict[str, np.ndarray]) -> np.ndarray:
    """取 [src_start, src_start+dur) 这一段，按 gain 缩放，并在前面垫上 dst_start 的静音。

    垫静音这一步不能省：`build()` 是把每条轨道**从头对齐相加**的，
    偏移只能靠这条前导静音表达（少了它，所有层都会挤在第 0 帧）。
    """
    data = cache[layer.source]
    start = int(round(layer.src_start * SAMPLE_RATE))
    length = int(round(layer.dur * SAMPLE_RATE))
    chunk = data[start:start + length]
    if chunk.size < length:
        chunk = np.pad(chunk, (0, length - chunk.size))
    offset = int(round(layer.dst_start * SAMPLE_RATE))
    return np.concatenate([np.zeros(offset), chunk * layer.gain])


def fade(samples: np.ndarray, fade_out: float) -> np.ndarray:
    fade_in_len = min(int(FADE_IN * SAMPLE_RATE), samples.size)
    fade_out_len = min(int(fade_out * SAMPLE_RATE), samples.size)
    if fade_in_len:
        samples[:fade_in_len] *= np.linspace(0.0, 1.0, fade_in_len)
    if fade_out_len:
        samples[-fade_out_len:] *= np.linspace(1.0, 0.0, fade_out_len)
    return samples


def encode(samples: np.ndarray, target: Path) -> None:
    target.parent.mkdir(parents=True, exist_ok=True)
    raw = (np.clip(samples, -1.0, 1.0) * 32767.0).astype("<i2").tobytes()
    subprocess.run(
        [FFMPEG, "-v", "error", "-y", "-f", "s16le", "-ar", str(SAMPLE_RATE), "-ac", "1", "-i", "-",
         "-c:a", "libvorbis", "-q:a", "5", str(target)],
        input=raw, check=True,
    )


def build(name: str, recipe: Recipe, cache: dict[str, np.ndarray]) -> tuple[float, float, float]:
    mixes = [place(layer, cache) for layer in recipe.layers]
    length = max(mix.size for mix in mixes)
    out = np.zeros(length)
    for mix in mixes:
        out[:mix.size] += mix
    out = fade(out, recipe.fade_out)
    # 归一化放在淡出之后：峰值落在尾巴里的那条（recall 的隆起在 0.85 s）否则会被淡掉。
    peak = float(np.max(np.abs(out)))
    if peak > 0:
        out *= 10 ** (recipe.peak_db / 20.0) / peak
    encode(out, OUT_DIR / f"{name}.ogg")
    rms = float(np.sqrt(np.mean(out ** 2)))
    return length / SAMPLE_RATE, 20 * np.log10(max(rms, 1e-9)), 20 * np.log10(max(float(np.max(np.abs(out))), 1e-9))


def main() -> None:
    print(f"素材目录 {CANDIDATES.relative_to(REPO)}")
    cache: dict[str, np.ndarray] = {}
    used = {layer.source for recipe in RECIPES.values() for layer in recipe.layers}
    for key in SOURCES:
        if key not in used:      # SOURCES 是全集（留档用），这里只解码配方真正用到的
            continue
        path = ensure_source(key)
        cache[key] = decode(path)
        print(f"  {key:<11} {cache[key].size / SAMPLE_RATE:6.2f} s  {SOURCES[key].license:<26} {SOURCES[key].credit}")

    print(f"\n输出 {OUT_DIR.relative_to(REPO)}")
    for name, recipe in RECIPES.items():
        dur, rms_db, peak_db = build(name, recipe, cache)
        size = (OUT_DIR / f"{name}.ogg").stat().st_size
        print(f"  {name + '.ogg':<14} {dur:5.2f} s  rms {rms_db:6.1f} dB  peak {peak_db:5.1f} dB  {size / 1024:5.1f} KB")


if __name__ == "__main__":
    main()
