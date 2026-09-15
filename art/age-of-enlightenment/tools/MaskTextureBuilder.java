import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 面具贴图生成器：吃一张 8x8 的脸部贴图，吐一张完整的模型贴图（64x64）。
 *
 * <p>为什么是 8x8：原版头是 8x8x8、1 texel = 1 模型单位，脸正好铺满模型 x/z ∈ [-4,4]、y ∈ [-8,0]。
 * 只要面具盒子的边界都落在整数坐标上，正脸中间的 8x8 就与原版脸 1:1 对齐，
 * 于是透明像素挖的眼洞能正好对上玩家自己的眼睛。
 *
 * <p>盒子：{@code addBox(-5, -9, -5, 10, 10, 5)} —— 正脸 10x10 texel，中间 8x8 = 你的画，
 * 四周各 1 texel 由你的边缘像素复制而来（当描边/边缘），
 * 侧厚 5、顶底各 5 也由边缘像素自动挤出并压暗。**你只需要画 8x8。**
 *
 * <p>用法（仓库根目录，Java 17）：
 * <pre>
 * javac -encoding UTF-8 -d build/mask-tool art/age-of-enlightenment/tools/*.java
 * java -cp build/mask-tool MaskTextureBuilder --demo                 # 生成示例图并跑一遍
 * java -cp build/mask-tool MaskTextureBuilder face8x8.png [out64.png]
 * </pre>
 *
 * <p>改动几何时，本文件的 BOX_* / TEX_OFF_* 必须与 MaskUvTemplate.java 和
 * {@code AgeOfEnlightenmentModel} 的 addBox / TEXTURE_* 同步。
 */
public final class MaskTextureBuilder {

    // ==== 几何：与 MaskUvTemplate.java / AgeOfEnlightenmentModel 保持一致 ====
    /** 面具盒子的整数边界：x -5..5、y -9..1、z -5..0。 */
    static final int BOX_W = 10;
    static final int BOX_H = 10;
    static final int BOX_D = 5;
    static final int TEX_OFF_U = 0;
    static final int TEX_OFF_V = 0;
    /** 贴图画布，够放下本体（30x15）再多留余量。 */
    static final int TEX = 64;

    /** 侧面比正脸暗多少（平涂，不做前后渐变，避免依赖 UV 的 U 方向）。 */
    static final float SIDE_SHADE = 0.72F;
    /** 顶/底面压暗程度。 */
    static final float CAP_SHADE = 0.84F;
    /** 内衬（贴着头那一面）亮度系数。 */
    static final float INNER_SHADE = 0.32F;

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("usage: MaskTextureBuilder <face8x8.png> [out64.png]");
            System.out.println("       MaskTextureBuilder --demo");
            return;
        }
        if (args[0].equals("--demo")) {
            Path dir = Path.of(System.getProperty("java.io.tmpdir"), "mask-build");
            Files.createDirectories(dir);
            Path facePath = dir.resolve("demo_face_8x8.png");
            ImageIO.write(demoFace(), "png", facePath.toFile());
            BufferedImage built = build(ImageIO.read(facePath.toFile()));
            Path out = dir.resolve("demo_mask64.png");
            ImageIO.write(built, "png", out.toFile());
            System.out.println("示例 8x8 : " + facePath);
            System.out.println("生成贴图 : " + out);
            printLayout();
            return;
        }

        BufferedImage face = ImageIO.read(new File(args[0]));
        if (face == null) {
            throw new IOException("not an image: " + args[0]);
        }
        if (face.getWidth() != 8 || face.getHeight() != 8) {
            throw new IOException("脸部贴图必须是 8x8，当前是 "
                    + face.getWidth() + "x" + face.getHeight());
        }
        Path outPath = args.length > 1
                ? Path.of(args[1])
                : Path.of(args[0].replaceAll("(?i)\\.png$", "") + "_mask64.png");
        ImageIO.write(build(face), "png", outPath.toFile());
        System.out.println("生成贴图 : " + outPath.toAbsolutePath());
        printLayout();
    }

    /** 8x8 脸部贴图 -> 64x64 模型贴图。 */
    static BufferedImage build(BufferedImage face) {
        BufferedImage out = new BufferedImage(TEX, TEX, BufferedImage.TYPE_INT_ARGB);
        int u = TEX_OFF_U;
        int v = TEX_OFF_V;
        int w = BOX_W;
        int h = BOX_H;
        int d = BOX_D;

        // 1) 正脸：8x8 摆在 10x10 的中间，四周各扩 1 texel（纯边缘复制，
        //    你的画透明的地方，扩展出来的边缘也透明，不会有莫名其妙的边）。
        for (int ry = 0; ry < h; ry++) {
            for (int rx = 0; rx < w; rx++) {
                out.setRGB(u + d + rx, v + d + ry, planePixel(face, rx, ry));
            }
        }

        // 2) 左右侧壁：取正脸板最外一列，往深度方向平涂 5 格，压暗。
        for (int ry = 0; ry < h; ry++) {
            int rightEdge = shade(planePixel(face, 0, ry), SIDE_SHADE);       // -X 面（戴者右侧）
            int leftEdge = shade(planePixel(face, w - 1, ry), SIDE_SHADE);   // +X 面（戴者左侧）
            for (int i = 0; i < d; i++) {
                out.setRGB(u + i, v + d + ry, rightEdge);
                out.setRGB(u + d + w + i, v + d + ry, leftEdge);
            }
        }

        // 3) 顶面/底面：取正脸板最外一行，往深度方向平涂 5 格，压暗。
        for (int rx = 0; rx < w; rx++) {
            int topEdge = shade(planePixel(face, rx, 0), CAP_SHADE);         // -Y 面（额头顶沿）
            int bottomEdge = shade(planePixel(face, rx, h - 1), CAP_SHADE);  // +Y 面（下颌底沿）
            for (int i = 0; i < d; i++) {
                out.setRGB(u + d + rx, v + i, topEdge);
                out.setRGB(u + d + w + rx, v + i, bottomEdge);
            }
        }

        // 4) 内衬：正脸不透明像素的平均色压暗后平涂（从背后看是一层实体壳，不是窟窿）。
        int inner = shade(averageOpaque(face), INNER_SHADE);
        for (int ry = 0; ry < h; ry++) {
            for (int rx = 0; rx < w; rx++) {
                out.setRGB(u + d + w + d + rx, v + d + ry, inner);
            }
        }
        return out;
    }

    /** 正脸板第 (rx,ry) 格：8x8 居中，外围 1 texel 用最近的边缘像素复制。 */
    private static int planePixel(BufferedImage face, int rx, int ry) {
        int sx = Math.max(0, Math.min(7, rx - 1));
        int sy = Math.max(0, Math.min(7, ry - 1));
        return face.getRGB(sx, sy);
    }

    private static int shade(int argb, float factor) {
        int a = argb >>> 24;
        if (a == 0) {
            return 0;
        }
        int r = Math.round(((argb >> 16) & 0xFF) * factor);
        int g = Math.round(((argb >> 8) & 0xFF) * factor);
        int b = Math.round((argb & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int averageOpaque(BufferedImage face) {
        long r = 0;
        long g = 0;
        long b = 0;
        int n = 0;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                int argb = face.getRGB(x, y);
                if ((argb >>> 24) > 0) {
                    r += (argb >> 16) & 0xFF;
                    g += (argb >> 8) & 0xFF;
                    b += argb & 0xFF;
                    n++;
                }
            }
        }
        if (n == 0) {
            return 0;
        }
        return 0xFF000000 | ((int) (r / n) << 16) | ((int) (g / n) << 8) | (int) (b / n);
    }

    private static void printLayout() {
        int u = TEX_OFF_U;
        int v = TEX_OFF_V;
        int w = BOX_W;
        int h = BOX_H;
        int d = BOX_D;
        System.out.printf("盒子 addBox(-5, -9, -5, %d, %d, %d) @ texOffs(%d, %d)，画布 %dx%d%n",
                w, h, d, u, v, TEX, TEX);
        System.out.printf("  正脸(north) @(%2d,%2d) %2dx%-2d  中间 8x8 在 (%d,%d)%n",
                u + d, v + d, w, h, u + d + 1, v + d + 1);
        System.out.printf("  右侧(west)  @(%2d,%2d) %2dx%-2d%n", u, v + d, d, h);
        System.out.printf("  左侧(east)  @(%2d,%2d) %2dx%-2d%n", u + d + w, v + d, d, h);
        System.out.printf("  顶(up)      @(%2d,%2d) %2dx%-2d%n", u + d, v, w, d);
        System.out.printf("  底(down)    @(%2d,%2d) %2dx%-2d%n", u + d + w, v, w, d);
        System.out.printf("  内面(south) @(%2d,%2d) %2dx%-2d%n", u + d + w + d, v + d, w, h);
    }

    /** 示例 8x8：整体实心 + 两个眼洞（透出玩家自己的眼睛）+ 一圈深色描边。 */
    private static BufferedImage demoFace() {
        BufferedImage face = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        int base = 0xFFB08A5A;
        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                boolean border = x == 0 || y == 0 || x == 7 || y == 7;
                face.setRGB(x, y, border ? 0xFF6E5232 : base);
            }
        }
        // 原版 Steve 的眼睛位置：局部第 4 行，第 1~2 列与第 5~6 列
        for (int x : new int[] {1, 2, 5, 6}) {
            face.setRGB(x, 4, 0x00000000);
        }
        // 鼻梁高光
        face.setRGB(3, 5, 0xFFD8B47C);
        face.setRGB(4, 5, 0xFFD8B47C);
        return face;
    }

    private MaskTextureBuilder() {
    }
}
