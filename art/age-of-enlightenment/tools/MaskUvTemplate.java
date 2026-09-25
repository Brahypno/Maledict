import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 生成面具饰品（curio）的 UV 布局参考图。
 * 原版 ModelPart.Cube 盒子展开：第一行（高 d）上面/下面，第二行（高 h）-X/正面/+X/背面。
 * 模型原点在眼睛高度，头占 x,z ∈ [-4,4]、y ∈ [-8,0]，-z 是脸朝向；盒子取整数边界，正脸 10x10 中间的 8x8 与原版脸 1:1。
 * 用法（仓库根目录，Java 17）：
 * javac -encoding UTF-8 -d build/mask-tool art/age-of-enlightenment/tools/MaskUvTemplate.java
 * java -cp build/mask-tool MaskUvTemplate
 */
public final class MaskUvTemplate {

    private static final int TEX = 64;
    private static final int SCALE = 8;
    private static final int MARGIN = 28;

    /** 面具盒子尺寸（模型像素）：10x10x5，整数边界才与原版脸 1:1 对齐。 */
    private static final int BOX_W = 10;
    private static final int BOX_H = 10;
    private static final int BOX_D = 5;

    private static final int BODY_U = 0;
    private static final int BODY_V = 0;
    private static final int SHELL_U = 0;
    private static final int SHELL_V = 17;

    private static final Color FACE = new Color(0xC8, 0x5A, 0x50);
    private static final Color SIDE = new Color(0x5A, 0x82, 0xC8);
    private static final Color CAP = new Color(0x5F, 0xAF, 0x6B);
    private static final Color BOTTOM = new Color(0x4E, 0x9A, 0xA0);
    private static final Color INNER = new Color(0x3A, 0x3A, 0x3A);
    private static final Color OUTLINE = new Color(0x10, 0x10, 0x10);
    private static final Color GRID = new Color(255, 255, 255, 26);
    private static final Color GRID_MAJOR = new Color(255, 255, 255, 64);
    private static final Color MARKER = new Color(0xFF, 0xE0, 0x60);

    private record Region(String id, String label, int x, int y, int w, int h, Color color, boolean shell) {
    }

    public static void main(String[] args) throws IOException {
        Path root = Path.of("art", "age-of-enlightenment");
        Path preview = root.resolve("preview");
        Files.createDirectories(preview);

        List<Region> regions = new ArrayList<>();
        addBox(regions, BODY_U, BODY_V, false);
        addBox(regions, SHELL_U, SHELL_V, true);

        BufferedImage template = paintTemplate(regions);
        ImageIO.write(template, "png", root.resolve("uv_template_64.png").toFile());
        ImageIO.write(paintPreview(regions, template), "png",
                preview.resolve("uv_template_64_preview.png").toFile());

        printTable(regions);
    }

    private static void addBox(List<Region> regions, int u, int v, boolean shell) {
        int w = BOX_W;
        int h = BOX_H;
        int d = BOX_D;
        String tag = shell ? "shell_" : "";
        regions.add(new Region(tag + "up", "顶", u + d, v, w, d, dim(CAP, shell), shell));
        regions.add(new Region(tag + "down", "底", u + d + w, v, w, d, dim(BOTTOM, shell), shell));
        regions.add(new Region(tag + "west", "右侧", u, v + d, d, h, dim(SIDE, shell), shell));
        regions.add(new Region(tag + "north", "正脸", u + d, v + d, w, h, dim(FACE, shell), shell));
        regions.add(new Region(tag + "east", "左侧", u + d + w, v + d, d, h, dim(SIDE, shell), shell));
        regions.add(new Region(tag + "south", "内面", u + d + w + d, v + d, w, h, dim(INNER, shell), shell));
    }

    private static Color dim(Color color, boolean shell) {
        if (!shell) {
            return color;
        }
        return new Color(
                Math.round(color.getRed() * 0.55F),
                Math.round(color.getGreen() * 0.55F),
                Math.round(color.getBlue() * 0.55F));
    }

    private static BufferedImage paintTemplate(List<Region> regions) {
        BufferedImage image = new BufferedImage(TEX, TEX, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setComposite(java.awt.AlphaComposite.Src);
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, TEX, TEX);
        g.setComposite(java.awt.AlphaComposite.SrcOver);

        // 不描边：描边会吃掉每个区域最外一圈 texel。
        for (Region region : regions) {
            g.setColor(region.color());
            g.fillRect(region.x(), region.y(), region.w(), region.h());
        }
        g.dispose();
        return image;
    }

    private static BufferedImage paintPreview(List<Region> regions, BufferedImage template) {
        int size = TEX * SCALE;
        BufferedImage image = new BufferedImage(size + MARGIN, size + MARGIN + 26, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x1A, 0x1A, 0x1E));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());

        for (int y = 0; y < TEX; y++) {
            for (int x = 0; x < TEX; x++) {
                if (((x >> 2) + (y >> 2)) % 2 == 0) {
                    g.setColor(new Color(0x2A, 0x2A, 0x30));
                    g.fillRect(MARGIN + x * SCALE, MARGIN + y * SCALE, SCALE, SCALE);
                }
            }
        }
        g.drawImage(template, MARGIN, MARGIN, size, size, null);

        // 区域框只画在预览图上，不占用贴图。
        for (Region region : regions) {
            if (region.shell()) {
                g.setStroke(new BasicStroke(2F, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                        1F, new float[] {6F, 4F}, 0F));
            } else {
                g.setStroke(new BasicStroke(2F));
            }
            g.setColor(new Color(OUTLINE.getRed(), OUTLINE.getGreen(), OUTLINE.getBlue(), 220));
            g.drawRect(MARGIN + region.x() * SCALE + 1, MARGIN + region.y() * SCALE + 1,
                    region.w() * SCALE - 2, region.h() * SCALE - 2);
        }
        g.setStroke(new BasicStroke(1F));

        for (int i = 0; i <= TEX; i++) {
            g.setColor(i % 8 == 0 ? GRID_MAJOR : GRID);
            g.drawLine(MARGIN + i * SCALE, MARGIN, MARGIN + i * SCALE, MARGIN + size);
            g.drawLine(MARGIN, MARGIN + i * SCALE, MARGIN + size, MARGIN + i * SCALE);
        }

        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(0xC8, 0xC8, 0xD0));
        for (int i = 0; i <= TEX; i += 8) {
            g.drawString(Integer.toString(i), MARGIN + i * SCALE - 6, MARGIN - 8);
            g.drawString(Integer.toString(i), 4, MARGIN + i * SCALE + 4);
        }

        Font font = cjkFont(11);
        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics();
        for (Region region : regions) {
            String text = region.label();
            int cx = MARGIN + region.x() * SCALE + region.w() * SCALE / 2;
            int cy = MARGIN + region.y() * SCALE + region.h() * SCALE / 2 + metrics.getAscent() / 2 - 2;
            int tw = metrics.stringWidth(text);
            g.setColor(new Color(0, 0, 0, 200));
            g.drawString(text, cx - tw / 2 + 1, cy + 1);
            g.setColor(region.shell() ? new Color(0xD0, 0xD0, 0xD0) : Color.WHITE);
            g.drawString(text, cx - tw / 2, cy);
        }

        g.setFont(font.deriveFont(13F));
        g.setColor(new Color(0xE0, 0xE0, 0xE8));
        g.drawString("框线只是本预览图的标注；贴图本体是纯色块，色块边界即 UV 分界", MARGIN, size + MARGIN + 18);

        drawIslandCaption(g, font, 8, new Color(0xE0, 0xE0, 0xE8), new String[] {
                "① 本体 @ texOffs(0, 0)",
                "addBox(-5, -9, -5, 10, 10, 5)"});
        drawIslandCaption(g, font, 24, new Color(0xA8, 0xA8, 0xB4), new String[] {
                "② 可选外扩壳 @ texOffs(0, 17)",
                "CubeDeformation(0.35)，不做就整块留空"});

        // 正脸里真正要画的 8x8：与原版脸 1:1 对齐。
        int inner = MARGIN + (BOX_D + 1) * SCALE;
        int innerSize = 8 * SCALE;
        g.setStroke(new BasicStroke(3F));
        g.setColor(MARKER);
        g.drawRect(inner, inner, innerSize, innerSize);
        g.setStroke(new BasicStroke(1F));
        g.setFont(font.deriveFont(12F));
        g.setColor(MARKER);
        g.drawString("↑ 你画的 8x8：与原版脸 1:1，眼洞会自动对上玩家的眼睛",
                inner, inner + innerSize + 16);
        g.dispose();
        return image;
    }

    private static void drawIslandCaption(Graphics2D g, Font base, int nativeCenterY, Color color, String[] lines) {
        Font font = base.deriveFont(12F);
        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics();
        int left = MARGIN + 28 * SCALE;
        int textX = left + 14;
        int centerY = MARGIN + nativeCenterY * SCALE;
        int firstBaseline = centerY - (lines.length - 1) * metrics.getHeight() / 2 + metrics.getAscent() / 2 - 4;

        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 120));
        g.setStroke(new BasicStroke(1F));
        g.drawLine(left, centerY, textX - 4, centerY);
        for (int i = 0; i < lines.length; i++) {
            g.setColor(new Color(0, 0, 0, 190));
            g.drawString(lines[i], textX + 1, firstBaseline + i * metrics.getHeight() + 1);
            g.setColor(color);
            g.drawString(lines[i], textX, firstBaseline + i * metrics.getHeight());
        }
    }

    private static Font cjkFont(int size) {
        for (String name : new String[] {"Microsoft YaHei", "SimHei", "Noto Sans CJK SC", "SansSerif"}) {
            Font font = new Font(name, Font.PLAIN, size);
            if (font.canDisplay('正') && font.canDisplay('面')) {
                return font;
            }
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, size);
    }

    private static void printTable(List<Region> regions) {
        System.out.printf("texture %dx%d, box %dx%dx%d, body uv (%d,%d), shell uv (%d,%d)%n",
                TEX, TEX, BOX_W, BOX_H, BOX_D, BODY_U, BODY_V, SHELL_U, SHELL_V);
        System.out.printf("%-12s %-8s %-6s %-6s %-6s %-6s%n", "region", "label", "x", "y", "w", "h");
        for (Region region : regions) {
            System.out.printf("%-12s %-8s %-6d %-6d %-6d %-6d%n",
                    region.id(), region.label(), region.x(), region.y(), region.w(), region.h());
        }
    }

    private MaskUvTemplate() {
    }
}
