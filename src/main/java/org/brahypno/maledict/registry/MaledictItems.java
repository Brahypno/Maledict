package org.brahypno.maledict.registry;

import com.sammy.malum.registry.common.SpiritTypeRegistry;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.block.MnemonicObeliskBlockEntity;
import org.brahypno.maledict.common.block.SoulwoodObeliskBlockEntity;
import org.brahypno.maledict.common.item.*;
import team.lodestar.lodestone.registry.common.LodestoneAttributeRegistry;
import team.lodestar.lodestone.systems.multiblock.MultiBlockItem;

public final class MaledictItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Maledict.MODID);

    public static final RegistryObject<Item> INCURSUS_BLADE = ITEMS.register("incursus_blade", () ->
            new IncursusBladeItem(MaledictItemTiers.INCURSUS, new Item.Properties().rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> REMEMBRANCE_BOW = ITEMS.register("remembrance_bow", () ->
            new RemembranceBowItem(new Item.Properties().durability(384).rarity(Rarity.RARE), false));
    public static final RegistryObject<Item> ELEGY_BOW = ITEMS.register("elegy_bow", () ->
            new RemembranceBowItem(new Item.Properties().durability(1111).rarity(Rarity.RARE), true));

    /**
     * Curio amulet (charm slot) that renders a skull mask over the wearer's face.
     */
    public static final RegistryObject<Item> AGE_OF_ENLIGHTENMENT = ITEMS.register("age_of_enlightenment", AgeOfEnlightenmentItem::new);

    /**
     * Claim ticket for confiscated curios that could not be delivered automatically.
     */
    public static final RegistryObject<Item> CURIO_RETURN_TOKEN = ITEMS.register("curio_return_token", CurioReturnTokenItem::new);

    /**
     * Void-line tablet: four malignant pewter platings bent into two of these by spirit infusion.
     * The Rune of Stagnant Evolution is cut into one.
     */
    public static final RegistryObject<Item> MALIGNANT_PEWTER_TABLET = ITEMS.register("malignant_pewter_tablet",
                                                                                      () -> new Item(new Item.Properties()));

    /**
     * Curio rune (rune slot) cut from a runewood tablet: keeps the Blessing of Life on its wearer,
     * doubling the natural regeneration the player's own saturation and hunger pay for.
     */
    public static final RegistryObject<Item> RUNE_OF_SATIATION = ITEMS.register("rune_of_satiation",
                                                                                () -> new PulseRuneItem(new Item.Properties().stacksTo(1),
                                                                                                        SpiritTypeRegistry.SACRED_SPIRIT,
                                                                                                        MaledictMobEffects.BLESSING_OF_LIFE,
                                                                                                        "maledict.blessing_of_life"));

    /**
     * Curio rune (rune slot) cut from a runewood tablet: the Rite of Decay, weakened to a level I
     * pulse that only ever grinds nearby hostiles down, never kills them.
     */
    public static final RegistryObject<Item> RUNE_OF_DECAY = ITEMS.register("rune_of_decay",
                                                                            () -> new PulseRuneItem(new Item.Properties().stacksTo(1),
                                                                                                    SpiritTypeRegistry.WICKED_SPIRIT,
                                                                                                    MaledictMobEffects.DECAY, "maledict.decay"));

    /**
     * Curio rune (rune slot) cut from a soulwood tablet: the Rite of Culling, weakened to a level I
     * pulse that strikes the outermost of any hostile crowd once every two seconds.
     *
     * <p>Named away from Malum's own {@code rune_of_culling} (Rune of Culling, its magic damage
     * rune); two runes sharing a name would be unreadable side by side in JEI.
     */
    public static final RegistryObject<Item> RUNE_OF_THINNING = ITEMS.register("rune_of_thinning",
                                                                               () -> new PulseRuneItem(new Item.Properties().stacksTo(1),
                                                                                                       SpiritTypeRegistry.WICKED_SPIRIT,
                                                                                                       MaledictMobEffects.THINNING, "maledict.thinning"));

    /**
     * Curio rune (rune slot) cut from a soulwood tablet, sacred spirit this time: the nourishing
     * pulse of the Rite of Nourishment turned inward, raising every share of experience the wearer
     * earns by a quarter.
     */
    public static final RegistryObject<Item> RUNE_OF_RIPENING = ITEMS.register("rune_of_ripening",
                                                                               () -> new PulseRuneItem(new Item.Properties().stacksTo(1),
                                                                                                       SpiritTypeRegistry.SACRED_SPIRIT,
                                                                                                       MaledictMobEffects.RIPENING, "maledict.ripening"));

    /**
     * Curio rune (rune slot) of the void line, cut into a malignant pewter tablet of our own making
     * with two fused consciousness as its pulse.
     *
     * <p>Carries an attribute modifier instead of a mob effect, see {@link AttributeRuneItem}.
     */
    public static final RegistryObject<Item> RUNE_OF_STAGNANT_EVOLUTION =
            ITEMS.register("rune_of_stagnant_evolution",
                           () -> new AttributeRuneItem(new Item.Properties().stacksTo(1),
                                                       SpiritTypeRegistry.ELDRITCH_SPIRIT,
                                                       LodestoneAttributeRegistry.MAGIC_RESISTANCE,
                                                       "Curio Magic Resistance",
                                                       0.8D,
                                                       AttributeModifier.Operation.MULTIPLY_TOTAL));

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
