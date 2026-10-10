"""Package the ImageGen wing frame and its separate fixed-size top crest.

This only samples the master and demonstrates the runtime geometry; artwork is authored by ImageGen.
No resources under src/generated are touched.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
SOURCE = Path(__file__).resolve().parent / "age-of-enlightenment/wing-master.png"
CREST_SOURCE = Path(__file__).resolve().parent / "age-of-enlightenment/crest-master.png"
TEXTURE = ROOT / "src/main/resources/assets/maledict/textures/gui/tooltip/age_of_enlightenment.png"
CREST_TEXTURE = TEXTURE.with_name("age_of_enlightenment_crest.png")
PREVIEW = ROOT / "build/tooltip-background-review/age_of_enlightenment-preview.png"
SIZE = (96, 144)
BORDER = (32, 28, 32, 28)
# Measured inner rail in the sampled artwork, matching MaledictTooltipBackgrounds.java.
OUTSET = (25, 22, 25, 27)
CREST_SIZE, CREST_OVERLAP = (72, 24), 2


def frame(texture, width, height):
    result = Image.new("RGBA", (width, height))
    left, top, right, bottom = BORDER
    xs, ys = (0, left, width-right, width), (0, top, height-bottom, height)
    us, vs = (0, left, SIZE[0]-right, SIZE[0]), (0, top, SIZE[1]-bottom, SIZE[1])
    for row in range(3):
        for column in range(3):
            if row == 1 and column == 1:
                continue  # TooltipBackground.Mode.DECORATE
            region = texture.crop((us[column], vs[row], us[column+1], vs[row+1]))
            drawn_width, drawn_height = xs[column+1]-xs[column], ys[row+1]-ys[row]
            # Stretch the connected wing body; keep shoulders and tips fixed.
            region = region.resize((drawn_width, drawn_height), Image.Resampling.NEAREST)
            result.alpha_composite(region, (xs[column], ys[row]))
    return result


def preview(texture, crest):
    image = Image.new("RGBA", (650, 230), (31, 27, 37, 255))
    draw = ImageDraw.Draw(image)
    font = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 10)
    title_font = ImageFont.truetype("C:/Windows/Fonts/msyh.ttc", 11)
    draw.text((12, 7), "启蒙之年 · 紫金羽翼框 · 离线布局预览（非游戏截图）", font=font, fill="#cab5df")
    for content_x, content_y, content_width, content_height in ((40, 76, 250, 94), (395, 76, 190, 104)):
        # TooltipFrameLayout.around with the registration's measured outset.
        left, top, right, bottom = OUTSET
        image.alpha_composite(frame(texture, content_width+8+left+right, content_height+8+top+bottom),
                              (content_x-4-left, content_y-4-top))
        image.alpha_composite(crest,
                              (content_x-4+(content_width+8-crest.width)//2,
                               content_y-4+CREST_OVERLAP-crest.height))
        draw.rectangle((content_x-4, content_y-3, content_x+content_width+3, content_y+content_height+2),
                       fill=(16, 0, 16, 240))
        draw.rectangle((content_x-3, content_y-4, content_x+content_width+2, content_y+content_height+3),
                       fill=(16, 0, 16, 240))
        draw.rectangle((content_x-3, content_y-3, content_x+content_width+2, content_y+content_height+2),
                       outline="#46335e")
        draw.text((content_x, content_y), "启蒙之年", font=title_font, fill="#df86f6")
        lines = ["佩戴时冷却速度加倍", "攻击半生命值目标时", "触发收获精魂时的效果", "按住 Shift 追问"]
        for index, text in enumerate(lines):
            draw.text((content_x, content_y+20+index*14), text, font=font,
                      fill="#bba8d0" if index < 3 else "#71657c")
    # Include real GUI-scale layout; enlargement uses nearest-neighbour, not smoothed artwork.
    image.convert("RGB").resize((1300, 460), Image.Resampling.NEAREST).save(PREVIEW)


def main():
    texture = Image.open(SOURCE).convert("RGBA").resize(SIZE, Image.Resampling.NEAREST)
    crest_master = Image.open(CREST_SOURCE).convert("RGBA")
    visible_box = crest_master.getchannel("A").point(lambda alpha: 255 if alpha >= 32 else 0).getbbox()
    crest_art = crest_master.crop(visible_box)
    crest_art.thumbnail(CREST_SIZE, Image.Resampling.NEAREST)
    crest = Image.new("RGBA", CREST_SIZE)
    crest.alpha_composite(crest_art, ((crest.width-crest_art.width)//2, crest.height-crest_art.height))
    TEXTURE.parent.mkdir(parents=True, exist_ok=True)
    PREVIEW.parent.mkdir(parents=True, exist_ok=True)
    texture.save(TEXTURE)
    crest.save(CREST_TEXTURE)
    preview(texture, crest)
    print(f"Texture: {TEXTURE} ({SIZE[0]}x{SIZE[1]}, RGBA)")
    print(f"Top crest: {CREST_TEXTURE} ({crest.width}x{crest.height}, RGBA)")
    print(f"Preview: {PREVIEW}")


if __name__ == "__main__":
    main()
