package org.brahypno.maledict.client.item.weapon.incursus.rift;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * 资源重载后清掉裂隙花的缓存：刀痕贴图可用性与按帧缓存的 RenderType 都可能已经变化。
 *
 * <p>Shader 本身由着色器注册事件在重载时重新注册，这里不碰它。
 */
public final class IncursusRiftReloadListener extends SimplePreparableReloadListener<Void> {
    @Override
    protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return null;
    }

    @Override
    protected void apply(Void unused, ResourceManager resourceManager, ProfilerFiller profiler) {
        IncursusRiftRenderTypes.invalidate();
    }
}
