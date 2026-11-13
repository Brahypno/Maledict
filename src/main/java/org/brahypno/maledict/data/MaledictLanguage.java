package org.brahypno.maledict.data;

import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.LanguageProvider;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictBlocks;
import org.brahypno.maledict.registry.MaledictItems;

import java.util.function.Supplier;

public final class MaledictLanguage extends LanguageProvider {
    private static final String OBELISKS_ENTRY = "void.maledict.obelisks";
    private static final String SOULWOOD_OBELISK_PAGE = OBELISKS_ENTRY + ".soulwood_obelisk";
    private static final String MNEMONIC_OBELISK_PAGE = OBELISKS_ENTRY + ".mnemonic_obelisk";

    private static final String ELEGY_BOW_ENTRY = "void.maledict.elegy_bow";

    private static final String BLEEDING_EYE_ENTRY = "void.maledict.bleeding_eye";
    private static final String HANGMAN_FOOL_PAGE = BLEEDING_EYE_ENTRY + ".hangman_fool";
    private static final String HIGH_PRIESTESS_PAGE = BLEEDING_EYE_ENTRY + ".high_priestess";

    private static final String TOTEMIC_RUNES_CONTINUED_ENTRY = "maledict.totemic_runes_continued";
    private static final String RUNE_OF_SATIATION_ENTRY = "maledict.rune_of_satiation";
    private static final String RUNE_OF_DECAY_ENTRY = "maledict.rune_of_decay";
    private static final String RUNE_OF_THE_PACK_ENTRY = "maledict.rune_of_the_pack";
    private static final String RUNE_OF_RIPENING_ENTRY = "maledict.rune_of_ripening";
    private static final String VOID_RUNEWORKING_ENTRY = "void.maledict.runeworking";
    private static final String RUNE_OF_STAGNANT_EVOLUTION_ENTRY = "void.maledict.rune_of_stagnant_evolution";
    private static final String RUNE_OF_ROTTEN_BONE_ENTRY = "void.maledict.rune_of_rotten_bone";
    private static final String UMBRAL_EXPERIMENT_ENTRY = "void.maledict.umbral_experiment";
    private static final String RUNE_OF_MELANCHOLIA_ENTRY = "void.maledict.rune_of_melancholia";
    private static final String RUNE_OF_BLISS_ENTRY = "void.maledict.rune_of_bliss";
    private static final String RUNE_OF_THE_FALLEN_ENTRY = "void.maledict.rune_of_the_fallen";

    private static final String RITE_ENTRY = "void.maledict.vicissitude_rite";
    private static final String VICISSITUDE_RITE_ID = "vicissitude_rite";
    private static final String GREATER_RITE_ID = "greater_vicissitude_rite";
    private static final String ELDRITCH_RITE_ID = "eldritch_vicissitude_rite";
    private static final String GREATER_ELDRITCH_RITE_ID = "greater_eldritch_vicissitude_rite";

    private static final String SACRIFICE_RITE_ID = "sacrifice_rite";

    /**
     * 牺牲仪式的键名。键值（各语言的文案）是作者写的，别动；键名按 Malum 的灵魂木惯例带 {@code corrupt}，
     * 这样 {@code RiteEntryObject} 与 {@code SpiritRiteRecipePage} 会自动画灵魂木那一套。
     * 页面文本键是短名，{@code SpiritRiteTextPage} 与 {@code HeadlineTextItemPage} 共用它。
     */
    private static final String SACRIFICE_RITE_ENTRY = "maledict.corrupt_sacrifice_rite";
    private static final String SACRIFICE_RITE_PAGE = "corrupt_sacrifice_rite";

    /**
     * 拘魂仪式在坠光秘典里的条目与两页仪式正文（灵魂木附加、符文木解除）。
     */
    private static final String SOULBINDING_RITE_ENTRY = "void.maledict.soulbinding_rite";
    private static final String SOULBINDING_ATTACH_PAGE = "corrupt_maledict_soulbinding_rite";
    private static final String SOULBINDING_RELEASE_PAGE = "maledict_soulbinding_rite";

    private static final String LUMENFALL_BISMUTH_ENTRY = "void.maledict.lumenfall_bismuth";
    private static final String EUTECTIC_ENTRY = "void.maledict.refracted_lead_bismuth_eutectic";
    private static final String AUFHEBEN_ARMOR_ENTRY = "void.maledict.aufheben_lightward";

    private final boolean chinese;

    public MaledictLanguage(PackOutput output, String locale) {
        super(output, Maledict.MODID, locale);
        chinese = "zh_cn".equals(locale);
    }

    // 译文按中文、英文排列。中文法典正文每 13 个可见字符加一个换行用的空格。
    @Override
    protected void addTranslations() {
        add("itemGroup.maledict", "咒邪", "Maledict");

        addBlightedRealmTranslations();
        addWeepingWitherTranslations();
        addDemiurgeTranslations();
        addRavenTranslations();
        addVicissitudeTranslations();
        addSacrificeTranslations();
        addAgeOfEnlightenmentTranslations();
        addBleedingEyeTranslations();
        addEnchantmentsTranslations();
        addAxesTranslations();
        addBowsTranslations();
        addSpiritArrowsTranslations();
        addIncursusBladeTranslations();
        addAufhebenLightwardTranslations();
        addTotemicRunesTranslations();
        addObelisksTranslations();
        addInvocationTranslations();
        addSoulbindingTranslations();
        addMaidBondTranslations();
        addLightfallCodexTranslations();
        addLumenfallMaterialsTranslations();
    }

    private void addBlightedRealmTranslations() {
        add("dimension.maledict.blighted_realm", "腐枯界", "The Blighted Realm");
        add("biome.maledict.blighted_wastes", "腐枯荒原", "Blighted Wastes");
        addBlock(MaledictBlocks.WITHERED_LEAF_LITTER, "败叶层", "Withered Leaf Litter");
        addBlock(MaledictBlocks.EXPOSED_SOULWOOD_ROOTS, "灵魂木露根", "Exposed Soulwood Roots");
        addBlock(MaledictBlocks.FALLEN_SOULWOOD_BRANCHES, "灵魂木折枝", "Fallen Soulwood Branches");
        addBlock(MaledictBlocks.WITHERLIGHT_LAMP, "枯枝蕈灯", "Witherlight Branch Lamp");
        addBlock(MaledictBlocks.WITHERLIGHT_MUSHROOM, "枯灯蕈", "Witherlight Mushroom");
        add("tooltip.maledict.witherlight_mushroom",
                "散发微光；使用骨粉可在附近空地繁生。",
                "Gives off a faint light. Bone meal spreads it onto nearby empty ground.");

        addBlock(MaledictBlocks.TAINTED_MALIGNANT_LEAD_ORE, "污秽石恶念之铅矿石", "Tainted Malignant Lead Ore");
        addBlock(MaledictBlocks.TWISTED_MALIGNANT_LEAD_ORE, "扭曲石恶念之铅矿石", "Twisted Malignant Lead Ore");
        addItem(MaledictItems.MALIGNANT_LEAD_FRAGMENT, "恶念之铅碎片", "Malignant Lead Fragment");
        add("message.maledict.well_unavailable",
                "彼端无处落足，深井将你拒回。",
                "There is no safe passage beyond. The well rejects you.");
        add("message.maledict.realm_no_respawn",
                "此界无法安置你的归处。",
                "You cannot bind your return to this realm.");

        add("book.maledict.realm.station", "井站残记", "Well Station Notes");
        add("book.maledict.realm.station.1",
                "井已经干了。我们沿旧梯下到井室，把从采掘点带回的铅碎片留在侧间。梁上的缺口与那边木架上的一样，"
                    + "看来先走的人也曾在这里歇脚。",
                "The well is dry. We took the old ladder down and stored the lead fragments from the dig in the "
                    + "side room. The beams bear the same notches as the timber frame there. Others must have rested "
                    + "here before us.");
        add("book.maledict.realm.station.2",
                "祭址那边又没有回应。回来时，井沿的刻痕还在，折枝旁的白蕈也亮着。",
                "Still no answer at the rite site. Back here, the marks on the rim remain; pale mushrooms glow by "
                    + "the branches.");
        add("book.maledict.realm.station.3",
                "我用捡来的枝条托起蕈簇，放在梯口。光比单独的蕈亮，至少能看清下一根横档。",
                "I made a stand of fallen twigs for a cluster and set it by the ladder. Brighter than a lone "
                    + "mushroom. Enough to see the next rung.");
        add("book.maledict.realm.station.4",
                "旧侧廊塌了，我们没有再挖。剩下的碎片收进箱里，灯留在井室。若有人循着祭址的记号来到这里，"
                    + "先检查梯子，别径直跳下去。",
                "The old side passage collapsed. We left it alone. The remaining fragments are in the chest; the "
                    + "lamps stay below. If you followed the rite site's marks here, check the ladder first. Do not "
                    + "jump straight down.");

        add("book.maledict.realm.rite", "祭址残记", "Notes from a Failed Rite");
        add("book.maledict.realm.rite.1",
                "柱子已经撤下。基座下的石头仍有余温，可最后一份精魂没有回应。我们留下材料，准备明日重试。",
                "The poles have been taken down. The stone beneath the base is still warm, but the last spirit "
                    + "gave no answer. We left the materials for another attempt tomorrow.");
        add("book.maledict.realm.rite.2",
                "又一次回到这里。井口的记号还在，树根却穿过了石缝。枯灯蕈已经长到散落的枝条旁。我带走了几株。",
                "Back again. The mark by the well remains, though roots have entered the stonework. Pale "
                    + "mushrooms grow beside the fallen branches. I took a few with me.");

        add("book.maledict.realm.dig", "采掘残记", "Notes from an Abandoned Dig");
        add("book.maledict.realm.dig.1",
                "照旧沿着矿脉挖掘。上次标记的地方，取出的仍是灰暗而沉重的碎片。箱子上写着金，里面已经装满了铅。",
                "We followed the vein as before. The place we marked yielded the same dull, heavy fragments. The "
                    + "crate says gold. It is full of lead.");
        add("book.maledict.realm.dig.2",
                "支撑梁开始断裂。我们把工具留在浅坑，沿河返回井口。若还有人来，这条路至少可以再走一次。",
                "The supports are splitting. We left our tools in the shallow pit and followed the river back to "
                    + "the well. Whoever comes after us can still take that road.");
    }

    private void addWeepingWitherTranslations() {
        addBlock(MaledictBlocks.CRYING_TAINTED_ROCK, "哭泣的污秽石", "Crying Tainted Rock");
        addBlock(MaledictBlocks.CRYING_TWISTED_ROCK, "哭泣的扭曲石", "Crying Twisted Rock");
        addBlock(MaledictBlocks.BLIGHTED_WITHER_SKULL, "腐枯凋灵头颅", "Blighted Wither Skull");
        addItem(MaledictItems.BLIGHTED_STAR, "腐枯之星", "Blighted Star");
        addItem(MaledictItems.LIGHT_FRAGMENT, "光的残片", "Fragment of Light");
        add("entity.maledict.weeping_wither", "腐枯凋灵", "Weeping Wither");
        add("entity.maledict.weeping_wither_skull", "腐枯凋灵之首", "Weeping Wither Skull");
        add("entity.maledict.weeping_wither_skeleton", "腐枯凋灵骷髅", "Weeping Wither Skeleton");

        add("malum.gui.book.entry.void.maledict.weeping_wither", "腐枯凋灵", "Weeping Wither");
        add("malum.gui.book.entry.void.maledict.weeping_wither.description",
                "世界遗留的悲伤",
                "The sorrow the world left behind");
        add("malum.gui.book.entry.page.headline.void.maledict.weeping_wither", "腐枯凋灵", "Weeping Wither");
        add("malum.gui.book.entry.page.text.void.maledict.weeping_wither.1",
                "在这个已经堕入腐枯的世界地 下，我找到了这种残存的生物 。它十分像是在下界见过的凋 灵，"
                    + "只是由于世界已然腐枯， 这种凋灵承载了世界遗留的悲 伤，从而变得更加强大。",
                "Beneath this world, already fallen to blight, I found this surviving creature. It closely "
                    + "resembles the Withers I once saw in the Nether. Yet, as the world has fallen to blight, this "
                    + "Wither bears the sorrow the world left behind, and has become all the stronger for it.");
        add("malum.gui.book.entry.page.text.void.maledict.weeping_wither.2",
                "将世界的泪水灌注于凋灵骷髅 的头颅中，我们可以用这种腐 枯的头颅和哭泣的石头们召唤 腐枯凋灵。",
                "By infusing the tears of the world into Wither Skeleton skulls, we can use these blighted skulls "
                    + "and crying stones to summon a Weeping Wither.");
        add("malum.gui.book.entry.page.text.void.maledict.weeping_wither.summoning",
                "如同召唤普通凋灵，先摆出石 头的Ｔ型：横排中间放哭泣的 污秽石，两侧和下方放哭泣的 扭曲石。"
                    + "底层两侧留空，再在 顶上并排放置三个腐枯凋灵头 颅。击败它，便能获得腐枯之 星。",
                "As with an ordinary Wither, arrange the stones in a T. Place Crying Tainted Rock at the center "
                    + "of the crossbar, with Crying Twisted Rock to either side and beneath it. Leave air beside the "
                    + "stem. Place three Blighted Wither Skulls across the top. Defeat it to claim a Blighted Star.");
    }

    private void addDemiurgeTranslations() {
        addBlock(MaledictBlocks.RELIQUARY_ALTAR, "倒悬灵柩祭坛", "Inverted Reliquary Altar");
        add("entity.maledict.demiurge_statue", "亚大伯斯之石像", "Yaldabaoth Statue");
        add("sounds.maledict.entity.demiurge.awakening_roar", "亚大伯斯之石像怒吼", "Yaldabaoth Statue roars");
    }

    private void addRavenTranslations() {
        add("entity.maledict.raven", "渡鸦", "The Raven");
        add("entity.maledict.raven_feather", "渡鸦飞羽", "Raven Feather");
        addItem(MaledictItems.RAVEN_SPAWN_EGG, "渡鸦刷怪蛋", "The Raven Spawn Egg");
        addItem(MaledictItems.RAVEN_FEATHER, "渡鸦羽毛", "Raven Feather");
        add("sounds.maledict.entity.raven.ambient", "渡鸦鸣叫", "Raven calls");
        add("sounds.maledict.entity.raven.hurt", "渡鸦受伤", "Raven hurts");
        add("sounds.maledict.entity.raven.death", "渡鸦死亡", "Raven dies");
        add("sounds.maledict.entity.raven.step", "渡鸦脚步声", "Raven steps");
        add("sounds.maledict.entity.raven.fly", "渡鸦振翅", "Raven flaps");
    }

    private void addVicissitudeTranslations() {
        add("entity.maledict.first_vicissitude", "无常(WIP)", "Vicissitude(WIP)");
        addItem(MaledictItems.FIRST_VICISSITUDE_PHASE_ONE_SPAWN_EGG,
                "无常刷怪蛋（一阶段）",
                "Vicissitude Spawn Egg (Phase One)");
        addItem(MaledictItems.FIRST_VICISSITUDE_PHASE_TWO_SPAWN_EGG,
                "无常刷怪蛋（二阶段）",
                "Vicissitude Spawn Egg (Phase Two)");
        add("sounds.maledict.music.vicissitude.phase_one", "无常：恶客造访", "Vicissitude: An Unwelcome Guest");
        add("sounds.maledict.music.vicissitude.phase_two", "无常：不得已", "Vicissitude: What Cannot Be Helped");
        add("message.maledict.first_vicissitude.adaptation",
                "墨守成规，因循守旧，无常视之如粪土",
                "Rigid convention, blind adherence to the old ways: Vicissitude regards them as filth.");
        add("message.maledict.first_vicissitude.attack",
                "命运总是会将人逼上悬崖，犹如恶客造访",
                "Fate always drives people to the edge of a cliff, like an unwelcome guest calling.");
        add("message.maledict.first_vicissitude.phase_two",
                "所谓无常，就是圣人行走在正道上，依然会遇见的命运",
                "Vicissitude is the fate even sages meet while walking the righteous path.");

        addItem(MaledictItems.CURIO_RETURN_TOKEN, "保管凭证", "Custody Token");
        add("tooltip.maledict.curio_return_token",
                "右键领取被无常没收、尚未归还的饰品。全部交付后凭证才会消失。",
                "Right-click to reclaim confiscated curios that are still in custody. The token only disappears "
                    + "once everything has been handed back.");
        add("message.maledict.first_vicissitude.curio_return_pending",
                "还有 %s 件饰品由无常保管：腾出背包或饰品栏空间后会自动归还",
                "%s curio(s) are still in custody: free up inventory or curio slots and they will be returned "
                    + "automatically");
        add("message.maledict.first_vicissitude.curio_return_complete",
                "被没收的饰品已全部归还",
                "Every confiscated curio has been returned");

        add("message.maledict.vicissitude_rite.warning",
                "仪式已聆听你的呼唤。然而，回应你的存在必不听命于你。",
                "The ritual has heard your call. What answers will not obey you.");
        add("message.maledict.vicissitude_rite.confirm",
                "再次右键，承担随之而来的后果。",
                "Right-click again to embrace the consequences.");
        add("message.maledict.vicissitude_rite.occupied",
                "附近已有同类存在，无法再次启动召唤仪式。",
                "A being of the same kind is already nearby. The summoning ritual cannot begin.");
        add("message.maledict.vicissitude_rite.cramped",
                "周围的空间太过狭小，无法洞开创口",
                "The surrounding space is too confined to tear open a wound.");
        add("malum.gui.rite." + VICISSITUDE_RITE_ID, "无常仪式", "Rite of Vicissitude");
        add("malum.gui.rite.corrupted_" + VICISSITUDE_RITE_ID, "无常仪式", "Rite of Vicissitude");
        add("malum.gui.rite." + GREATER_RITE_ID, "无常大仪式", "Greater Rite of Vicissitude");
        add("malum.gui.rite.corrupted_" + GREATER_RITE_ID, "无常大仪式", "Greater Rite of Vicissitude");
        add("malum.gui.rite." + ELDRITCH_RITE_ID, "邪术无常仪式(WIP!)", "Eldritch Rite of Vicissitude(WIP!)");
        add("malum.gui.rite.corrupted_" + ELDRITCH_RITE_ID, "邪术无常仪式", "Eldritch Rite of Vicissitude");
        add("malum.gui.rite." + GREATER_ELDRITCH_RITE_ID, "邪术无常大仪式", "Greater Eldritch Rite of Vicissitude");
        add("malum.gui.rite.corrupted_" + GREATER_ELDRITCH_RITE_ID,
                "邪术无常大仪式",
                "Greater Eldritch Rite of Vicissitude");

        add("malum.gui.book.entry." + RITE_ENTRY, "无常仪式", "Rite of Vicissitude");
        add("malum.gui.book.entry." + RITE_ENTRY + ".description",
                "唤来无法收回之物",
                "Calling what cannot be recalled");
        add("malum.gui.book.entry.page.headline." + RITE_ENTRY, "无常仪式", "Rite of Vicissitude");
        add("malum.gui.book.entry.page.text." + RITE_ENTRY + ".1",
                "无常不是我能够创造的存在， 它只是从伤口侵入的影子。遵 从未知的法则，用三枚奥术精 魂 "
                    + "开启一扇微小的门扉，四枚推 得更开； 那顺门扉而来之物即是命运的 恶客。",
                "Vicissitude is not a being I can create; it is only a shadow that enters through the wound. By "
                    + "laws I do not know, three arcane spirits pry open a crack and four pry it wider - and what "
                    + "comes through that door is fate's own unwelcome guest.");
        add("malum.gui.book.entry.page.text." + RITE_ENTRY + ".2",
                "使用邪术精魂会扩大无常仪式 的影响力。更完整的影子照落 在此处，虽然影子很 难说完整。"
                    + "而两枚邪术精魂则 会召唤更加完整的一面。",
                "Using eldritch spirits extends the reach of the Rite of Vicissitude. A more complete shadow "
                    + "falls upon this place, though a shadow can hardly be called complete. Two eldritch spirits "
                    + "summon a more complete aspect still.");
    }

    private void addSacrificeTranslations() {
        add("effect.maledict.sacrifice", "献祭", "Sacrifice");
        add("effect.maledict.sacrifice.description",
                "贤者献祭生命，因而获取智慧。然而并非每一次牺牲都会带来智慧，只有模仿先贤的仪式，"
                    + "才有可能获得先贤的智慧",
                "Sages gave their lives in sacrifice, and thereby gained wisdom. Yet not every sacrifice brings "
                    + "wisdom: only by imitating the rites of the sages might one gain a sage's wisdom.");
        add("advancements.maledict.wisdom_sacrifice.title", "智慧的牺牲", "Wisdom's Sacrifice");
        add("advancements.maledict.wisdom_sacrifice.description",
                "熬过九天九夜的献祭，获得贤者的智慧。",
                "Survive nine days and nights of sacrifice to gain a sage's wisdom.");
        add("curios.identifier.delusion", "视界", "Vision");
        add("curios.modifiers.delusion", "装备于视界槽位时：", "When equipped in a vision slot:");

        add("malum.gui.rite.corrupted_" + SACRIFICE_RITE_ID, "牺牲仪式", "Rite of Sacrifice");
        add("malum.gui.book.entry.page.text.corrupt_" + SACRIFICE_RITE_ID + ".hover",
                "在撑过献祭后，获得一个额外 的符文槽位和一个视界槽位。",
                "Survive the sacrifice to gain an additional rune slot and a vision slot.");
        add("malum.gui.book.entry." + SACRIFICE_RITE_ENTRY, "牺牲仪式", "Rite of Sacrifice");
        add("malum.gui.book.entry." + SACRIFICE_RITE_ENTRY + ".description", "智慧的代价", "The Price of Wisdom");
        add("malum.gui.book.entry.page.text." + SACRIFICE_RITE_PAGE,
                "智慧从何而来？于一场刻意的 仪式中，使用提尔锋伤害自己 ，妄想获得贤人的智慧，这能 够实现吗？",
                "Where does wisdom come from? Could I truly gain a sage's wisdom by wounding myself with Tyrving "
                    + "in a deliberate rite, as I so foolishly hope?");
        add("malum.gui.book.entry.page.text." + SACRIFICE_RITE_PAGE + ".2",
                "在九天九夜的折磨中，我确实 看到了些什么，也知道了些什 么。我的身体有额外的位置能 够刻画符文，"
                    + "而我的眼睛变成 了一处门扉。 透过这献祭的视界，我能够更 清楚的看这世界。",
                "Through nine days and nine nights of torment I did see something, and came to know something as "
                    + "well. My body has gained another place where runes may be carved, and my eyes have become a "
                    + "doorway. Through this sacrificial vision, I can see the world more clearly.");
    }

    private void addAgeOfEnlightenmentTranslations() {
        addItem(MaledictItems.AGE_OF_ENLIGHTENMENT, "启蒙之年", "Age of Enlightenment");
        add("effect.maledict.age_of_enlightenment", "启蒙之年", "Age of Enlightenment");
        add("effect.maledict.age_of_enlightenment.description",
                "人们把蒙昧与黑暗错认为神圣，你的近战攻击伤害翻倍；每级额外给予 1 点法杖暂存弹数。",
                "People mistake ignorance and darkness for the sacred. Your melee attacks deal double damage; "
                    + "each level also grants 1 additional Reserve Staff Charge.");
        add("effect.maledict.age_of_darkness", "黑暗时代", "Age of Darkness");
        add("effect.maledict.age_of_darkness.description",
                "任由野蛮焚烧文明的黑暗时代啊，每级降低 20% 魔法抗性、灵魂护盾容量与灵魂护盾稳固度。",
                "O dark age that lets barbarism burn civilization: each level reduces magic resistance, Soul Ward "
                    + "capacity and Soul Ward integrity by 20%.");

        add("malum.gui.curio.effect.maledict.age_of_enlightenment.cooldown",
                "佩戴时冷却速度加倍",
                "Cooldowns recover twice as fast while worn");
        add("malum.gui.curio.effect.maledict.age_of_enlightenment.spirit_void",
                "攻击半生命值目标时触发收获精魂时的效果",
                "Attacking targets at half health triggers spirit-harvesting effects");
        add("malum.gui.curio.effect.maledict.age_of_enlightenment.enlightenment",
                "击杀敌人时获得启蒙之年，已有则延长时间",
                "Slaying an enemy grants Age of Enlightenment, or extends its duration if already active");
        add("tooltip.maledict.age_of_enlightenment.hold_shift",
                "按住 Shift 追问一个不该问的问题",
                "Hold Shift to ask a question better left unasked");
        add("tooltip.maledict.age_of_enlightenment.shift",
                "黑暗的时代曾经存在过吗",
                "Did an age of darkness ever exist?");
        add("tooltip.maledict.age_of_enlightenment.shift.enlightened",
                "那无穷，无限，永动的启蒙之年啊",
                "O infinite, boundless, ever-turning Age of Enlightenment");
    }

    private void addBleedingEyeTranslations() {
        addItem(MaledictItems.BLEEDING_EYE, "流血之眼", "Bleeding Eye");
        add("tooltip.maledict.bleeding_eye", "鲜红", "Bright red");
        add("malum.gui.book.entry." + BLEEDING_EYE_ENTRY, "流血之眼", "Bleeding Eye");
        add("malum.gui.book.entry." + BLEEDING_EYE_ENTRY + ".description", "门扉之遗", "The left of Door");
        add("malum.gui.book.entry.page.headline." + BLEEDING_EYE_ENTRY, "流血之眼", "Bleeding Eye");
        add("malum.gui.book.entry.page.text." + BLEEDING_EYE_ENTRY + ".1",
                "提尔锋干净的切下了我的一只 眼睛。这是预期的牺牲。",
                "Tyrfing cleanly cut out one of my eyes. It was the sacrifice I had expected.");
        add("malum.gui.book.entry.page.text." + BLEEDING_EYE_ENTRY + ".2",
                "这留下的眼睛因而被祝圣，\n仍然在源源不断的露出鲜血。\n 一片鲜红。但是值得。",
                "The eye that remained was consecrated by it,\nand still bleeds without end.\nEverything is "
                    + "stained crimson. But it was worth it.");

        addItem(MaledictItems.HANGMAN_FOOL, "倒吊人的愚见", "Hangman`s fool");
        add("tooltip.maledict.hangman_fool.flavor.0",
                "献出力量，以求取人智",
                "\"strength\" is sacrificed for \"human wisdom\"");
        add("tooltip.maledict.hangman_fool.flavor.1",
                "智性虽有缺陷，却并非谬误",
                "Intelligence that mortals grasp is flawed, it cannot be said to be false");
        add("tooltip.maledict.hangman_fool.flavor.2",
                "沉沦于黑暗，以期光明",
                "Light sinks into the dark, seeking to redeem the radiance.");
        add("tooltip.maledict.hangman_fool.flavor.3", "", "");
        add("tooltip.maledict.hangman_fool.flavor.4", "", "");
        add("entity.maledict.hangman_trap", "倒吊人的陷阱法阵", "Hangman's Trap Circle");
        add("malum.gui.curio.effect.maledict.hangman_fool.trap",
                "弹射物落到方块上留下陷阱法阵，接触生物时触发原有命中效果",
                "Projectiles leave a trap on blocks, triggering their original hit effects on contact");
        add("malum.gui.curio.effect.maledict.hangman_fool.reflect",
                "有概率反弹弹射物，生命越低概率越高",
                "Chance to reflect projectiles increases as health falls");
        add("malum.gui.curio.effect.maledict.hangman_fool.relay",
                "弹射物从自身16格内的法阵按近到远轮流射出，瞄准视线64格内的首个实体或方块，不消耗法阵",
                "Shots cycle through your circles within 16 blocks, nearest first, aiming at the first entity or "
                    + "block within 64 blocks of your gaze without consuming circles");
        add("malum.gui.curio.effect.maledict.hangman_fool.restrictions",
                "无法举盾或触发不死图腾",
                "Cannot raise shields or activate totems of undying");
        add("malum.gui.book.entry.page.headline." + HANGMAN_FOOL_PAGE, "倒吊人的愚见", "Hangman`s fool");
        add("malum.gui.book.entry.page.text." + HANGMAN_FOOL_PAGE + ".1",
                "升临高天，\n落入深谷。\n 倒吊自身，\n坠入深井，",
                "Ascend to the highest heavens,\ndescend into the deepest abyss.\nHang thyself inverted,\nand "
                    + "plunge into the bottomless well.");
        add("malum.gui.book.entry.page.text." + HANGMAN_FOOL_PAGE + ".2",
                "让灵知踏过七重长阶，\n借以倒映纯粹的真知。\n 我的功业将成。",
                "Let gnosis traverse the sevenfold stair,\nthat it may reflect the purest truth.\nMy Great Work "
                    + "is nigh upon completion.");

        addItem(MaledictItems.EIDOLON_OF_THE_HIGH_PRIESTESS, "女祭司之灵相", "Eidolon of the High Priestess");
        add("entity.maledict.soul_echo", "灵魂残影", "Soul Echo");
        add("malum.gui.curio.effect.maledict.high_priestess.life_manifestation",
                "生命显影：看见16格内生物的轮廓，包括隔墙与隐身的生物，仅自己可见。",
                "Life Manifestation: only you see the outlines of living creatures within 16 blocks, even through "
                    + "walls or invisibility.");
        add("malum.gui.curio.effect.maledict.high_priestess.soul_displacement",
                "灵魂离位：法术命中使目标在原地留下3秒残影。法术击中残影会伤及本体，但不会刷新残影。",
                "Soul Displacement: spell hits leave a 3-second echo where the target stood. Spells hitting the "
                    + "echo damage its body without refreshing the echo.");
        add("malum.gui.curio.effect.maledict.high_priestess.wound_yet_to_come",
                "未至之伤：伤害与治疗缓慢结算。造成伤害时获得等量治疗，先抵消待结算伤害，余量再缓慢回复生命。",
                "The Wound Yet to Come: damage and healing settle gradually. Damage dealt grants equal healing, "
                    + "first canceling pending damage, then gradually restoring health with any remainder.");
        add("malum.gui.book.entry.page.headline." + HIGH_PRIESTESS_PAGE,
                "女祭司之灵相",
                "Eidolon of the High Priestess");
        add("malum.gui.book.entry.page.text." + HIGH_PRIESTESS_PAGE + ".1",
                "在面纱之后，渴望超越的道路",
                "Beyond the veil, yearning for the path to transcendence.");
    }

    private void addEnchantmentsTranslations() {
        add("enchantment.maledict.ectoplasm", "灵质", "Ectoplasm");
        add("enchantment.maledict.ectoplasm.desc",
                "每级使射出的箭矢速度提高 8%，每发消耗背包中（等级 + 1）÷ 2 枚精魂；精魂不足时不生效。",
                "Each level increases arrow speed by 8%. Each shot consumes (level + 1) / 2 spirits from your "
                    + "inventory; the enchantment has no effect without enough spirits.");

        add("enchantment.maledict.reminiscence", "追忆", "Reminiscence");
        add("enchantment.maledict.reminiscence.desc",
                "射出的箭矢可穿过一次方块，能够穿透的厚度为 2 + 等级 格。",
                "Arrows can pass through blocks once, up to a thickness of 2 + level blocks.");

        add("enchantment.maledict.aftertaste", "回味", "Aftertaste");
        add("enchantment.maledict.aftertaste.desc",
                "镰刀命中时，按这一击占目标最大生命的比例，尝到其掉落物本可提供的饥饿与饱食度；1 至 3 级分别回复 "
                    + "50%、60%、70%，掉落物附带的食物效果也会一并生效。",
                "When a scythe strikes, you taste the hunger and saturation that the target's drops would "
                    + "restore, scaled by the damage dealt as a fraction of its maximum health. Levels 1 to 3 restore "
                    + "50%, 60% and 70% of that amount, respectively. Any food effects from those drops also apply.");

        add("enchantment.maledict.diplopia", "复视", "Diplopia");
        add("enchantment.maledict.diplopia.desc",
                "回旋投出的镰刀每级额外分裂出一把幻影镰刀。幻影造成本体 50% 的伤害。",
                "Each level of Rebound throws one additional phantom scythe. Phantoms deal 50% of the original "
                    + "throw's damage");
    }

    private void addAxesTranslations() {
        addItem(MaledictItems.MELTFALL_AXE, "熔坠之斧", "Meltfall Axe");
        add("tooltip.maledict.meltfall_axe.flavor",
                "熔炼，坠落，秘仪的开始",
                "Smelting, falling, the beginning of the mysteries");
        add("malum.gui.book.entry.maledict.meltfall_axe", "熔坠之斧", "Meltfall Axe");
        add("malum.gui.book.entry.maledict.meltfall_axe.description",
                "熔炼，坠落，秘仪的开始",
                "Smelting, falling, the beginning of the mysteries");
        add("malum.gui.book.entry.page.headline.maledict.meltfall_axe", "熔坠之斧", "Meltfall Axe");
        add("malum.gui.book.entry.page.text.maledict.meltfall_axe.1",
                "将腐枯的随机性拟化为堕落， 我得到了一种极其不受控的武 器。",
                "By giving the randomness of blight the form of Descent, I have created an extremely "
                    + "uncontrollable weapon.");
        add("malum.gui.book.entry.page.text.maledict.meltfall_axe.2",
                "持有它会使造成和受到的伤害 都大大提升。",
                "Holding it greatly increases both the damage I deal and the damage I take.");

        addItem(MaledictItems.LOVER_AXE, "恋人之斧", "The Lover's AX");
        add("tooltip.maledict.lover_axe.flavor", "纵然是死亡也无法使我们分离", "Even death cannot part us");
        add("tooltip.maledict.lover_axe.wound",
                "每次有效近战命中使目标最大生命值降低 2%，真正死亡后清除。",
                "Each effective melee hit reduces the target's maximum health by 2%, cleared on true death.");
        add("tooltip.maledict.lover_axe.descent",
                "主手或副手持有时每秒增加 0.1 堕落；离手每秒减少 0.1。",
                "Gain 0.1 Descent per second in either hand; lose 0.1 per second when not held.");
        add("tooltip.maledict.lover_axe.charged",
                "蓄力至少 90% 的有效命中额外增加 2 堕落。",
                "Effective hits with at least 90% charge grant 2 additional Descent.");
        add("attribute.name.maledict.descent", "堕落", "Descent");
        add("attribute.name.maledict.descent.desc",
                "提高造成与受到的伤害，倍率随双方堕落值之和增加而增加",
                "Increases damage dealt and taken, with the multiplier increasing as the sum of both entities' "
                    + "Descent rises");
    }

    private void addBowsTranslations() {
        addItem(MaledictItems.REMEMBRANCE_BOW, "思念弓", "Remembrance Bow");
        add("malum.gui.book.entry.maledict.remembrance_bow", "思念弓", "Remembrance Bow");
        add("malum.gui.book.entry.maledict.remembrance_bow.description", "穿越阻隔的思念", "Longing Beyond Barriers");
        add("malum.gui.book.entry.page.headline.maledict.remembrance_bow", "思念弓", "Remembrance Bow");
        add("malum.gui.book.entry.page.text.maledict.remembrance_bow.1",
                "若能汇集末影人的魔法并以星 灵织物承载，就能使灵魂木产 生奇妙的性质。",
                "Gathering the magic of endermen into Astral Weave can give Soulwood wondrous properties.");
        add("malum.gui.book.entry.page.text.maledict.remembrance_bow.2",
                "拉动这种材质的弓只需要一般 弓一半的时间，射出的箭矢不 仅更快，还可以有限度的穿过 方块。",
                "A bow made from this wood takes half as long to draw as an ordinary bow. Its arrows fly faster "
                    + "and can pass through blocks to a limited depth.");

        addItem(MaledictItems.ELEGY_BOW, "哀歌弓", "Elegy Bow");
        add("malum.gui.book.entry." + ELEGY_BOW_ENTRY, "哀歌弓", "Elegy Bow");
        add("malum.gui.book.entry." + ELEGY_BOW_ENTRY + ".description", "停不下的思念", "Remembrance Without Respite");
        add("malum.gui.book.entry.page.headline." + ELEGY_BOW_ENTRY, "哀歌弓", "Elegy Bow");
        add("malum.gui.book.entry.page.text." + ELEGY_BOW_ENTRY + ".1",
                "我走不出来。  我走不出来。  我走不出来。  我走不出来。  我走不出来。  我走不出来。  我走不出来。",
                "I cannot move on. I cannot move on. I cannot move on. I cannot move on. I cannot move on. I "
                    + "cannot move on. I cannot move on.");
        add("malum.gui.book.entry.page.text." + ELEGY_BOW_ENTRY + ".2",
                "我走不出来。  我走不出来。  我走不出来。  我走不出来。  我将我不止息的哀歌灌注到弓 中，"
                    + "使之具有了满蓄力自动发 射的能力。",
                "I cannot move on. I cannot move on. I cannot move on. I cannot move on. I poured my unceasing "
                    + "elegy into the bow, allowing it to fire automatically at full draw.");
    }

    private void addSpiritArrowsTranslations() {
        add("malum.gui.book.entry.maledict.spirit_arrows", "精魂箭", "Spirit Arrows");
        add("malum.gui.book.entry.maledict.spirit_arrows.description", "以精魂为锋", "Spirits for arrowheads");
        add("malum.gui.book.entry.page.headline.maledict.spirit_arrows", "精魂箭", "Spirit Arrows");
        add("malum.gui.book.entry.page.text.maledict.spirit_arrows.1",
                "我试着以精魂代替燧石，制成 了八种箭矢。它们可以用弓或 弩射出，命中时也能暴露生物 的灵魂。"
                    + "不同精魂的性质仍留 在箭头上，选对箭矢，便能省 去不少麻烦。",
                "I tried replacing flint with spirits and made eight kinds of arrows. They can be fired from bows "
                    + "or crossbows, and expose the souls of creatures they strike. Each spirit retains its nature in "
                    + "the arrowhead. Choosing the right arrow can spare me a good deal of trouble.");

        addItem(MaledictItems.SACRED_SPIRIT_ARROW, "神圣精魂箭", "Sacred Spirit Arrow");
        add("malum.gui.book.entry.maledict.sacred_spirit_arrow", "神圣精魂箭", "Sacred Spirit Arrow");
        add("malum.gui.book.entry.maledict.sacred_spirit_arrow.description", "驱逐亡者", "Driving back the dead");
        add("malum.gui.book.entry.page.headline.maledict.sacred_spirit_arrow", "神圣精魂箭", "Sacred Spirit Arrow");
        add("malum.gui.book.entry.page.text.maledict.sacred_spirit_arrow.1",
                "神圣精魂排斥死而不息之物。 对亡灵生物造成的箭矢伤害提 高一半。",
                "Sacred spirits reject those who persist beyond death. Arrow damage against undead creatures is "
                    + "increased by half.");

        addItem(MaledictItems.WICKED_SPIRIT_ARROW, "邪恶精魂箭", "Wicked Spirit Arrow");
        add("malum.gui.book.entry.maledict.wicked_spirit_arrow", "邪恶精魂箭", "Wicked Spirit Arrow");
        add("malum.gui.book.entry.maledict.wicked_spirit_arrow.description", "侵蚀生者", "Preying on the living");
        add("malum.gui.book.entry.page.headline.maledict.wicked_spirit_arrow", "邪恶精魂箭", "Wicked Spirit Arrow");
        add("malum.gui.book.entry.page.text.maledict.wicked_spirit_arrow.1",
                "邪恶精魂更善于侵蚀生者。对 非亡灵生物造成的箭矢伤害提 高一半。",
                "Wicked spirits are better suited to harming the living. Arrow damage against creatures that are "
                    + "not undead is increased by half.");

        addItem(MaledictItems.ARCANE_SPIRIT_ARROW, "奥术精魂箭", "Arcane Spirit Arrow");
        add("malum.gui.book.entry.maledict.arcane_spirit_arrow", "奥术精魂箭", "Arcane Spirit Arrow");
        add("malum.gui.book.entry.maledict.arcane_spirit_arrow.description", "箭锋之外", "Beyond the arrowhead");
        add("malum.gui.book.entry.page.headline.maledict.arcane_spirit_arrow", "奥术精魂箭", "Arcane Spirit Arrow");
        add("malum.gui.book.entry.page.text.maledict.arcane_spirit_arrow.1",
                "奥术精魂将魔法一同送入伤口 。命中后，额外造成三点有力 的伤害。",
                "Arcane spirits carry magic into the wound. On hit, they deal 3 additional points of potent damage.");

        addItem(MaledictItems.ELDRITCH_SPIRIT_ARROW, "邪术精魂箭", "Eldritch Spirit Arrow");
        add("malum.gui.book.entry.maledict.eldritch_spirit_arrow", "邪术精魂箭", "Eldritch Spirit Arrow");
        add("malum.gui.book.entry.maledict.eldritch_spirit_arrow.description", "伤口的回响", "An echo in the wound");
        add("malum.gui.book.entry.page.headline.maledict.eldritch_spirit_arrow", "邪术精魂箭", "Eldritch Spirit Arrow");
        add("malum.gui.book.entry.page.text.maledict.eldritch_spirit_arrow.1",
                "邪术精魂使箭矢的伤害有了回 响。命中后，额外造成相当于 箭矢伤害一半的有力的伤害。 "
                    + "这一份不计暴击增伤。",
                "Eldritch spirits make the arrow's damage echo. On hit, they deal additional potent damage equal "
                    + "to half the arrow's damage, excluding bonus damage from critical hits.");

        addItem(MaledictItems.AERIAL_SPIRIT_ARROW, "澄空精魂箭", "Aerial Spirit Arrow");
        add("malum.gui.book.entry.maledict.aerial_spirit_arrow", "澄空精魂箭", "Aerial Spirit Arrow");
        add("malum.gui.book.entry.maledict.aerial_spirit_arrow.description", "乘风而行", "Riding the wind");
        add("malum.gui.book.entry.page.headline.maledict.aerial_spirit_arrow", "澄空精魂箭", "Aerial Spirit Arrow");
        add("malum.gui.book.entry.page.text.maledict.aerial_spirit_arrow.1",
                "澄空精魂使箭矢更加轻快。射 出时的速度提高一半，也因此 飞得更远，命中更重。",
                "Aerial spirits lend the arrow swiftness. Its launch speed is increased by half, allowing it to "
                    + "fly farther and strike harder.");

        addItem(MaledictItems.AQUEOUS_SPIRIT_ARROW, "碧水精魂箭", "Aqueous Spirit Arrow");
        add("malum.gui.book.entry.maledict.aqueous_spirit_arrow", "碧水精魂箭", "Aqueous Spirit Arrow");
        add("malum.gui.book.entry.maledict.aqueous_spirit_arrow.description", "穿行水中", "Through the water");
        add("malum.gui.book.entry.page.headline.maledict.aqueous_spirit_arrow", "碧水精魂箭", "Aqueous Spirit Arrow");
        add("malum.gui.book.entry.page.text.maledict.aqueous_spirit_arrow.1",
                "碧水精魂使箭矢得以穿行水中 。在水中不会受到额外阻力， 也不会因重力下坠。",
                "Aqueous spirits let the arrow travel through water without the added drag or the downward pull "
                    + "of gravity.");

        addItem(MaledictItems.EARTHEN_SPIRIT_ARROW, "大地精魂箭", "Earthen Spirit Arrow");
        add("malum.gui.book.entry.maledict.earthen_spirit_arrow", "大地精魂箭", "Earthen Spirit Arrow");
        add("malum.gui.book.entry.maledict.earthen_spirit_arrow.description", "沉重的箭锋", "A weightier arrowhead");
        add("malum.gui.book.entry.page.headline.maledict.earthen_spirit_arrow", "大地精魂箭", "Earthen Spirit Arrow");
        add("malum.gui.book.entry.page.text.maledict.earthen_spirit_arrow.1",
                "大地精魂使箭头更加沉重。基 础伤害提高一点五，实际增加 的伤害随箭矢速度而变化。",
                "Earthen spirits lend weight to the arrowhead. Base damage is increased by 1.5; the actual damage "
                    + "gained varies with the arrow's speed.");

        addItem(MaledictItems.INFERNAL_SPIRIT_ARROW, "狱火精魂箭", "Infernal Spirit Arrow");
        add("malum.gui.book.entry.maledict.infernal_spirit_arrow", "狱火精魂箭", "Infernal Spirit Arrow");
        add("malum.gui.book.entry.maledict.infernal_spirit_arrow.description", "无处遁形", "Nowhere to hide");
        add("malum.gui.book.entry.page.headline.maledict.infernal_spirit_arrow", "狱火精魂箭", "Infernal Spirit Arrow");
        add("malum.gui.book.entry.page.text.maledict.infernal_spirit_arrow.1",
                "狱火精魂使箭矢燃烧，能够点 燃目标。命中后还会使目标发 光十秒，让它无处遁形。",
                "Infernal spirits set the arrow ablaze, allowing it to ignite its target. On hit, the target also "
                    + "glows for 10 seconds, leaving it nowhere to hide.");
    }

    private void addIncursusBladeTranslations() {
        addItem(MaledictItems.INCURSUS_BLADE, "神侵恶刃", "The Incursus Blade");
        add("attribute.name.maledict.powder_snow_damage", "冻结伤害", "Freezing Damage");
        add("attribute.name.maledict.transcend_omen", "超越预兆", "Transcend Omen");
        add("attribute.name.maledict.transcend_omen.desc",
                "与恶念转化互相抵消，余量按转化配方消耗部分护甲、护甲韧性与魔抗，以独立乘区按百分比增强法术属性。"
                    + "仅增强已偏离默认值的法术属性；多条有效路线分摊收益，共用防御只扣除一次。",
                "Cancels Malignant Conversion, then uses the remainder to trade some armor, armor toughness and "
                    + "magic resistance for percentage increases to magical attributes through its conversion recipes, "
                    + "multiplying their existing totals. Only magical attributes already different from their default "
                    + "values participate. Active routes share the gains, while shared defenses are "
                    + "consumed only once.");

        add("sounds.maledict.item.incursus_blade.throw",
                "神侵恶刃：清脆的鸣叫 回旋而去",
                "Incursus Blade: Keening, Whirling Away");
        add("sounds.maledict.item.incursus_blade.recall",
                "神侵恶刃：撕风裂帛 金声玉振",
                "Incursus Blade: Rending Wind and Silk, Ringing Gold and Jade");
        add("sounds.maledict.item.incursus_blade.ascension", "神侵恶刃：振翅", "Incursus Blade: Wings Beat");
        add("sounds.maledict.item.incursus_blade.slash", "神侵恶刃：侵蚀", "Incursus Blade: Erosion");
        add("sounds.maledict.item.incursus_blade.crit", "神侵恶刃：狂喜咆哮", "Incursus Blade: Ecstatic Roar");

        add("tooltip.maledict.incursus_blade.description",
                "吞噬精魂成长，向周身挥出锋刃的凶邪镰刀。",
                "A sinister scythe that grows by devouring spirits and sweeps its blade around the wielder.");
        add("tooltip.maledict.incursus_blade.medium_unlock_hint",
                "精魂之力均到达神圣之数会解锁更本征的力量",
                "When all spirit power reaches the sacred number, a more intrinsic power is unlocked.");
        add("tooltip.maledict.incursus_blade.hold_shift", "按住 Shift 查看精魂详情", "Hold Shift for spirit details");
        add("tooltip.maledict.incursus_blade.infusion",
                "在背包中拿精魂右键此物品以永久注入：",
                "Right-click this item with spirits in the inventory to infuse them permanently:");
        add("tooltip.maledict.incursus_blade.spirit.earthen",
                "大地 %s（%s/%s）：攻击力",
                "Earthen %s (%s/%s): Attack damage");
        add("tooltip.maledict.incursus_blade.spirit.aqueous",
                "碧水 %s（%s/%s）：冻结伤害",
                "Aqueous %s (%s/%s): Freezing damage");
        add("tooltip.maledict.incursus_blade.spirit.arcane",
                "奥术 %s（%s/%s）：魔法伤害",
                "Arcane %s (%s/%s): Magic damage");
        add("tooltip.maledict.incursus_blade.spirit.aerial",
                "澄空 %s%%（%s/%s）：净化随机负面并获得随机正面效果",
                "Aerial %s%% (%s/%s): Remove random harmful effects and gain random beneficial effects");
        add("tooltip.maledict.incursus_blade.spirit.sacred",
                "神圣 %s%%（%s/%s）：缩短敌方正面效果，延长或施加负面效果",
                "Sacred %s%% (%s/%s): Shorten enemies' beneficial effects; extend or inflict harmful effects");
        add("tooltip.maledict.incursus_blade.spirit.infernal",
                "狱火 %s（%s/%s）：额外抢夺等级",
                "Infernal %s (%s/%s): Additional looting level");
        add("tooltip.maledict.incursus_blade.spirit.eldritch",
                "邪术 %s%%（%s/%s）：伤害按比例转化为有上限的黄心，并立即恢复灵魂护盾",
                "Eldritch %s%% (%s/%s): Convert a portion of damage into absorption up to a cap, and immediately "
                    + "restore Soul Ward");
        add("tooltip.maledict.incursus_blade.spirit.wicked",
                "邪恶 %s%%（%s/%s）：暴击伤害",
                "Wicked %s%% (%s/%s): Critical damage");

        add("malum.gui.book.entry.void.maledict.incursus_blade", "神侵恶刃", "The Incursus Blade");
        add("malum.gui.book.entry.void.maledict.incursus_blade.description", "永无止境的饥渴", "A hunger without end");
        add("malum.gui.book.entry.page.headline.void.maledict.incursus_blade", "神侵恶刃", "The Incursus Blade");
        add("malum.gui.book.entry.page.text.void.maledict.incursus_blade.1",
                "也许是坠井击碎了我对禁忌的 畏怖。恶念白镴对知识消退的 渴望铸造了挽魂锋镰。",
                "Perhaps falling into the well shattered my fear of the forbidden. Malignant Pewter's hunger for "
                    + "the erosion of knowledge forged the Edge of Deliverance.");
        add("malum.gui.book.entry.page.text.void.maledict.incursus_blade.2",
                "那么能否将这种排斥转化为一 种显性的魔法性质呢？之前的 研究提醒了我，如果能够构建 "
                    + "一个动态稳定的环境，在八种 精魂脉动的刺激下，也许第九 种奥术能量的脉动得以显现。",
                "Could that rejection be transformed into an overt magical property? My earlier research reminded "
                    + "me that, if I could construct a dynamically stable environment, the pulses of the eight spirits "
                    + "might provoke the pulse of a ninth arcane energy to reveal itself.");
        add("malum.gui.book.entry.page.text.void.maledict.incursus_blade.3",
                "最后我得到了这种武器，挽魂 锋镰本身的意志被虚空吞噬殆 尽，这片空无渴求着吞并一切 ，"
                    + "而八种精魂完全受其操控， 甚至脉动被扭曲，从而对外展 现出意料之外的能力。",
                "At last I obtained this weapon. The will within the Edge of Deliverance was utterly devoured by "
                    + "the void. This emptiness hungers to consume everything, while the eight spirits submit entirely "
                    + "to its control; even their pulses are distorted, manifesting unexpected abilities.");
        add("malum.gui.book.entry.page.text.void.maledict.incursus_blade.4",
                "随着投入精魂数量的增加，这 柄武器也更加的难以捉摸。我 将之称为神侵恶刃，并希望这 "
                    + "片虚空能够满足于脉动。",
                "As more spirits are offered, the weapon becomes ever more inscrutable. I have named it the "
                    + "Incursus Blade, and hope this void can be sated by the pulses.");
    }

    private void addAufhebenLightwardTranslations() {
        addItem(MaledictItems.AUFHEBEN_LIGHTWARD_HELMET, "扬弃逐光头盔", "Aufheben Lightward Helmet");
        addItem(MaledictItems.AUFHEBEN_LIGHTWARD_CHESTPLATE, "扬弃逐光胸甲", "Aufheben Lightward Chestplate");
        addItem(MaledictItems.AUFHEBEN_LIGHTWARD_LEGGINGS, "扬弃逐光护腿", "Aufheben Lightward Leggings");
        addItem(MaledictItems.AUFHEBEN_LIGHTWARD_BOOTS, "扬弃逐光靴子", "Aufheben Lightward Boots");
        add("tooltip.maledict.aufheben_lightward.set", "套装效果（%s/6）", "Set effect (%s/6)");
        add("tooltip.maledict.aufheben_lightward.composition", "扬弃逐光套装组成：", "Aufheben Lightward set:");
        add("tooltip.maledict.aufheben_lightward.composition.armor",
                "扬弃逐光头盔，扬弃逐光胸甲，扬弃逐光护腿，扬弃逐光靴子",
                "Aufheben Lightward Helmet, Aufheben Lightward Chestplate, Aufheben Lightward Leggings, Aufheben "
                    + "Lightward Boots");
        add("tooltip.maledict.aufheben_lightward.composition.weapons",
                "神侵恶刃，熔坠之斧，恋人之斧",
                "The Incursus Blade, Meltfall Axe, Lover's Axe");
        add("tooltip.maledict.aufheben_lightward.effect.2",
                "2：弃形：灵魂护盾尚存时移动速度 +15%",
                "2: Abnegation of Flesh: +15% movement speed while the Soul Ward holds");
        add("tooltip.maledict.aufheben_lightward.effect.3",
                "3：以魂代形：灵魂护盾尚存时额外抵消 %s%% 的最终伤害",
                "3: Soul over Flesh: absorb a further %s%% of the final damage while the Soul Ward holds");
        add("tooltip.maledict.aufheben_lightward.effect.4",
                "4：未竟飞升：附魔等级按灵魂护盾充盈度 +1 / +2 / +3",
                "4: Unfulfilled Ascension: enchantment levels +1 / +2 / +3 by Soul Ward fill");
        add("tooltip.maledict.aufheben_lightward.effect.5", "5：武器攻击更加真实", "5: Weapon attacks land more truly");
        add("tooltip.maledict.aufheben_lightward.enchantment", "%s（%s+%s）", "%s (%s+%s)");

        add("malum.gui.book.entry." + AUFHEBEN_ARMOR_ENTRY, "扬弃逐光盔甲", "Aufheben Lightward Armor");
        add("malum.gui.book.entry." + AUFHEBEN_ARMOR_ENTRY + ".description", "未竟的飞升", "Ascension unfulfilled");
        add("malum.gui.book.entry.page.headline." + AUFHEBEN_ARMOR_ENTRY, "扬弃逐光盔甲", "Aufheben Lightward Armor");
        add("malum.gui.book.entry.page.text." + AUFHEBEN_ARMOR_ENTRY + ".1",
                "魂染钢曾以灵魂庇护肉体。我 以折辉铅铋共晶重塑其甲胄， 又将一颗下界之星纳入锻造。",
                "Soulstained Steel once sheltered the flesh through the strength of the soul. I have reshaped "
                    + "that armor with Refracted Lead–Bismuth Eutectic and worked a Nether Star into its forging.");
        add("malum.gui.book.entry.page.text." + AUFHEBEN_ARMOR_ENTRY + ".2",
                "这份庇护由此走向更为激进的 形态：舍去一部分血肉的防御 ，将更多伤害托付给灵魂承担 。"
                    + "这副盔甲呈银色，晶体 表面的青紫折辉隐约流转。",
                "That protection now takes a more radical form: surrender a measure of the body's defenses, and "
                    + "entrust more of its suffering to the soul. The armor is silver with crystalline surfaces "
                    + "touched by faint refractions of violet and cyan.");
        add("malum.gui.book.entry.page.text." + AUFHEBEN_ARMOR_ENTRY + ".3",
                "头盔之上的冠冕始终无法闭合 ，胸前的光则仿佛被封存在残 缺的圣龛中。那不是已经完成 的飞升，"
                    + "而是对飞升不肯止息 的渴望。",
                "The crown above its helm can never quite close; the light upon its breast seems sealed within a "
                    + "broken reliquary. This is not ascension fulfilled, but a longing for ascension that refuses to "
                    + "cease.");
        add("malum.gui.book.entry.page.text." + AUFHEBEN_ARMOR_ENTRY + ".4",
                "穿戴者没有因披上它而脱离堕 落。肉体依旧脆弱，灵魂的庇 护也终有耗尽之时。唯有当灵 魂趋近完整，"
                    + "刻印于器物之上 的魔咒才得以越过原有的界限 。",
                "The wearer is not freed from the Fall by donning it. Flesh remains frail, and even the soul's "
                    + "ward can be exhausted. Only as the soul approaches wholeness do the enchantments inscribed upon "
                    + "one's possessions reach beyond their former limits.");
        add("malum.gui.book.entry.page.text." + AUFHEBEN_ARMOR_ENTRY + ".5",
                "我将这套甲胄称为扬弃逐光： 所弃者是对肉体的依赖，所扬 者是灵魂对圆满的追求。因为 圆满尚未抵达，"
                    + "逐光便不能停 止。",
                "I call this armor AUFHEBEN_LIGHTWARD: the relinquishing of reliance upon flesh, and the "
                    + "elevation of the soul's longing for the Pleroma. So long as wholeness remains beyond reach, the "
                    + "pursuit of light must continue.");
    }

    private void addTotemicRunesTranslations() {
        add("malum.gui.book.entry." + TOTEMIC_RUNES_CONTINUED_ENTRY, "图腾符文：续", "Totemic Runes: Continued");
        add("malum.gui.book.entry." + TOTEMIC_RUNES_CONTINUED_ENTRY + ".description",
                "重复实验直至推翻",
                "Repeated Until Disproven");
        add("malum.gui.book.entry.page.headline." + TOTEMIC_RUNES_CONTINUED_ENTRY,
                "图腾符文：续",
                "Totemic Runes: Continued");
        add("malum.gui.book.entry.page.text." + TOTEMIC_RUNES_CONTINUED_ENTRY + ".1",
                "在适当调整符文仪式的画法后 ，我又成功刻印了其他灵气仪 式在符板上。",
                "After adjusting the pattern of the runic rite, I succeeded in inscribing other aura rites onto "
                    + "tablets.");
        add("malum.gui.book.entry.page.text." + TOTEMIC_RUNES_CONTINUED_ENTRY + ".2",
                "看来之前得出的只有基本元素 对应的仪式才能起效是不完 全正确的。原因是这些脉动更 加的单纯，"
                    + "因而效果与完整的 仪式有些区别。",
                "My earlier conclusion that only rites of the basic elements would work was not entirely correct. "
                    + "These pulses are simpler, so their effects differ somewhat from those of the full rites.");

        addItem(MaledictItems.RUNE_OF_SATIATION, "饱食符文", "Rune of Satiation");
        add("effect.maledict.blessing_of_life", "生灵之祝", "Blessing of Life");
        add("effect.maledict.blessing_of_life.description",
                "生灵的祝祷随饱足流入血脉：自然回复生命时额外回复一份。",
                "The prayers of the living flow through your veins with nourishment: natural health regeneration "
                    + "restores an additional share.");
        add("malum.gui.curio.effect.maledict.blessing_of_life",
                "自然回复生命时额外回复等量的一份。",
                "Natural health regeneration restores an additional equal amount.");
        add("malum.gui.book.entry." + RUNE_OF_SATIATION_ENTRY, "饱食符文", "Rune of Satiation");
        add("malum.gui.book.entry." + RUNE_OF_SATIATION_ENTRY + ".description",
                "饱足里的第二份",
                "A Second Share of Fullness");
        add("malum.gui.book.entry.page.headline." + RUNE_OF_SATIATION_ENTRY, "饱食符文", "Rune of Satiation");
        add("malum.gui.book.entry.page.text." + RUNE_OF_SATIATION_ENTRY + ".1",
                "饱食符文是治疗仪式的弱化延 伸。缺少了足够的场域，改由 精炼食物中的能量来恢复生命 。"
                    + "由于饱足而能恢复生命时， 会额外恢复一份。这一份与原 本食物能够提供的等量。",
                "The Rune of Satiation is a weaker extension of the Rite of Healing. Without a sufficient field, "
                    + "it instead refines the energy in food to restore health. Whenever nourishment restores health, "
                    + "the rune restores an additional amount equal to what the food itself would provide.");

        addItem(MaledictItems.RUNE_OF_DECAY, "衰朽符文", "Rune of Decay");
        add("effect.maledict.decay", "衰朽之息", "Breath of Decay");
        add("effect.maledict.decay.description",
                "佩戴者身边的敌对之物每两秒被磨去半颗心，永不致命。",
                "Hostile creatures near the wearer lose half a heart every two seconds. This damage is never fatal.");
        add("malum.gui.curio.effect.maledict.decay",
                "身边敌对之物每两秒被磨去半颗心，永不致命。",
                "Nearby hostile creatures lose half a heart every two seconds. This damage is never fatal.");
        add("malum.gui.book.entry." + RUNE_OF_DECAY_ENTRY, "衰朽符文", "Rune of Decay");
        add("malum.gui.book.entry." + RUNE_OF_DECAY_ENTRY + ".description", "磨去血肉", "Grinding Down the Flesh");
        add("malum.gui.book.entry.page.headline." + RUNE_OF_DECAY_ENTRY, "衰朽符文", "Rune of Decay");
        add("malum.gui.book.entry.page.text." + RUNE_OF_DECAY_ENTRY + ".1",
                "衰朽仪式的脉动刻进符板，也 变得迟钝了。佩戴者身边的敌 对之物会被缓缓磨去血肉，但 却无法因此而死。",
                "The pulse of the Rite of Decay grows sluggish when inscribed on a tablet. It slowly wears away "
                    + "the flesh of hostile creatures near the wearer, but cannot kill them.");

        addItem(MaledictItems.RUNE_OF_THE_PACK, "兽群符文", "Rune of the Pack");
        add("malum.gui.curio.effect.maledict.pack_boon",
                "十六格内属于自己的随从获得抗性提升、力量与迅捷各 I 级。",
                "Your companions within sixteen blocks gain Resistance I, Strength I and Speed I.");
        add("malum.gui.book.entry." + RUNE_OF_THE_PACK_ENTRY, "兽群符文", "Rune of the Pack");
        add("malum.gui.book.entry." + RUNE_OF_THE_PACK_ENTRY + ".description", "同行的獠牙", "The Fangs That Follow");
        add("malum.gui.book.entry.page.headline." + RUNE_OF_THE_PACK_ENTRY, "兽群符文", "Rune of the Pack");
        add("malum.gui.book.entry.page.text." + RUNE_OF_THE_PACK_ENTRY + ".1",
                "赋能的脉动从前刻在怪物身上 ， 如今拗回来只认自己的兽群。 十六格内的随从获得各一级抗 "
                    + "性提升、力量与迅捷。",
                "The pulse of empowerment was once inscribed upon monsters. Now I have bent it back to recognize "
                    + "only my own pack. Companions within sixteen blocks gain Resistance I, Strength I and Speed I.");

        addItem(MaledictItems.RUNE_OF_RIPENING, "熟成符文", "Rune of Ripening");
        add("effect.maledict.thinning", "汰余之令", "Edict of Thinning");
        add("effect.maledict.thinning.description",
                "佩戴者身边同种敌对生物超过八只时，每2秒对外围的敌人造成1.5的伤害。",
                "When more than eight hostile creatures of the same type surround the wearer, those on the "
                    + "outskirts take 1.5 damage every two seconds.");
        add("effect.maledict.ripening", "熟成之赐", "Boon of Ripening");
        add("effect.maledict.ripening.description",
                "佩戴者获得的每一份经验都多出四分之一。",
                "The wearer gains an extra quarter of all experience earned.");
        add("malum.gui.curio.effect.maledict.ripening", "获得的经验多出四分之一。", "Gain a quarter more experience.");
        add("malum.gui.book.entry." + RUNE_OF_RIPENING_ENTRY, "熟成符文", "Rune of Ripening");
        add("malum.gui.book.entry." + RUNE_OF_RIPENING_ENTRY + ".description", "点滴积累", "Little by Little");
        add("malum.gui.book.entry.page.headline." + RUNE_OF_RIPENING_ENTRY, "熟成符文", "Rune of Ripening");
        add("malum.gui.book.entry.page.text." + RUNE_OF_RIPENING_ENTRY + ".1",
                "把滋养仪式的力量拗转而导向 自身，使滋养生灵肉体的力量 改为强健我的精神。熟成符文 "
                    + "可以相对稳定的增加四分之一 经验获取。不多，但是够用。",
                "I have turned the power of the Rite of Nourishment inward, strengthening my mind with what once "
                    + "nourished living flesh. The Rune of Ripening provides a fairly steady increase of one quarter "
                    + "to the experience I gain. Not much, but enough.");

        addItem(MaledictItems.MALIGNANT_PEWTER_TABLET, "恶念白镴符板", "Malignant Pewter Tablet");
        add("malum.gui.book.entry." + VOID_RUNEWORKING_ENTRY, "虚空符文工艺：拾遗", "Voidish Runecraft: Addenda");
        add("malum.gui.book.entry." + VOID_RUNEWORKING_ENTRY + ".description", "拗转白镴", "Bending Pewter to My Will");
        add("malum.gui.book.entry.page.headline." + VOID_RUNEWORKING_ENTRY,
                "虚空符文工艺：拾遗",
                "Voidish Runecraft: Addenda");
        add("malum.gui.book.entry.page.text." + VOID_RUNEWORKING_ENTRY + ".1",
                "作为与这本书原作者不同的魔 法施行者，将恶念白镴这一反 魔法金属拗转到能为魔法所用 "
                    + "是我一直以来的研究。",
                "My practice differs from that of this book's original author. I have long sought to bend "
                    + "Malignant Pewter, an anti-magic metal, to magical use.");
        add("malum.gui.book.entry.page.text." + VOID_RUNEWORKING_ENTRY + ".2",
                "基于虚空符文的工艺，我引入 了一些变量来突出白镴的一面 而拒绝其另一面。用白镴施行 "
                    + "魔法既不实用又不安全，但这 就是我想要的。",
                "Building on Voidish Runecraft, I introduced variables to emphasize one aspect of the pewter "
                    + "while suppressing the other. Working magic through pewter is neither practical nor safe, but it "
                    + "is what I want.");

        addItem(MaledictItems.RUNE_OF_STAGNANT_EVOLUTION, "演进凝滞符文", "Rune of Stagnant Evolution");
        add("malum.gui.book.entry." + RUNE_OF_STAGNANT_EVOLUTION_ENTRY, "演进凝滞符文", "Rune of Stagnant Evolution");
        add("malum.gui.book.entry." + RUNE_OF_STAGNANT_EVOLUTION_ENTRY + ".description",
                "阻止演进",
                "To Halt Evolution");
        add("malum.gui.book.entry.page.headline." + RUNE_OF_STAGNANT_EVOLUTION_ENTRY,
                "演进凝滞符文",
                "Rune of Stagnant Evolution");
        add("malum.gui.book.entry.page.text." + RUNE_OF_STAGNANT_EVOLUTION_ENTRY + ".1",
                "阻止演进，是白镴的核心要 素。与意识相配后，具有一定 的倾向性，能够阻止由外而来 的演进。",
                "Halting evolution is pewter's defining property. When paired with consciousness, it gains a "
                    + "measure of direction, allowing it to halt evolution imposed from outside.");

        addItem(MaledictItems.RUNE_OF_ROTTEN_BONE, "朽骨符文", "Rune of Rotten Bone");
        add("malum.gui.curio.effect.maledict.rotten_bone",
                "若生命上限大于1点，则死亡时以25%最大生命值为代价逃离死亡",
                "If your maximum health is greater than 1, escape death at the cost of 25% of your maximum health.");
        add("malum.gui.book.entry." + RUNE_OF_ROTTEN_BONE_ENTRY, "朽骨符文", "Rune of Rotten Bone");
        add("malum.gui.book.entry." + RUNE_OF_ROTTEN_BONE_ENTRY + ".description", "拥抱枯骨", "To Embrace Mortal Bone");
        add("malum.gui.book.entry.page.headline." + RUNE_OF_ROTTEN_BONE_ENTRY, "朽骨符文", "Rune of Rotten Bone");
        add("malum.gui.book.entry.page.text." + RUNE_OF_ROTTEN_BONE_ENTRY + ".1",
                "终有一天，\n她将离开他们而独自离去， \n她会厌倦天国天使们的歌声，\n而更愿意去拥抱凡人的枯骨。",
                "One day\nshe will leave them and depart alone,\nweary of the songs of heaven's angels,\n"
                    + "preferring instead to embrace the bones of mortals.");

        add("malum.gui.book.entry." + UMBRAL_EXPERIMENT_ENTRY, "幽影精魂的实验", "The Umbral Spirit Experiment");
        add("malum.gui.book.entry." + UMBRAL_EXPERIMENT_ENTRY + ".description",
                "探求禁忌……有意义吗",
                "Seeking the Forbidden… Is There Any Point?");
        add("malum.gui.book.entry.page.headline." + UMBRAL_EXPERIMENT_ENTRY,
                "幽影精魂的实验",
                "The Umbral Spirit Experiment");
        add("malum.gui.book.entry.page.text." + UMBRAL_EXPERIMENT_ENTRY + ".1",
                "在获取了大量幽影精魂后，我 用已经证明过有效的符板进行 自由的创作。在一些弯路过后 ，"
                    + "我获得了以下的符板。",
                "Having gathered a great quantity of umbral spirits, I began experimenting freely with tablets "
                    + "already proven to work. After a few wrong turns, I produced the following tablets.");

        addItem(MaledictItems.RUNE_OF_MELANCHOLIA, "抑郁符文", "Rune of Melancholia");
        add("malum.gui.curio.effect.maledict.melancholia",
                "会逐渐获得同类伤害的抗性。治疗和伤害对你来说都很麻木。",
                "You gradually become resistant to repeated damage of the same kind. Healing and harm alike leave "
                    + "you numb.");
        add("malum.gui.book.entry." + RUNE_OF_MELANCHOLIA_ENTRY, "抑郁符文", "Rune of Melancholia");
        add("malum.gui.book.entry." + RUNE_OF_MELANCHOLIA_ENTRY + ".description",
                "谵妄的幻想，终归",
                "Delirium's Fancy, After All");
        add("malum.gui.book.entry.page.headline." + RUNE_OF_MELANCHOLIA_ENTRY, "抑郁符文", "Rune of Melancholia");
        add("malum.gui.book.entry.page.text." + RUNE_OF_MELANCHOLIA_ENTRY + ".1",
                "我好忧愁。  我好忧愁。  我好忧愁。  我好忧愁。",
                "I am so sorrowful. I am so sorrowful. I am so sorrowful. I am so sorrowful.");

        addItem(MaledictItems.RUNE_OF_BLISS, "无忧符文", "Rune of Bliss");
        add("malum.gui.curio.effect.maledict.bliss.damage",
                "增加对血量比例不同的造成、受到的伤害。",
                "Increases the damage dealt to and taken from those whose proportion of health differs from yours.");
        add("malum.gui.curio.effect.maledict.bliss.forced_hit",
                "强制使双方的攻击可以命中。",
                "Forces both sides' attacks to be able to land.");
        add("malum.gui.book.entry." + RUNE_OF_BLISS_ENTRY, "无忧符文", "Rune of Bliss");
        add("malum.gui.book.entry." + RUNE_OF_BLISS_ENTRY + ".description",
                "无爱亦无忧愁",
                "Untouched by Love or Sorrow");
        add("malum.gui.book.entry.page.headline." + RUNE_OF_BLISS_ENTRY, "无忧符文", "Rune of Bliss");
        add("malum.gui.book.entry.page.text." + RUNE_OF_BLISS_ENTRY + ".1",
                "可是，有什么办法呢？\n 谁在爱，\n 谁就应该与他所爱的人 分担命运。\n  永恒的坠落，\n无爱亦无忧愁，"
                    + "\n 即是最终的虚无。",
                "But what is to be done?\nWhoever loves\nmust share the fate of the one he loves.\nThe eternal "
                    + "fall,\nwithout love and so without sorrow,\nis the final nothingness.");

        addItem(MaledictItems.RUNE_OF_THE_FALLEN, "堕落符文", "Rune of the Fallen");
        add("effect.maledict.fallen", "堕落", "Fallen");
        add("effect.maledict.fallen.description", "生命回复会堕落成伤口", "Healing decays into wounds.");
        add("malum.gui.curio.effect.maledict.fallen",
                "使对方的生命回复堕落成伤口",
                "Makes the target's healing decay into wounds.");
        add("malum.gui.book.entry." + RUNE_OF_THE_FALLEN_ENTRY, "堕落符文", "Rune of the Fallen");
        add("malum.gui.book.entry." + RUNE_OF_THE_FALLEN_ENTRY + ".description",
                "选择，或是未被选择",
                "Chosen, or Unchosen");
        add("malum.gui.book.entry.page.headline." + RUNE_OF_THE_FALLEN_ENTRY, "堕落符文", "Rune of the Fallen");
        add("malum.gui.book.entry.page.text." + RUNE_OF_THE_FALLEN_ENTRY + ".1",
                "由金中的金，究极的神圣堕落 为原始而沉重的铅。所谓的恶 意即是这种痛苦的具现化。我 精制了这份恶念，"
                    + "制作成了这 一份符文。它会将这份恶念施 加在对方身上，使每一次回复 生命堕化为一次苦痛。",
                "The gold of golds, the ultimate holy thing, falls into base and heavy lead. So-called malice is "
                    + "the embodiment of that pain. I refined that malice and made this rune of it. It lays the malice "
                    + "upon the other, so that every recovery of life decays into a pang of pain.");
    }

    private void addObelisksTranslations() {
        addBlock(MaledictBlocks.MNEMONIC_OBELISK, "回忆方尖碑", "Mnemonic Obelisk");
        addBlock(MaledictBlocks.SOULWOOD_OBELISK, "灵魂木方尖碑", "Soulwood Obelisk");

        add("malum.gui.book.entry." + OBELISKS_ENTRY, "异质方尖碑", "Esoteric Obelisks");
        add("malum.gui.book.entry." + OBELISKS_ENTRY + ".description", "排斥与回忆", "Rejection and remembrance");
        add("malum.gui.book.entry.page.headline." + SOULWOOD_OBELISK_PAGE, "灵魂木方尖碑", "Soulwood Obelisk");
        add("malum.gui.book.entry.page.text." + SOULWOOD_OBELISK_PAGE + ".1",
                "巧妙的利用恶念金属对魔法的 排斥可使神圣金的力量更加集 中，从而得到二分之一的加速 效果。",
                "By exploiting malignant metal's rejection of magic, I can focus the power of Hallowed Gold more "
                    + "intensely, yielding an acceleration of one half.");
        add("malum.gui.book.entry.page.headline." + MNEMONIC_OBELISK_PAGE, "回忆方尖碑", "Mnemonic Obelisk");
        add("malum.gui.book.entry.page.text." + MNEMONIC_OBELISK_PAGE + ".1",
                "经验晶已经证明了它的成功。 加入更多忆念残片中凝结的力 量则可以使附魔的力量更加辉 煌，"
                    + "达到惊人的十个书架的效 果。",
                "Brilliance has already proven its worth. Adding more of the power condensed in Mnemonic "
                    + "Fragments makes their enchanting power shine brighter still, reaching the astonishing strength "
                    + "of ten bookshelves.");
    }

    private void addInvocationTranslations() {
        addItem(MaledictItems.INVOCATION_SIGIL, "召唤印记", "Invocation Sigil");

        add("tooltip.maledict.invocation.target", "召唤对象：%s", "Summons: %s");
        add("tooltip.maledict.invocation.variant", "变种：%s", "Variant: %s");
        add("invocation.maledict.variant.blue", "蓝色", "Blue");
        add("invocation.maledict.variant.brown", "棕色", "Brown");
        add("tooltip.maledict.invocation.runewood", "需要符文木图腾", "Requires a Runewood totem");
        add("tooltip.maledict.invocation.soulwood",
                "需要灵魂木图腾，将召唤敌对生物",
                "Requires a Soulwood totem; summons a hostile mob");
        add("tooltip.maledict.invocation.use",
                "主手手持印记并进行对应的图腾仪式",
                "Hold in your main hand and perform the matching rite");
        add("tooltip.maledict.invocation.consume",
                "仪式开始时消耗；失败时掉落返还",
                "Consumed when the rite begins; dropped back on failure");

        add("message.maledict.invocation.invalid",
                "召唤印记无效或目标不受支持。",
                "This sigil is invalid or its target is unsupported.");
        add("message.maledict.invocation.runewood", "此印记需要符文木图腾。", "This sigil requires a Runewood totem.");
        add("message.maledict.invocation.soulwood", "此印记需要灵魂木图腾。", "This sigil requires a Soulwood totem.");
        add("message.maledict.invocation.busy",
                "图腾正在召唤或仍有印记等待返还。",
                "This totem is busy or has a sigil awaiting return.");
        add("message.maledict.invocation.structure",
                "图腾精魂排列、朝向或材质不匹配。",
                "The totem's spirits, facing or wood do not match.");
        add("message.maledict.invocation.peaceful",
                "和平难度下无法召唤怪物。",
                "Hostile mobs cannot be summoned on Peaceful.");
        add("message.maledict.invocation.space",
                "图腾前方缺少合适的召唤场地。",
                "There is no suitable summoning space in front of the totem.");

        add("malum.gui.rite.maledict_invocation_rite", "生命召回", "Recall of Life");
        add("malum.gui.rite.corrupted_maledict_invocation_rite", "禁忌召唤", "Forbidden Invocation");
        add("malum.gui.book.entry.page.text.maledict_invocation_rite.hover",
                "消耗召唤印记，凝聚中立或友善生物。",
                "Consume a sigil to manifest a neutral or friendly creature.");
        add("malum.gui.book.entry.page.text.corrupt_maledict_invocation_rite.hover",
                "消耗召唤印记，凝聚敌对生物。",
                "Consume a sigil to manifest a hostile creature.");

        add("malum.gui.book.entry.maledict.invocation_sigil", "召唤印记", "Invocation Sigils");
        add("malum.gui.book.entry.maledict.invocation_sigil.description",
                "形态封于印记，生命凝于精魂",
                "A form remembered, a life recalled");
        add("malum.gui.book.entry.page.headline.maledict.invocation_sigil", "召唤印记", "Invocation Sigils");
        add("malum.gui.book.entry.page.text.maledict.invocation_sigil.1",
                "精魂灌注能够将生物的形态封 入召唤印记。主手手持印记并 进行对应的图腾仪式，条件满 "
                    + "足后立即消耗一枚，精魂随之 凝聚为活物。",
                "Spirit infusion can seal a creature's form within an invocation sigil. Hold the sigil in your "
                    + "main hand and perform the matching totemic rite. Once its conditions are met, one sigil is "
                    + "consumed immediately, and the spirits gather into a living creature.");
        add("malum.gui.book.entry.page.text.maledict.invocation_sigil.2",
                "自下而上排列奥术、大地、神 圣、澄空精魂。中立与友善生 物使用符文木，怪物使用灵魂 木。"
                    + "图腾前方需留出空间；水 生生物需要水，附着生物需要 坚实支撑与足够的活动空间。",
                "From bottom to top, arrange Arcane, Earthen, Sacred and Aerial spirits. Use Runewood for neutral "
                    + "and friendly creatures, Soulwood for hostile mobs. Leave room in front of the totem. "
                    + "Aquatic creatures need water; creatures that attach to surfaces need solid support and enough "
                    + "room to move.");
        add("malum.gui.book.entry.page.text.maledict.invocation_sigil.3",
                "仪式开始后，无需继续手持印 记。若仪式中断或召唤失败， 已消耗的印记会在原图腾附近 作为掉落物返还。"
                    + "图腾可反复 使用，每次仪式消耗一枚印记 。",
                "Once the rite has begun, you may put the sigil away. If the rite is interrupted or the summoning "
                    + "fails, the consumed sigil drops near the original totem. The totem can be reused; each rite "
                    + "consumes one sigil.");
    }

    private void addMaidBondTranslations() {
        addItem(MaledictItems.YEARNING_SWORD, "眷恋", "Yearning");
        addItem(MaledictItems.FORSAKEN_VOW, "弃约", "Forsaken Vow");
        add("message.maledict.maid_bond.death_1",
                "「主人……是我做错了什么吗？」",
                "\"Master... did I do something wrong?\"");
        add("message.maledict.maid_bond.death_2",
                "「请不要这样……我会听话的。」",
                "\"Please don't... I will be obedient.\"");
        add("message.maledict.maid_bond.death_3",
                "「……即使如此，我也依然爱着您。」",
                "\"...Even so, I still love you.\"");
        add("tooltip.maledict.maid_bond.original",
                "羁绊证明：%s 与 %s",
                "Proof of the bond between %s and %s");
        add("tooltip.maledict.maid_bond.forsaken",
                "%s的魂魄仍在此痛苦的哀嚎",
                "%s's soul still wails here in agony");
        add("tooltip.maledict.maid_bond.unknown_owner", "未知主人", "Unknown owner");
        add("tooltip.maledict.maid_bond.unknown_maid", "未知女仆", "Unknown maid");
    }

    private void addSoulbindingTranslations() {
        addItem(MaledictItems.SOULBINDING_SCROLL, "拘魂卷轴", "Soulbinding Scroll");
        addItem(MaledictItems.SOULBINDING_SEAL, "拘魂封印", "Soulbinding Seal");
        add("tooltip.maledict.soulbinding.armed", "[待拘魂]", "[Awaiting Binding]");
        add("tooltip.maledict.soulbinding.vacant", "精魂封印：空缺", "Soul Seal: Vacant");
        add("tooltip.maledict.soulbinding.next_kill",
                "下一次有效击杀将决定封印对象",
                "The next valid kill will decide the bound spirit");
        add("tooltip.maledict.soulbinding.bound", "[已封印]", "[Bound]");
        add("tooltip.maledict.soulbinding.spirit", "精魂封印：%s", "Soul Seal: %s");
        add("tooltip.maledict.soulbinding.unknown", "未知精魂", "Unknown Spirit");

        add("message.maledict.soulbinding.already_known",
                "你已经懂得了拘魂仪式。",
                "You already know the Rite of Soulbinding.");
        add("message.maledict.soulbinding.learned", "你已习得拘魂仪式。", "You have learned the Rite of Soulbinding.");
        add("message.maledict.soulbinding.no_advancement",
                "你尚未习得拘魂仪式。",
                "You have not learned the Rite of Soulbinding.");
        add("message.maledict.soulbinding.invalid_tool",
                "主手必须持有一件可封印的工具。",
                "Your main hand must hold a bindable tool.");
        add("message.maledict.soulbinding.already_armed",
                "这件工具已经可以封印精魂或已封印精魂。",
                "This tool is already awaiting a spirit or already holds one.");
        add("message.maledict.soulbinding.no_seal",
                "副手需要一枚拘魂封印。",
                "A Soulbinding Seal is required in your offhand.");
        add("message.maledict.soulbinding.not_armed",
                "主手的工具没有可解除的拘魂。",
                "The main-hand tool has no binding to release.");
        add("message.maledict.soulbinding.cannot_use",
                "此处无法启动拘魂仪式。",
                "The Rite of Soulbinding cannot be started here.");

        add("malum.gui.rite.maledict_soulbinding_rite", "拘魂仪式", "Rite of Soulbinding");
        add("malum.gui.rite.corrupted_maledict_soulbinding_rite", "拘魂仪式", "Rite of Soulbinding");
        add("malum.gui.book.entry.page.text.maledict_soulbinding_rite.hover",
                "副手持一枚拘魂封印，主手工具持有已经封印的工具，解除工具的精魂封印。",
                "Releases a tool's bound spirit on Runewood.");
        add("malum.gui.book.entry.page.text.corrupt_maledict_soulbinding_rite.hover",
                "副手持一枚拘魂封印，主手工具持有合适的工具，使工具在下一次击杀时能够封印生物的精魂。",
                "On Soulwood, spends a Soulbinding Seal to ready the main-hand tool for its next kill.");

        add("malum.gui.book.entry." + SOULBINDING_RITE_ENTRY, "拘魂仪式", "The Rite of Soulbinding");
        add("malum.gui.book.entry." + SOULBINDING_RITE_ENTRY + ".description",
                "一缕精魂，封于器物",
                "One spirit, sealed in a vessel");
        add("malum.gui.book.entry.page.headline." + SOULBINDING_RITE_ENTRY, "拘魂之术", "The Binding of Souls");
        add("malum.gui.book.entry.page.text." + SOULBINDING_RITE_ENTRY + ".1",
                "在研究遗迹中残留的卷轴后， 我发现它记载了一份腐枯界居 民特有的技艺。这种技艺与沉 "
                    + "辉铋本质是同样的恶行。封印 来自他者的光，并把这份光用 作己用。",
                "The scroll I found among the ruins records a craft known only to the people of the Blighted Realm. "
                    + "It is the same transgression as Lumenfall Bismuth: sealing away another's light to make it "
                    + "my own.");
        add("malum.gui.book.entry.page.text." + SOULBINDING_ATTACH_PAGE,
                "施行这种腐枯秘仪极其简单： 副手拿着精魂封印，主手拿着 "
                    + "工具，就这么一拍。工具就做 好了吸收灵魂的准备。",
                "The blighted rite is simple. Hold a Soulbinding Seal in your off hand and a tool in your main hand, "
                    + "then tap the totem. The tool is now ready to take in a soul.");
        add("malum.gui.book.entry.page.text." + SOULBINDING_RELEASE_PAGE,
                "我改良了一下，同样的仪式， 也能用来释放灵魂。",
                "With a small change, I found the same rite could also release the soul.");
    }

    private void addLightfallCodexTranslations() {
        addItem(MaledictItems.LIGHTFALL_CODEX, "坠光秘典", "Lightfall Codex");
        add("malum.gui.book.entry.maledict.lightfall.descent", "降临别传", "The Descent");
        add("malum.gui.book.entry.maledict.lightfall.descent.description", "光不在的故事", "The Tale Without Light");
    }

    private void addLumenfallMaterialsTranslations() {
        addBlock(MaledictBlocks.LUMENFALL_BISMUTH_ORE, "沉辉铋矿", "Lumenfall Bismuth Ore");
        addBlock(MaledictBlocks.LUMENFALL_BISMUTH_BLOCK, "沉辉铋块", "Block of Lumenfall Bismuth");
        addItem(MaledictItems.LUMENFALL_BISMUTH_INGOT, "沉辉铋锭", "Lumenfall Bismuth Ingot");
        add("malum.gui.book.entry." + LUMENFALL_BISMUTH_ENTRY, "沉辉铋", "Lumenfall Bismuth");
        add("malum.gui.book.entry." + LUMENFALL_BISMUTH_ENTRY + ".description",
                "光曾抵达的证明",
                "Proof that light once reached here");
        add("malum.gui.book.entry.page.headline." + LUMENFALL_BISMUTH_ENTRY, "沉辉铋", "Lumenfall Bismuth");
        add("malum.gui.book.entry.page.text." + LUMENFALL_BISMUTH_ENTRY + ".1",
                "腐枯界早已无力孕生属于自己 的光。然而，从主世界坠入此 地的事物，仍携带着微不足道 的光辉。"
                    + "它们无法使这片行将 枯竭的土地重获光明。",
                "The withering realm has long since lost the power to bring forth light of its own. Yet the "
                    + "things that fall into it from the Overworld still carry the faintest traces of radiance. They "
                    + "cannot restore the dying land.");
        add("malum.gui.book.entry.page.text." + LUMENFALL_BISMUTH_ENTRY + ".2",
                "那些光从坠落之物中缓缓逸散 ，沉入岩层，历经漫长的积聚 ，终于凝结为灰白色的晶体。 晶体的外表黯淡，"
                    + "近乎枯骨与 冷却的灰烬。唯有经过冶炼， 其内部的虹彩才逐渐显露。",
                "Instead, that light slowly escapes its fallen vessels, sinks into the strata below, and, over "
                    + "countless years, condenses into pale, ash-white crystals. Their surfaces are dull, resembling "
                    + "old bone and cold ash. Only under the heat of the crucible do their hidden colors emerge.");
        add("malum.gui.book.entry.page.text." + LUMENFALL_BISMUTH_ENTRY + ".3",
                "紫、青与金色交错于晶面，仿 佛一些不属于此界的光，仍拒 绝彻底消失。它们无法拯救腐 枯界，"
                    + "却证明光曾经抵达这里 。我将这种晶体称为沉辉铋。",
                "Violet, cyan, and gold pass across crystalline facets, as though some remnant of a foreign light "
                    + "still refuses to vanish. These crystals cannot save the realm. They are proof, however, that "
                    + "light once reached it. I call them Lumenfall Bismuth.");

        addItem(MaledictItems.REFRACTED_LEAD_BISMUTH_EUTECTIC, "折辉铅铋共晶", "Refracted Lead–Bismuth Eutectic");
        add("malum.gui.book.entry." + EUTECTIC_ENTRY, "折辉铅铋共晶", "Refracted Lead–Bismuth Eutectic");
        add("malum.gui.book.entry." + EUTECTIC_ENTRY + ".description",
                "否定而不归于虚无",
                "Negated, yet not reduced to nothing");
        add("malum.gui.book.entry.page.headline." + EUTECTIC_ENTRY, "折辉铅铋共晶", "Refracted Lead–Bismuth Eutectic");
        add("malum.gui.book.entry.page.text." + EUTECTIC_ENTRY + ".1",
                "沉辉铋保存着异乡坠落之物所 遗留的微光，恶念之铅则承载 着截然不同的沉重本质。我以 "
                    + "灵气余焰促使二者熔合，借紫 水晶碎片引导其结晶，并以邪 恶与神圣的精魂完成灌注。",
                "Lumenfall Bismuth preserves the faint light left behind by things fallen from another world. "
                    + "Malignant Lead bears a different, far heavier nature. I bring the two together with Auric "
                    + "Embers, guide their crystallization with Amethyst Shards, and complete the infusion with Wicked "
                    + "and Sacred Spirits.");
        add("malum.gui.book.entry.page.text." + EUTECTIC_ENTRY + ".2",
                "最终所得并非寻常金属，而是 一种深银色的共晶：其晶面在 暗处近乎铅黑，转动时却显现 青紫交错的折辉。"
                    + "光未曾驱逐 恶念，恶念亦未曾吞没光。二 者原本的形态遭到否定，却没 有被彻底抹去。",
                "What emerges is no ordinary metal, but a dark-silver eutectic crystal: nearly lead-black in "
                    + "shadow, yet alive with refracted violet and cyan when turned toward the light. The light has "
                    + "not driven out the malice, nor has the malice swallowed the light. "
                    + "Their former states are negated, but neither is simply erased.");
        add("malum.gui.book.entry.page.text." + EUTECTIC_ENTRY + ".3",
                "它们在新的晶体中得以保存， 也因这种结合而发生改变。我 借用「扬弃」来理解这一过程 ："
                    + "否定而不归于虚无，保存而 不拘泥于原状。然而，这种晶 体既不宣称救赎，也不许诺圆 满。"
                    + "它只是将矛盾留在自身的 结构之中。",
                "Both are preserved within a new crystalline structure, and both are changed by their union. I "
                    + "understand this transformation through Aufheben: to negate without reducing to nothing, to "
                    + "preserve without leaving unchanged. Yet the crystal promises neither salvation nor perfection. "
                    + "It merely holds the contradiction within itself.");
    }

    private void add(String key, String zhCn, String enUs) {
        add(key, chinese ? zhCn : enUs);
    }

    private void addItem(Supplier<? extends Item> item, String zhCn, String enUs) {
        addItem(item, chinese ? zhCn : enUs);
    }

    private void addBlock(Supplier<? extends Block> block, String zhCn, String enUs) {
        addBlock(block, chinese ? zhCn : enUs);
    }
}
