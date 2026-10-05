import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

/** Pixel atlas for RavenModel: original charcoal feathers, cool edges and blood-red eyes. */
public final class RavenTextureBuilder {
    public static void main(String[] args) throws Exception {
        BufferedImage texture = new BufferedImage(128, 64, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 128; x++) {
                int shade = ((x * 17 + y * 31) % 7) - 3;
                texture.setRGB(x, y, color(20 + shade, 23 + shade, 31 + shade));
            }
        }
        feathers(texture, 0, 0, 44, 21);       // torso, 8 x 7 x 14
        feathers(texture, 0, 24, 24, 12);      // head, 6 x 6 x 6
        feathers(texture, 48, 0, 30, 9);       // shoulder, 8 x 2 x 7
        feathers(texture, 48, 16, 28, 19);     // individual flight feathers
        feathers(texture, 80, 16, 32, 6);     // wing tip
        feathers(texture, 32, 40, 22, 19);    // stepped wedge tail
        fill(texture, 112, 0, 16, 8, color(39, 42, 48));
        fill(texture, 112, 10, 6, 2, color(30, 32, 38));
        fill(texture, 0, 40, 4, 4, color(44, 43, 47));
        fill(texture, 8, 40, 14, 5, color(36, 35, 40));
        // Only the separate eye cubes use this island; all their faces are red.
        fill(texture, 96, 0, 8, 4, color(184, 8, 24));
        texture.setRGB(97, 1, color(242, 32, 43));
        Path output = Path.of("src/main/resources/assets/maledict/textures/entity/raven.png");
        Files.createDirectories(output.getParent());
        ImageIO.write(texture, "png", output.toFile());
    }

    private static void feathers(BufferedImage image, int x, int y, int width, int height) {
        for (int v = 0; v < height; v++) {
            for (int u = 0; u < width; u++) {
                int edge = (u + v / 4) % 4 == 0 ? 8 : 0;
                int vane = v % 5 == 0 ? 3 : 0;
                image.setRGB(x + u, y + v, color(18 + edge + vane, 21 + edge + vane, 29 + edge + vane));
            }
        }
    }

    private static void fill(BufferedImage image, int x, int y, int width, int height, int color) {
        for (int v = y; v < y + height; v++) {
            for (int u = x; u < x + width; u++) {
                image.setRGB(u, v, color);
            }
        }
    }

    private static int color(int r, int g, int b) {
        return 0xFF000000 | r << 16 | g << 8 | b;
    }
}
