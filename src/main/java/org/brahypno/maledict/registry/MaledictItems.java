package org.brahypno.maledict.registry;

import com.sammy.malum.registry.common.item.ItemTiers;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.IncursusBladeItem;

public final class MaledictItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Maledict.MODID);

    public static final RegistryObject<Item> INCURSUS_BLADE = ITEMS.register("incursus_blade", () ->
            new IncursusBladeItem(
                    ItemTiers.ItemTierEnum.MALIGNANT_ALLOY,
                    new Item.Properties().rarity(Rarity.EPIC)));

    private MaledictItems() {
    }
}
