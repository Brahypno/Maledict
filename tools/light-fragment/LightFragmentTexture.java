import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

/** Native 16x16 redraw of the imagegen reference; every character specifies one final texel. */
public final class LightFragmentTexture {
    private static final String[] PIXELS = {
            "................",
            "...vv..aa.......",
            "..v...addbaa....",
            ".u..addccbgba...",
            "...adccbbabgca..",
            "..adcaa....bba..",
            "...ba...g...aa..",
            ".......gh.......",
            ".......hig......",
            "..da...gih..a...",
            "..dca...g..cba..",
            "..bcda....cbbau.",
            "...bcgbbaccba.v.",
            "....bbgbbbaa.v..",
            "......aaa...u...",
            "................"
    };

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length == 0 ? "." : args[0]);
        Path textures = root.resolve("art/light-fragment/candidate-v1");
        Path previews = root.resolve("art/light-fragment/preview");
        Files.createDirectories(previews);
        Files.createDirectories(textures);
        BufferedImage base = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        BufferedImage glow = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        BufferedImage combined = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int baseCount = 0, glowCount = 0;
        for (int y = 0; y < 16; y++) {
            if (PIXELS[y].length() != 16) throw new IllegalStateException("Row " + y);
            for (int x = 0; x < 16; x++) {
                char symbol = PIXELS[y].charAt(x);
                int pixel = color(symbol);
                boolean emissive = symbol == 'g' || symbol == 'h' || symbol == 'i';
                (emissive ? glow : base).setRGB(x, y, pixel);
                combined.setRGB(x, y, pixel);
                if (pixel != 0) {
                    if (emissive) glowCount++; else baseCount++;
                }
            }
        }
        ImageIO.write(base, "png", textures.resolve("light_fragment.png").toFile());
        ImageIO.write(glow, "png", textures.resolve("light_fragment_glow.png").toFile());
        ImageIO.write(combined, "png", previews.resolve("light_fragment_combined_16.png").toFile());
        ImageIO.write(enlarge(combined, 16), "png", previews.resolve("light_fragment_256.png").toFile());
        ImageIO.write(enlarge(base, 16), "png", previews.resolve("base_256.png").toFile());
        ImageIO.write(enlarge(glow, 16), "png", previews.resolve("glow_256.png").toFile());
        ImageIO.write(comparison(base, glow), "png", previews.resolve("light_fragment_comparison.png").toFile());
        System.out.printf("Native 16x16 RGBA: %d base texels, %d emissive texels; 9 opaque colors; alpha 0/255.%n",
                baseCount, glowCount);
    }

    private static int color(char symbol) {
        return switch (symbol) {
            case '.' -> 0;
            case 'a' -> 0xff211629;
            case 'b' -> 0xff35243f;
            case 'c' -> 0xff51365f;
            case 'd' -> 0xff795181;
            case 'u' -> 0xff8c6636;
            case 'v' -> 0xffc59b53;
            case 'g' -> 0xffe8c77a;
            case 'h' -> 0xfffff0c4;
            case 'i' -> 0xfffffdf3;
            default -> throw new IllegalArgumentException("Unknown texel " + symbol);
        };
    }

    private static BufferedImage enlarge(BufferedImage image, int scale) {
        BufferedImage result = new BufferedImage(16 * scale, 16 * scale, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < result.getHeight(); y++) for (int x = 0; x < result.getWidth(); x++) {
            result.setRGB(x, y, image.getRGB(x / scale, y / scale));
        }
        return result;
    }

    /** Dark panel is an illustrative dimming of layer0, not a capture of Minecraft lighting. */
    private static BufferedImage comparison(BufferedImage base, BufferedImage glow) {
        BufferedImage result = new BufferedImage(648, 264, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        graphics.setColor(new Color(0x17151d));
        graphics.fillRect(0, 0, 648, 264);
        graphics.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        String[] titles = {"COMPOSITE / 12x", "DARK PREVIEW / 12x", "GLOW ONLY / 12x"};
        for (int panel = 0; panel < 3; panel++) {
            int left = 12 + panel * 216;
            graphics.setColor(new Color(0xc8c2ce));
            graphics.drawString(titles[panel], left, 24);
            for (int y = 0; y < 192; y++) for (int x = 0; x < 192; x++) {
                int normal = base.getRGB(x / 12, y / 12);
                int lit = glow.getRGB(x / 12, y / 12);
                int background = panel == 0 ? (((x / 12 + y / 12) % 2 == 0) ? 0x34303d : 0x2c2834) : 0x100e16;
                int pixel = background;
                if (panel != 2 && (normal >>> 24) != 0) {
                    pixel = normal & 0xffffff;
                    if (panel == 1) {
                        pixel = (((normal >> 16) & 255) / 4 << 16)
                                | (((normal >> 8) & 255) / 4 << 8) | ((normal & 255) / 4);
                    }
                }
                if ((lit >>> 24) != 0) pixel = lit & 0xffffff;
                result.setRGB(left + x, 40 + y, pixel);
            }
        }
        graphics.setColor(new Color(0xa49caa));
        graphics.drawString("16x16 native / 9 colors / binary alpha / dark panel is illustrative", 12, 252);
        graphics.dispose();
        return result;
    }
}
