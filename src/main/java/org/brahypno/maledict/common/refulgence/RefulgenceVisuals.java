package org.brahypno.maledict.common.refulgence;

import com.sammy.malum.registry.common.ParticleEffectTypeRegistry;
import com.sammy.malum.visual_effects.networked.data.ColorEffectData;
import net.minecraft.world.entity.LivingEntity;

import java.awt.Color;

/**
 * 反照的表现层：派发 Malum 既有的 {@code HEX_BOLT_IMPACT}（法术弹命中式的爆散），不新增粒子类型
 * 与网络包，客户端也不用改。
 *
 * <p>配色取自共晶材料自己的 {@code tints}：{@code 9ee0e6} 的青与 {@code 7b6a92} 的紫灰，正是法典里
 * 「青紫交错的折辉」。
 */
public final class RefulgenceVisuals {
    private static final Color PRIMARY = new Color(0x9EE0E6);
    private static final Color SECONDARY = new Color(0x7B6A92);

    public static void release(LivingEntity victim) {
        ParticleEffectTypeRegistry.HEX_BOLT_IMPACT.createEntityEffect(
                victim, new ColorEffectData(PRIMARY, SECONDARY));
    }

    private RefulgenceVisuals() {
    }
}
