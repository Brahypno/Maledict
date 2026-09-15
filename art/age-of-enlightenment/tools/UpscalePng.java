import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 把一张像素贴图按整数倍放大成便于查看的预览图（最近邻，带 texel 网格和坐标刻度）。
 *
 * 用法：java -cp build/mask-tool UpscalePng <输入.png> <输出.png> [倍数]
 */
public final class UpscalePng {

    private static final int MARGIN = 28;

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("usage: UpscalePng <in.png> <out.png> [scale]");
            System.exit(1);
        }
        int scale = args.length > 2 ? Integer.parseInt(args[2]) : 6;
        BufferedImage source = ImageIO.read(new File(args[0]));
        if (source == null) {
            throw new IOException("not an image: " + args[0]);
        }

        int w = source.getWidth();
        int h = source.getHeight();
        BufferedImage image = new BufferedImage(
                w * scale + MARGIN, h * scale + MARGIN, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x1A, 0x1A, 0x1E));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                // 浅色棋盘：深色贴图（比如黑礼帽）才看得清形状
                g.setColor(((x >> 2) + (y >> 2)) % 2 == 0
                        ? new Color(0xC4, 0xC4, 0xCA)
                        : new Color(0xA0, 0xA0, 0xA8));
                g.fillRect(MARGIN + x * scale, MARGIN + y * scale, scale, scale);
            }
        }
        g.drawImage(source, MARGIN, MARGIN, w * scale, h * scale, null);

        for (int i = 0; i <= w; i++) {
            g.setColor(i % 8 == 0 ? new Color(0, 0, 0, 70) : new Color(0, 0, 0, 26));
            g.drawLine(MARGIN + i * scale, MARGIN, MARGIN + i * scale, MARGIN + h * scale);
        }
        for (int i = 0; i <= h; i++) {
            g.setColor(i % 8 == 0 ? new Color(0, 0, 0, 70) : new Color(0, 0, 0, 26));
            g.drawLine(MARGIN, MARGIN + i * scale, MARGIN + w * scale, MARGIN + i * scale);
        }

        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        g.setColor(new Color(0xC8, 0xC8, 0xD0));
        for (int i = 0; i <= w; i += 16) {
            g.drawString(Integer.toString(i), MARGIN + i * scale - 6, MARGIN - 8);
        }
        for (int i = 0; i <= h; i += 16) {
            g.drawString(Integer.toString(i), 4, MARGIN + i * scale + 4);
        }
        g.setStroke(new BasicStroke(1F));
        g.setColor(new Color(0, 0, 0, 120));
        g.drawRect(MARGIN - 1, MARGIN - 1, w * scale + 1, h * scale + 1);
        g.dispose();

        Path out = Path.of(args[1]);
        ImageIO.write(image, "png", out.toFile());
        System.out.printf("%s %dx%d -> %s %dx%d (x%d)%n",
                args[0], w, h, out, image.getWidth(), image.getHeight(), scale);
    }

    private UpscalePng() {
    }
}
