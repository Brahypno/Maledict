import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;

/** Registers the ImageGen edit to vanilla's 64px UV grid, retaining untouched source pixels. */
public final class WeepingWitherTextures {
    static Path root = Path.of("").toAbsolutePath();
    static Path source = root.resolve("art/weeping-wither/source");
    static Path output = root.resolve("src/main/resources/assets/maledict/textures/entity");
    static BufferedImage vanilla, edit, crackEdit, backEdit;

    public static void main(String[] args) throws Exception {
        vanilla = ImageIO.read(source.resolve("wither.png").toFile());
        edit = ImageIO.read(source.resolve("weeping-wither-imagegen-v2.png").toFile());
        crackEdit = ImageIO.read(source.resolve("weeping-wither-imagegen-v3.png").toFile());
        backEdit = ImageIO.read(source.resolve("weeping-wither-imagegen-v4.png").toFile());
        BufferedImage body = copy(vanilla);
        // Retain vanilla geometry, UV islands and mottling, while extending weathering over
        // the actual head, shoulder and rib surfaces so it reads beyond close-up distance.
        weather(body, 0, 0, 32, 16);
        weather(body, 32, 0, 24, 12);
        weather(body, 0, 16, 46, 6);
        weather(body, 0, 22, 50, 13);
        face(body, 8, 8, 8);
        face(body, 38, 6, 6);
        erodeFace(body, 8, 8, 8);
        erodeFace(body, 38, 6, 6);
        for (int x = 3; x < 23; x++) {
            if (x % 5 < 2) wear(body, x, 19);
            if (x % 7 == 3) body.setRGB(x, 21, 0xff171619);
        }
        for (int x = 26; x < 37; x++) {
            if (x % 4 != 0) wear(body, x, 24);
            if (x % 5 == 0) body.setRGB(x, 25, 0xff222025);
        }
        for (int y = 25; y < 35; y++) {
            body.setRGB(3, y, y % 3 == 0 ? 0xff827a80 : 0xff59545b);
            body.setRGB(4, y, 0xff18131e);
            body.setRGB(5, y, y % 4 == 0 ? 0xff27202e : 0xff6c626e);
        }
        BufferedImage eyes = blank(), fissure = blank();
        // Locate the eyes using the original face's light pixels, retaining the vanilla expression.
        eyes(body, eyes, 8, 8, 8);
        eyes(body, eyes, 38, 6, 6);
        for (int y : new int[]{26, 29, 32}) {
            fissure.setRGB(4, y, 0xff695376);
        }
        addCracks(body, fissure);
        BufferedImage reforming = copy(body);
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) {
            if ((eyes.getRGB(x, y) >>> 24) != 0) reforming.setRGB(x, y, 0xff16141b);
            if ((fissure.getRGB(x, y) >>> 24) != 0) {
                reforming.setRGB(x, y, blend(body.getRGB(x, y), 0xff8930bc, 0.55));
            }
        }
        for (int y = 25; y < 35; y++) {
            reforming.setRGB(4, y, 0xff493550);
            if (y % 3 == 0) reforming.setRGB(3, y, 0xff91858f);
        }
        BufferedImage invulnerable = copy(body);
        // Keep the same pattern and small-eye expression through vanilla's spawn flicker.
        for (int y = 0; y < 35; y++) for (int x = 0; x < 64; x++) {
            if ((body.getRGB(x, y) >>> 24) != 0) {
                invulnerable.setRGB(x, y, blend(body.getRGB(x, y), 0xff8c8595, 0.32));
            }
        }
        BufferedImage armor = ImageIO.read(source.resolve("wither_armor.png").toFile());
        for (int y = 0; y < armor.getHeight(); y++) for (int x = 0; x < armor.getWidth(); x++) {
            int pixel = armor.getRGB(x, y);
            int lum = ((pixel >> 16 & 255) + (pixel >> 8 & 255) + (pixel & 255)) / 3;
            int r = lum * 76 / 100, g = lum * 22 / 100, b = lum * 95 / 100;
            armor.setRGB(x, y, pixel & 0xff000000 | r << 16 | g << 8 | b);
        }
        Files.createDirectories(output);
        save(body, "weeping_wither"); save(invulnerable, "weeping_wither_invulnerable");
        save(reforming, "weeping_wither_reforming"); save(eyes, "weeping_wither_eyes");
        save(fissure, "weeping_wither_fissure"); save(armor, "weeping_wither_armor");
        saveProjectile(body, eyes, fissure);
        int changed = 0, opaque = 0;
        for (int y = 0; y < 64; y++) for (int x = 0; x < 64; x++) {
            if ((vanilla.getRGB(x, y) >>> 24) != 0) opaque++;
            if (vanilla.getRGB(x, y) != body.getRGB(x, y)) changed++;
            if ((vanilla.getRGB(x, y) >>> 24) != (body.getRGB(x, y) >>> 24)) {
                throw new IllegalStateException("Changed vanilla UV alpha at " + x + "," + y);
            }
        }
        System.out.println("64x64 vanilla UVs preserved; changed " + changed + " of " + opaque + " opaque source pixels.");
        BufferedImage sheet = new BufferedImage(1152, 450, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = sheet.createGraphics();
        graphics.setColor(new Color(43, 44, 49)); graphics.fillRect(0, 0, 1152, 450);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18)); graphics.setColor(Color.WHITE);
        BufferedImage[] tiles = {vanilla, body, reforming};
        String[] labels = {"Vanilla / original pixels", "Weeping / registered edit", "Reforming / eyes extinguished"};
        for (int i = 0; i < tiles.length; i++) {
            graphics.drawString(labels[i], i * 384 + 16, 30);
            graphics.drawImage(tiles[i], i * 384, 50, 384, 384, null);
        }
        graphics.dispose();
        ImageIO.write(sheet, "png", root.resolve("art/weeping-wither/preview/textures.png").toFile());
    }

    static void face(BufferedImage target, int left, int top, int width) {
        for (int y = top; y < top + width; y++) for (int x = left; x < left + width; x++) {
            target.setRGB(x, y, blend(target.getRGB(x, y), sample(x, y), 0.60));
        }
    }

    static void saveProjectile(BufferedImage body, BufferedImage eyes, BufferedImage fissure) throws Exception {
        BufferedImage skull = blank(), charged = blank(), light = blank();
        // Vanilla's projectile cube uses the same 8x8 head unwrap, offset 35 pixels down.
        // Repack approved boss pixels exactly; do not paint an independent skull design.
        for (int y = 0; y < 16; y++) for (int x = 0; x < 32; x++) {
            int pixel = body.getRGB(x, y);
            skull.setRGB(x, y + 35, pixel);
            charged.setRGB(x, y + 35, (pixel >>> 24) == 0 ? pixel : blend(pixel, 0xff5d2884, 0.30));
            int eye = eyes.getRGB(x, y);
            light.setRGB(x, y + 35, (eye >>> 24) != 0 ? eye : fissure.getRGB(x, y));
        }
        save(skull, "weeping_wither_skull");
        save(charged, "weeping_wither_skull_charged");
        save(light, "weeping_wither_skull_emissive");
    }

    static void addCracks(BufferedImage body, BufferedImage mask) {
        // Register branching cracks onto real UV faces. Purple edits elsewhere in the generated
        // atlas are excluded so the eyes, charcoal bone and unchanged UV layout remain intact.
        int[][] cracks = {
                {12,8},{12,9},{11,10},{10,10},{13,10},
                {3,8},{3,9},{4,10},{4,11},{5,12},
                {19,8},{19,9},{18,10},{18,11},
                {41,6},{41,7},{40,7},
                {34,6},{34,7},{35,8},{35,9},
                {47,6},{47,7},{48,8},
                {5,19},{6,20},{7,20},{8,21},
                {18,19},{17,20},{16,20},{16,21},
                {28,24},{29,25},{34,24},{33,25},
                {4,25},{4,26},{4,27},{3,28},{4,29},{4,30},{5,31},{4,32},{4,33},
                {16,26},{16,27},{17,28},{16,29}
        };
        paintCracks(body, mask, cracks, crackEdit);
        int[][] rearCracks = {
                // Main skull back (24..31, 8..15), with a branch crossing its side seam.
                {19,12},{20,12},{21,13},{22,13},{23,13},
                {24,13},{25,12},{26,12},{27,11},{27,10},{28,9},{28,8},
                {28,11},{29,11},{30,12},{30,13},{29,14},
                // Shared side-skull back (50..55, 6..11), continuing around the cheek edge.
                {49,9},{50,9},{51,9},{52,8},{53,8},{53,7},{54,6},
                {52,9},{53,10},{54,11},
                // Shoulder beam back (26..45, 19..21).
                {28,19},{29,20},{30,20},{31,21},
                {35,19},{36,20},{37,20},{38,21},{39,20},{40,20},{41,19},
                // Rib backs (39..49, 24..25), shared by the three original rib cubes.
                {41,24},{42,25},{45,24},{46,25},{47,24},
                // Spine back (9..11, 25..34).
                {10,25},{10,26},{11,27},{10,28},{10,29},{9,30},{10,31},{10,32},{11,33},{10,34},
                // Tail back (21..23, 25..30).
                {22,25},{22,26},{21,27},{22,28},{23,29},{22,30}
        };
        paintCracks(body, mask, rearCracks, backEdit);
    }

    static void paintCracks(BufferedImage body, BufferedImage mask, int[][] cracks, BufferedImage sourceEdit) {
        for (int i = 0; i < cracks.length; i++) {
            int x = cracks[i][0], y = cracks[i][1];
            boolean saturated = i % 5 == 1 || i % 11 == 0;
            int generated = sourceEdit.getRGB((int)((x + 0.5) * sourceEdit.getWidth() / 64),
                    (int)((y + 0.5) * sourceEdit.getHeight() / 64));
            int purple = saturated ? 0xff973bd0 : 0xff57306f;
            // Let the generated crack variation affect luminance without washing out the palette.
            int luminance = ((generated >> 16 & 255) + (generated & 255)) / 2;
            body.setRGB(x, y, blend(purple, saturated ? 0xffc050e8 : 0xff774491, luminance / 1024.0));
            mask.setRGB(x, y, saturated ? 0xff9635d6 : 0xff4c2568);
        }
    }

    static void weather(BufferedImage target, int left, int top, int width, int height) {
        for (int y = top; y < top + height; y++) for (int x = left; x < left + width; x++) {
            int pixel = vanilla.getRGB(x, y);
            if ((pixel >>> 24) == 0) continue;
            int lum = ((pixel >> 16 & 255) * 3 + (pixel >> 8 & 255) * 5 + (pixel & 255) * 2) / 10;
            int charcoal = 0xff000000 | lum << 16 | Math.max(0, lum - 3) << 8 | lum;
            target.setRGB(x, y, blend(charcoal, sample(x, y), 0.30));
        }
    }

    static void erodeFace(BufferedImage target, int left, int top, int width) {
        // Broken brows and cheeks surround the original socket and jaw positions.
        int[][] chips = {{0,0}, {1,0}, {0,1}, {width-2,0}, {width-1,1},
                {width-1,2}, {0,width-3}, {width-1,width-2}, {1,width-1}};
        for (int[] chip : chips) wear(target, left + chip[0], top + chip[1]);
        target.setRGB(left + width / 2, top, 0xff19171b);
        target.setRGB(left + width / 2, top + 1, 0xff222025);
        target.setRGB(left + width / 2 - 1, top + 2, 0xff1d1920);
        wear(target, left + width / 2 + 1, top + 1);
        if (width == 6) {
            wear(target, left + 2, top + 1);
            target.setRGB(left + 2, top + width - 1, 0xff151318);
        }
    }

    static void eyes(BufferedImage body, BufferedImage mask, int left, int top, int width) {
        for (int y = top; y <= top + width / 2; y++) for (int x = left; x < left + width; x++) {
            int pixel = vanilla.getRGB(x, y);
            if ((pixel >> 16 & 255) < 110 || (pixel >> 8 & 255) < 110 || (pixel & 255) < 110) continue;
            body.setRGB(x, y, 0xff24212b);
            // A single pinpoint per socket; the rest remains a dark, recognizable eye recess.
            if (x == left || (vanilla.getRGB(x - 1, y) >> 16 & 255) < 110) {
                mask.setRGB(x, y, 0xffa698b7);
                body.setRGB(x, y, 0xff7c6d8b);
                for (int drop = 1; drop <= 3 && y + drop < top + width; drop++) {
                    body.setRGB(x, y + drop, drop == 1 ? 0xff43334e : 0xff302737);
                }
            }
        }
    }

    static void wear(BufferedImage target, int x, int y) {
        target.setRGB(x, y, blend(target.getRGB(x, y), 0xffa39a9e, 0.70));
    }
    static int sample(int x, int y) {
        int pixel = edit.getRGB((int)((x + 0.5) * edit.getWidth() / 64), (int)((y + 0.5) * edit.getHeight() / 64));
        return 0xff000000 | pixel & 0xffffff;
    }
    static int blend(int a, int b, double amount) {
        int rgb = a & 0xff000000;
        for (int shift : new int[]{16, 8, 0}) rgb |= (int)(((a >> shift & 255) * (1 - amount)) + (b >> shift & 255) * amount) << shift;
        return rgb;
    }
    static BufferedImage blank() { return new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB); }
    static BufferedImage copy(BufferedImage image) {
        BufferedImage result = blank();
        result.setRGB(0, 0, 64, 64, image.getRGB(0, 0, 64, 64, null, 0, 64), 0, 64);
        return result;
    }
    static void save(BufferedImage image, String name) throws Exception {
        ImageIO.write(image, "png", output.resolve(name + ".png").toFile());
    }
}
