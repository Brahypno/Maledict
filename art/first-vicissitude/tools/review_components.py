#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""把候选中央件贴回真实血条，出评审图。

判断标准不是「放大好看」，是「1x GUI 缩放下还认不认得出」。所以每行给两格：
  raw  —— 候选本身，深底放大
  ctx  —— 贴回 first_vicissitude_bar_phase_one.png 后裁中心区域，1x 与 3x

overlay 坐标系（VicissitudeBossBarOverlay 的常量）：
  BAR_OFFSET_X=1 BAR_OFFSET_Y=7 / OVERLAY_OFFSET_X=-6 OVERLAY_OFFSET_Y=-9
  -> 血条落在 overlay 画布的 u7..188, v16..20，中心 (97.5, 18)
  -> 名字带 v0..8 在 u84..112 必须透明
"""

from __future__ import annotations

import argparse
import pathlib
import re

import numpy as np
from PIL import Image, ImageDraw, ImageFont

BAR = pathlib.Path(r"C:\Users\Jingfan\IdeaProjects\Maledict\src\main\resources"
                   r"\assets\maledict\textures\gui\boss_bar\first_vicissitude_bar_phase_one.png")
OVERLAY_W, OVERLAY_H = 256, 32
BAR_IN_OVERLAY = (7, 16)          # 血条左上角在 overlay 里的坐标
BAR_W = 182                       # u7..188
BAR_H = 5                         # v16..20
CX = BAR_IN_OVERLAY[0] + (BAR_W - 1) / 2.0   # 97.5
CY = BAR_IN_OVERLAY[1] + (BAR_H - 1) / 2.0   # 18.0
CROP = (56, 2, 140, 31)           # 评审裁切窗口，含名字带下沿和血条上下
PAD = 8
LABEL_H = 14
GEN_BG = (24, 22, 28)


def rekey(img: Image.Image) -> Image.Image:
    """补抠漏网的洋红。

    pixelgen 的 key_out 判定是 |rgb-(255,0,255)| 的 L1 <= 100，
    模型把「洞」画成偏色的洋红（例如 200,0,200，L1=110）就漏出来，
    在深色底上是一块刺眼的品红。这里按色相兜底：R 和 B 都明显高于 G 就是背景。
    不会误吃 01_SPEC 的 #51436D（R-G=14, B-G=42）。
    """
    a = np.asarray(img.convert("RGBA")).astype(np.int16)
    r, g, b = a[:, :, 0], a[:, :, 1], a[:, :, 2]
    bg = ((r - g) > 60) & ((b - g) > 60)
    a[:, :, 3] = np.where(bg, 0, a[:, :, 3])
    return Image.fromarray(a.astype(np.uint8))


def font():
    return ImageFont.load_default()


def mock_bar() -> Image.Image:
    """overlay 画布：真实血条贴到 u7,v16。"""
    canvas = Image.new("RGBA", (OVERLAY_W, OVERLAY_H), (0, 0, 0, 0))
    if BAR.exists():
        base = Image.open(BAR).convert("RGBA")
        canvas.alpha_composite(base, BAR_IN_OVERLAY)
    else:
        d = ImageDraw.Draw(canvas)
        d.rectangle([BAR_IN_OVERLAY[0], BAR_IN_OVERLAY[1],
                     BAR_IN_OVERLAY[0] + BAR_W - 1, BAR_IN_OVERLAY[1] + BAR_H - 1],
                    fill=(30, 26, 36, 255))
    return canvas


def paste_centered(canvas: Image.Image, img: Image.Image) -> None:
    """把候选的几何中心对齐到血条中心。"""
    x = int(round(CX - img.width / 2.0))
    y = int(round(CY - img.height / 2.0))
    canvas.alpha_composite(img, (x, y))


def cell_raw(img: Image.Image, zoom: int, w: int, h: int) -> Image.Image:
    c = Image.new("RGB", (w, h), GEN_BG)
    big = img.resize((img.width * zoom, img.height * zoom), Image.Resampling.NEAREST)
    c.paste(big, ((w - big.width) // 2, (h - big.height) // 2), big)
    return c


def cell_ctx(img: Image.Image, zoom: int, w: int, h: int) -> Image.Image:
    canvas = mock_bar()
    paste_centered(canvas, img)
    crop = canvas.crop(CROP)
    big = crop.resize((crop.width * zoom, crop.height * zoom), Image.Resampling.NEAREST)
    c = Image.new("RGB", (w, h), GEN_BG)
    c.paste(big, ((w - big.width) // 2, (h - big.height) // 2), big)
    return c


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("root", help="候选图目录，递归收集 png")
    ap.add_argument("out")
    ap.add_argument("--title", default="")
    ap.add_argument("--raw-zoom", type=int, default=6)
    ap.add_argument("--ctx-zoom", type=int, default=3)
    ap.add_argument("--per-row", type=int, default=4)
    ap.add_argument("--prefix", default="C", help="候选编号前缀，编号全局连续，方便口头点单")
    ap.add_argument("--group-regex", default="",
                    help="从目录名里抽分组名的正则（取第一个捕获组）。"
                         "batch2 的 eye_closed_d0.45 要靠它归回 eye_closed")
    ap.add_argument("--pick", default="",
                    help="只出这几个编号，逗号分隔，例如 M14,M17,M36 —— "
                         "配合上一次生成的 .index.txt，用户报编号就能直接出终选图")
    ap.add_argument("--index", default="", help="--pick 用的 index 文件，默认取 <out>.index.txt")
    args = ap.parse_args()

    root = pathlib.Path(args.root)
    out = pathlib.Path(args.out)
    files = sorted(p for p in root.rglob("*.png") if p.name != "contact_sheet.png")
    orig_id: dict[str, str] = {}

    if args.pick:
        idx_path = pathlib.Path(args.index) if args.index else out.with_suffix(".index.txt")
        want = [t.strip() for t in args.pick.replace(";", ",").split(",") if t.strip()]
        wanted_files: dict[str, str] = {}
        for line in idx_path.read_text(encoding="utf8").splitlines():
            if line.startswith("#") or not line.strip():
                continue
            parts = line.split("\t")
            if len(parts) >= 3 and parts[0] in want:
                wanted_files[parts[0]] = parts[2]
        picked = []
        for cid in want:                       # 按用户报的顺序排
            fname = wanted_files.get(cid)
            if fname is None:
                print(f"[warn] {cid} not in {idx_path.name}")
                continue
            picked += [p for p in files if p.name == fname]
            orig_id[fname] = cid
        files = picked

    if not files:
        raise SystemExit(f"no png under {root}")

    # 按组件分组：默认用父目录名；给了 --group-regex 就按正则把组件名抽回来
    rx = re.compile(args.group_regex) if args.group_regex else None
    groups: dict[str, list[pathlib.Path]] = {}
    for p in files:
        key = p.parent.name if p.parent != root else p.stem.split("_seed")[0]
        if rx:
            m = rx.search(key)
            if m:
                key = m.group(1)
        groups.setdefault(key, []).append(p)

    zoom = args.raw_zoom
    ctx_zoom = args.ctx_zoom
    cw = max(max(i.width for i in (Image.open(f) for f in v)) * zoom for v in groups.values()) + PAD * 2
    ctx_crop_w = (CROP[2] - CROP[0]) * ctx_zoom
    ctx_crop_h = (CROP[3] - CROP[1]) * ctx_zoom
    cw = max(cw, ctx_crop_w) + PAD * 2
    raw_h = max(max(i.height for i in (Image.open(f) for f in v)) * zoom for v in groups.values())
    row_h = raw_h + ctx_crop_h + LABEL_H * 2 + PAD * 3

    total_w = args.per_row * cw
    total_h = sum(
        ((len(v) + args.per_row - 1) // args.per_row) * row_h + LABEL_H + PAD
        for v in groups.values()
    ) + PAD + (LABEL_H if args.title else 0)
    sheet = Image.new("RGB", (total_w, total_h), (16, 15, 18))
    d = ImageDraw.Draw(sheet)
    f = font()

    y = PAD + (LABEL_H if args.title else 0)
    idx = 0
    index_lines = []
    for name, paths in groups.items():
        d.text((PAD, y), f"{name}  ({len(paths)})", fill=(230, 230, 240), font=f)
        y += LABEL_H
        for i, p in enumerate(paths):
            img = rekey(Image.open(p).convert("RGBA"))
            idx += 1
            # --pick 时保留用户认得的原编号，别重新编号
            cid = orig_id.get(p.name) or f"{args.prefix}{idx:02d}"
            index_lines.append(f"{cid}\t{name}\t{p.name}")
            col, row = i % args.per_row, i // args.per_row
            x = col * cw + PAD // 2
            yy = y + row * row_h
            sheet.paste(cell_raw(img, zoom, cw - PAD, raw_h), (x, yy))
            sheet.paste(cell_ctx(img, ctx_zoom, cw - PAD, ctx_crop_h), (x, yy + raw_h + LABEL_H))
            d.text((x + 3, yy + raw_h + 2), cid, fill=(255, 214, 120), font=f)
            d.text((x + 34, yy + raw_h + 2), p.stem[:28], fill=(150, 150, 165), font=f)
            d.text((x + 3, yy + raw_h + LABEL_H + ctx_crop_h + 1), f"{img.width}x{img.height} px",
                   fill=(120, 120, 135), font=f)
        y += ((len(paths) + args.per_row - 1) // args.per_row) * row_h + PAD

    if args.title:
        d.text((PAD, 2), args.title, fill=(255, 255, 255), font=f)
    out.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out)
    idx_path = out.with_suffix(".index.txt")
    idx_path.write_text(f"# {args.title}\n" + "\n".join(index_lines) + "\n", encoding="utf8")
    print(f"[done] {len(files)} candidates, {len(groups)} groups -> {out}")
    print(f"[done] index -> {idx_path}")


if __name__ == "__main__":
    main()
