package org.brahypno.maledict.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.entity.SpiritArrowEntity;

public final class MaledictEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Maledict.MODID);

    public static final RegistryObject<EntityType<SpiritArrowEntity>> SPIRIT_ARROW =
            ENTITY_TYPES.register("spirit_arrow", () -> EntityType.Builder
                    .<SpiritArrowEntity>of(SpiritArrowEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(4)
                    .updateInterval(20)
                    .build("spirit_arrow"));

    private MaledictEntities() {
    }
}
