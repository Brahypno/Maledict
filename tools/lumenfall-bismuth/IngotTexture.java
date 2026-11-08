import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import javax.imageio.ImageIO;

/** Native texel source: geometry and plane lighting are independent of the iridescent palette. */
public final class IngotTexture {
    private static final int UPPER_EDGE = 0xff9a9daa;
    private static final int LOWER_EDGE = 0xff6a6b78;
    // Walk the left silhouette down toward the bright vertical bevel, then the right cap down.
    private static final int[][] LEFT_EDGE = {{1, 6}, {1, 7}, {1, 8}, {1, 9}, {2, 10}, {3, 11}};
    private static final int[][] RIGHT_EDGE = {{13, 5}, {14, 6}, {14, 7}, {14, 8}};
    // User-marked junction of the iridescent top, front face, and left vertical bevel.
    private static final int[][] BEVEL_HIGHLIGHTS = {
            {2, 7}, {3, 8}, {4, 9}, {4, 10}, {4, 11},
            {5, 9}, {6, 9}, {7, 9}, {8, 9}, {9, 8}, {10, 8}, {11, 7}, {12, 7}, {13, 6}
    };
    // Each character is one final texel, with no scaling, interpolation, or partial alpha.
    // The colored bands run along the long axis; H/h form the narrow front bevel.
    // The full exposed perimeter is shaded separately, keeping the inner bevel bright.
    private static final String[] PIXELS = {
            "................",
            "................",
            "................",
            "..........eg....",
            ".......eaabgt...",
            "....eaabbvVggt..",
            ".ehhaabbvVppPgS.",
            ".LHabbbvVppPhMS.",
            ".LhHVVppPhhMMsS.",
            ".MLhHPhhLMMLsD..",
            "..MLHLLMMLsD....",
            "...MHLMLs.......",
            "....sMMD........",
            "................",
            "................",
            "................"
    };

    public static void main(String[] args) throws Exception {
        Path root = Path.of(".").toAbsolutePath().normalize();
        Path art = root.resolve("art/lumenfall-bismuth");
        Files.createDirectories(art);
        BufferedImage sprite = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            if (PIXELS[y].length() != 16) throw new IllegalStateException("Row " + y + " is not 16 texels");
            for (int x = 0; x < 16; x++) sprite.setRGB(x, y, color(PIXELS[y].charAt(x)));
        }
        shadePerimeter(sprite);
        replaceEdge(sprite, LEFT_EDGE, UPPER_EDGE);
        replaceEdge(sprite, RIGHT_EDGE, LOWER_EDGE);
        brightenBevel(sprite);
        validate(sprite);
        Path candidate = art.resolve("lumenfall_bismuth_ingot-v12.png");
        ImageIO.write(sprite, "png", candidate.toFile());
        ImageIO.write(enlarge(sprite, 16), "png", art.resolve("ingot-v12-preview.png").toFile());
        ImageIO.write(backgrounds(sprite), "png", art.resolve("ingot-v12-backgrounds.png").toFile());
        if (args.length == 1 && args[0].equals("--install")) {
            Path target = root.resolve("src/main/resources/assets/maledict/textures/item/lumenfall_bismuth_ingot.png");
            Path previous = art.resolve("lumenfall_bismuth_ingot-before-v12.png");
            if (Files.exists(target) && !Files.exists(previous)) Files.copy(target, previous);
            Files.copy(candidate, target, StandardCopyOption.REPLACE_EXISTING);
            validate(ImageIO.read(target.toFile()));
            System.out.println("Installed " + target);
        }
    }

    private static void replaceEdge(BufferedImage sprite, int[][] path, int color) {
        for (int[] point : path) sprite.setRGB(point[0], point[1], color);
    }

    private static void brightenBevel(BufferedImage sprite) {
        for (int[] point : BEVEL_HIGHLIGHTS) {
            int original = sprite.getRGB(point[0], point[1]), brighter = 0xff000000;
            // Raise the marked highlights toward white, retaining their silver/pink/gold tint.
            for (int shift : new int[]{16, 8, 0}) {
                int channel = (((original >>> shift) & 255) * 45 + 255 * 55 + 50) / 100;
                brighter |= channel << shift;
            }
            sprite.setRGB(point[0], point[1], brighter);
        }
    }

    /** Two uniform outline tones: lighter upper/back edges and darker lower/front edges. */
    private static void shadePerimeter(BufferedImage sprite) {
        int upperCount = 0, lowerCount = 0;
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            int pixel = sprite.getRGB(x, y);
            if (pixel >>> 24 == 0) continue;
            boolean perimeter = false;
            for (int[] step : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nx = x + step[0], ny = y + step[1];
                if (nx < 0 || ny < 0 || nx >= 16 || ny >= 16
                        || sprite.getRGB(nx, ny) >>> 24 == 0) perimeter = true;
            }
            if (!perimeter) continue;
            // Top contour and the short right cap share one tone. The entire lower
            // contour, including the left end, shares the darker tone. Neither tone
            // is derived from the local rainbow or silver color inside the sprite.
            boolean upper = y == 0 || sprite.getRGB(x, y - 1) >>> 24 == 0 || x == 14;
            sprite.setRGB(x, y, upper ? UPPER_EDGE : LOWER_EDGE);
            if (upper) upperCount++; else lowerCount++;
        }
        System.out.printf("Uniform perimeter tones: %d upper, %d lower texels.%n", upperCount, lowerCount);
    }

    private static int color(char symbol) {
        int rgb = switch (symbol) {
            case '.' -> 0;
            // Original cool silver palette, without the near-black imagegen outline.
            case 'H' -> 0xf1f8fc;
            case 'h' -> 0xdbe6ec;
            case 'L' -> 0xc1c5d0;
            case 'M' -> 0xb0b5c1;
            case 'S', 'e' -> 0x8c8f9b;
            case 's' -> 0x777884;
            case 'D' -> 0x6a6b78;
            // Our aqua/lilac/rose/champagne surface colors, not the reference's metal palette.
            case 'a' -> 0xb9dbdd;
            case 'b' -> 0xa4bfda;
            case 'v' -> 0xaaa5da;
            case 'V' -> 0xc4b0e2;
            case 'p' -> 0xd0a7ce;
            case 'P' -> 0xf2c0c3;
            case 'g' -> 0xfde2b1;
            case 't' -> 0xdccab1;
            default -> throw new IllegalArgumentException("Unknown texel " + symbol);
        };
        return symbol == '.' ? 0 : 0xff000000 | rgb;
    }

    private static BufferedImage enlarge(BufferedImage sprite, int scale) {
        BufferedImage result = new BufferedImage(16 * scale, 16 * scale, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < result.getHeight(); y++) for (int x = 0; x < result.getWidth(); x++) {
            result.setRGB(x, y, sprite.getRGB(x / scale, y / scale));
        }
        return result;
    }

    private static BufferedImage backgrounds(BufferedImage sprite) {
        BufferedImage result = new BufferedImage(512, 256, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 256; y++) for (int x = 0; x < 512; x++) {
            int pixel = sprite.getRGB((x % 256) / 16, y / 16);
            result.setRGB(x, y, pixel >>> 24 == 0 ? (x < 256 ? 0x24252c : 0xe3e6ec) : pixel);
        }
        return result;
    }

    private static void validate(BufferedImage sprite) {
        if (sprite.getWidth() != 16 || sprite.getHeight() != 16) throw new IllegalStateException("Expected 16x16");
        int opaque = 0, start = -1;
        for (int y = 0; y < 16; y++) for (int x = 0; x < 16; x++) {
            int pixel = sprite.getRGB(x, y), alpha = pixel >>> 24;
            if (alpha != 0 && alpha != 255) throw new IllegalStateException("Partial alpha at " + x + "," + y);
            if (alpha == 0 && pixel != 0) throw new IllegalStateException("Hidden RGB outside silhouette");
            if (alpha == 255) {
                opaque++;
                start = y * 16 + x;
                if (x == 0 || y == 0 || x == 15 || y == 15) throw new IllegalStateException("No transparent margin");
            }
        }
        boolean[] seen = new boolean[256];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        if (start >= 0) { queue.add(start); seen[start] = true; }
        int connected = 0;
        while (!queue.isEmpty()) {
            int index = queue.removeFirst(), x = index % 16, y = index / 16;
            connected++;
            for (int[] step : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nx = x + step[0], ny = y + step[1], next = ny * 16 + nx;
                if (nx < 0 || ny < 0 || nx >= 16 || ny >= 16 || seen[next]) continue;
                if (sprite.getRGB(nx, ny) >>> 24 == 255) { seen[next] = true; queue.add(next); }
            }
        }
        if (opaque == 0 || connected != opaque) throw new IllegalStateException("Detached texels");
        System.out.printf("16x16 RGBA; %d opaque texels, %d transparent; no partial alpha or detached pixels.%n", opaque, 256 - opaque);
    }
}
