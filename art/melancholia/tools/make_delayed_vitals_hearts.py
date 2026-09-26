"""生成延迟池血条用的心形遮罩（带高光与阴影）。

底图取原版 `assets/minecraft/textures/gui/icons.png` 里那颗满心填色（u=52, v=0, 9x9）：
把它的形状（alpha）和明暗（亮度）原样搬过来，亮度归一化后写成灰度遮罩 —— 颜色交给代码
`setColor` 上色，**高光与阴影留在贴图里**，所以染出来的心是有立体感的，不是一块平色。

左半心与右半心直接取同一颗满心的左 5 列与右 5 列，所以「左半 + 右半」能无缝拼回整颗心。

输出 `src/main/resources/assets/maledict/textures/gui/delayed_vitals_hearts.png`，29x9 RGBA：
x=1 满心、x=10 左半心、x=19 那一格里放右半心（占该格第 4..8 列，所以实际落在 x=23）。
对应 `DelayedVitalsOverlay` 里的 `MASK_U_FULL / MASK_U_LEFT_HALF / MASK_U_RIGHT_HALF` 与
`RIGHT_HALF_INSET`，改这里就得同步改那边。

用法（仓库根目录）：`python art/melancholia/tools/make_delayed_vitals_hearts.py`
"""

import io
import zipfile
from pathlib import Path

from PIL import Image

CLIENT_JAR = (Path.home() / ".gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client.jar")
ICONS_ENTRY = "assets/minecraft/textures/gui/icons.png"

# 原版 icons.png 里普通满心的填色（半心在 u=61，这里不用它：两半都从满心切，拼回去才严丝合缝）。
HEART_UV = (52, 0)
HEART = 9

SHEET_WIDTH = 29
SHEET_HEIGHT = 9
U_FULL = 1
U_LEFT_HALF = 10
U_RIGHT_HALF = 19
RIGHT_HALF_INSET = 4

# 明暗归一化后的灰度区间。下限别压太黑：setColor 是乘法，底色暗一档整个颜色就发闷
# （神圣的粉会被看成偏紫），所以这里只留 0.7~1.0 那一段渐变，接近原版红心自己的明暗幅度。
SHADE_LOW = 0.7
SHADE_HIGH = 1.0

OUTPUT = (Path(__file__).resolve().parents[3]
          / "src/main/resources/assets/maledict/textures/gui/delayed_vitals_hearts.png")

RAMP = " .:-=+*#%@"


def load_vanilla_heart():
    if not CLIENT_JAR.exists():
        raise SystemExit(f"找不到原版 client.jar：{CLIENT_JAR}")
    with zipfile.ZipFile(CLIENT_JAR) as jar:
        with jar.open(ICONS_ENTRY) as stream:
            icons = Image.open(io.BytesIO(stream.read())).convert("RGBA")
    x, y = HEART_UV
    return icons.crop((x, y, x + HEART, y + HEART))


def luminance(pixel):
    red, green, blue = pixel[0], pixel[1], pixel[2]
    return 0.299 * red + 0.587 * green + 0.114 * blue


def to_shaded_mask(heart):
    """形状取 alpha，明暗取亮度并归一化到 [SHADE_LOW, SHADE_HIGH]。"""
    opaque = [(x, y) for y in range(HEART) for x in range(HEART)
              if heart.getpixel((x, y))[3] > 0]
    values = [luminance(heart.getpixel(pixel)) for pixel in opaque]
    low, high = min(values), max(values)

    mask = Image.new("RGBA", (HEART, HEART), (0, 0, 0, 0))
    for x, y in opaque:
        pixel = heart.getpixel((x, y))
        ratio = (luminance(pixel) - low) / (high - low) if high > low else 0.0
        level = round(255 * (SHADE_LOW + ratio * (SHADE_HIGH - SHADE_LOW)))
        mask.putpixel((x, y), (level, level, level, pixel[3]))
    return mask


def preview(mask):
    """打在终端上的肉眼核对：亮的地方是 @，暗的地方是 .，透明是空格。"""
    for y in range(HEART):
        row = ""
        for x in range(HEART):
            pixel = mask.getpixel((x, y))
            if pixel[3] == 0:
                row += " "
                continue
            row += RAMP[min(len(RAMP) - 1, pixel[0] * len(RAMP) // 256)]
        print(row)


def main():
    heart = to_shaded_mask(load_vanilla_heart())

    sheet = Image.new("RGBA", (SHEET_WIDTH, SHEET_HEIGHT), (0, 0, 0, 0))
    sheet.paste(heart, (U_FULL, 0))
    sheet.paste(heart.crop((0, 0, 5, HEART)), (U_LEFT_HALF, 0))
    sheet.paste(heart.crop((4, 0, 9, HEART)), (U_RIGHT_HALF + RIGHT_HALF_INSET, 0))

    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(OUTPUT)
    print(f"wrote {OUTPUT} ({sheet.width}x{sheet.height})")
    print("--- 满心 ---")
    preview(heart)


if __name__ == "__main__":
    main()
