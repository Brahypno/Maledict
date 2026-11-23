package org.brahypno.maledict.registry;

import javax.annotation.Nullable;

import com.sammy.malum.registry.common.magic.MalumSpiritTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.item.weapon.incursus.MaledictItemTiers;
import org.brahypno.maledict.common.ritual.invocation.InvocationSigilItem;
import org.brahypno.maledict.common.block.MnemonicObeliskBlockEntity;
import org.brahypno.maledict.common.block.SoulwoodObeliskBlockEntity;
import org.brahypno.maledict.common.entity.vicissitude.FirstVicissitudeBossEntity.BossDifficulty;
import org.brahypno.maledict.common.entity.vicissitude.FirstVicissitudeBossEntity.BossSpawnPhase;
import org.brahypno.maledict.common.entity.vicissitude.VicissitudeSpawnEggItem;
import org.brahypno.maledict.common.entity.wither.WeepingWitherSpawnEggItem;
import org.brahypno.maledict.common.item.weapon.remembrance.SpiritArrowItem;
import org.brahypno.maledict.common.item.weapon.remembrance.SpiritArrowType;
import org.brahypno.maledict.common.item.curio.enlightenment.AgeOfEnlightenmentItem;
import org.brahypno.maledict.common.item.curio.eggshell.EmperorInTheEggshellItem;
import org.brahypno.maledict.common.entity.vicissitude.curio.CurioReturnTokenItem;
import org.brahypno.maledict.common.item.curio.hangman.HangmanFoolItem;
import org.brahypno.maledict.common.item.curio.rune.*;
import org.brahypno.maledict.common.item.curio.priestess.EidolonOfTheHighPriestessItem;
import org.brahypno.maledict.common.item.weapon.lover.LoverAxeItem;
import org.brahypno.maledict.common.item.weapon.remembrance.RemembranceBowItem;
import org.brahypno.maledict.common.item.weapon.incursus.IncursusBladeItem;
import team.lodestar.lodestone.registry.common.LodestoneAttributes;
import team.lodestar.lodestone.systems.multiblock.MultiBlockItem;

import java.util.List;

public final class MaledictItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(BuiltInRegistries.ITEM, Maledict.MODID);

    public static final DeferredHolder<Item, Item> INVOCATION_SIGIL = ITEMS.register("invocation_sigil", InvocationSigilItem::new);

    public static final DeferredHolder<Item, Item> WITHERED_LEAF_LITTER = ITEMS.register("withered_leaf_litter",
            () -> new BlockItem(MaledictBlocks.WITHERED_LEAF_LITTER.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> EXPOSED_SOULWOOD_ROOTS = ITEMS.register("exposed_soulwood_roots",
            () -> new BlockItem(MaledictBlocks.EXPOSED_SOULWOOD_ROOTS.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> FALLEN_SOULWOOD_BRANCHES = ITEMS.register("fallen_soulwood_branches",
            () -> new BlockItem(MaledictBlocks.FALLEN_SOULWOOD_BRANCHES.get(), new Item.Properties()) {
                @Override
                public int getBurnTime(ItemStack stack, @Nullable net.minecraft.world.item.crafting.RecipeType<?> recipeType) {
                    return 100; // Same fuel value as one vanilla stick.
                }
            });
    public static final DeferredHolder<Item, Item> WITHERLIGHT_LAMP = ITEMS.register("witherlight_lamp",
            () -> new BlockItem(MaledictBlocks.WITHERLIGHT_LAMP.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> WITHERLIGHT_MUSHROOM = ITEMS.register("witherlight_mushroom",
            () -> new BlockItem(MaledictBlocks.WITHERLIGHT_MUSHROOM.get(), new Item.Properties()) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                    tooltip.add(Component.translatable("tooltip.maledict.witherlight_mushroom"));
                }
            });

    public static final DeferredHolder<Item, Item> CRYING_TAINTED_ROCK = ITEMS.register("crying_tainted_rock",
            () -> new BlockItem(MaledictBlocks.CRYING_TAINTED_ROCK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> CRYING_TWISTED_ROCK = ITEMS.register("crying_twisted_rock",
            () -> new BlockItem(MaledictBlocks.CRYING_TWISTED_ROCK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> BLIGHTED_WITHER_SKULL = ITEMS.register("blighted_wither_skull",
            () -> new BlockItem(MaledictBlocks.BLIGHTED_WITHER_SKULL.get(),
                    new Item.Properties().rarity(Rarity.RARE)));

    public static final DeferredHolder<Item, Item> TAINTED_MALIGNANT_LEAD_ORE = ITEMS.register("tainted_malignant_lead_ore",
            () -> new BlockItem(MaledictBlocks.TAINTED_MALIGNANT_LEAD_ORE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> TWISTED_MALIGNANT_LEAD_ORE = ITEMS.register("twisted_malignant_lead_ore",
            () -> new BlockItem(MaledictBlocks.TWISTED_MALIGNANT_LEAD_ORE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> MALIGNANT_LEAD_FRAGMENT = ITEMS.register("malignant_lead_fragment",
            () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> RAVEN_SPAWN_EGG = ITEMS.register("raven_spawn_egg", () ->
            new DeferredSpawnEggItem(MaledictEntities.RAVEN, 0x151820, 0xB80F22, new Item.Properties()));
    public static final DeferredHolder<Item, Item> WEEPING_WITHER_SPAWN_EGG = ITEMS.register("weeping_wither_spawn_egg", () ->
            new WeepingWitherSpawnEggItem(MaledictEntities.WEEPING_WITHER, 0x2A2232, 0xB03CE0,
                    new Item.Properties(), true));
    public static final DeferredHolder<Item, Item> WEEPING_WITHER_SKELETON_SPAWN_EGG =
            ITEMS.register("weeping_wither_skeleton_spawn_egg", () ->
                    new DeferredSpawnEggItem(MaledictEntities.WEEPING_WITHER_SKELETON,
                            0xC9C5CB, 0x5A2E7A, new Item.Properties()));
    public static final DeferredHolder<Item, Item> RAVEN_FEATHER = ITEMS.register("raven_feather", () ->
            new Item(new Item.Properties()));

    /**
     * 扬弃逐光四件套。1.21 把耐久从材质挪到了物品属性上，所以每件自带 {@code durability}；
     * 不挂任何隐藏组件 —— {@code HIDE_ADDITIONAL_TOOLTIP} 只挡 {@code appendHoverText}，
     * 既遮不住原版附魔行（那行由 {@code ItemStack#getTooltipLines} 另写），本套装又没有别的
     * hover 文案可挡。套装 tooltip 由 {@code LightwardTooltips} 在事件里补，原版那几行附魔也在那里摘掉。
     */
    public static final DeferredHolder<Item, Item> AUFHEBEN_LIGHTWARD_HELMET = ITEMS.register("aufheben_lightward_helmet",
            () -> new org.brahypno.maledict.common.item.equipment.lightward.LightwardArmorItem(
                    MaledictArmorMaterials.LIGHTWARD, ArmorItem.Type.HELMET, lightwardProperties(ArmorItem.Type.HELMET)));
    public static final DeferredHolder<Item, Item> AUFHEBEN_LIGHTWARD_CHESTPLATE = ITEMS.register("aufheben_lightward_chestplate",
            () -> new org.brahypno.maledict.common.item.equipment.lightward.LightwardArmorItem(
                    MaledictArmorMaterials.LIGHTWARD, ArmorItem.Type.CHESTPLATE, lightwardProperties(ArmorItem.Type.CHESTPLATE)));
    public static final DeferredHolder<Item, Item> AUFHEBEN_LIGHTWARD_LEGGINGS = ITEMS.register("aufheben_lightward_leggings",
            () -> new org.brahypno.maledict.common.item.equipment.lightward.LightwardArmorItem(
                    MaledictArmorMaterials.LIGHTWARD, ArmorItem.Type.LEGGINGS, lightwardProperties(ArmorItem.Type.LEGGINGS)));
    public static final DeferredHolder<Item, Item> AUFHEBEN_LIGHTWARD_BOOTS = ITEMS.register("aufheben_lightward_boots",
            () -> new org.brahypno.maledict.common.item.equipment.lightward.LightwardArmorItem(
                    MaledictArmorMaterials.LIGHTWARD, ArmorItem.Type.BOOTS, lightwardProperties(ArmorItem.Type.BOOTS)));

    private static Item.Properties lightwardProperties(ArmorItem.Type type) {
        return new Item.Properties().rarity(Rarity.EPIC).fireResistant()
                .durability(org.brahypno.maledict.common.item.equipment.lightward.LightwardArmorItem.durabilityFor(type));
    }

    public static final DeferredHolder<Item, Item> LUMENFALL_BISMUTH_ORE = ITEMS.register("lumenfall_bismuth_ore",
            () -> new BlockItem(MaledictBlocks.LUMENFALL_BISMUTH_ORE.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> LUMENFALL_BISMUTH_BLOCK = ITEMS.register("lumenfall_bismuth_block",
            () -> new BlockItem(MaledictBlocks.LUMENFALL_BISMUTH_BLOCK.get(), new Item.Properties()));
    public static final DeferredHolder<Item, Item> LUMENFALL_BISMUTH_INGOT = ITEMS.register("lumenfall_bismuth_ingot",
            () -> new Item(new Item.Properties()));

    /**
     * 折辉铅铋共晶：沉辉铋与恶念之铅经精魂灌注后析出的共晶宝石，与恶念白镴并列而非其升级品。
     * 名字里刻意不带「锭」——它是晶体，不是可再锻的金属锭。
     */
    public static final DeferredHolder<Item, Item> REFRACTED_LEAD_BISMUTH_EUTECTIC =
            ITEMS.register("refracted_lead_bismuth_eutectic", () -> new Item(new Item.Properties()));

    /** 伯爵的礼杖：铁剑档的仪式用剑，主/副手持有都给移速与可见度乘数。 */
    public static final DeferredHolder<Item, Item> BARON_CEREMONIAL_STAFF = ITEMS.register("baron_ceremonial_staff",
            org.brahypno.maledict.common.item.weapon.baron.BaronStaffItem::new);

    /** 拘魂卷轴：使用一次即习得拘魂仪式。 */
    public static final DeferredHolder<Item, Item> SOULBINDING_SCROLL =
            ITEMS.register("soulbinding_scroll", org.brahypno.maledict.common.ritual.soulbinding.SoulbindingScrollItem::new);
    /** 拘魂封印：灵魂木拘魂仪式的副手消耗品。 */
    public static final DeferredHolder<Item, Item> SOULBINDING_SEAL =
            ITEMS.register("soulbinding_seal", () -> new Item(new Item.Properties()));

    public static final DeferredHolder<Item, Item> INCURSUS_BLADE = ITEMS.register("incursus_blade", () ->
            new IncursusBladeItem(MaledictItemTiers.INCURSUS, new team.lodestar.lodestone.systems.item.LodestoneItemProperties().rarity(Rarity.EPIC)));
    public static final DeferredHolder<Item, Item> LOVER_AXE = ITEMS.register("lover_axe", () -> new LoverAxeItem());    public static final DeferredHolder<Item, Item> MELTFALL_AXE = ITEMS.register("meltfall_axe", () -> new LoverAxeItem(false));
    public static final DeferredHolder<Item, Item> REMEMBRANCE_BOW = ITEMS.register("remembrance_bow", () ->
            new RemembranceBowItem(new Item.Properties().durability(384).rarity(Rarity.RARE), false));
    public static final DeferredHolder<Item, Item> ELEGY_BOW = ITEMS.register("elegy_bow", () ->
            new RemembranceBowItem(new Item.Properties().durability(1111).rarity(Rarity.RARE), true));

    /**
     * Curio amulet (charm slot) that renders a skull mask over the wearer's face.
     */
    public static final DeferredHolder<Item, Item> AGE_OF_ENLIGHTENMENT = ITEMS.register("age_of_enlightenment", AgeOfEnlightenmentItem::new);
    public static final DeferredHolder<Item, Item> HANGMAN_FOOL = ITEMS.register("hangman_fool", HangmanFoolItem::new);
    public static final DeferredHolder<Item, Item> EIDOLON_OF_THE_HIGH_PRIESTESS =
            ITEMS.register("eidolon_of_the_high_priestess", EidolonOfTheHighPriestessItem::new);

    /**
     * 蛋壳中的皇帝：护符（charm 槽）。佩戴时把单次受击压在最大生命的一定比例内，并叠一条受击后的
     * 时间窗；代价是方块与实体交互距离各 -50%。层挂在实体身上、由物品自锁，见
     * {@link EmperorInTheEggshellItem} 与 {@code MaledictConfig} 的 {@code emperorInTheEggshell} 段。
     */
    public static final DeferredHolder<Item, Item> EMPEROR_IN_THE_EGGSHELL =
            ITEMS.register("emperor_in_the_eggshell", EmperorInTheEggshellItem::new);

    public static final DeferredHolder<Item, Item> BLEEDING_EYE = ITEMS.register("bleeding_eye", () ->
            new Item(new Item.Properties()) {
                @Override
                public Component getName(ItemStack stack) {
                    return super.getName(stack).copy().withStyle(style -> style.withColor(0xCF210A));
                }

                @Override
                public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                    tooltip.add(Component.translatable("tooltip.maledict.bleeding_eye").withStyle(style -> style.withColor(0xCF210A)));
                }
            });

    /**
     * Claim ticket for confiscated curios that could not be delivered automatically.
     */
    public static final DeferredHolder<Item, Item> CURIO_RETURN_TOKEN = ITEMS.register("curio_return_token", CurioReturnTokenItem::new);

    /**
     * Curio rune (rune slot) that keeps the Blessing of Life on its wearer.
     */
    public static final DeferredHolder<Item, Item> RUNE_OF_SATIATION = ITEMS.register("rune_of_satiation",
                                                                                      () -> new PulseRuneItem(new Item.Properties().stacksTo(1),
                                                                                                              MalumSpiritTypes.SACRED_SPIRIT,
                                                                                                              MaledictMobEffects.BLESSING_OF_LIFE,
                                                                                                              "maledict.blessing_of_life"));

    /**
     * Curio rune (rune slot): level I pulse that grinds nearby hostiles down but never kills them.
     */
    public static final DeferredHolder<Item, Item> RUNE_OF_DECAY = ITEMS.register("rune_of_decay",
                                                                                  () -> new PulseRuneItem(new Item.Properties().stacksTo(1),
                                                                                                          MalumSpiritTypes.WICKED_SPIRIT,
                                                                                                          MaledictMobEffects.DECAY, "maledict.decay"));

    /**
     * Curio rune (rune slot) buffing the wearer's own companions; it hands the effects out itself
     * instead of going through a mob effect, see {@link PackRuneItem}.
     */
    public static final DeferredHolder<Item, Item> RUNE_OF_THE_PACK = ITEMS.register("rune_of_the_pack",
                                                                                     () -> new PackRuneItem(new Item.Properties().stacksTo(1),
                                                                                                            MalumSpiritTypes.WICKED_SPIRIT,
                                                                                                            "maledict.pack_boon"));

    /**
     * Curio rune (rune slot) raising every share of experience the wearer earns by a quarter.
     */
    public static final DeferredHolder<Item, Item> RUNE_OF_RIPENING = ITEMS.register("rune_of_ripening",
                                                                                     () -> new PulseRuneItem(new Item.Properties().stacksTo(1),
                                                                                                             MalumSpiritTypes.SACRED_SPIRIT,
                                                                                                             MaledictMobEffects.RIPENING, "maledict.ripening"));

    /**
     * Curio rune (rune slot) carrying an attribute modifier instead of a mob effect, see
     * {@link AttributeRuneItem}.
     */
    public static final DeferredHolder<Item, Item> RUNE_OF_STAGNANT_EVOLUTION =
            ITEMS.register("rune_of_stagnant_evolution",
                           () -> new AttributeRuneItem(new Item.Properties().stacksTo(1),
                                                       MalumSpiritTypes.ELDRITCH_SPIRIT,
                                                       LodestoneAttributes.MAGIC_RESISTANCE,
                                                       "Curio Magic Resistance",
                                                       1.0D,
                                                       AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    /**
     * 虚空线符文：替佩戴者去死，最多四次，每次从生命上限抽走四分之一；抽走的骨头摘下符文也不还，
     * 只有真正死一次才清账。
     */
    public static final DeferredHolder<Item, Item> RUNE_OF_ROTTEN_BONE =
            ITEMS.register("rune_of_rotten_bone",
                           () -> new RuneOfRottenBoneItem(new Item.Properties().stacksTo(1),
                                                          MalumSpiritTypes.ELDRITCH_SPIRIT));

    /**
     * 抑郁符文：污染石档 + 幽影精魂。适应的账目写在物品自己的 NBT 上，延迟池挂在玩家身上，
     * 见 {@link RuneOfMelancholiaItem} 与 {@code DelayedVitalsEvents}。
     */
    public static final DeferredHolder<Item, Item> RUNE_OF_MELANCHOLIA =
            ITEMS.register("rune_of_melancholia",
                           () -> new RuneOfMelancholiaItem(new Item.Properties().stacksTo(1),
                                                           MalumSpiritTypes.UMBRAL_SPIRIT));

    /**
     * 无忧符文：抑郁符文的另一半，虚空石符板 + 32 幽影精魂。把自己的命运和对手绑在一起 ——
     * 血量比例差多少这一击就重多少、双方都躲不掉，佩戴者手里的神侵恶刃还整体抬一档。
     * 见 {@link RuneOfBlissItem}、{@code BlissRuneEvents} 与 {@code IncursusBladeItem.DamageTier}。
     */
    public static final DeferredHolder<Item, Item> RUNE_OF_BLISS =
            ITEMS.register("rune_of_bliss",
                           () -> new RuneOfBlissItem(new Item.Properties().stacksTo(1),
                                                     MalumSpiritTypes.UMBRAL_SPIRIT));

    /**
     * 堕落符文：白镴档的第三枚，与演进凝滞、朽骨同用恶念白镴锭。命中给目标落下 {@code Fallen}
     * （堕落窗口），窗口里目标每回复一次生命就积攒休眠腐败（每次把旧的一半转成活性），再次命中时把活性
     * 腐败收走 —— 收永远不致死，致命的那一下仍旧由这一记普通攻击完成。
     * 见 {@link RuneOfTheFallenItem} 与 {@code CorruptionRuneEvents}。
     */
    public static final DeferredHolder<Item, Item> RUNE_OF_THE_FALLEN =
            ITEMS.register("rune_of_the_fallen",
                           () -> new RuneOfTheFallenItem(new Item.Properties().stacksTo(1),
                                                         MalumSpiritTypes.ELDRITCH_SPIRIT));

    public static final DeferredHolder<Item, Item> MNEMONIC_OBELISK = ITEMS.register("mnemonic_obelisk", () ->
            new MultiBlockItem(MaledictBlocks.MNEMONIC_OBELISK.get(), new Item.Properties(), MnemonicObeliskBlockEntity.STRUCTURE));
    public static final DeferredHolder<Item, Item> SOULWOOD_OBELISK = ITEMS.register("soulwood_obelisk", () ->
            new MultiBlockItem(MaledictBlocks.SOULWOOD_OBELISK.get(), new Item.Properties(), SoulwoodObeliskBlockEntity.STRUCTURE));

    /**
     * 调试用刷怪蛋：只做简单档（换难度请用祭坛），一颗直接开始一阶段、一颗直接落到二阶段。
     */
    public static final DeferredHolder<Item, Item> FIRST_VICISSITUDE_PHASE_ONE_SPAWN_EGG =
            ITEMS.register("first_vicissitude_phase_one_spawn_egg", () ->
                    new VicissitudeSpawnEggItem(MaledictEntities.FIRST_VICISSITUDE,
                                                BossDifficulty.SIMPLE, BossSpawnPhase.PHASE_ONE,
                                                0x1B1A24, 0xE6EDF5, new Item.Properties()));
    public static final DeferredHolder<Item, Item> FIRST_VICISSITUDE_PHASE_TWO_SPAWN_EGG =
            ITEMS.register("first_vicissitude_phase_two_spawn_egg", () ->
                    new VicissitudeSpawnEggItem(MaledictEntities.FIRST_VICISSITUDE,
                                                BossDifficulty.SIMPLE, BossSpawnPhase.PHASE_TWO,
                                                0x1B1A24, 0x8A5CF6, new Item.Properties()));

    public static final DeferredHolder<Item, Item> SACRED_SPIRIT_ARROW = registerSpiritArrow(SpiritArrowType.SACRED);
    public static final DeferredHolder<Item, Item> WICKED_SPIRIT_ARROW = registerSpiritArrow(SpiritArrowType.WICKED);
    public static final DeferredHolder<Item, Item> ARCANE_SPIRIT_ARROW = registerSpiritArrow(SpiritArrowType.ARCANE);
    public static final DeferredHolder<Item, Item> ELDRITCH_SPIRIT_ARROW = registerSpiritArrow(SpiritArrowType.ELDRITCH);
    public static final DeferredHolder<Item, Item> AERIAL_SPIRIT_ARROW = registerSpiritArrow(SpiritArrowType.AERIAL);
    public static final DeferredHolder<Item, Item> AQUEOUS_SPIRIT_ARROW = registerSpiritArrow(SpiritArrowType.AQUEOUS);
    public static final DeferredHolder<Item, Item> EARTHEN_SPIRIT_ARROW = registerSpiritArrow(SpiritArrowType.EARTHEN);
    public static final DeferredHolder<Item, Item> INFERNAL_SPIRIT_ARROW = registerSpiritArrow(SpiritArrowType.INFERNAL);

    private static DeferredHolder<Item, Item> registerSpiritArrow(SpiritArrowType arrowType) {
        return ITEMS.register(arrowType.name().toLowerCase() + "_spirit_arrow", () ->
                new SpiritArrowItem(arrowType, new Item.Properties()));
    }

    public static DeferredHolder<Item, Item> getSpiritArrow(SpiritArrowType arrowType) {
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

    /**
     * 苏醒雕像用的腐枯之星：消耗一颗，把石像从“沉睡”拨到完整觉醒后的状态。
     * <p>1.21 没有 {@code SimpleFoiledItem}，附魔光效改成 {@code minecraft:enchantment_glint_override} 组件。
     */
    public static final DeferredHolder<Item, Item> BLIGHTED_STAR = ITEMS.register("blighted_star",
            () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON).fireResistant()
                                            .component(net.minecraft.core.component.DataComponents
                                                               .ENCHANTMENT_GLINT_OVERRIDE, true)));

    /**
     * 雕像死亡时必定掉落的碎片（不走掉落表，与下界之星同款处理）。
     */
    public static final DeferredHolder<Item, Item> LIGHT_FRAGMENT = ITEMS.register("light_fragment",
            () -> new Item(new Item.Properties().rarity(Rarity.RARE).fireResistant()
                                            .component(net.minecraft.core.component.DataComponents
                                                               .ENCHANTMENT_GLINT_OVERRIDE, true)));

    /** 未激活的石像：落地是沉睡态，要拿腐枯之星去唤醒。 */
    public static final DeferredHolder<Item, Item> DEMIURGE_STATUE_SPAWN_EGG =
            ITEMS.register("demiurge_statue_spawn_egg",
                    () -> new org.brahypno.maledict.common.entity.demiurge.DemiurgeSpawnEggItem(
                            MaledictEntities.DEMIURGE_STATUE, 0x6A6472, 0xC2BCC8,
                            new Item.Properties(), true, false));

    /** 直接是觉醒后的地面站姿：跳过五秒苏醒与那场雨。 */
    public static final DeferredHolder<Item, Item> DEMIURGE_STATUE_AWAKENED_SPAWN_EGG =
            ITEMS.register("demiurge_statue_awakened_spawn_egg",
                    () -> new org.brahypno.maledict.common.entity.demiurge.DemiurgeSpawnEggItem(
                            MaledictEntities.DEMIURGE_STATUE, 0x9A6BD6, 0xE8C8FF,
                            new Item.Properties(), true, true));

    /**
     * 坠光秘典：本模组自己的法典，右键打开。
     *
     * <p>1.20.1 的 {@code LightfallCodexItem} 原样搬语义，只是开屏那一趟改由
     * {@code MaledictNetwork#openLightfallCodex} 发出 1.21 的自定义报文。
     */
    public static final DeferredHolder<Item, Item> LIGHTFALL_CODEX =
            ITEMS.register("lightfall_codex", org.brahypno.maledict.common.item.codex.LightfallCodexItem::new);

    /**
     * 东方女仆联动：赠礼之剑（眷恋）与其拘魂转化形态（弃约）。
     *
     * <p>两件物品本身**不依赖女仆模组**（{@code MaidBondSwordItem} 与 {@code MaidBondData} 都不 import
     * 任何 TLM 类，只按注册名认人），所以无条件注册：剑被扔在地上、存档进一个没装 TLM 的整合包，
     * 也还能正常读出来。它们只在创造栏里受 TLM 在场与否控制，与 1.20.1 一致。
     */
    public static final DeferredHolder<Item, Item> YEARNING_SWORD =
            ITEMS.register("yearning_sword", () -> new org.brahypno.maledict.compat.maid.MaidBondSwordItem(false));
    public static final DeferredHolder<Item, Item> FORSAKEN_VOW =
            ITEMS.register("forsaken_vow", () -> new org.brahypno.maledict.compat.maid.MaidBondSwordItem(true));

    private MaledictItems() {
    }
}
