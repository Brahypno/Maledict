#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""first-vicissitude 血条中央件的 16x16 组件草图。

这些是 *草图*，不是成品：pixelgen 的 sketch2tex.py 拿它们当 img2img 的底图，
所以只需要定死「轮廓 + 颜色位置」，阴影和质感交给模型。
输出只写 build/，不碰 src/。

01_SPEC 五色：
    D  #19151F  深黑紫   描边
    g  #49404F  灰紫     凹陷 / 阴影
    b  #B8B8C4  冷骨白   壳体主体
    w  #E6EDF5  冷白     瞳孔
    e  #51436D  暗紫能量 虹膜
"""

from __future__ import annotations

import pathlib
import sys

from PIL import Image, ImageDraw, ImageFont

TILE = 16
PALETTE = {
    ".": None,
    "D": (0x19, 0x15, 0x1F),
    "g": (0x49, 0x40, 0x4F),
    "b": (0xB8, 0xB8, 0xC4),
    "w": (0xE6, 0xED, 0xF5),
    "e": (0x51, 0x43, 0x6D),
}
ZOOM = 10

HERE = pathlib.Path(__file__).resolve()
REVIEW = HERE.parents[3] / "build" / "first-vicissitude-review" / "components"
OUT = REVIEW / "sketch"


def blank() -> list[list[str]]:
    return [["." for _ in range(TILE)] for _ in range(TILE)]


def span(g, y, x0, x1, ch):
    for x in range(x0, x1 + 1):
        g[y][x] = ch


def outline(g, rows, thick_caps=True):
    """把每一行的两端改成描边；首行/末行整行描边。"""
    ys = sorted(rows)
    for i, y in enumerate(ys):
        x0, x1 = rows[y]
        if thick_caps and i in (0, len(ys) - 1):
            span(g, y, x0, x1, "D")
        else:
            g[y][x0] = "D"
            g[y][x1] = "D"


def dump(g) -> list[str]:
    return ["".join(r) for r in g]


def to_image(g) -> Image.Image:
    im = Image.new("RGBA", (TILE, TILE), (0, 0, 0, 0))
    px = im.load()
    for y in range(TILE):
        for x in range(TILE):
            c = PALETTE[g[y][x]]
            if c is not None:
                px[x, y] = c + (255,)
    return im


# --------------------------------------------------------------------------
# 组件 1：眼睛。14x11 的透镜，水平对称轴在 x7.5，垂直中心 y7。
# --------------------------------------------------------------------------
LENS = {
    2: (6, 9),
    3: (4, 11),
    4: (2, 13),
    5: (1, 14),
    6: (1, 14),
    7: (1, 14),
    8: (1, 14),
    9: (1, 14),
    10: (2, 13),
    11: (4, 11),
    12: (6, 9),
}


def lens_base() -> list[list[str]]:
    g = blank()
    for y, (x0, x1) in LENS.items():
        span(g, y, x0, x1, "b")
        g[y][x0] = "D"
        g[y][x1] = "D"
        # 下半部内侧压一层阴影，给模型一个「上受光下背光」的提示
        if y >= 7 and x1 - x0 >= 3:
            g[y][x0 + 1] = "g"
            g[y][x1 - 1] = "g"
    return g


def eye_closed() -> list[list[str]]:
    """闭合：两片壳沿竖直中缝贴合，只有一条淡淡的接缝。"""
    g = lens_base()
    for y in range(6, 9):
        g[y][7] = "g"
        g[y][8] = "g"
    return g


def eye_open_slit() -> list[list[str]]:
    """开裂：中缝处透出冷白的光，上下收细（不是一根等宽的柱子）。"""
    g = lens_base()
    core = {5: 1, 6: 2, 7: 2, 8: 2, 9: 1}
    for y, w in core.items():
        xs = (7, 8) if w == 2 else (7,)
        for x in xs:
            g[y][x] = "w"
        if w == 2:
            g[y][6] = "D"
            g[y][9] = "D"
    return g


# 裂口两端收细，中间最宽 —— 矩形缝会读成投币口 / 电池仓
CRACK = {5: (7, 8), 6: (6, 9), 7: (6, 9), 8: (6, 9), 9: (7, 8)}


def eye_open_gap() -> list[list[str]]:
    """裂开：两片壳真的分开了，缝里是暗的，只有中间浮着一小块瞳。"""
    g = lens_base()
    for y, (a, b) in CRACK.items():
        span(g, y, a, b, "D")
    for y in range(6, 9):
        g[y][7] = "w"
        g[y][8] = "w"
    return g


def eye_open_iris() -> list[list[str]]:
    """裂开 + 虹膜：缝里是暗紫能量，中间一枚冷白瞳。"""
    g = lens_base()
    for y, (a, b) in CRACK.items():
        span(g, y, a, b, "e")
    for y in range(6, 9):
        g[y][7] = "w"
        g[y][8] = "w"
    return g


# --------------------------------------------------------------------------
# 组件 2：羽饰单元。一片朝下的骨羽，边缘带锯齿，中轴一根灰杆。
# --------------------------------------------------------------------------
FEATHER = {
    1: (7, 8),
    2: (6, 9),
    3: (5, 10),
    4: (5, 10),
    5: (4, 11),
    6: (4, 11),
    7: (4, 11),
    8: (4, 11),
    9: (5, 10),
    10: (5, 10),
    11: (6, 9),
    12: (6, 9),
    13: (7, 8),
    14: (7, 8),
}


def feather_shard() -> list[list[str]]:
    """朝下的骨羽：中轴一根灰杆，两侧交错外探一格当羽枝。

    最早是「往里啃最外一列」，结果每行只剩 2~3px，读成虚线。
    外探而不是内啃，体积才保得住，锯齿也才读得出羽枝。
    """
    g = blank()
    rows = dict(FEATHER)
    for y in range(3, 12):
        x0, x1 = rows[y]
        if y % 2 == 0:
            rows[y] = (x0 - 1, x1)
        else:
            rows[y] = (x0, x1 + 1)
    for y, (x0, x1) in rows.items():
        span(g, y, x0, x1, "b")
    outline(g, rows)
    for y in range(2, 14):
        if g[y][7] == "b":
            g[y][7] = "g"
    return g


# --------------------------------------------------------------------------
# 组件 3：护甲板框。带切角的方环，中间留洞给眼睛，四角各一颗铆钉。
# --------------------------------------------------------------------------
PLATE = {
    2: (4, 11),
    3: (2, 13),
    4: (1, 14),
    5: (1, 14),
    6: (1, 14),
    7: (1, 14),
    8: (1, 14),
    9: (1, 14),
    10: (1, 14),
    11: (1, 14),
    12: (2, 13),
    13: (4, 11),
}
HOLE = {6: (5, 10), 7: (5, 10), 8: (5, 10), 9: (5, 10)}


def plate_frame() -> list[list[str]]:
    g = blank()
    for y, (x0, x1) in PLATE.items():
        span(g, y, x0, x1, "b")
    outline(g, PLATE)
    for y, (x0, x1) in HOLE.items():
        span(g, y, x0, x1, ".")
        g[y][x0 - 1] = "D"
        g[y][x1 + 1] = "D"
    for y in (5, 10):
        span(g, y, 5, 10, "D")
    for x, y in ((3, 4), (12, 4), (3, 11), (12, 11)):
        g[y][x] = "D"
    return g


COMPONENTS = [
    ("eye_closed", eye_closed, "a closed eye carved in bone"),
    ("eye_open_slit", eye_open_slit, "a vertical slit eye glowing cold white"),
    ("eye_open_gap", eye_open_gap, "a narrow eye slit in a dark crack"),
    ("eye_open_iris", eye_open_iris, "an eye with a cold white pupil"),
    ("feather_shard", feather_shard, "a single bone feather shard"),
    ("plate_frame", plate_frame, "a bone armor plate frame"),
]


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    made, labels = [], []
    ascii_lines = []
    for name, fn, prompt in COMPONENTS:
        g = fn()
        rows = dump(g)
        for i, r in enumerate(rows):
            assert len(r) == TILE, f"{name} row {i} is {len(r)} chars"
        im = to_image(g)
        im.save(OUT / f"{name}.png")
        made.append(im)
        labels.append(name)
        ascii_lines.append(f"== {name}  ({prompt})")
        ascii_lines += [f"   {r}" for r in rows]
        ascii_lines.append("")

    # 拼一张放大的对照图，草图本身 + 名字
    cell, pad, lab = TILE * ZOOM, 12, 16
    cols = 3
    rows_n = (len(made) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * (cell + pad) + pad, rows_n * (cell + pad + lab) + pad),
                      (24, 22, 28))
    d = ImageDraw.Draw(sheet)
    font = ImageFont.load_default()
    for i, (im, name) in enumerate(zip(made, labels)):
        cx, cy = i % cols, i // cols
        x = pad + cx * (cell + pad)
        y = pad + cy * (cell + pad + lab)
        big = im.resize((cell, cell), Image.Resampling.NEAREST)
        sheet.paste(big, (x, y), big)
        d.text((x + 2, y + cell + 2), name, fill=(210, 210, 220), font=font)
    sheet.save(REVIEW / "sketch_sheet.png")
    (REVIEW / "sketch_ascii.txt").write_text("\n".join(ascii_lines), encoding="utf8")

    print("\n".join(ascii_lines))
    print(f"[done] {len(made)} sketches -> {OUT}", file=sys.stderr)
    print(f"[done] sheet -> {REVIEW / 'sketch_sheet.png'}", file=sys.stderr)


if __name__ == "__main__":
    main()
