#!/usr/bin/env python3
"""中央件的几何草模（probe）—— 验证 §2.8 的坐标，以及"胸洞里的眼"在真实尺寸下读不读得出来。

规格见 docs/design/first-vicissitude/04_AUDIO_AND_BOSS_BAR.md §2.8。
本脚本只写 build/ 下的检查图，**不碰** src/main/resources 里的四张贴图。

坐标（overlay 画布 256x32，与 VicissitudeBossBarOverlay.java 的常量一致）：
    血条窗口   u 7..188, v 16..20      中心 = 97 与 98 之间的缝
    两条轨     v 14 / v 21
    名字带     v 0..8（u 84..112 必须透明）
    可用竖带   v 9..31

结构：**瞳孔 = 两片壳之间的缝。**
    合 = 两片壳在中线相接，留下 1px 冷骨白折痕
    开 = 两片壳左右分开，分缝变阴影，空腔里只剩一根冷白竖瞳（偏心 1px）

五级明度（01_SPEC §1 的色板刚好够用，不需要调色）：
    深黑紫 #19151F 空腔 < 灰紫 #49404F 壳片 < 暗紫能量 #51436D 甲片环
    < 冷骨白 #B8B8C4 倒角与折痕 < 冷白 #E6EDF5 瞳孔

注意：所有多边形必须是**简单闭合路径**。半片壳的走法是
    内缘上端 → 上弧（中心→眼角）→ 下弧（眼角→中心）→ 内缘下端 → 闭合
中间不能插跳点，否则自交，PIL 填充会在中线留出一条横带。
"""
import math
from pathlib import Path

from PIL import Image, ImageDraw

# ---------------------------------------------------------------- 规格常量
W, H = 256, 32
SEAM = 97.5                  # 血条中心：97 与 98 之间那条缝
BAR = (7, 16, 189, 21)       # u0, v0, u1(不含), v1(不含)
RAIL_TOP_Y, RAIL_BOT_Y = 14, 21
NAME_BAND = (0, 9, 84, 113)  # v0, v1, u0, u1

DARK = (0x19, 0x15, 0x1F, 255)
GREY = (0x49, 0x40, 0x4F, 255)
BONE = (0xB8, 0xB8, 0xC4, 255)
WHITE = (0xE6, 0xED, 0xF5, 255)
ENERGY = (0x51, 0x43, 0x6D, 255)

# ---------------------------------------------------------------- 可调旋钮
EYE_CY = 19        # 眼轴；血条 v16..20 的中心是 18，往上顶会被名字带挡住
LENS_HW = 11       # 透镜半宽
LENS_HH = 6        # 透镜半高
LID_GAP = 3        # 壳内缘落在 97-GAP / 98+GAP（"开"时）
PUPIL_W = 3        # 竖瞳宽度（px）
PUPIL_HH = 4       # 竖瞳半高
RIM = 2            # 洞沿厚度
PLATE_TOP = 2      # 眉甲厚度（上方只有 v9..15，得薄）
PLATE_BOT = 4      # 颊甲厚度（下方到 v31，可以厚）
TIP = 4            # 眼角楔伸出


def ellipse_hh(dx: float, hw: float, hh: float) -> float:
    return hh * math.sqrt(max(0.0, 1.0 - (dx / hw) ** 2))


def arc(hw, hh, steps=40):
    """上弧，从左眼角到右眼角。"""
    return [(SEAM + dx, EYE_CY - ellipse_hh(dx, hw, hh))
            for dx in (-hw + 2 * hw * i / steps for i in range(steps + 1))]


def lens_poly(hw, hh):
    top = arc(hw, hh)
    return top + [(x, 2 * EYE_CY - y) for x, y in reversed(top)]


def half_lens(side: int, gap: float, hw, hh):
    """半片壳：内缘是竖直线（97-gap 或 98+gap），外侧是透镜弧。"""
    edge = (97.0 - gap) if side < 0 else (98.0 + gap)
    steps = 20
    xs = [SEAM + side * hw * i / steps for i in range(steps + 1)]      # 中心 → 眼角
    upper = [(x, EYE_CY - ellipse_hh(x - SEAM, hw, hh)) for x in xs]
    lower = [(x, 2 * EYE_CY - y) for x, y in reversed(upper)]          # 眼角 → 中心
    return [(edge, upper[0][1])] + upper[1:] + lower[:-1] + [(edge, lower[-1][1])]


def band_outside(hw, hh, t, above=True):
    """贴着弧外侧的一条带（眉甲 / 颊甲）：外弧去，内弧回，绕向一致。"""
    inner = arc(hw, hh)
    outer = [(x, y - t) for x, y in inner] if above else [(x, y + t) for x, y in inner]
    return outer + list(reversed(inner))


def draw_piece(d: ImageDraw.ImageDraw, open_state: bool, ecc_h: int, ecc_v: int):
    def poly(pts, fill, outline=None):
        d.polygon([(float(x), float(y)) for x, y in pts], fill=fill, outline=outline)

    gap = LID_GAP if open_state else 0.0

    poly(lens_poly(LENS_HW + RIM, LENS_HH + RIM), ENERGY)   # 甲片环 / 洞沿
    poly(lens_poly(LENS_HW, LENS_HH), DARK)                 # 空腔（不透明，否则满血亮填充会冲掉"洞"）

    if open_state:                                          # 竖瞳：整数像素，整体带偏心
        px0 = 97 - ecc_h
        y0, y1 = EYE_CY - PUPIL_HH + ecc_v, EYE_CY + PUPIL_HH + ecc_v
        d.rectangle([px0, y0, px0 + PUPIL_W - 1, y1], fill=WHITE)

    for side in (-1, 1):                                    # 两片壳
        poly(half_lens(side, gap, LENS_HW, LENS_HH), GREY, DARK)
        edge = (97.0 - gap) if side < 0 else (98.0 + gap)
        # 合：折痕是冷骨白（读成"闭着的睑"）；开：分缝是阴影，别和瞳孔抢白
        d.line([(edge, EYE_CY - LENS_HH + 1), (edge, EYE_CY + LENS_HH - 1)],
               fill=BONE if not open_state else DARK)
    if not open_state:                                      # 合：中线一条 1px 暗缝
        d.line([(97, EYE_CY - LENS_HH), (97, EYE_CY + LENS_HH)], fill=DARK)

    for above, t in ((True, PLATE_TOP), (False, PLATE_BOT)):   # 眉甲 / 颊甲
        poly(band_outside(LENS_HW + RIM - 1, LENS_HH + RIM - 1, t, above=above), ENERGY, DARK)
        edge_arc = arc(LENS_HW + RIM - 1, LENS_HH + RIM - 1)
        d.line([(x, y - t) if above else (x, y + t) for x, y in edge_arc], fill=BONE)
    for side in (-1, 1):                                       # 眼角楔
        tx = SEAM + side * (LENS_HW + RIM)
        poly([(tx, EYE_CY - 2), (tx + side * TIP, EYE_CY - 1),
              (tx + side * TIP, EYE_CY + 1), (tx, EYE_CY + 2)], ENERGY, DARK)


def make_overlay(open_state: bool, ecc_h=0, ecc_v=0, with_context=True) -> Image.Image:
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    if with_context:
        d.line([(BAR[0], RAIL_TOP_Y), (BAR[2] - 1, RAIL_TOP_Y)], fill=GREY)
        d.line([(BAR[0], RAIL_BOT_Y), (BAR[2] - 1, RAIL_BOT_Y)], fill=GREY)
        d.rectangle([BAR[0], BAR[1], BAR[0] + 100, BAR[3] - 1], fill=ENERGY)
    draw_piece(d, open_state, ecc_h, ecc_v)
    return im


VARIANTS = [("PHASE 1  closed", False, 0, 0),
            ("PHASE 2  open / pupil 1px left", True, 1, 0),
            ("PHASE 2  open / pupil 1px down", True, 0, 1)]


def review_sheet() -> Image.Image:
    Z, pad = 8, 8
    crop = (66, 0, 130, 32)
    cw = crop[2] - crop[0]
    strips = [1, 2, 3]
    sheet = Image.new("RGBA", (cw * Z + pad * 2,
                               pad + len(VARIANTS) * (32 * Z + 22)
                               + sum(40 * f + 10 for f in strips) + pad),
                      (24, 20, 28, 255))
    y = pad
    for _, open_state, eh, ev in VARIANTS:
        sheet.alpha_composite(
            make_overlay(open_state, eh, ev).crop(crop).resize((cw * Z, 32 * Z), Image.NEAREST), (pad, y))
        y += 32 * Z + 22
    for f in strips:
        strip = Image.new("RGBA", (200, 40), (24, 20, 28, 255))
        sd = ImageDraw.Draw(strip)
        sd.rectangle([9, 18, 9 + 182, 23], fill=DARK)
        sd.rectangle([9, 18, 9 + 100, 23], fill=ENERGY)
        strip.alpha_composite(make_overlay(True, 1, 0, with_context=False).crop(crop), (66, 0))
        sheet.alpha_composite(strip.resize((200 * f, 40 * f), Image.NEAREST), (pad, y))
        y += 40 * f + 10
    return sheet


def checks():
    for label, open_state, eh, ev in VARIANTS:
        clean = make_overlay(open_state, eh, ev, with_context=False)
        v0, v1, u0, u1 = NAME_BAND
        band = clean.getchannel("A").crop((u0, v0, u1, v1)).getextrema()
        bbox = clean.getbbox()
        print(f"{label:32s} name-band clear={band == (0, 0)}  bbox={bbox}  inside v9..31={bbox[1] >= 9}")


if __name__ == "__main__":
    out = Path("build/first-vicissitude-review/center_eye_probe.png")
    out.parent.mkdir(parents=True, exist_ok=True)
    review_sheet().save(out)
    checks()
    print("wrote", out)
