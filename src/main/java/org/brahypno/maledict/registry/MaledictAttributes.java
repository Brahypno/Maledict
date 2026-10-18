package org.brahypno.maledict.registry;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.brahypno.maledict.Maledict;

@EventBusSubscriber(modid = Maledict.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class MaledictAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(BuiltInRegistries.ATTRIBUTE, Maledict.MODID);
    public static final DeferredHolder<Attribute, Attribute> TRANSCEND_OMEN = ATTRIBUTES.register("transcend_omen",
            () -> new RangedAttribute("attribute.name.maledict.transcend_omen", 0, 0, 1).setSyncable(true));
    public static final DeferredHolder<Attribute, Attribute> DESCENT = ATTRIBUTES.register("descent",
            () -> new RangedAttribute("attribute.name.maledict.descent", 0, 0, Double.MAX_VALUE).setSyncable(true));

    @SubscribeEvent
    public static void addAttributes(EntityAttributeModificationEvent event) {
        event.getTypes().forEach(type -> {
            event.add(type, TRANSCEND_OMEN);
            event.add(type, DESCENT);
        });
    }
}
