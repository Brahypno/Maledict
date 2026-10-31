import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.WitherBossModel;
import net.minecraft.client.model.SkullModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import org.brahypno.maledict.client.realm.WeepingWitherModel;
import org.brahypno.maledict.client.realm.WeepingWitherRenderer;
import net.minecraft.client.renderer.entity.WitherSkullRenderer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

/** Software preview of the actual vanilla and modified baked models; no second geometry. */
public final class WeepingWitherPreview {
    record Vertex(double x, double y, double z, float u, float v, float nx, float ny, float nz) {}
    record Screen(double x, double y, double depth, Vertex vertex) {}
    static final class Collector implements VertexConsumer {
        final List<Vertex> vertices = new ArrayList<>();
        double x,y,z;
        float u,v,nx,ny,nz;
        public VertexConsumer vertex(double x,double y,double z) { this.x=x;this.y=y;this.z=z;return this; }
        public VertexConsumer color(int r,int g,int b,int a) { return this; }
        public VertexConsumer uv(float u,float v) { this.u=u;this.v=v;return this; }
        public VertexConsumer overlayCoords(int u,int v) { return this; }
        public VertexConsumer uv2(int u,int v) { return this; }
        public VertexConsumer normal(float x,float y,float z) { nx=x;ny=y;nz=z;return this; }
        public void endVertex() { vertices.add(new Vertex(x,y,z,u,v,nx,ny,nz)); }
        public void defaultColor(int r,int g,int b,int a) {}
        public void unsetDefaultColor() {}
    }
    static Path root;
    static int[] shieldBackground;
    static double projectionScale=122;
    static double eyeGlow=0.55;
    static final int WIDTH=1920, HEIGHT=800, PANEL=480;
    public static void main(String[] args) throws Exception {
        root=Path.of(args[0]);
        BufferedImage output=new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics=output.createGraphics();
        graphics.setColor(new Color(83,85,91));graphics.fillRect(0,0,WIDTH,HEIGHT);
        graphics.setColor(new Color(235,235,239));graphics.setFont(new Font(Font.SANS_SERIF,Font.BOLD,26));
        graphics.drawString("WEEPING WITHER / VANILLA MODEL + TEXTURE DERIVATIVE",30,42);
        graphics.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,19));
        String[] labels={"Vanilla / original", "Weeping / purple fissures", "Half health / violet shield", "Reforming / pulsing fissures"};
        for(int i=0;i<4;i++) graphics.drawString(labels[i],i*PANEL+24,100);
        graphics.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,17));
        graphics.drawString("Actual baked Minecraft cubes and UVs. Same world scale: modified entity is 1.2x vanilla. Software preview, not an in-game capture.",30,760);
        graphics.drawString("Shield uses the actual armor texture and vertex tint. Additive blending approximated; revival particle motion requires an in-game check.",30,786);
        graphics.dispose();
        for(int panel=0;panel<4;panel++) render(output,panel);
        ImageIO.write(output,"png",root.resolve("art/weeping-wither/preview/comparison.png").toFile());
        renderViews();
        renderProjectiles();
    }
    static void render(BufferedImage output,int panel) throws Exception {
        render(output,panel,panel==0,panel==3,panel>=2,panel>=2?0.55:0.0);
    }
    static void renderViews() throws Exception {
        BufferedImage output=new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics=output.createGraphics();
        graphics.setColor(new Color(83,85,91));graphics.fillRect(0,0,WIDTH,HEIGHT);
        graphics.setColor(new Color(235,235,239));graphics.setFont(new Font(Font.SANS_SERIF,Font.BOLD,26));
        graphics.drawString("WEEPING WITHER / CURRENT TEXTURE FROM ALL SIDES",30,42);
        graphics.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,19));
        String[] labels={"Front", "Side", "Back", "Rear three-quarter"};
        for(int i=0;i<4;i++)graphics.drawString(labels[i],i*PANEL+24,100);
        graphics.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,17));
        graphics.drawString("Current production texture and baked model, without the shield. Software preview, not an in-game capture.",30,760);
        graphics.drawString("Purple fissures wrap onto skull backs, shoulder backs, rib backs, spine and tail. Existing front detailing retained.",30,786);
        graphics.dispose();
        double[] yaw={0,Math.PI/2,Math.PI,Math.PI-0.55};
        projectionScale=110;
        for(int panel=0;panel<4;panel++)render(output,panel,false,false,false,yaw[panel]);
        projectionScale=122;
        ImageIO.write(output,"png",root.resolve("art/weeping-wither/preview/all-sides-v4.png").toFile());
    }
    static void renderProjectiles() throws Exception {
        BufferedImage output=new BufferedImage(WIDTH,HEIGHT,BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics=output.createGraphics();
        graphics.setColor(new Color(83,85,91));graphics.fillRect(0,0,WIDTH,HEIGHT);
        graphics.setColor(new Color(235,235,239));graphics.setFont(new Font(Font.SANS_SERIF,Font.BOLD,26));
        graphics.drawString("WEEPING WITHER SKULL / VANILLA PROJECTILE MODEL",30,42);
        graphics.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,19));
        String[] labels={"Normal / front", "Normal / three-quarter", "Charged / three-quarter", "Normal / back"};
        for(int i=0;i<4;i++)graphics.drawString(labels[i],i*PANEL+24,100);
        graphics.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,17));
        graphics.drawString("Actual vanilla projectile cube and UVs; approved boss head pixels repacked into its UV region. Software preview.",30,760);
        graphics.dispose();
        BufferedImage light=ImageIO.read(root.resolve("src/main/resources/assets/maledict/textures/entity/weeping_wither_skull_emissive.png").toFile());
        BufferedImage blank=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);
        projectionScale=360;
        double[] angles={0,0.55,0.55,Math.PI};
        for(int panel=0;panel<4;panel++) {
            ModelPart part=WitherSkullRenderer.createSkullLayer().bakeRoot();
            new SkullModel(part).setupAnim(0,0,0);
            PoseStack pose=new PoseStack();pose.translate(0,1.55,0);
            Collector collector=new Collector();part.render(pose,collector,0xF000F0,0);
            BufferedImage texture=ImageIO.read(root.resolve("src/main/resources/assets/maledict/textures/entity/weeping_wither_skull"+(panel==2?"_charged":"")+".png").toFile());
            double[] depth=new double[WIDTH*HEIGHT];Arrays.fill(depth,Double.NEGATIVE_INFINITY);
            eyeGlow=panel==2?0.95:0.55;
            for(int i=0;i<collector.vertices.size();i+=4) {
                Screen[] quad=new Screen[4];
                for(int j=0;j<4;j++)quad[j]=project(collector.vertices.get(i+j),panel,angles[panel]);
                triangle(output,texture,light,blank,depth,quad[0],quad[1],quad[2],false,false,false);
                triangle(output,texture,light,blank,depth,quad[0],quad[2],quad[3],false,false,false);
            }
            if(collector.vertices.size()!=24)throw new IllegalStateException("Expected original projectile cube");
        }
        projectionScale=122;eyeGlow=0.55;
        ImageIO.write(output,"png",root.resolve("art/weeping-wither/preview/projectiles.png").toFile());
    }
    static void render(BufferedImage output,int panel,boolean vanilla,boolean reforming,boolean shield,double yaw) throws Exception {
        ModelPart part=WitherBossModel.createBodyLayer(CubeDeformation.NONE).bakeRoot();
        WitherBossModel<WitherBoss> model=vanilla?new WitherBossModel<>(part):new WeepingWitherModel(part);
        model.setupAnim(null,0,0,0,0,0);
        if(reforming) reformPose(part);
        PoseStack pose=new PoseStack();float scale=vanilla?2F:2.4F;pose.scale(scale,scale,scale);
        Collector collector=new Collector();part.render(pose,collector,0xF000F0,0);
        String texture=vanilla?"art/weeping-wither/source/wither.png":"src/main/resources/assets/maledict/textures/entity/weeping_wither"+(reforming?"_reforming":"")+".png";
        BufferedImage body=ImageIO.read(root.resolve(texture).toFile());
        BufferedImage eyes=ImageIO.read(root.resolve("src/main/resources/assets/maledict/textures/entity/weeping_wither_eyes.png").toFile());
        BufferedImage fissure=ImageIO.read(root.resolve("src/main/resources/assets/maledict/textures/entity/weeping_wither_fissure.png").toFile());
        double[] depth=new double[WIDTH*HEIGHT];Arrays.fill(depth,Double.NEGATIVE_INFINITY);
        for(int i=0;i<collector.vertices.size();i+=4) {
            Screen[] quad=new Screen[4];
            for(int j=0;j<4;j++) quad[j]=project(collector.vertices.get(i+j),panel,yaw);
            triangle(output,body,eyes,fissure,depth,quad[0],quad[1],quad[2],vanilla,reforming,false);
            triangle(output,body,eyes,fissure,depth,quad[0],quad[2],quad[3],vanilla,reforming,false);
        }
        if(shield) {
            shieldBackground=output.getRGB(0,0,WIDTH,HEIGHT,null,0,WIDTH);
            ModelPart armorPart=WitherBossModel.createBodyLayer(new CubeDeformation(0.5F)).bakeRoot();
            new WeepingWitherModel(armorPart).setupAnim(null,0,0,0,0,0);
            if(reforming) reformPose(armorPart);
            Collector armorCollector=new Collector();armorPart.render(pose,armorCollector,0xF000F0,0);
            BufferedImage armor=ImageIO.read(root.resolve("src/main/resources/assets/maledict/textures/entity/weeping_wither_armor.png").toFile());
            for(int i=0;i<armorCollector.vertices.size();i+=4) {
                Screen[] quad=new Screen[4];
                for(int j=0;j<4;j++)quad[j]=project(armorCollector.vertices.get(i+j),panel,yaw);
                triangle(output,armor,eyes,fissure,depth,quad[0],quad[1],quad[2],false,false,true);
                triangle(output,armor,eyes,fissure,depth,quad[0],quad[2],quad[3],false,false,true);
            }
        }
        if(collector.vertices.size()!=216) throw new IllegalStateException("Expected vanilla's nine cubes");
        System.out.println("Panel "+panel+": original nine cubes, 64x64 UVs, world scale "+scale);
    }
    static void reformPose(ModelPart part) {
        part.getChild("center_head").xRot+=0.13F;
        part.getChild("left_head").xRot+=0.19F;
        part.getChild("right_head").xRot+=0.19F;
    }
    static Screen project(Vertex v,int panel,double yaw) {
        double pitch=0.12;
        double horizontal=Math.cos(yaw)*v.x+Math.sin(yaw)*v.z;
        double near=Math.sin(yaw)*v.x-Math.cos(yaw)*v.z;
        double vertical=(v.y-1.3)*Math.cos(pitch)+near*Math.sin(pitch);
        double depth=near*Math.cos(pitch)-(v.y-1.3)*Math.sin(pitch);
        return new Screen(panel*PANEL+PANEL/2.0+horizontal*projectionScale,410+vertical*projectionScale,depth,v);
    }
    static double edge(Screen a,Screen b,double x,double y) { return (x-a.x)*(b.y-a.y)-(y-a.y)*(b.x-a.x); }
    static void triangle(BufferedImage output,BufferedImage texture,BufferedImage eyes,BufferedImage fissure,double[] depth,
                         Screen a,Screen b,Screen c,boolean vanilla,boolean reforming,boolean shield) {
        double area=edge(a,b,c.x,c.y);if(Math.abs(area)<1e-7)return;
        int minX=Math.max(0,(int)Math.floor(Math.min(a.x,Math.min(b.x,c.x))));
        int maxX=Math.min(WIDTH-1,(int)Math.ceil(Math.max(a.x,Math.max(b.x,c.x))));
        int minY=Math.max(115,(int)Math.floor(Math.min(a.y,Math.min(b.y,c.y))));
        int maxY=Math.min(725,(int)Math.ceil(Math.max(a.y,Math.max(b.y,c.y))));
        double lighting=0.72+0.28*Math.max(0,a.vertex.nx*0.3-a.vertex.ny*0.8-a.vertex.nz*0.5);
        for(int y=minY;y<=maxY;y++)for(int x=minX;x<=maxX;x++) {
            double wa=edge(b,c,x+0.5,y+0.5)/area,wb=edge(c,a,x+0.5,y+0.5)/area,wc=1-wa-wb;
            if(wa<0||wb<0||wc<0)continue;
            double z=wa*a.depth+wb*b.depth+wc*c.depth;int index=y*WIDTH+x;if(z<depth[index])continue;
            double textureU=wa*a.vertex.u+wb*b.vertex.u+wc*c.vertex.u;
            double textureV=wa*a.vertex.v+wb*b.vertex.v+wc*c.vertex.v;
            // Same repeat sampling and UV offsets as energySwirl, at age 30 ticks.
            int u=shield?Math.floorMod((int)Math.floor((textureU+Math.cos(30*0.02)*3)*64),64)
                    :Math.max(0,Math.min(63,(int)(textureU*64)));
            int v=shield?Math.floorMod((int)Math.floor((textureV+0.3)*64),64)
                    :Math.max(0,Math.min(63,(int)(textureV*64)));
            int pixel=texture.getRGB(u,v);if((pixel>>>24)==0)continue;
            depth[index]=z;
            int eye=eyes.getRGB(u,v),crack=fissure.getRGB(u,v);
            int rgb=0;
            for(int shift:new int[]{16,8,0}) {
                double value=(pixel>>shift&255)*lighting;
                if(shield) {
                    double tint=shift==16?WeepingWitherRenderer.ARMOR_RED:shift==8?WeepingWitherRenderer.ARMOR_GREEN:WeepingWitherRenderer.ARMOR_BLUE;
                    value=(shieldBackground[index]>>shift&255)+(pixel>>shift&255)*tint*(pixel>>>24)/255.0;
                } else if(!vanilla) {
                    if(!reforming&&(eye>>>24)!=0)value+=(eye>>shift&255)*eyeGlow;
                    if((crack>>>24)!=0)value+=(crack>>shift&255)*(reforming?0.95:0.50);
                }
                rgb|=Math.min(255,(int)value)<<shift;
            }
            output.setRGB(x,y,rgb);
        }
    }
}
