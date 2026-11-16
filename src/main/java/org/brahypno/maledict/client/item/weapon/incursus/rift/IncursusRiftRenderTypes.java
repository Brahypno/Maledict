package org.brahypno.maledict.client.item.weapon.incursus.rift;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * 裂隙花唯一的绘制通道与它绑定的刀痕贴图。
 *
 * <p>贴图直接引用 Malum 的 {@code heavy_slash_00..03}（末帧重复），所以 Maledict 的 jar 里
 * 不含任何 Malum 资源文件；文件缺失时这里返回 null，由调用方退回旧的粒子斩击。
 *
 * <p>只有一层：裂口内部的内容。早先还有一层边缘高光，会额外给裂口描一圈颜色，
 * 与"一刀划开、后面就是夜空"的观感冲突，已经去掉。
 */
public final class IncursusRiftRenderTypes extends RenderStateShard {
    private static final ResourceLocation[] MASK_TEXTURES = {
            malumTexture("heavy_slash_00"),
            malumTexture("heavy_slash_01"),
            malumTexture("heavy_slash_02"),
            malumTexture("heavy_slash_03"),
            malumTexture("heavy_slash_03")
    };
    private static final String[] MASK_RESOURCE_PATHS = {
            "textures/particle/slash/heavy_slash_00.png",
            "textures/particle/slash/heavy_slash_01.png",
            "textures/particle/slash/heavy_slash_02.png",
            "textures/particle/slash/heavy_slash_03.png",
            "textures/particle/slash/heavy_slash_03.png"
    };

    /**
     * 缓存槽位：bit0 = 是否使用自研 shader，bit1 = 是否尊重世界深度。
     */
    private static final RenderType[] CORE = new RenderType[MASK_TEXTURES.length * 4];
    private static final Boolean[] MASK_PRESENT = new Boolean[MASK_TEXTURES.length];

    private static ShaderStateShard customShader;
    private static ShaderStateShard vanillaShader;

    public static int frameCount() {
        return MASK_TEXTURES.length;
    }

    /**
     * 本帧刀痕贴图是否可用；第一次询问时检查资源管理器，结果随资源重载失效。
     */
    public static boolean maskAvailable(int frame) {
        if (frame < 0 || frame >= MASK_TEXTURES.length){
            return false;
        }
        Boolean cached = MASK_PRESENT[frame];
        if (cached != null){
            return cached;
        }
        boolean present = Minecraft.getInstance().getResourceManager()
                                   .getResource(ResourceLocation.fromNamespaceAndPath("malum", MASK_RESOURCE_PATHS[frame]))
                                   .isPresent();
        MASK_PRESENT[frame] = present;
        return present;
    }

    public static RenderType core(int frame, boolean custom, boolean depthTest) {
        if (frame < 0 || frame >= MASK_TEXTURES.length){
            return null;
        }
        int slot = frame * 4 + (custom ? 1 : 0) + (depthTest ? 2 : 0);
        RenderType cached = CORE[slot];
        if (cached != null){
            return cached;
        }
        if (custom && !IncursusRiftShaders.available()){
            return null;
        }
        if (!maskAvailable(frame)){
            return null;
        }
        RenderType created = create(frame, custom, depthTest, MASK_TEXTURES[frame]);
        CORE[slot] = created;
        return created;
    }

    private static RenderType create(
            int frame, boolean custom, boolean depthTest, ResourceLocation texture) {
        RenderType.CompositeState state = RenderType.CompositeState.builder()
                                                                   .setShaderState(shaderState(custom))
                                                                   .setTextureState(new TextureStateShard(texture, false, false))
                                                                   .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                                                                   .setDepthTestState(depthState(depthTest))
                                                                   // 只写颜色：八片花瓣本来就共处同一水平面（旧效果如此），
                                                                   // 一旦写深度，同深度的面片会互相剔除成斑块。
                                                                   .setWriteMaskState(COLOR_WRITE)
                                                                   .setCullState(NO_CULL)
                                                                   .setLightmapState(NO_LIGHTMAP)
                                                                   .setOverlayState(NO_OVERLAY)
                                                                   .createCompositeState(false);
        return RenderType.create(name(frame, custom, depthTest),
                                 DefaultVertexFormat.POSITION_COLOR_TEX, VertexFormat.Mode.QUADS,
                                 512, false, true, state);
    }

    /**
     * 旧粒子是 noClip，默认不做深度测试；玩家可以在配置里要求尊重世界深度。
     */
    private static DepthTestStateShard depthState(boolean depthTest) {
        return depthTest ? LEQUAL_DEPTH_TEST : NO_DEPTH_TEST;
    }

    private static ShaderStateShard shaderState(boolean custom) {
        if (custom){
            if (customShader == null){
                customShader = new ShaderStateShard(IncursusRiftShaders::shader);
            }
            return customShader;
        }
        if (vanillaShader == null){
            vanillaShader = new ShaderStateShard(GameRenderer::getPositionColorTexShader);
        }
        return vanillaShader;
    }

    private static String name(int frame, boolean custom, boolean depthTest) {
        return "maledict_incursus_rift_" + frame
               + (custom ? "_shader" : "_plain") + (depthTest ? "_depth" : "_noclip");
    }

    /**
     * 绑定用的贴图路径必须带 {@code .png}：TextureStateShard 走的是原始资源路径，
     * 漏掉后缀会拿到原版缺失贴图，整块面片变成紫黑棋盘格。
     */
    private static ResourceLocation malumTexture(String frame) {
        return ResourceLocation.fromNamespaceAndPath("malum", "textures/particle/slash/" + frame + ".png");
    }

    /**
     * 资源重载后 ShaderInstance 与贴图可用性都可能变，缓存的 RenderType 必须重算。
     */
    public static void invalidate() {
        for (int slot = 0; slot < CORE.length; slot++) {
            CORE[slot] = null;
        }
        for (int frame = 0; frame < MASK_PRESENT.length; frame++) {
            MASK_PRESENT[frame] = null;
        }
        customShader = null;
        vanillaShader = null;
    }

    private IncursusRiftRenderTypes() {
        super("maledict_incursus_rift", () -> {
        }, () -> {
        });
    }
}
