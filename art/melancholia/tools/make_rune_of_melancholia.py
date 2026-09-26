"""Generate the placeholder texture for the Rune of Melancholia.

The rune is worked from a `malum:tainted_rock_tablet` plus umbral spirit, so it
belongs to Malum's tainted-rock rune tier -- the eight runes whose runeworking
recipes consume a tainted rock tablet (`rune_of_culling`, `rune_of_dexterity`,
`rune_of_fervor`, `rune_of_reinforcement`, ...). Its tablet therefore borrows
that tier's own stone colours, sampled straight out of the Malum jar.

Only the colours come from Malum. The silhouette and the sigil shape stay this
mod's own: the alpha mask is carried over from `rune_of_rotten_bone.png`
byte for byte, and the sigil keeps the umbral violet ramp.

The source image is read as an exact 15-entry palette, so the alpha mask and
every shading step of the original survive the recolor unchanged. Nothing is
resampled, blurred or blended, which is what keeps the 16x16 pixel art crisp.

Run from anywhere:

    python art/melancholia/tools/make_rune_of_melancholia.py

Add `--preview <path>` to also write a nearest-neighbour upscale for eyeballing
(that preview is a review artefact and is not shipped in the mod jar).
"""
from __future__ import annotations

import argparse
from pathlib import Path

from PIL import Image

REPO_ROOT = Path(__file__).resolve().parents[3]
SOURCE = REPO_ROOT / "src/main/resources/assets/maledict/textures/item/runes/rune_of_rotten_bone.png"
TARGET = REPO_ROOT / "src/main/resources/assets/maledict/textures/item/runes/rune_of_melancholia.png"

# Source RGB (the 15 colours of rune_of_rotten_bone.png) -> output RGB.
#
# Two ordered ramps, darkest first. Source and target are both monotonic in
# luma, so the original shading order survives step for step.
#
# TABLET -- the tainted-rock tier's own stone colours. Intersecting the
# palettes of all eight tainted-rock runes leaves exactly eleven shared
# colours, which is that tier's grey-violet stone ramp plus its rim:
#
#     #28232F #413A49 #4B4552 #55505A #635A69 #6F6774
#     #797081 #857A8C #918496 #A091AB #B1A1BF
#
# This mod's rune silhouette only has six tablet steps, so six of those eleven
# are picked to sit at the same relative positions the source's own six steps
# occupied inside its range (0.00 / 0.31 / 0.47 / 0.69 / 0.89 / 1.00).
#
# The rim needs no remapping at all: #28232F occupies the exact same fifteen
# pixels in this mod's rune as it does in Malum's tainted-rock runes, so the
# outline comes out identical to the rest of the tier by construction.
TABLET_RAMP = {
    0x28232F: 0x28232F,  # rim, shared by every tainted-rock rune
    0x705B7C: 0x55505A,  # body, dark
    0x947AA3: 0x635A69,  # body
    0xC9A1D0: 0x857A8C,  # body, lit
    0xECCEF6: 0xA091AB,  # light
    0xFFEAF7: 0xB1A1BF,  # top highlight
}
# SIGIL -- the umbral violet, anchored on the colours Malum's own Umbral Spirit
# assets use (#050916 / #190D27 / #270221 for the near-black body, #382864 /
# #343D6F / #66376A for its cold violet highlights).
#
# The ramp is lifted well clear of the tablet. The tier's stone is itself a
# grey-violet, so an umbral sigil shares its hue and has to separate on
# saturation and value alone; the first attempt, tuned against a near-black
# tablet, sank into tainted rock and read as a dull hollow. The tier's own
# runes settle this the same way -- rune_of_culling paints a vivid
# #9200CA / #CA32D4 / #E857DE glyph across stone of matching luma -- so the
# violet is pushed up until it glows: darkest step at luma 56, the main glyph
# body at 105 against a stone body of 83-127, and a near-white spark on top.
SIGIL_RAMP = {
    0x360023: 0x4A1F86,  # dark core of the sigil
    0x561026: 0x57269C,
    0x72132D: 0x6730BE,
    0xA3112B: 0x7F45E4,
    0xCE1844: 0x9658F2,
    0xFF0022: 0xAC72FB,
    0xFF7A31: 0xC193FF,
    0xFF9E46: 0xD8B6FF,
    0xFFD08A: 0xF0E2FF,  # brightest spark
}
COLOR_MAP = {**TABLET_RAMP, **SIGIL_RAMP}


def rgb(hex_color: int) -> tuple[int, int, int]:
    return ((hex_color >> 16) & 0xFF, (hex_color >> 8) & 0xFF, hex_color & 0xFF)


# Pillow hands back (r, g, b) tuples, so look the palette up in that form.
RGB_MAP = {rgb(src): rgb(dst) for src, dst in COLOR_MAP.items()}


def build() -> Image.Image:
    source = Image.open(SOURCE).convert("RGBA")
    px = source.load()

    unmapped = {
        px[x, y][:3]
        for y in range(source.height)
        for x in range(source.width)
        if px[x, y][3] > 0 and px[x, y][:3] not in RGB_MAP
    }
    if unmapped:
        raise SystemExit(
            "unmapped source colours, extend COLOR_MAP: "
            + ", ".join(f"#{r:02X}{g:02X}{b:02X}" for r, g, b in sorted(unmapped))
        )

    out = Image.new("RGBA", source.size, (0, 0, 0, 0))
    dst = out.load()
    for y in range(source.height):
        for x in range(source.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            nr, ng, nb = RGB_MAP[(r, g, b)]
            # Alpha is carried over verbatim: the outline is the shared rune shape.
            dst[x, y] = (nr, ng, nb, a)
    return out


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--preview", type=Path, default=None,
                        help="optional path for a nearest-neighbour upscale preview")
    parser.add_argument("--scale", type=int, default=16, help="preview scale factor")
    args = parser.parse_args()

    source = Image.open(SOURCE).convert("RGBA")
    out = build()

    assert out.size == (16, 16), out.size
    assert out.getchannel("A").tobytes() == source.getchannel("A").tobytes(), \
        "alpha silhouette drifted from the source rune"

    TARGET.parent.mkdir(parents=True, exist_ok=True)
    out.save(TARGET)
    print(f"wrote {TARGET.relative_to(REPO_ROOT)}  {out.size[0]}x{out.size[1]}")

    if args.preview:
        args.preview.parent.mkdir(parents=True, exist_ok=True)
        out.resize((out.width * args.scale, out.height * args.scale), Image.NEAREST).save(args.preview)
        print(f"wrote preview {args.preview}")


if __name__ == "__main__":
    main()
