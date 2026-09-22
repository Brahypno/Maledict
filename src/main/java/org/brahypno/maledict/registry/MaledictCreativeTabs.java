package org.brahypno.maledict.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.maledict.Maledict;

public final class MaledictCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Maledict.MODID);

    public static final RegistryObject<CreativeModeTab> MALEDICT = CREATIVE_TABS.register("maledict", () ->
            CreativeModeTab.builder()
                           .title(Component.translatable("itemGroup.maledict"))
                           .icon(() -> MaledictItems.INCURSUS_BLADE.get().getDefaultInstance())
                           .displayItems((parameters, output) -> {
                               output.accept(MaledictItems.INCURSUS_BLADE.get());
                               output.accept(MaledictItems.REMEMBRANCE_BOW.get());
                               output.accept(MaledictItems.ELEGY_BOW.get());
                               output.accept(MaledictItems.MNEMONIC_OBELISK.get());
                               output.accept(MaledictItems.SOULWOOD_OBELISK.get());
                               output.accept(MaledictItems.SACRED_SPIRIT_ARROW.get());
                               output.accept(MaledictItems.WICKED_SPIRIT_ARROW.get());
                               output.accept(MaledictItems.ARCANE_SPIRIT_ARROW.get());
                               output.accept(MaledictItems.ELDRITCH_SPIRIT_ARROW.get());
                               output.accept(MaledictItems.AERIAL_SPIRIT_ARROW.get());
                               output.accept(MaledictItems.AQUEOUS_SPIRIT_ARROW.get());
                               output.accept(MaledictItems.EARTHEN_SPIRIT_ARROW.get());
                               output.accept(MaledictItems.INFERNAL_SPIRIT_ARROW.get());
                               output.accept(MaledictItems.AGE_OF_ENLIGHTENMENT.get());
                               output.accept(MaledictItems.MALIGNANT_PEWTER_TABLET.get());
                               output.accept(MaledictItems.RUNE_OF_SATIATION.get());
                               output.accept(MaledictItems.RUNE_OF_DECAY.get());
                               output.accept(MaledictItems.RUNE_OF_THINNING.get());
                               output.accept(MaledictItems.RUNE_OF_RIPENING.get());
                               output.accept(MaledictItems.RUNE_OF_STAGNANT_EVOLUTION.get());
                           })
                           .build());

    private MaledictCreativeTabs() {
    }
}
