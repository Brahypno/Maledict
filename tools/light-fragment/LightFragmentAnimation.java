import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.IIOImage;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

/** Pixel-grid animation of the approved sprite; --install copies the reviewed resources into the mod. */
public final class LightFragmentAnimation {
    private static final int SIZE = 16, FRAMES = 96, TICKS = 4, HALO_STEPS = 8;
    private static final int GOLD_DARK = 0xff8c6636, GOLD = 0xffc59b53;
    private final BufferedImage shell = image(), core = image();
    private record Crack(int x, int y, int color) {}
    private final List<Crack> cracks = new ArrayList<>();

    private LightFragmentAnimation(Path source) throws Exception {
        BufferedImage base = ImageIO.read(source.resolve("light_fragment.png").toFile());
        BufferedImage glow = ImageIO.read(source.resolve("light_fragment_glow.png").toFile());
        for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) {
            int b = base.getRGB(x, y), g = glow.getRGB(x, y);
            if (b != GOLD && b != GOLD_DARK) shell.setRGB(x, y, b);
            if ((g >>> 24) != 0) {
                if (x >= 7 && x <= 9 && y >= 6 && y <= 10) core.setRGB(x, y, g);
                else {
                    cracks.add(new Crack(x, y, g));
                    shell.setRGB(x, y, 0xff51365f);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length == 0 ? "." : args[0]);
        Path output = root.resolve("art/light-fragment/animation-v7-breathing-soul");
        Files.createDirectories(output);
        var animator = new LightFragmentAnimation(root.resolve("art/light-fragment/candidate-v1"));
        BufferedImage baseStrip = new BufferedImage(16, 16 * FRAMES, BufferedImage.TYPE_INT_ARGB);
        BufferedImage glowStrip = new BufferedImage(16, 16 * FRAMES, BufferedImage.TYPE_INT_ARGB);
        BufferedImage sheet = new BufferedImage(8 * 128, 12 * 128, BufferedImage.TYPE_INT_RGB);
        List<BufferedImage> previews = new ArrayList<>(), comparisons = new ArrayList<>();
        int minimumCore = Integer.MAX_VALUE;
        for (int frame = 0; frame < FRAMES; frame++) {
            BufferedImage combined = animator.frame(frame);
            BufferedImage base = image(), glow = image();
            int coreCount = 0, crackCount = 0;
            for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) {
                int pixel = combined.getRGB(x, y), alpha = pixel >>> 24;
                if (alpha != 0 && alpha != 255) throw new IllegalStateException("Partial alpha");
                boolean emits = pixel == 0xffe8c77a || pixel == 0xfffff0c4 || pixel == 0xfffffdf3;
                (emits ? glow : base).setRGB(x, y, pixel);
                baseStrip.setRGB(x, frame * SIZE + y, emits ? 0 : pixel);
                glowStrip.setRGB(x, frame * SIZE + y, emits ? pixel : 0);
                if (emits) {
                    if (x >= 7 && x <= 9 && y >= 5 && y <= 11) coreCount++;
                    else crackCount++;
                }
            }
            minimumCore = Math.min(minimumCore, coreCount);
            if (coreCount < 5) throw new IllegalStateException("Core lost at frame " + frame);
            if (crackCount != animator.cracks.size()) throw new IllegalStateException("Crack texel lost at frame " + frame);
            verifyCrackPairs(glow);
            BufferedImage preview = preview(base, glow, false, 16);
            previews.add(preview);
            BufferedImage comparison = new BufferedImage(544, 296, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = comparison.createGraphics();
            graphics.setColor(new Color(0x17151d));
            graphics.fillRect(0, 0, 544, 296);
            graphics.drawImage(preview, 8, 30, null);
            graphics.drawImage(preview(base, glow, true, 16), 280, 30, null);
            graphics.setColor(new Color(0xc8c2ce));
            graphics.drawString("COMPOSITE / 16x", 8, 20);
            graphics.drawString("DARK PREVIEW / 16x", 280, 20);
            graphics.dispose();
            comparisons.add(comparison);
            Graphics2D contact = sheet.createGraphics();
            contact.drawImage(preview(base, glow, false, 8), (frame % 8) * 128, (frame / 8) * 128, null);
            contact.dispose();
        }
        if (!Arrays.equals(animator.frame(0).getRGB(0, 0, 16, 16, null, 0, 16),
                animator.frame(FRAMES).getRGB(0, 0, 16, 16, null, 0, 16))) {
            throw new IllegalStateException("Loop does not close");
        }
        ImageIO.write(baseStrip, "png", output.resolve("light_fragment.png").toFile());
        ImageIO.write(glowStrip, "png", output.resolve("light_fragment_glow.png").toFile());
        ImageIO.write(sheet, "png", output.resolve("contact-sheet.png").toFile());
        ImageIO.write(previews.get(0), "png", output.resolve("first-frame.png").toFile());
        String metadata = """
                {
                  "animation": {
                    "width": 16,
                    "height": 16,
                    "frametime": %d,
                    "interpolate": false
                  }
                }
                """.formatted(TICKS);
        Files.writeString(output.resolve("light_fragment.png.mcmeta"), metadata);
        Files.writeString(output.resolve("light_fragment_glow.png.mcmeta"), metadata);
        gif(output.resolve("light_fragment_animation_256.gif"), previews);
        gif(output.resolve("light_fragment_comparison.gif"), comparisons);
        Files.writeString(output.resolve("README.md"), """
                # 光的残片：动画候选 v7（灵魂浮动，恢复金环前景显示）

                每帧原生 16×16；96 帧纵向排列为 16×1536 RGBA PNG。
                普通层与发光层具有完全相同的 mcmeta：每帧 4 ticks，关闭插值，总周期 19.2 秒。

                - 紫色破壳与其发光裂缝：屏幕平面顺时针，每周期 1 圈（19.2 秒）。
                - 外围金色残环：屏幕平面顺时针，每周期 2 圈（每圈 9.6 秒）。
                  每圈仅 8 个角度，每个角度停留 6 帧（1.2 秒），省略中间角度。
                - 中央光核：绕屏幕内上下轴自转，每周期 4 圈（每圈 4.8 秒）。
                  按旋转角投影宽度与正背面左右关系；侧面保留至少一列发光像素。
                  同时以 4.8 秒为周期，在原位、上浮 1 像素、原位、下沉 1 像素间循环。
                  整体平移光核，不改变颜色、形状或尺寸。

                光源固定在屏幕左上方：破壳每帧先旋转几何，再重算受光；深色轮廓保留。
                金色残环使用固定半径、厚度、弧长的两个刚性圆弧，仅栅格化 8 个方向。
                金环每个屏幕位置的明暗恒定，不携带初始贴图的亮暗顺序旋转。
                灵魂及裂缝仍是自发光材质，保持原亮度。
                两处裂缝各是一块双像素形状，两个像素始终共用一条完整边。
                每处裂缝仅投影整体中心，再选择水平或竖直的刚性双像素模板；不分别旋转两个像素。
                裂缝方向按四个直角方向切换，避免旋转取整让裂缝伸长、断开或斜向分离。
                整组定位到距离投影中心最近的石壳表面位置，四个裂缝像素始终保留。
                撤回金环被石壳遮挡的修改：外围金环恢复在石壳前方显示，交叉时不会消失。
                放大图使用整数最近邻；暗处图仅为普通层乘以 1/4 的视觉示意，并非游戏截图。
                这里保存独立候选与预览；附加 --install 参数才会复制四份资源到 src/main/resources。
                物品模型由 MaledictItemModels 生成，更新模型须运行 runData。

                重新生成：Java 17 运行 tools/light-fragment/LightFragmentAnimation.java。
                """);
        if (args.length > 1 && args[1].equals("--install")) {
            Path textures = root.resolve("src/main/resources/assets/maledict/textures/item");
            for (String file : List.of("light_fragment.png", "light_fragment_glow.png",
                    "light_fragment.png.mcmeta", "light_fragment_glow.png.mcmeta")) {
                Files.copy(output.resolve(file), textures.resolve(file), StandardCopyOption.REPLACE_EXISTING);
            }
            System.out.println("Installed approved animation textures and metadata into src/main/resources.");
        }
        System.out.printf("Verified %d synchronized 16x16 frames; RGBA binary alpha; minimum core %d pixels; closed loop; %d ticks/frame.%n",
                FRAMES, minimumCore, TICKS);
    }

    private BufferedImage frame(int frame) {
        double phase = Math.floorMod(frame, FRAMES) * Math.PI * 2 / FRAMES;
        BufferedImage result = image();
        double cos = Math.cos(phase), sin = Math.sin(phase);
        // Inverse nearest-neighbor sampling preserves the shell's solid pixel clusters.
        for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) {
            double dx = x - 7.5, dy = y - 7.5;
            int sx = (int) Math.round(7.5 + dx * cos + dy * sin);
            int sy = (int) Math.round(7.5 - dx * sin + dy * cos);
            if (sx >= 0 && sx < SIZE && sy >= 0 && sy < SIZE) {
                int material = shell.getRGB(sx, sy);
                // Source dark outlines describe the silhouette; source surface brightness is discarded.
                if (material == 0xff35243f || material == 0xff51365f || material == 0xff795181) {
                    material = stoneLight(x, y);
                }
                result.setRGB(x, y, material);
            }
        }
        // Treat each two-texel mark as one rigid domino. Independently rounding the two
        // projected points changes their separation, even when their source anchors are fixed.
        for (int i = 0; i < cracks.size(); i += 2) {
            placeCrackPair(result, cracks.get(i), cracks.get(i + 1), phase);
        }
        // Rotate rigid arc geometry, then light in screen coordinates. Independent movement of
        // rounded source texels made the old tiny arcs appear to crawl and change thickness.
        int haloStep = Math.floorMod(frame, FRAMES) * 2 * HALO_STEPS / FRAMES;
        double haloAngle = haloStep * Math.PI * 2 / HALO_STEPS;
        for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) {
            double dx = x - 7.5, dy = y - 7.5;
            double radius = Math.hypot(dx, dy);
            if (radius < 6.15 || radius > 7.15) continue;
            double angle = Math.atan2(dy, dx) - haloAngle;
            double wrapped = Math.atan2(Math.sin(angle + Math.toRadians(123)), Math.cos(angle + Math.toRadians(123)));
            // Two opposite 34-degree remnants of the same broken circular halo.
            if (Math.abs(wrapped) <= Math.toRadians(17)
                    || Math.abs(wrapped) >= Math.PI - Math.toRadians(17)) {
                result.setRGB(x, y, haloLight(x, y));
            }
        }
        // A double-sided luminous shape projected around its vertical axis. Brightest texel wins
        // when columns coincide edge-on, so the living light remains readable throughout the turn.
        double width = Math.cos(phase * 4);
        BufferedImage projectedCore = image();
        for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) {
            int pixel = core.getRGB(x, y);
            if ((pixel >>> 24) == 0) continue;
            int tx = 8 + (int) Math.round((x - 8) * width);
            int existing = projectedCore.getRGB(tx, y);
            if ((existing >>> 24) == 0 || (pixel & 0xffffff) > (existing & 0xffffff)) projectedCore.setRGB(tx, y, pixel);
        }
        int breathFrame = Math.floorMod(frame, FRAMES / 4);
        int bob = -snapAnchor(Math.sin(breathFrame * Math.PI * 2 / (FRAMES / 4)));
        for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) {
            int pixel = projectedCore.getRGB(x, y);
            if ((pixel >>> 24) != 0) result.setRGB(x, y + bob, pixel);
        }
        return result;
    }

    private static BufferedImage image() { return new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB); }

    private static void placeCrackPair(BufferedImage target, Crack first, Crack second, double angle) {
        double cx = (first.x() + second.x()) * 0.5 - 7.5;
        double cy = (first.y() + second.y()) * 0.5 - 7.5;
        double projectedX = 7.5 + cx * Math.cos(angle) - cy * Math.sin(angle);
        double projectedY = 7.5 + cx * Math.sin(angle) + cy * Math.cos(angle);
        double sourceDirection = Math.atan2(second.y() - first.y(), second.x() - first.x());
        int direction = Math.floorMod(snapAnchor((sourceDirection + angle) / (Math.PI * 0.5)), 4);
        int ux = new int[]{1, 0, -1, 0}[direction];
        int uy = new int[]{0, 1, 0, -1}[direction];
        int nominalX = snapAnchor(projectedX - ux * 0.5);
        int nominalY = snapAnchor(projectedY - uy * 0.5);
        int bestX = -1, bestY = -1;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
            int x = nominalX + dx, y = nominalY + dy;
            if (!occupied(target, x, y) || !occupied(target, x + ux, y + uy)) continue;
            double distance = Math.pow(x + ux * 0.5 - projectedX, 2)
                    + Math.pow(y + uy * 0.5 - projectedY, 2);
            if (distance < bestDistance - 1e-9) {
                bestDistance = distance;
                bestX = x;
                bestY = y;
            }
        }
        if (bestX < 0) throw new IllegalStateException("No attached rigid crack pose");
        target.setRGB(bestX, bestY, first.color());
        target.setRGB(bestX + ux, bestY + uy, second.color());
    }

    private static boolean occupied(BufferedImage target, int x, int y) {
        return x >= 0 && x < SIZE && y >= 0 && y < SIZE && (target.getRGB(x, y) >>> 24) != 0;
    }

    private static void verifyCrackPairs(BufferedImage glow) {
        for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++) {
            if (!isCrack(glow, x, y)) continue;
            int neighbors = 0;
            for (int[] offset : new int[][]{{-1, 0}, {1, 0}, {0, -1}, {0, 1}}) {
                if (isCrack(glow, x + offset[0], y + offset[1])) neighbors++;
            }
            if (neighbors != 1) throw new IllegalStateException("Crack pair separated or merged");
        }
    }

    private static boolean isCrack(BufferedImage glow, int x, int y) {
        return !(x >= 7 && x <= 9 && y >= 5 && y <= 11) && occupied(glow, x, y);
    }

    private static int snapAnchor(double position) {
        // Choose the same texel at exact half-pixel ties despite trigonometric rounding noise.
        return (int) Math.floor(position + 0.5 + 1e-9);
    }

    private static int stoneLight(int x, int y) {
        // Fixed screen-space light field, quantized to the original four stone colors.
        int diagonal = x + y;
        if (diagonal <= 10) return 0xff795181;
        if (diagonal <= 16) return 0xff51365f;
        if (diagonal <= 23) return 0xff35243f;
        return 0xff211629;
    }

    private static int haloLight(int x, int y) {
        return x + y <= 15 ? GOLD : GOLD_DARK;
    }

    private static BufferedImage preview(BufferedImage base, BufferedImage glow, boolean dark, int scale) {
        BufferedImage result = new BufferedImage(SIZE * scale, SIZE * scale, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < result.getHeight(); y++) for (int x = 0; x < result.getWidth(); x++) {
            int b = base.getRGB(x / scale, y / scale), g = glow.getRGB(x / scale, y / scale);
            int pixel = dark ? 0x100e16 : (((x / scale + y / scale) % 2 == 0) ? 0x34303d : 0x2c2834);
            if ((b >>> 24) != 0) pixel = dark
                    ? (((b >> 16) & 255) / 4 << 16) | (((b >> 8) & 255) / 4 << 8) | ((b & 255) / 4)
                    : b & 0xffffff;
            if ((g >>> 24) != 0) pixel = g & 0xffffff;
            result.setRGB(x, y, pixel);
        }
        return result;
    }

    private static void gif(Path path, List<BufferedImage> frames) throws Exception {
        ImageWriter writer = ImageIO.getImageWritersBySuffix("gif").next();
        try (ImageOutputStream output = ImageIO.createImageOutputStream(path.toFile())) {
            writer.setOutput(output);
            writer.prepareWriteSequence(null);
            for (int i = 0; i < frames.size(); i++) {
                BufferedImage frame = frames.get(i);
                IIOMetadata metadata = writer.getDefaultImageMetadata(javax.imageio.ImageTypeSpecifier.createFromRenderedImage(frame), null);
                String format = metadata.getNativeMetadataFormatName();
                var tree = (IIOMetadataNode) metadata.getAsTree(format);
                var control = (IIOMetadataNode) tree.getElementsByTagName("GraphicControlExtension").item(0);
                control.setAttribute("disposalMethod", "none");
                control.setAttribute("delayTime", Integer.toString(TICKS * 5));
                control.setAttribute("transparentColorFlag", "FALSE");
                if (i == 0) {
                    var extensions = new IIOMetadataNode("ApplicationExtensions");
                    var loop = new IIOMetadataNode("ApplicationExtension");
                    loop.setAttribute("applicationID", "NETSCAPE");
                    loop.setAttribute("authenticationCode", "2.0");
                    loop.setUserObject(new byte[]{1, 0, 0});
                    extensions.appendChild(loop);
                    tree.appendChild(extensions);
                }
                metadata.setFromTree(format, tree);
                writer.writeToSequence(new IIOImage(frame, null, metadata), null);
            }
            writer.endWriteSequence();
        } finally { writer.dispose(); }
    }
}
