package org.brahypno.maledict.common.realm.wither;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictItems;

@Mod.EventBusSubscriber(modid = Maledict.MODID)
public final class WeepingWitherDrops {
    @SubscribeEvent
    public static void replaceStar(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof WeepingWitherEntity)) return;
        for (var drop : event.getDrops()) {
            if (drop.getItem().is(Items.NETHER_STAR)) {
                // Keep the vanilla item entity, including the star's extended lifetime.
                drop.setItem(new ItemStack(MaledictItems.BLIGHTED_STAR.get(), drop.getItem().getCount()));
            }
        }
    }
}
