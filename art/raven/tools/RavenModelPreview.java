import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.brahypno.maledict.client.model.RavenModel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Renders the actual baked Minecraft model and UVs, without a game window or a second geometry. */
public final class RavenModelPreview {
    record Vertex(double x, double y, double z, float u, float v, float nx, float ny, float nz) {}
    record Screen(double x, double y, double depth, Vertex vertex) {}

    static final class Collector implements VertexConsumer {
        final List<Vertex> vertices = new ArrayList<>();
        double x, y, z;
        float u, v, nx, ny, nz;
        public VertexConsumer vertex(double x, double y, double z) { this.x=x; this.y=y; this.z=z; return this; }
        public VertexConsumer color(int r, int g, int b, int a) { return this; }
        public VertexConsumer uv(float u, float v) { this.u=u; this.v=v; return this; }
        public VertexConsumer overlayCoords(int u, int v) { return this; }
        public VertexConsumer uv2(int u, int v) { return this; }
        public VertexConsumer normal(float x, float y, float z) { nx=x; ny=y; nz=z; return this; }
        public void endVertex() { vertices.add(new Vertex(x,y,z,u,v,nx,ny,nz)); }
        public void defaultColor(int r, int g, int b, int a) {}
        public void unsetDefaultColor() {}
    }

    public static void main(String[] args) throws Exception {
        Path workspace = args.length > 0 ? Path.of(args[0]) : Path.of(".");
        BufferedImage texture = ImageIO.read(workspace.resolve("src/main/resources/assets/maledict/textures/entity/raven.png").toFile());
        BufferedImage result = new BufferedImage(1280, 620, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        graphics.setColor(new Color(222, 225, 232));
        graphics.fillRect(0,0,1280,620);
        graphics.setColor(new Color(33, 38, 48));
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        graphics.drawString("RAVEN  /  BAKED MINECRAFT MODEL", 32, 40);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 17));
        graphics.drawString("Folded wings / ground", 210, 90);
        graphics.drawString("Spread wings / flight", 850, 90);
        graphics.drawString("Original geometry and texture. 16 model units = 1 block. Software preview, not an in-game capture.", 32, 593);
        graphics.dispose();
        render(result, texture, 0, 0.0F);
        render(result, texture, 640, 1.0F);
        Path output = workspace.resolve("art/raven/preview/raven.png");
        Files.createDirectories(output.getParent());
        ImageIO.write(result, "png", output.toFile());
    }

    static void render(BufferedImage output, BufferedImage texture, int offset, float flight) throws Exception {
        ModelPart root = RavenModel.createBodyLayer().bakeRoot();
        ModelPart body = root.getChild("body");
        Method fold = RavenModel.class.getDeclaredMethod("foldWing", ModelPart.class, int.class, float.class, float.class);
        fold.setAccessible(true);
        fold.invoke(null, body.getChild("left_wing"), 1, flight, 0.0F);
        fold.invoke(null, body.getChild("right_wing"), -1, flight, 0.0F);
        body.getChild("left_leg").xRot = -flight * 0.85F;
        body.getChild("right_leg").xRot = -flight * 0.85F;
        Collector consumer = new Collector();
        root.render(new PoseStack(), consumer, 0xF000F0, 0);
        double[] depthBuffer = new double[output.getWidth()*output.getHeight()];
        Arrays.fill(depthBuffer, Double.NEGATIVE_INFINITY);
        for (int i=0; i<consumer.vertices.size(); i+=4) {
            Screen[] quad = new Screen[4];
            for (int k=0;k<4;k++) quad[k]=project(consumer.vertices.get(i+k), offset);
            triangle(output, texture, depthBuffer, quad[0],quad[1],quad[2]);
            triangle(output, texture, depthBuffer, quad[0],quad[2],quad[3]);
        }
        double[] min={Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY,Double.POSITIVE_INFINITY};
        double[] max={Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY};
        for (Vertex vertex : consumer.vertices) {
            double[] xyz={vertex.x,vertex.y,vertex.z};
            for(int i=0;i<3;i++) { min[i]=Math.min(min[i],xyz[i]); max[i]=Math.max(max[i],xyz[i]); }
        }
        String bounds=String.format(java.util.Locale.ROOT,"Bounds: %.2f wide x %.2f long x %.2f high",max[0]-min[0],max[2]-min[2],max[1]-min[1]);
        System.out.println((flight==0 ? "Ground: " : "Flight: ")+bounds+", cubes="+consumer.vertices.size()/24);
        if (max[0]-min[0] > (flight==0 ? 1.01 : 3.01) || max[2]-min[2] > 2.01 || max[1]-min[1] > 1.01) {
            throw new IllegalStateException("Raven model exceeds requested dimensions");
        }
        Graphics2D graphics=output.createGraphics();
        graphics.setColor(new Color(33,38,48));
        graphics.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,16));
        graphics.drawString(bounds, offset+120, 548);
        graphics.dispose();
    }

    static Screen project(Vertex vertex, int offset) {
        double yaw=0.6, pitch=0.45;
        double horizontal=Math.cos(yaw)*vertex.x+Math.sin(yaw)*vertex.z;
        double near=Math.sin(yaw)*vertex.x-Math.cos(yaw)*vertex.z;
        double vertical=(vertex.y-0.8)*Math.cos(pitch)+near*Math.sin(pitch);
        double depth=near*Math.cos(pitch)-(vertex.y-0.8)*Math.sin(pitch);
        return new Screen(offset+320+horizontal*165,310+vertical*165,depth,vertex);
    }

    static double edge(Screen a, Screen b, double x, double y) {
        return (x-a.x)*(b.y-a.y)-(y-a.y)*(b.x-a.x);
    }

    static void triangle(BufferedImage output, BufferedImage texture, double[] depthBuffer, Screen a, Screen b, Screen c) {
        double area=edge(a,b,c.x,c.y);
        if (Math.abs(area)<1E-7) return;
        int minX=Math.max(0,(int)Math.floor(Math.min(a.x,Math.min(b.x,c.x))));
        int maxX=Math.min(output.getWidth()-1,(int)Math.ceil(Math.max(a.x,Math.max(b.x,c.x))));
        int minY=Math.max(100,(int)Math.floor(Math.min(a.y,Math.min(b.y,c.y))));
        int maxY=Math.min(525,(int)Math.ceil(Math.max(a.y,Math.max(b.y,c.y))));
        double lighting=0.65+0.35*Math.max(0, a.vertex.nx*0.3-a.vertex.ny*0.8-a.vertex.nz*0.5);
        for(int y=minY;y<=maxY;y++) for(int x=minX;x<=maxX;x++) {
            double wa=edge(b,c,x+0.5,y+0.5)/area;
            double wb=edge(c,a,x+0.5,y+0.5)/area;
            double wc=1-wa-wb;
            if(wa<0 || wb<0 || wc<0) continue;
            double depth=wa*a.depth+wb*b.depth+wc*c.depth;
            int index=y*output.getWidth()+x;
            if(depth<depthBuffer[index]) continue;
            depthBuffer[index]=depth;
            double u=wa*a.vertex.u+wb*b.vertex.u+wc*c.vertex.u;
            double v=wa*a.vertex.v+wb*b.vertex.v+wc*c.vertex.v;
            int pixel=texture.getRGB(Math.max(0,Math.min(127,(int)(u*128))),Math.max(0,Math.min(63,(int)(v*64))));
            int r=(int)((pixel>>16&255)*lighting),g=(int)((pixel>>8&255)*lighting),bl=(int)((pixel&255)*lighting);
            output.setRGB(x,y,r<<16|g<<8|bl);
        }
    }
}
