package org.brahypno.maledict.registry;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.block.MnemonicObeliskBlockEntity;
import org.brahypno.maledict.common.block.SoulwoodObeliskBlockEntity;
import org.brahypno.maledict.common.item.IncursusBladeItem;
import org.brahypno.maledict.common.item.RemembranceBowItem;
import org.brahypno.maledict.common.item.SpiritArrowItem;
import org.brahypno.maledict.common.item.SpiritArrowType;
import team.lodestar.lodestone.systems.multiblock.MultiBlockItem;

public final class MaledictItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Maledict.MODID);

    public static final RegistryObject<Item> INCURSUS_BLADE = ITEMS.register("incursus_blade", () ->
            new IncursusBladeItem(MaledictItemTiers.INCURSUS, new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> REMEMBRANCE_BOW = ITEMS.register("remembrance_bow", () ->
            new RemembranceBowItem(new Item.Properties().durability(384).rarity(Rarity.RARE), false));
    public static final RegistryObject<Item> ELEGY_BOW = ITEMS.register("elegy_bow", () ->
            new RemembranceBowItem(new Item.Properties().durability(384).rarity(Rarity.RARE), true));

    public static final RegistryObject<Item> MNEMONIC_OBELISK = ITEMS.register("mnemonic_obelisk", () ->
            new MultiBlockItem(MaledictBlocks.MNEMONIC_OBELISK.get(), new Item.Properties(), MnemonicObeliskBlockEntity.STRUCTURE));
    public static final RegistryObject<Item> SOULWOOD_OBELISK = ITEMS.register("soulwood_obelisk", () ->
            new MultiBlockItem(MaledictBlocks.SOULWOOD_OBELISK.get(), new Item.Properties(), SoulwoodObeliskBlockEntity.STRUCTURE));

    public static final RegistryObject<Item> SACRED_SPIRIT_ARROW = registerSpiritArrow(SpiritArrowType.SACRED);
    public static final RegistryObject<Item> WICKED_SPIRIT_ARROW = registerSpiritArrow(SpiritArrowType.WICKED);
    public static final RegistryObject<Item> ARCANE_SPIRIT_ARROW = registerSpiritArrow(SpiritArrowType.ARCANE);
    public static final RegistryObject<Item> ELDRITCH_SPIRIT_ARROW = registerSpiritArrow(SpiritArrowType.ELDRITCH);
    public static final RegistryObject<Item> AERIAL_SPIRIT_ARROW = registerSpiritArrow(SpiritArrowType.AERIAL);
    public static final RegistryObject<Item> AQUEOUS_SPIRIT_ARROW = registerSpiritArrow(SpiritArrowType.AQUEOUS);
    public static final RegistryObject<Item> EARTHEN_SPIRIT_ARROW = registerSpiritArrow(SpiritArrowType.EARTHEN);
    public static final RegistryObject<Item> INFERNAL_SPIRIT_ARROW = registerSpiritArrow(SpiritArrowType.INFERNAL);

    private static RegistryObject<Item> registerSpiritArrow(SpiritArrowType arrowType) {
        return ITEMS.register(arrowType.name().toLowerCase() + "_spirit_arrow", () ->
                new SpiritArrowItem(arrowType, new Item.Properties()));
    }

    public static RegistryObject<Item> getSpiritArrow(SpiritArrowType arrowType) {
        return switch (arrowType) {
            case SACRED -> SACRED_SPIRIT_ARROW;
            case WICKED -> WICKED_SPIRIT_ARROW;
            case ARCANE -> ARCANE_SPIRIT_ARROW;
            case ELDRITCH -> ELDRITCH_SPIRIT_ARROW;
            case AERIAL -> AERIAL_SPIRIT_ARROW;
            case AQUEOUS -> AQUEOUS_SPIRIT_ARROW;
            case EARTHEN -> EARTHEN_SPIRIT_ARROW;
            case INFERNAL -> INFERNAL_SPIRIT_ARROW;
        };
    }

    private MaledictItems() {
    }
}
