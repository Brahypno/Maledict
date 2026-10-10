package org.brahypno.maledict.client.vfx;

import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.resources.ResourceLocation;
import org.brahypno.maledict.Maledict;
import team.lodestar.lodestone.systems.postprocess.PostProcessor;

/** Lodestone owns the chain; the synced summoning timeline controls its uniforms. */
public final class VicissitudeSummoningPostProcessor extends PostProcessor {
    public static final VicissitudeSummoningPostProcessor INSTANCE = new VicissitudeSummoningPostProcessor();
    private float strength;
    private float flash;

    private VicissitudeSummoningPostProcessor() {
        setActive(false);
    }

    public void setStrength(float strength, float flash) {
        this.strength = strength;
        this.flash = flash;
        setActive(strength > 0.001F || flash > 0.001F);
    }

    @Override
    public ResourceLocation getPostChainLocation() {
        return ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "vicissitude_summoning");
    }

    @Override
    public void beforeProcess(PoseStack viewModelStack) {
        if (effects == null) return;
        for (EffectInstance effect : effects) {
            Uniform strengthUniform = effect.getUniform("Strength");
            Uniform flashUniform = effect.getUniform("Flash");
            if (strengthUniform != null) strengthUniform.set(strength);
            if (flashUniform != null) flashUniform.set(flash);
        }
    }

    @Override
    public void afterProcess() {
    }
}
