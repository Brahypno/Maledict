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
                    .displayItems((parameters, output) -> output.accept(MaledictItems.INCURSUS_BLADE.get()))
                    .build());

    private MaledictCreativeTabs() {
    }
}
