"""Package pixel-refined artwork at 2 texels per GUI pixel, without enlarging the HUD.

Masters live in art/first-vicissitude/boss-bar, never in src/generated.
The base retains two 182x5 rows in a 256x16 canvas. The decorative frame is
512x64, drawn into 256x32 GUI pixels by VicissitudeBossBarOverlay.java.
"""
from pathlib import Path
import json
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[3]
SOURCE = ROOT / "art/first-vicissitude/boss-bar"
OUT_DIR = ROOT / "src/main/resources/assets/maledict/textures/gui/boss_bar"
REVIEW = ROOT / "build/first-vicissitude-review/boss-bar"

SCALE = 2
PREVIEW_SCALE = 4
BAR_WIDTH, BAR_HEIGHT = 182, 5
BASE_TEX = (256, 16)
GUI_FRAME_SIZE = (256, 32)
FRAME_SIZE = (512, 64)
ART_SIZE, ART_ORIGIN = (384, 60), (64, 2)
# Retain headroom after removing the halo, rather than enlarging the silhouette.
MASTER_TOP = 158
FRAME_OFFSET, BAR_OFFSET = (-37, 0), (0, 19)
BAR_IN_FRAME = (37, 19)  # GUI coordinates, not texture texels.
INCREMENT = GUI_FRAME_SIZE[1] + 11


def make_frame(phase):
    master = Image.open(SOURCE / f"{phase}-master.png").convert("RGBA")
    alpha = master.getchannel("A").point(lambda a: 255 if a >= 128 else 0)
    box = alpha.getbbox()
    box = (box[0], MASTER_TOP, box[2], box[3])
    master.putalpha(alpha)
    cropped = master.crop(box)
    # Sample the original artwork directly at the final texture resolution.
    # Premultiplied alpha excludes transparent RGB from edge colours; a binary
    # silhouette preserves crisp edges without requiring a different blend mode.
    sampled = cropped.convert("RGBa").resize(ART_SIZE, Image.Resampling.BOX).convert("RGBA")
    sampled.putalpha(cropped.getchannel("A").resize(ART_SIZE, Image.Resampling.NEAREST))
    frame = Image.new("RGBA", FRAME_SIZE)
    frame.paste(sampled, ART_ORIGIN)
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
    # Compare both resolutions at the same physical GUI scale. Never shrink the
    # 4x baseline to the new 2x texture before comparing it.
    size = tuple(v*PREVIEW_SCALE for v in GUI_FRAME_SIZE)
    image = Image.new("RGBA", size)
    if phase_two:
        position = tuple(v*PREVIEW_SCALE for v in BAR_IN_FRAME)
        empty = base.crop((0, 0, BAR_WIDTH, BAR_HEIGHT))
        image.alpha_composite(empty.resize((BAR_WIDTH*PREVIEW_SCALE, BAR_HEIGHT*PREVIEW_SCALE),
                                           Image.Resampling.NEAREST), position)
        filled = int(BAR_WIDTH * fraction)
        if filled:
            fill = base.crop((0, BAR_HEIGHT, filled, BAR_HEIGHT*2))
            image.alpha_composite(fill.resize((filled*PREVIEW_SCALE, BAR_HEIGHT*PREVIEW_SCALE),
                                              Image.Resampling.NEAREST), position)
    image.alpha_composite(frame.resize(size, Image.Resampling.NEAREST))
    return image


def save_sheet(size, labels, sprites, path, background=(18, 15, 23)):
    """Labels use GUI pixels; both versions are shown at the same GUI scale."""
    image = Image.new("RGBA", size, background + (255,))
    draw = ImageDraw.Draw(image)
    font = ImageFont.load_default(size=9)
    for position, label in labels:
        draw.text(position, label, fill=(202, 190, 212), font=font)
    image = image.resize(tuple(v*PREVIEW_SCALE for v in size), Image.Resampling.NEAREST)
    for position, sprite in sprites:
        image.alpha_composite(sprite, tuple(v*PREVIEW_SCALE for v in position))
    image.convert("RGB").save(path)


def make_previews(base, frames):
    samples = (("phase-one", 1.0), *(("phase-two", f) for f in (1.0, .51, .50, .49, .10, 0.0)))
    labels, sprites = [], []
    for idx, (phase, fraction) in enumerate(samples):
        y = 5 + idx*48
        stage = 1 if phase == "phase-one" else 2
        label = f"PHASE {stage} | " + ("VEILED" if stage == 1 else f"{int(fraction*100)}%")
        labels.append(((16, y), label))
        sprites.append(((16, y+12), composite(base, frames[phase], fraction, stage == 2)))
    save_sheet((288, len(samples)*48+6), labels, sprites,
               REVIEW.parent / "boss_bar_preview.png")

    labels, sprites = [], []
    for stage, phase in enumerate(("phase-one", "phase-two")):
        y = 7 + stage*60
        labels.append(((16, y), f"PHASE {stage+1}"))
        sprites.append(((16, y+14), composite(base, frames[phase], .72, stage == 1)))
    save_sheet((288, 126), labels, sprites, REVIEW / "overview.png")

    labels, sprites = [], []
    font = ImageFont.load_default(size=9)
    name = "FIRST VICISSITUDE"
    name_x = round(160-font.getlength(name)/2)
    for stage, phase in enumerate(("phase-one", "phase-two")):
        y = 12 + stage*INCREMENT
        labels.append(((name_x, y-9), name))
        sprites.append(((160-BAR_WIDTH//2+FRAME_OFFSET[0], y),
                        composite(base, frames[phase], .55, stage == 1)))
    save_sheet((320, 120), labels, sprites, REVIEW / "stacked-layout.png", (38, 35, 44))

    before = REVIEW / "highres-before"
    if (before / "first_vicissitude_bar_phase_one_frame.png").exists():
        labels, sprites = [], []
        for col, label in enumerate(("BEFORE | 768x120 ART", "AFTER | 384x60 ART")):
            labels.append(((16+col*288, 8), label))
            for stage, phase in enumerate(("phase-one", "phase-two")):
                frame = frames[phase]
                if col == 0:
                    suffix = phase.replace("-", "_")
                    frame = Image.open(before / f"first_vicissitude_bar_{suffix}_frame.png").convert("RGBA")
                y = 29+stage*62
                labels.append(((16+col*288, y), f"PHASE {stage+1}"))
                sprites.append(((16+col*288, y+12), composite(base, frame, .50, stage == 1)))
        labels.append(((16, 161), "OFFLINE / SAME GUI SIZE / 4X GUI SCALE"))
        save_sheet((576, 182), labels, sprites, REVIEW / "comparison.png")


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
        assert frame.size == FRAME_SIZE
        assert set(frame.getchannel("A").get_flattened_data()) == {0, 255}
    make_previews(base, frames)
    # Phase one never draws health, even where feathers leave transparent gaps.
    assert composite(base, frames["phase-one"], 0, False).tobytes() == composite(
        base, frames["phase-one"], 1, False).tobytes()
    report = dict(frame_texture_size=FRAME_SIZE, frame_gui_size=GUI_FRAME_SIZE,
                  texels_per_gui_pixel=SCALE, art_texture_size=ART_SIZE,
                  art_gui_size=(192, 30), base_size=BASE_TEX,
                  bar_size=(BAR_WIDTH, BAR_HEIGHT), bar_in_frame_gui=BAR_IN_FRAME,
                  frame_offset=FRAME_OFFSET, bar_offset=BAR_OFFSET, increment=INCREMENT,
                  phase_one_health_visible=False, palette_quantized=False,
                  alpha_values=[0, 255], in_game_verified=False)
    (REVIEW / "layout.json").write_text(json.dumps(report, indent=2)+"\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
