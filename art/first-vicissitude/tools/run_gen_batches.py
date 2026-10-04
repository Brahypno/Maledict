#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""first-vicissitude 中央件：pixelmon 批次定义。

批次参数全部固化在这里，而不是靠记忆敲命令行 —— 项目规矩是美术产物必须可复现。

    python run_gen_batches.py --batch 1     # 广度：6 组件 x 6 张（36 张 / 738s）
    python run_gen_batches.py --batch 2     # 保轮廓：草图当 --init，扫 denoise

踩过的坑，都写在这里免得再踩：
  * --batch 是按逗号切分的，所以 prompt 里绝对不能有逗号。
  * 中文 Windows 控制台是 GBK，pixelmon 会 print emoji，必须 PYTHONIOENCODING=utf-8，
    否则跑到最后一步 UnicodeEncodeError 崩掉。
  * SDXL 见到 "eye" 当主语就想画 Q 版生物脸（batch1 的 M01~M12 全是小人）。
    batch 2 用 --init 把轮廓钉死，并用 negative 明确排除 face / two eyes / creature。
  * 别用 .ps1 装这个：UTF-8 无 BOM 加中文注释会被 PowerShell 判错编码，
    解析器报的行号和文件实际行号能差 10 行，直接 Missing closing '}'。
"""

from __future__ import annotations

import argparse
import os
import pathlib
import subprocess
import sys

PY = r"C:\Users\Jingfan\ComfyUI_windows_portable\python_embeded\python.exe"
PIXELMON = r"C:\Users\Jingfan\pixelmon\pixelmon.py"
REVIEW = pathlib.Path(r"C:\Users\Jingfan\IdeaProjects\Maledict\build"
                      r"\first-vicissitude-review\components")

# 01_SPEC 五色。第三批上色时才用，前两批不锁色 —— 先挑形状，颜色后面细调。
HEX = "#19151F #49404F #B8B8C4 #E6EDF5 #51436D"

NO_FACE = ("face, two eyes, creature, skull, portrait, head, character, "
           "3d render, realistic, photograph, blurry, smooth gradient, "
           "antialiased, text, watermark")

# 组件 -> (img2img 底图, prompt)。prompt 描述的是材质和光，形状由 --init 决定。
COMPONENTS = [
    ("eye_closed",    "a carved bone eye emblem"),
    ("eye_open_slit", "a glowing white eye slit emblem"),
    ("eye_open_gap",  "a dark eye socket emblem"),
    ("eye_open_iris", "a violet iris eye emblem"),
    ("feather_shard", "a bone feather ornament"),
    ("plate_frame",   "a bone armor plate"),
]


def run(args: list[str]) -> int:
    env = dict(os.environ, PYTHONIOENCODING="utf-8", PYTHONUTF8="1")
    print("[run] " + " ".join(args[1:])[:160], flush=True)
    rc = subprocess.run([PY, PIXELMON] + args, env=env).returncode
    if rc != 0:
        print(f"[warn] pixelmon exit {rc}", flush=True)
    return rc


def batch1() -> None:
    subjects = ",".join([
        "a closed eye carved in bone armor plate game sprite",
        "a vertical slit eye glowing cold white between two bone shells game sprite",
        "a narrow eye slit in a dark crack with cold white pupil game sprite",
        "an eye with cold white pupil and dark violet iris game sprite",
        "a single bone feather shard game sprite",
        "a hollow bone armor plate frame game sprite",
    ])
    run(["--batch", subjects, "-n", "6", "--size", "64", "--transparent", "--fast",
         "--no-open", "--create-dirs", "--output-to", str(REVIEW / "pixelmon" / "batch1")])


def batch2(denoises: list[float]) -> None:
    """轮廓是我的，质感是它的：--init 拿草图，denoise 决定改写程度。

    目录名带 _d<denoise>，评审图用 --group-regex 把组件名抽回来分组。
    """
    for d in denoises:
        for name, prompt in COMPONENTS:
            init = REVIEW / "init" / f"{name}_1024.png"
            if not init.exists():
                print(f"[skip] missing init {init}", file=sys.stderr)
                continue
            run([prompt, "--init", str(init), "--denoise", str(d), "-n", "1",
                 "--size", "64", "--transparent", "--fast", "--negative", NO_FACE,
                 "--no-open", "--create-dirs",
                 "--output-to", str(REVIEW / "pixelmon" / "batch2" / f"{name}_d{d}")])


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--batch", type=int, required=True, choices=(1, 2))
    ap.add_argument("--denoise", type=float, action="append", default=None)
    args = ap.parse_args()
    if args.batch == 1:
        batch1()
    else:
        batch2(args.denoise or [0.45, 0.65])


if __name__ == "__main__":
    main()
