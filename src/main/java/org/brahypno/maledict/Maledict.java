package org.brahypno.maledict;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.brahypno.maledict.compat.tetra.TetraCompat;
import org.brahypno.maledict.common.lumenfall.LumenfallRadianceEffect;
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
import org.brahypno.maledict.registry.MaledictParticles;
import org.brahypno.maledict.registry.MaledictSounds;
import org.brahypno.maledict.registry.MaledictRecipeTypes;

@Mod(Maledict.MODID)
public final class Maledict {
    public static final String MODID = "maledict";

    @SuppressWarnings({"removal"})
    public Maledict() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MaledictConfig.COMMON_SPEC);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, MaledictConfig.CLIENT_SPEC);
        MaledictEnchantments.ENCHANTMENTS.register(modBus);
        MaledictAttributes.ATTRIBUTES.register(modBus);
        MaledictEntities.ENTITY_TYPES.register(modBus);
        MaledictBlocks.BLOCKS.register(modBus);
        org.brahypno.maledict.registry.MaledictRealmFeatures.FEATURES.register(modBus);
        org.brahypno.maledict.registry.MaledictStructures.TYPES.register(modBus);
        org.brahypno.maledict.registry.MaledictStructures.PIECES.register(modBus);
        MaledictBlockEntities.BLOCK_ENTITY_TYPES.register(modBus);
        MaledictItems.ITEMS.register(modBus);
        MaledictRecipeTypes.SERIALIZERS.register(modBus);
        MaledictRecipeTypes.TYPES.register(modBus);
        MaledictMobEffects.MOB_EFFECTS.register(modBus);
        MaledictParticles.PARTICLE_TYPES.register(modBus);
        MaledictSounds.SOUND_EVENTS.register(modBus);
        MaledictCreativeTabs.CREATIVE_TABS.register(modBus);
        MaledictNetwork.register();
        // Tetra 是可选的：这里只记一个「装没装」，装着才会去触碰桥接类里的 se.mickelus.tetra 类型。
        TetraCompat.init();
        // 自己那份带强度参数的放射光晕（替换哭泣之井那发）。两端都要登记，客户端才认得出这个 id。
        LumenfallRadianceEffect.register();
    }
}
