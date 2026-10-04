"""Package the original crystal/pauldron boss-bar artwork for Minecraft.

Masters live in art/first-vicissitude/boss-bar, never in src/generated.
The frame uses nearest-neighbour sampling and a fixed pixel-art palette.
The base has two 182x5 rows in a 256x16 canvas; the frame is 256x32.
Geometry below must match VicissitudeBossBarOverlay.java.
"""
from pathlib import Path
import json
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[3]
SOURCE = ROOT / "art/first-vicissitude/boss-bar"
OUT_DIR = ROOT / "src/main/resources/assets/maledict/textures/gui/boss_bar"
REVIEW = ROOT / "build/first-vicissitude-review/boss-bar"

BAR_WIDTH, BAR_HEIGHT = 182, 5
BASE_TEX = (256, 16)
FRAME_SIZE = (256, 32)
ART_SIZE, ART_ORIGIN = (192, 30), (32, 1)
FRAME_OFFSET, BAR_OFFSET = (-37, 0), (0, 19)
BAR_IN_FRAME = (37, 19)
# Name occupies y-9..y; next name must clear the current frame and a 2px gap.
INCREMENT = FRAME_SIZE[1] + 11

# A fixed, restrained palette removes antialiasing noise in the raster masters.
# Transparency stays transparency; no opaque background is introduced.
PALETTE = (
    (20, 8, 32), (30, 17, 44), (44, 24, 63), (56, 33, 82),
    (70, 42, 101), (88, 51, 126), (107, 63, 145), (129, 43, 171),
    (146, 83, 174), (164, 111, 188), (189, 103, 219),
    (65, 54, 79), (86, 73, 103), (108, 94, 125),
    (133, 121, 151), (156, 143, 173), (179, 167, 192),
    (202, 190, 212), (230, 223, 235), (245, 239, 242),
    (111, 79, 63), (158, 117, 71), (201, 157, 92), (225, 182, 108),
)


def make_frame(phase):
    master = Image.open(SOURCE / f"{phase}-master.png").convert("RGBA")
    # Use the visible silhouette, ignoring the generator's faint alpha speckles.
    alpha = master.getchannel("A").point(lambda a: 255 if a >= 128 else 0)
    box = alpha.getbbox()
    master.putalpha(alpha)
    sampled = master.crop(box).resize(ART_SIZE, Image.Resampling.NEAREST)
    frame = Image.new("RGBA", FRAME_SIZE)
    cache = {}
    for y in range(sampled.height):
        for x in range(sampled.width):
            r, g, b, a = sampled.getpixel((x, y))
            if not a:
                continue
            rgb = (r, g, b)
            if rgb not in cache:
                cache[rgb] = min(PALETTE, key=lambda c:
                    2 * (r-c[0])**2 + 3 * (g-c[1])**2 + (b-c[2])**2)
            frame.putpixel((x + ART_ORIGIN[0], y + ART_ORIGIN[1]),
                           cache[rgb] + (255,))
    return frame


def make_base():
    image = Image.new("RGBA", BASE_TEX)
    draw = ImageDraw.Draw(image)
    empty = ((33, 15, 46), (44, 24, 63), (56, 33, 82),
             (44, 24, 63), (20, 8, 32))
    filled = ((88, 51, 126), (230, 191, 241), (189, 103, 219),
              (129, 43, 171), (56, 33, 82))
    for offset, colors in ((0, empty), (BAR_HEIGHT, filled)):
        for row, color in enumerate(colors):
            draw.line((0, offset + row, BAR_WIDTH-1, offset + row), fill=color)
    return image


def composite(base, frame, fraction, phase_two):
    image = Image.new("RGBA", FRAME_SIZE)
    # Phase one is a feather shroud: no health information shows through its gaps.
    # The server-side health/progress and the phase switch remain unchanged.
    if phase_two:
        image.alpha_composite(base.crop((0, 0, BAR_WIDTH, BAR_HEIGHT)), BAR_IN_FRAME)
        filled = int(BAR_WIDTH * fraction)
        if filled:
            image.alpha_composite(
                base.crop((0, BAR_HEIGHT, filled, BAR_HEIGHT * 2)), BAR_IN_FRAME)
    image.alpha_composite(frame)
    return image


def make_preview(base, frames):
    width, scale = 288, 4
    image = Image.new("RGB", (width, 294), (18, 15, 23))
    draw = ImageDraw.Draw(image)
    for stage, phase in enumerate(("phase-one", "phase-two")):
        for idx, fraction in enumerate((1.0, 0.55, 0.18)):
            y = 5 + (stage * 3 + idx) * 48
            label = f"PHASE {stage+1}  |  {'VEILED' if stage == 0 else str(int(fraction*100))+'%'}"
            draw.text((16, y), label, fill=(202, 190, 212), font=ImageFont.load_default(size=9))
            sprite = composite(base, frames[phase], fraction, stage == 1)
            image.paste(sprite, (16, y+12), sprite)
    image.resize((width*scale, image.height*scale), Image.Resampling.NEAREST).save(
        ROOT / "build/first-vicissitude-review/boss_bar_preview.png")


def make_overview(base, frames):
    image = Image.new("RGB", (288, 126), (18, 15, 23))
    draw = ImageDraw.Draw(image)
    for stage, phase in enumerate(("phase-one", "phase-two")):
        y = 7 + stage * 60
        draw.text((16, y), f"PHASE {stage+1}", fill=(202, 190, 212),
                  font=ImageFont.load_default(size=9))
        sprite = composite(base, frames[phase], 0.72, stage == 1)
        image.paste(sprite, (16, y+14), sprite)
    image.resize((1152, 504), Image.Resampling.NEAREST).save(REVIEW / "overview.png")


def make_stacked_preview(base, frames):
    # Same event coordinates, increment and name placement as the Java renderer.
    image = Image.new("RGB", (320, 120), (38, 35, 44))
    draw = ImageDraw.Draw(image)
    event_x, event_y = 160 - BAR_WIDTH // 2, 12
    font = ImageFont.load_default(size=9)
    for idx, phase in enumerate(("phase-one", "phase-two")):
        y = event_y + idx * INCREMENT
        name = "FIRST VICISSITUDE"
        draw.text((160 - draw.textlength(name, font=font) / 2, y - 9),
                  name, font=font, fill=(230, 237, 245))
        sprite = composite(base, frames[phase], 0.55, idx == 1)
        image.paste(sprite, (event_x+FRAME_OFFSET[0], y+FRAME_OFFSET[1]), sprite)
    image.resize((960, 360), Image.Resampling.NEAREST).save(REVIEW / "stacked-layout.png")


def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    REVIEW.mkdir(parents=True, exist_ok=True)
    base = make_base()
    frames = {}
    for phase, suffix in (("phase-one", "phase_one"), ("phase-two", "phase_two")):
        frame = make_frame(phase)
        frames[phase] = frame
        base.save(OUT_DIR / f"first_vicissitude_bar_{suffix}.png")
        frame.save(OUT_DIR / f"first_vicissitude_bar_{suffix}_frame.png")
        frame.resize((1024, 128), Image.Resampling.NEAREST).save(REVIEW / f"{phase}-frame-4x.png")
    make_preview(base, frames)
    make_overview(base, frames)
    make_stacked_preview(base, frames)
    phase_two = frames["phase-two"]
    # Measure actual alpha coverage in the fill band, excluding the emblem/end caps.
    clear, count = 0, 0
    for x in (*range(51, 100), *range(157, 206)):
        for y in range(BAR_IN_FRAME[1], BAR_IN_FRAME[1]+BAR_HEIGHT):
            clear += phase_two.getpixel((x, y))[3] == 0
            count += 1
    report = dict(frame_size=FRAME_SIZE, art_size=ART_SIZE, base_size=BASE_TEX,
                  bar_size=(BAR_WIDTH, BAR_HEIGHT), bar_in_frame=BAR_IN_FRAME,
                  frame_offset=FRAME_OFFSET, bar_offset=BAR_OFFSET, increment=INCREMENT,
                  phase_one_health_visible=False, phase_two_channel_clear_pixels=clear,
                  phase_two_channel_sampled_pixels=count,
                  phase_two_clear_channel_rows=sum(
                      all(phase_two.getpixel((x, y))[3] == 0
                          for x in (*range(51, 100), *range(157, 206)))
                      for y in range(BAR_IN_FRAME[1], BAR_IN_FRAME[1]+BAR_HEIGHT)),
                  palette_colors=len(PALETTE), alpha_values=[0, 255],
                  native_frames={k: v.getbbox() for k, v in frames.items()},
                  in_game_verified=False)
    (REVIEW / "layout.json").write_text(json.dumps(report, indent=2)+"\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
