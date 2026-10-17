package org.brahypno.maledict.registry;

import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraftforge.event.entity.EntityAttributeModificationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.maledict.Maledict;

@Mod.EventBusSubscriber(modid = Maledict.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MaledictAttributes {
    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(ForgeRegistries.ATTRIBUTES, Maledict.MODID);
    public static final RegistryObject<Attribute> TRANSCEND_OMEN = ATTRIBUTES.register("transcend_omen",
            () -> new RangedAttribute("attribute.name.maledict.transcend_omen", 0, 0, 1).setSyncable(true));
    public static final RegistryObject<Attribute> DESCENT = ATTRIBUTES.register("descent",
            () -> new RangedAttribute("attribute.name.maledict.descent", 0, 0, Double.MAX_VALUE).setSyncable(true));

    @SubscribeEvent
    public static void addAttributes(EntityAttributeModificationEvent event) {
        event.getTypes().forEach(type -> {
            event.add(type, TRANSCEND_OMEN.get());
            event.add(type, DESCENT.get());
        });
    }
}
