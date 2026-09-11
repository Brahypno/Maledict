package org.brahypno.maledict.client;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.RemembranceBowItem;
import org.brahypno.maledict.registry.MaledictItems;

@Mod.EventBusSubscriber(modid = Maledict.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MaledictItemProperties {
    @SubscribeEvent
    @SuppressWarnings("removal")
    public static void registerItemProperties(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            registerBowProperties(MaledictItems.REMEMBRANCE_BOW.get());
            registerBowProperties(MaledictItems.ELEGY_BOW.get());
        });
    }

    @SuppressWarnings("removal")
    private static void registerBowProperties(Item item) {
        ItemProperties.register(item, new ResourceLocation("pull"),
                (stack, level, entity, seed) -> {
                    if (entity == null || entity.getUseItem() != stack
                            || !(stack.getItem() instanceof RemembranceBowItem bow)) {
                        return 0.0F;
                    }
                    int usedTicks = stack.getUseDuration() - entity.getUseItemRemainingTicks();
                    return Mth.clamp(
                            usedTicks * bow.getDrawSpeedMultiplier(stack) / BowItem.MAX_DRAW_DURATION,
                            0.0F, 1.0F);
                });
        ItemProperties.register(item, new ResourceLocation("pulling"),
                (stack, level, entity, seed) -> entity != null
                        && entity.isUsingItem()
                        && entity.getUseItem() == stack ? 1.0F : 0.0F);
    }

    private MaledictItemProperties() {
    }
}
