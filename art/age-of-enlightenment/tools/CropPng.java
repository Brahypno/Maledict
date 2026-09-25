import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * 从一张 PNG 里裁一块另存（不改像素，不做缩放）。
 * 用法：java -cp build/mask-tool CropPng <输入.png> <输出.png> <x> <y> <w> <h>
 */
public final class CropPng {

    public static void main(String[] args) throws IOException {
        if (args.length < 6) {
            System.err.println("usage: CropPng <in.png> <out.png> <x> <y> <w> <h>");
            System.exit(1);
        }
        BufferedImage source = ImageIO.read(new File(args[0]));
        if (source == null) {
            throw new IOException("not an image: " + args[0]);
        }
        int x = Integer.parseInt(args[2]);
        int y = Integer.parseInt(args[3]);
        int w = Integer.parseInt(args[4]);
        int h = Integer.parseInt(args[5]);
        if (x < 0 || y < 0 || x + w > source.getWidth() || y + h > source.getHeight()) {
            throw new IOException("crop out of bounds: " + source.getWidth() + "x" + source.getHeight());
        }
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                out.setRGB(i, j, source.getRGB(x + i, y + j));
            }
        }
        Path outPath = Path.of(args[1]);
        ImageIO.write(out, "png", outPath.toFile());
        System.out.printf("%s -> %s  裁自 (%d,%d) %dx%d%n", args[0], outPath, x, y, w, h);
    }

    private CropPng() {
    }
}
