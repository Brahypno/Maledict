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
import org.brahypno.maledict.common.entity.FirstVicissitudeBossEntity.BossDifficulty;
import org.brahypno.maledict.common.entity.FirstVicissitudeBossEntity.BossSpawnPhase;
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

    /** Curio amulet (charm slot) that renders a skull mask over the wearer's face. */
    public static final RegistryObject<Item> AGE_OF_ENLIGHTENMENT = ITEMS.register("age_of_enlightenment", AgeOfEnlightenmentItem::new);

    /** Claim ticket for confiscated curios that could not be delivered automatically. */
    public static final RegistryObject<Item> CURIO_RETURN_TOKEN = ITEMS.register("curio_return_token", CurioReturnTokenItem::new);

    public static final RegistryObject<Item> MALIGNANT_PEWTER_TABLET = ITEMS.register("malignant_pewter_tablet",
                                                                                      () -> new Item(new Item.Properties()));

    /** Curio rune (rune slot) that keeps the Blessing of Life on its wearer. */
    public static final RegistryObject<Item> RUNE_OF_SATIATION = ITEMS.register("rune_of_satiation",
                                                                                () -> new PulseRuneItem(new Item.Properties().stacksTo(1),
                                                                                                        SpiritTypeRegistry.SACRED_SPIRIT,
                                                                                                        MaledictMobEffects.BLESSING_OF_LIFE,
                                                                                                        "maledict.blessing_of_life"));

    /** Curio rune (rune slot): level I pulse that grinds nearby hostiles down but never kills them. */
    public static final RegistryObject<Item> RUNE_OF_DECAY = ITEMS.register("rune_of_decay",
                                                                            () -> new PulseRuneItem(new Item.Properties().stacksTo(1),
                                                                                                    SpiritTypeRegistry.WICKED_SPIRIT,
                                                                                                    MaledictMobEffects.DECAY, "maledict.decay"));

    /**
     * Curio rune (rune slot) buffing the wearer's own companions; it hands the effects out itself
     * instead of going through a mob effect, see {@link PackRuneItem}.
     */
    public static final RegistryObject<Item> RUNE_OF_THE_PACK = ITEMS.register("rune_of_the_pack",
                                                                               () -> new PackRuneItem(new Item.Properties().stacksTo(1),
                                                                                                      SpiritTypeRegistry.WICKED_SPIRIT,
                                                                                                      "maledict.pack_boon"));

    /** Curio rune (rune slot) raising every share of experience the wearer earns by a quarter. */
    public static final RegistryObject<Item> RUNE_OF_RIPENING = ITEMS.register("rune_of_ripening",
                                                                               () -> new PulseRuneItem(new Item.Properties().stacksTo(1),
                                                                                                       SpiritTypeRegistry.SACRED_SPIRIT,
                                                                                                       MaledictMobEffects.RIPENING, "maledict.ripening"));

    /**
     * Curio rune (rune slot) carrying an attribute modifier instead of a mob effect, see
     * {@link AttributeRuneItem}.
     */
    public static final RegistryObject<Item> RUNE_OF_STAGNANT_EVOLUTION =
            ITEMS.register("rune_of_stagnant_evolution",
                           () -> new AttributeRuneItem(new Item.Properties().stacksTo(1),
                                                       SpiritTypeRegistry.ELDRITCH_SPIRIT,
                                                       LodestoneAttributeRegistry.MAGIC_RESISTANCE,
                                                       "Curio Magic Resistance",
                                                       1.0D,
                                                       AttributeModifier.Operation.MULTIPLY_TOTAL));

    /**
     * 虚空线符文：替佩戴者去死，最多四次，每次从生命上限抽走四分之一；抽走的骨头摘下符文也不还，
     * 只有真正死一次才清账。
     */
    public static final RegistryObject<Item> RUNE_OF_ROTTEN_BONE =
            ITEMS.register("rune_of_rotten_bone",
                           () -> new RuneOfRottenBoneItem(new Item.Properties().stacksTo(1),
                                                          SpiritTypeRegistry.ELDRITCH_SPIRIT));

    /**
     * 抑郁符文：污染石档 + 幽影精魂。适应的账目写在物品自己的 NBT 上，延迟池挂在玩家身上，
     * 见 {@link RuneOfMelancholiaItem} 与 {@code DelayedVitalsEvents}。
     */
    public static final RegistryObject<Item> RUNE_OF_MELANCHOLIA =
            ITEMS.register("rune_of_melancholia",
                           () -> new RuneOfMelancholiaItem(new Item.Properties().stacksTo(1),
                                                           SpiritTypeRegistry.UMBRAL_SPIRIT));

    public static final RegistryObject<Item> MNEMONIC_OBELISK = ITEMS.register("mnemonic_obelisk", () ->
            new MultiBlockItem(MaledictBlocks.MNEMONIC_OBELISK.get(), new Item.Properties(), MnemonicObeliskBlockEntity.STRUCTURE));
    public static final RegistryObject<Item> SOULWOOD_OBELISK = ITEMS.register("soulwood_obelisk", () ->
            new MultiBlockItem(MaledictBlocks.SOULWOOD_OBELISK.get(), new Item.Properties(), SoulwoodObeliskBlockEntity.STRUCTURE));

    /** 调试用刷怪蛋：只做简单档（换难度请用祭坛），一颗直接开始一阶段、一颗直接落到二阶段。 */
    public static final RegistryObject<Item> FIRST_VICISSITUDE_PHASE_ONE_SPAWN_EGG =
            ITEMS.register("first_vicissitude_phase_one_spawn_egg", () ->
                    new VicissitudeSpawnEggItem(MaledictEntities.FIRST_VICISSITUDE,
                                                BossDifficulty.SIMPLE, BossSpawnPhase.PHASE_ONE,
                                                0x1B1A24, 0xE6EDF5, new Item.Properties()));
    public static final RegistryObject<Item> FIRST_VICISSITUDE_PHASE_TWO_SPAWN_EGG =
            ITEMS.register("first_vicissitude_phase_two_spawn_egg", () ->
                    new VicissitudeSpawnEggItem(MaledictEntities.FIRST_VICISSITUDE,
                                                BossDifficulty.SIMPLE, BossSpawnPhase.PHASE_TWO,
                                                0x1B1A24, 0x8A5CF6, new Item.Properties()));

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
