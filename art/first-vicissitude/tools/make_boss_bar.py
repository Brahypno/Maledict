"""生成无常 Boss 战的自定义血条贴图。

和 `art/melancholia/tools/make_delayed_vitals_hearts.py` 是同一套路：贴图由脚本生成，
改了参数重跑即可，不做手工修图。输出的几何参数和 `VicissitudeBossBarOverlay` 里的常量一一对应，
**改这里就得同步改那边**。

## 结构（对齐同类模组的通用做法）

原版 `BossHealthOverlay` 画血条是两次 blit，从**同一张图的两行**取：先空槽、再按血量截断填充。
所以 base 是一张 256x16 的图，里面装两条 182x5 的实心条：

    v=0..5    空槽：填充色的暗版（同色系压暗），带 1px 描边
    v=5..10   填充：亮色竖向渐变，上沿最亮
    v=10..16  留空（声明尺寸要用，别删）

overlay 是**另一张 256x32 的装饰图**，1:1 画在血条上层，**大部分像素是透明的**，
实体部分是同一条横轴上的：中轴细轨 + 两端护套 + 周期性挂件 + 中央大挂件。
血条从这些挂件后面穿过去——这是这类血条看起来"有东西"的原因，光画一圈边框是出不来的。

## 设计取自律动

Boss 本体的特征是「头后断环，4 个不等长断片，缺口永久存在」（01_SPEC §1）。
所以 overlay 的中轴轨就是**同样四段不等长的断环**摊平，缺口处放卡扣，缺口位置和身上的环一致。
二阶段断口更宽、卡扣开裂、两端外扩。

色板取自 01_SPEC §1：深黑紫 #19151F / 灰紫 #49404F / 冷骨白 #B8B8C4 /
冷白 #E6EDF5 / 暗紫能量 #51436D。

用法（仓库根目录）：`python art/first-vicissitude/tools/make_boss_bar.py`
"""

from pathlib import Path

from PIL import Image, ImageDraw

# ---------------------------------------------------------------- 几何（改这里要同步改 Java）

BAR_WIDTH = 182          # 血条本体宽度，原版就是 182
BAR_HEIGHT = 5           # 血条本体高度，原版就是 5
BASE_TEX = (256, 16)     # base 贴图尺寸；声明尺寸，UV 归一化用它

FRAME_SIZE = (256, 32)   # overlay 贴图尺寸，1:1 绘制
FRAME_OFFSET = (-6, -9)  # overlay 相对 (x, y) 的偏移
BAR_OFFSET = (1, 7)      # 血条本体相对 (x, y) 的偏移
# 多只无常同时在场时血条会竖着叠。原版每画完一条就 y += increment，而每条的实际占位是
# [y-9, y+23)（名字在 y-9，overlay 32 高），所以 increment 必须 >= FRAME_SIZE[1]，
# 否则下一条的名字会落进上一条 overlay 的底部。这里直接取满高，改 overlay 高度时它跟着变。
INCREMENT = FRAME_SIZE[1]

# 血条在 overlay 画布内的窗口，由上面两个推出来；overlay 的装饰必须绕开它
BAR_IN_FRAME = (BAR_OFFSET[0] - FRAME_OFFSET[0], BAR_OFFSET[1] - FRAME_OFFSET[1])
BAR_WINDOW = (BAR_IN_FRAME[0], BAR_IN_FRAME[1],
              BAR_IN_FRAME[0] + BAR_WIDTH, BAR_IN_FRAME[1] + BAR_HEIGHT)

# 两条贯通全宽的轨夹住血条，血条走中间那条槽。这是量同类模组的 overlay 得出的结论：
# maledictus 的 256x32 overlay 在 y=7/8 和 y=14/15 各有 190+ 个不透明像素（满宽），
# 而血条所在的 y=9..13 只有约 100 个（只有挂件横穿），所以轨是**上下各一条**，不是一条穿心。
RAIL_TOP_Y = BAR_IN_FRAME[1] - 2
RAIL_BOT_Y = BAR_IN_FRAME[1] + BAR_HEIGHT

# ---------------------------------------------------------------- 色板

VOID = (0x19, 0x15, 0x1F)     # 深黑紫
ASH = (0x49, 0x40, 0x4F)      # 灰紫
BONE = (0xB8, 0xB8, 0xC4)     # 冷骨白
PALE = (0xE6, 0xED, 0xF5)     # 冷白
ARCANE = (0x51, 0x43, 0x6D)   # 暗紫能量

PHASE_FILL = ((0x8A, 0x74, 0xBE), (0x51, 0x43, 0x6D))   # 一阶段 亮 -> 暗
PHASE_TWO_FILL = ((0xB0, 0x74, 0xA8), (0x54, 0x33, 0x5C))  # 二阶段偏赤

# 断环四段，摊平到 overlay 全宽。长度刻意不等（0.30 / 0.12 / 0.25 / 0.17），
# 等长等距会渲染成一条虚线，读不出「断环」。缺口永久存在（01_SPEC §1）。
RING_SEGMENTS = ((0.000, 0.300), (0.345, 0.465), (0.520, 0.770), (0.845, 1.000))

SCALE = 3

ROOT = Path(__file__).resolve().parents[3]
OUT_DIR = ROOT / "src/main/resources/assets/maledict/textures/gui/boss_bar"
# 技术检查产物不进 preview/ —— 按 art/first-vicissitude/AGENTS.md，那里只留固定五个文件
PREVIEW_DIR = ROOT / "build/first-vicissitude-review"


def lerp(a, b, t):
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


# ---------------------------------------------------------------- base：两条实心条

def draw_strip(img, oy, top, mid, bottom, edge):
    """一条 182x5 的实心条：上下各留 1px 描边，中间三行做竖向渐变。"""
    d = ImageDraw.Draw(img)
    x0, x1 = 0, BAR_WIDTH - 1
    d.line([x0, oy, x1, oy], fill=edge)
    d.line([x0, oy + BAR_HEIGHT - 1, x1, oy + BAR_HEIGHT - 1], fill=edge)
    d.line([x0 + 1, oy + 1, x1 - 1, oy + 1], fill=top)
    d.line([x0 + 1, oy + 2, x1 - 1, oy + 2], fill=mid)
    d.line([x0 + 1, oy + 3, x1 - 1, oy + 3], fill=bottom)


def make_base(phase_two):
    img = Image.new("RGBA", BASE_TEX, (0, 0, 0, 0))
    bright, dark = PHASE_TWO_FILL if phase_two else PHASE_FILL
    # 空槽：同色系压暗，这样"还差多少"是靠亮度读出来的，不是靠一块死黑
    draw_strip(img, 0, lerp(dark, VOID, 0.62), lerp(dark, VOID, 0.78), lerp(dark, VOID, 0.9), VOID)
    # 填充：上沿提亮到接近冷白，和空槽拉开足够对比，否则整条读起来是一根灰线
    draw_strip(img, BAR_HEIGHT, lerp(bright, PALE, 0.55), lerp(bright, PALE, 0.18), dark,
               lerp(dark, VOID, 0.45))
    return img


# ---------------------------------------------------------------- overlay：轨 + 护套 + 挂件

def draw_rails(img, phase_two):
    """上下两条贯通全宽的断环轨，四段不等长，缺口处留空。血条在两条轨之间。"""
    d = ImageDraw.Draw(img)
    w = FRAME_SIZE[0] - 1
    grow = 3 if phase_two else 0
    rail_col = BONE if not phase_two else lerp(BONE, PHASE_TWO_FILL[0], 0.35)
    for y, inward in ((RAIL_TOP_Y, -1), (RAIL_BOT_Y, 1)):
        for i, (a, b) in enumerate(RING_SEGMENTS):
            x0 = round(a * w) + (grow if i else 0)
            x1 = round(b * w) - (grow if i < len(RING_SEGMENTS) - 1 else 0)
            if x1 <= x0:
                continue
            d.line([x0, y, x1, y], fill=rail_col)
            d.line([x0, y + inward, x1, y + inward], fill=lerp(ASH, VOID, 0.4))
            d.point((x0, y), fill=PALE)
            d.point((x1, y), fill=PALE)


def draw_clasp(img, cx, phase_two):
    """缺口处的卡扣：把上下两条轨和中间的血条一起箍住，所以它是横穿血条的。"""
    d = ImageDraw.Draw(img)
    top, bot = RAIL_TOP_Y - 1, RAIL_BOT_Y + 1
    d.line([cx, top, cx, bot], fill=ASH)
    d.line([cx + 1, top, cx + 1, bot], fill=lerp(ASH, VOID, 0.5))
    for y in (top, bot):
        d.line([cx - 2, y, cx + 3, y], fill=BONE)
    d.point((cx, top), fill=PALE)
    if phase_two:
        # 卡扣本身开裂：斜切一刀，让上下两半错位
        d.line([cx - 3, RAIL_TOP_Y, cx + 2, RAIL_BOT_Y], fill=VOID)
        d.point((cx + 2, RAIL_TOP_Y + 1), fill=BONE)


def draw_cap(img, cx, inward, phase_two):
    """两端的护套：把两条轨和血条一起收口，并向外挑出尖。"""
    d = ImageDraw.Draw(img)
    top, bot = RAIL_TOP_Y - 2, RAIL_BOT_Y + 2
    d.line([cx, top, cx, bot], fill=ASH)
    d.line([cx + inward, top + 1, cx + inward, bot - 1], fill=lerp(ASH, VOID, 0.5))
    for y in (top, bot):
        d.line([cx, y, cx + inward * 6, y], fill=BONE)
    d.line([cx - inward * 3, top - 3, cx, top], fill=ASH)
    d.line([cx - inward * 3, bot + 3, cx, bot], fill=ASH)
    d.point((cx - inward * 3, top - 3), fill=BONE)
    d.point((cx - inward * 3, bot + 3), fill=BONE)
    if phase_two:
        d.line([cx - inward, top - 2, cx - inward * 5, top - 6], fill=ASH)
        d.line([cx - inward, bot + 2, cx - inward * 5, bot + 6], fill=ASH)


def draw_mount(img, cx, phase_two):
    """周期性挂件：骑在血条上把它压住的一小簇，上下都咬进轨里。"""
    d = ImageDraw.Draw(img)
    accent = PHASE_TWO_FILL[0] if phase_two else PHASE_FILL[0]
    top, bot = RAIL_TOP_Y - 1, RAIL_BOT_Y + 1
    d.line([cx, top, cx, bot], fill=ASH)
    d.line([cx - 2, top, cx + 2, top], fill=lerp(accent, PALE, 0.3))
    d.line([cx - 2, bot, cx + 2, bot], fill=lerp(accent, VOID, 0.3))
    d.point((cx, top - 1), fill=PALE)
    d.point((cx - 2, BAR_IN_FRAME[1] + 2), fill=accent)
    d.point((cx + 2, BAR_IN_FRAME[1] + 2), fill=accent)


def draw_center_mount(img, phase_two):
    """中央大挂件：整条最重的一处，比普通挂件宽，对应 Boss 胸腔那个结构。"""
    d = ImageDraw.Draw(img)
    accent = PHASE_TWO_FILL[0] if phase_two else PHASE_FILL[0]
    cx = FRAME_SIZE[0] // 2
    top, bot = RAIL_TOP_Y - 4, RAIL_BOT_Y + 4
    d.polygon([(cx - 9, RAIL_TOP_Y), (cx, top), (cx + 9, RAIL_TOP_Y),
               (cx + 9, RAIL_BOT_Y), (cx, bot), (cx - 9, RAIL_BOT_Y)], fill=ASH)
    d.polygon([(cx - 6, RAIL_TOP_Y), (cx, top + 3), (cx + 6, RAIL_TOP_Y),
               (cx + 6, RAIL_BOT_Y), (cx, bot - 3), (cx - 6, RAIL_BOT_Y)],
              fill=lerp(accent, VOID, 0.35))
    d.polygon([(cx - 2, RAIL_TOP_Y), (cx, top + 5), (cx + 2, RAIL_TOP_Y),
               (cx + 2, RAIL_BOT_Y), (cx, bot - 5), (cx - 2, RAIL_BOT_Y)],
              fill=lerp(accent, PALE, 0.5))
    d.point((cx, top), fill=PALE)
    d.point((cx, bot), fill=PALE)
    d.point((cx - 9, RAIL_TOP_Y), fill=BONE)
    d.point((cx + 9, RAIL_BOT_Y), fill=BONE)


def make_overlay(phase_two):
    img = Image.new("RGBA", FRAME_SIZE, (0, 0, 0, 0))
    draw_rails(img, phase_two)
    w = FRAME_SIZE[0] - 1
    for a, _ in RING_SEGMENTS[1:]:
        draw_clasp(img, round(a * w), phase_two)
    for a, b in RING_SEGMENTS:
        draw_mount(img, round((a + b) * 0.5 * w), phase_two)
    draw_center_mount(img, phase_two)
    draw_cap(img, 6, 1, phase_two)
    draw_cap(img, w - 6, -1, phase_two)
    return img


# ---------------------------------------------------------------- 检查图

def build_preview(bases, overlays):
    ow, oh = FRAME_SIZE
    left, gap, label_h = 16, 30, 16
    rows = 6
    cell_h = oh + gap
    width = ow + left * 2
    img = Image.new("RGB", (width, label_h + (cell_h * rows) + 30), (0x0A, 0x08, 0x0D))
    d = ImageDraw.Draw(img)

    for idx, (label, phase_two, fractions) in enumerate((
            ("phase 1", False, (1.0, 0.55, 0.18)),
            ("phase 2", True, (1.0, 0.55, 0.18)))):
        base, overlay = bases[phase_two], overlays[phase_two]
        for j, frac in enumerate(fractions):
            row = idx * 3 + j
            ox, oy = left, label_h + row * cell_h
            # 名字：原版画在 y-9，这里按 event 的 (x, y) 反推 overlay 顶边在 y-2
            name_y = oy + FRAME_OFFSET[1] - 9
            d.text((ox + 4, name_y - 9), f"{label}  {int(frac * 100)}%", fill=(0x88, 0x84, 0x96))
            d.line([ox - 8, name_y, ox + ow + 8, name_y], fill=(0x33, 0x2C, 0x3D))
            # 空槽
            img.paste(base.crop((0, 0, BAR_WIDTH, BAR_HEIGHT)),
                      (ox + BAR_IN_FRAME[0], oy + BAR_IN_FRAME[1]))
            # 填充
            filled = max(0, round(BAR_WIDTH * frac))
            if filled:
                img.paste(base.crop((0, BAR_HEIGHT, filled, BAR_HEIGHT * 2)),
                          (ox + BAR_IN_FRAME[0], oy + BAR_IN_FRAME[1]))
            # overlay 盖在最上层
            img.paste(overlay, (ox, oy), overlay)
    return img.resize((width * SCALE, img.height * SCALE), Image.NEAREST)


def main():
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    PREVIEW_DIR.mkdir(parents=True, exist_ok=True)

    bases, overlays = {}, {}
    for phase_two, suffix in ((False, "phase_one"), (True, "phase_two")):
        base = make_base(phase_two)
        overlay = make_overlay(phase_two)
        bases[phase_two], overlays[phase_two] = base, overlay
        base.save(OUT_DIR / f"first_vicissitude_bar_{suffix}.png")
        overlay.save(OUT_DIR / f"first_vicissitude_bar_{suffix}_frame.png")
        print(f"wrote first_vicissitude_bar_{suffix}.png ({base.width}x{base.height})"
              f" + _frame.png ({overlay.width}x{overlay.height})")

    preview = build_preview(bases, overlays)
    out = PREVIEW_DIR / "boss_bar_preview.png"
    preview.save(out)
    print(f"wrote preview -> {out}  ({preview.width}x{preview.height})")
    print(f"\nJava 侧常量：BAR={BAR_WIDTH}x{BAR_HEIGHT} BAR_OFFSET={BAR_OFFSET} "
          f"OVERLAY={FRAME_SIZE} OVERLAY_OFFSET={FRAME_OFFSET} INCREMENT={INCREMENT}")
    print(f"overlay 内血条窗口 x{BAR_WINDOW[0]}..{BAR_WINDOW[2]} y{BAR_WINDOW[1]}..{BAR_WINDOW[3]}"
          f"，轨在 y={RAIL_TOP_Y} 与 y={RAIL_BOT_Y}")


if __name__ == "__main__":
    main()
