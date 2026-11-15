package org.brahypno.maledict.mixin;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import org.brahypno.maledict.common.MaledictTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.lodestar.lodestone.handlers.LodestoneAttributeEventHandler;
import team.lodestar.lodestone.registry.common.tag.LodestoneDamageTypeTags;

/**
 * Takes the {@code MAGIC_DAMAGE} attribute over for Maledict's tagged weapons.
 *
 * <p>Lodestone cashes the attacker's magic attribute into an extra magic hit inside the victim's
 * {@code LivingHurtEvent}. When the attacker holds an item in
 * {@link MaledictTags#LODESTONE_MAGIC_TAKEOVER}, ChangeLib's attribute dispatcher deals that magic
 * request instead (with the original source retained). Cancel Lodestone's copy so the swing does
 * not deal both. The {@code IS_MAGIC} branch is deliberately left alone: our dedicated
 * {@code minecraft:magic} source must still go through Lodestone's magic resistance.
 */
@Mixin(LodestoneAttributeEventHandler.class)
public abstract class LodestoneAttributeEventHandlerMixin {
    @Inject(method = "processAttributes", at = @At("HEAD"), cancellable = true, remap = false)
    private static void maledict$takeOverMagicForTaggedWeapons(LivingHurtEvent event, CallbackInfo callback) {
        if (event.isCanceled() || event.getAmount() <= 0.0F) {
            return;
        }
        DamageSource source = event.getSource();
        if (!source.is(LodestoneDamageTypeTags.CAN_TRIGGER_MAGIC)
                || source.is(LodestoneDamageTypeTags.IS_MAGIC)) {
            return;
        }
        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        if (!attacker.getMainHandItem().is(MaledictTags.LODESTONE_MAGIC_TAKEOVER)) {
            return;
        }
        callback.cancel();
    }
}
