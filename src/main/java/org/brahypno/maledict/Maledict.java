package org.brahypno.maledict;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.brahypno.maledict.config.MaledictConfig;
import org.brahypno.maledict.network.MaledictNetwork;
import org.brahypno.maledict.registry.MaledictCreativeTabs;
import org.brahypno.maledict.registry.MaledictAttributes;
import org.brahypno.maledict.registry.MaledictEnchantments;
import org.brahypno.maledict.registry.MaledictEntities;
import org.brahypno.maledict.registry.MaledictBlockEntities;
import org.brahypno.maledict.registry.MaledictBlocks;
import org.brahypno.maledict.registry.MaledictItems;
import org.brahypno.maledict.registry.MaledictMobEffects;
import org.brahypno.maledict.registry.MaledictSounds;

@Mod(Maledict.MODID)
public final class Maledict {
    public static final String MODID = "maledict";

    public Maledict(IEventBus modBus, ModContainer container) {

        container.registerConfig(ModConfig.Type.COMMON, MaledictConfig.COMMON_SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, MaledictConfig.CLIENT_SPEC);
        org.brahypno.maledict.registry.MaledictAttachments.TYPES.register(modBus);
        org.brahypno.maledict.registry.MaledictRites.EFFECTS.register(modBus);
        org.brahypno.maledict.registry.MaledictRites.RITES.register(modBus);
        MaledictAttributes.ATTRIBUTES.register(modBus);
        MaledictEntities.ENTITY_TYPES.register(modBus);
        MaledictBlocks.BLOCKS.register(modBus);
        MaledictBlockEntities.BLOCK_ENTITY_TYPES.register(modBus);
        MaledictItems.ITEMS.register(modBus);
        MaledictMobEffects.MOB_EFFECTS.register(modBus);
        MaledictSounds.SOUND_EVENTS.register(modBus);
        MaledictCreativeTabs.CREATIVE_TABS.register(modBus);
        MaledictNetwork.register(modBus);
    }
}
