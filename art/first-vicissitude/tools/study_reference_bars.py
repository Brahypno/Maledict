#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""把参考项目的血条贴图摊开看清楚 —— 重点是「中央件怎么和导轨连成一体」。

之前三批生成全是浮在血条上的独立图标，不是血条中央件。
要搞懂差在哪，得先看参考里中央那一块是怎么画的、占多少、怎么接导轨。

输出：build/first-vicissitude-review/reference/study.png
"""

from __future__ import annotations

import pathlib

from PIL import Image, ImageDraw, ImageFont

REF = pathlib.Path(r"C:\Users\Jingfan\Downloads\_maledict_ref")
OUT = pathlib.Path(r"C:\Users\Jingfan\IdeaProjects\Maledict\build"
                   r"\first-vicissitude-review\reference")

CAT = REF / "cataclysm/src/main/resources/assets/cataclysm/textures/gui/boss_bar"
LM = (REF / "Legendary-Monsters-1.20.1-master/src/main/resources/assets"
      r"\legendary_monsters/textures/gui/boss_bar")

BOSSES = [
    ("cataclysm/maledictus", CAT / "maledictus_bar_base.png", CAT / "maledictus_bar_overlay.png"),
    ("cataclysm/maledictus_rage", CAT / "maledictus_rage_bar_base.png", CAT / "maledictus_rage_bar_overlay.png"),
    ("cataclysm/harbinger", CAT / "harbinger_bar_base.png", CAT / "harbinger_bar_overlay.png"),
    ("cataclysm/ender_guardian", CAT / "ender_guardian_bar_base.png", CAT / "ender_guardian_bar_overlay.png"),
    ("cataclysm/scylla", CAT / "scylla_bar_base.png", CAT / "scylla_bar_overlay.png"),
    ("cataclysm/leviathan", CAT / "leviathan_bar_base.png", CAT / "leviathan_bar_overlay.png"),
    ("cataclysm/ignis", CAT / "ignis_bar_base.png", CAT / "ignis_bar_overlay.png"),
    ("cataclysm/monstrosity", CAT / "monstrosity_bar_base.png", CAT / "monstrosity_bar_overlay.png"),
    ("LM/possessed", LM / "possessed_bar_base.png", LM / "possessed_bar_overlay_0.png"),
    ("LM/the_warped_one", LM / "the_warped_one_bar_base.png", LM / "the_warped_one_bar_overlay.png"),
    ("LM/cloud_golem", LM / "cloud_golem_bar_base.png", LM / "cloud_golem_bar_overlay.png"),
]

PAD = 10
LABEL = 14
BG = (18, 17, 21)


def load(p: pathlib.Path) -> Image.Image | None:
    return Image.open(p).convert("RGBA") if p.exists() else None


def zoom(im: Image.Image, z: int) -> Image.Image:
    return im.resize((im.width * z, im.height * z), Image.Resampling.NEAREST)


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    f = ImageFont.load_default()
    rows = []
    for name, bp, op in BOSSES:
        b, o = load(bp), load(op)
        if b is None and o is None:
            continue
        rows.append((name, b, o))
        print(f"{name:28s} base={None if b is None else b.size} overlay={None if o is None else o.size}")

    # 每行：base 3x | overlay 3x | overlay 中央 6x
    zb, zo, zc = 3, 3, 6
    any_b = next((b for _, b, _ in rows if b), None)
    any_o = next((o for _, _, o in rows if o), None)
    wb = (any_b.width if any_b else 0) * zb
    wo = (any_o.width if any_o else 0) * zo
    center = (60, 0, 140, any_o.height if any_o else 32)
    wc = (center[2] - center[0]) * zc
    rowh = max((any_o.height if any_o else 32) * zo, (any_b.height if any_b else 16) * zb) + LABEL
    total_w = PAD * 4 + wb + wo + wc
    total_h = PAD + len(rows) * (rowh + PAD) + LABEL
    sheet = Image.new("RGB", (total_w, total_h), BG)
    d = ImageDraw.Draw(sheet)

    d.text((PAD, 2), "base 3x            |            overlay 3x            |       overlay center 6x",
           fill=(255, 255, 255), font=f)
    y = PAD + LABEL
    for name, b, o in rows:
        x = PAD
        d.text((x, y), name, fill=(255, 214, 120), font=f)
        yy = y + LABEL
        for im, z, w in ((b, zb, wb), (o, zo, wo)):
            if im is not None:
                big = zoom(im, z)
                cell = Image.new("RGB", (w, big.height), BG)
                cell.paste(big, (0, 0), big)
                sheet.paste(cell, (x, yy))
            x += w + PAD
        if o is not None:
            crop = o.crop((center[0], 0, min(center[2], o.width), o.height))
            big = zoom(crop, zc)
            cell = Image.new("RGB", (wc, big.height), BG)
            cell.paste(big, (0, 0), big)
            sheet.paste(cell, (x, yy))
        y += rowh + PAD

    sheet.save(OUT / "study.png")
    print(f"[done] -> {OUT / 'study.png'}  ({sheet.width}x{sheet.height})")


if __name__ == "__main__":
    main()
