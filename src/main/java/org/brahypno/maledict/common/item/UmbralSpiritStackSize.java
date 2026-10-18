package org.brahypno.maledict.common.item;

import com.sammy.malum.registry.common.item.MalumItems;
import net.minecraft.core.component.DataComponents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;
import org.brahypno.maledict.Maledict;

/** Keep umbral spirits stackable to 64 for the runeworking recipes. */
@EventBusSubscriber(modid = Maledict.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class UmbralSpiritStackSize {
    @SubscribeEvent
    public static void modifyComponents(ModifyDefaultComponentsEvent event) {
        event.modify(MalumItems.UMBRAL_SPIRIT.get(), components -> components.set(DataComponents.MAX_STACK_SIZE, 64));
    }

    private UmbralSpiritStackSize() {}
}
