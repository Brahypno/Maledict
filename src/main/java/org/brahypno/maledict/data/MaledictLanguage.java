package org.brahypno.maledict.data;

import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictBlocks;
import org.brahypno.maledict.registry.MaledictItems;

public final class MaledictLanguage extends LanguageProvider {
    private final String locale;

    public MaledictLanguage(PackOutput output, String locale) {
        super(output, Maledict.MODID, locale);
        this.locale = locale;
    }

    @Override
    protected void addTranslations() {
        if ("zh_cn".equals(locale)){
            add("itemGroup.maledict", "咒邪");
            add("enchantment.maledict.ectoplasm", "灵质");
            add("enchantment.maledict.reminiscence", "追忆");
            add("enchantment.maledict.aftertaste", "回味");
            add("enchantment.maledict.ectoplasm.desc",
                "每级使射出的箭矢速度提高 8%，每发消耗背包中（等级 + 1）÷ 2 枚精魂；精魂不足时不生效。");
            add("enchantment.maledict.reminiscence.desc",
                "射出的箭矢可穿过一次方块，能够穿透的厚度为 2 + 等级 格。");
            add("enchantment.maledict.aftertaste.desc",
                "镰刀命中时，按这一击占目标最大生命的比例，尝到其掉落物本可提供的饥饿与饱食度；"
                + "1 至 3 级分别回复 50%、60%、70%，掉落物附带的食物效果也会一并生效。");
            addItem(MaledictItems.INCURSUS_BLADE, "神侵恶刃");
            addItem(MaledictItems.REMEMBRANCE_BOW, "思念弓");
            addItem(MaledictItems.ELEGY_BOW, "哀歌弓");
            add("malum.gui.book.entry.maledict.remembrance_bow", "思念弓");
            add("malum.gui.book.entry.maledict.remembrance_bow.description", "穿越阻隔的思念");
            add("malum.gui.book.entry.page.headline.maledict.remembrance_bow", "思念弓");
            add("malum.gui.book.entry.page.text.maledict.remembrance_bow.1",
                "若能汇集末影人的魔法并以星 灵织物承载，就能使灵魂木产 生奇妙的性质。拉动这种材质 的弓只需要一般弓一半的时间 ，射出的箭矢不仅更快，还可 以有限度的穿过方块。");
            add("malum.gui.book.entry." + ELEGY_BOW_ENTRY, "哀歌弓");
            add("malum.gui.book.entry." + ELEGY_BOW_ENTRY + ".description", "停不下的思念");
            add("malum.gui.book.entry.page.headline." + ELEGY_BOW_ENTRY, "哀歌弓");
            add("malum.gui.book.entry.page.text." + ELEGY_BOW_ENTRY + ".1",
                "我走不出来。 我走不出来。 我走不出来。 我走不出来。 我走不出来。我走不出来。 我走不出来。 我走不出来。 我走不出来。 我走不出来。 我走不出来。 我将我不止息的哀歌灌注到弓 中，使之具有了满蓄力自动发 射的能力。");
            add("malum.gui.book.entry." + TOTEMIC_RUNES_CONTINUED_ENTRY, "图腾符文：续");
            add("malum.gui.book.entry." + TOTEMIC_RUNES_CONTINUED_ENTRY + ".description", "重复实验直至推翻");
            add("malum.gui.book.entry.page.headline." + TOTEMIC_RUNES_CONTINUED_ENTRY, "图腾符文：续");
            add("malum.gui.book.entry.page.text." + TOTEMIC_RUNES_CONTINUED_ENTRY + ".1",
                "在适当调整符文仪式的画法后 ，我又成功刻印了其他灵气仪 式在符板上。看来之前得出的 只有基本元素对应的仪式才能 起效是不完全正确的。原因是 这些脉动更加的单纯，因而效 果与完整的仪式有些区别。");
            add("malum.gui.book.entry." + RUNE_OF_SATIATION_ENTRY, "饱食符文");
            add("malum.gui.book.entry." + RUNE_OF_SATIATION_ENTRY + ".description", "饱足里的第二份");
            add("malum.gui.book.entry.page.headline." + RUNE_OF_SATIATION_ENTRY, "饱食符文");
            add("malum.gui.book.entry.page.text." + RUNE_OF_SATIATION_ENTRY + ".1",
                "饱食符文是治疗仪式的弱化延 伸。缺少了足够的场域，改由 精炼食物中的能量来恢复生命 。由于饱足而能恢复生命时， 会额外恢复一份。这一份与原 本食物能够提供的等量。");
            add("malum.gui.book.entry." + RUNE_OF_DECAY_ENTRY, "衰朽符文");
            add("malum.gui.book.entry." + RUNE_OF_DECAY_ENTRY + ".description", "磨去血肉");
            add("malum.gui.book.entry.page.headline." + RUNE_OF_DECAY_ENTRY, "衰朽符文");
            add("malum.gui.book.entry.page.text." + RUNE_OF_DECAY_ENTRY + ".1",
                "衰朽仪式的脉动刻进符板，也 变得迟钝了。佩戴者身边的敌 对之物会被缓缓磨去血肉，但 却无法因此而死。");
            add("malum.gui.book.entry." + RUNE_OF_THE_PACK_ENTRY, "兽群符文");
            add("malum.gui.book.entry." + RUNE_OF_THE_PACK_ENTRY + ".description", "同行的獠牙");
            add("malum.gui.book.entry.page.headline." + RUNE_OF_THE_PACK_ENTRY, "兽群符文");
            add("malum.gui.book.entry.page.text." + RUNE_OF_THE_PACK_ENTRY + ".1",
                "赋能的脉动从前刻在怪物身上 ， 如今拗回来只认自己的兽群。 十六格内的随从获得各一级抗 性提升、力量与迅捷。");
            add("malum.gui.book.entry." + RUNE_OF_RIPENING_ENTRY, "熟成符文");
            add("malum.gui.book.entry." + RUNE_OF_RIPENING_ENTRY + ".description", "点滴积累");
            add("malum.gui.book.entry.page.headline." + RUNE_OF_RIPENING_ENTRY, "熟成符文");
            add("malum.gui.book.entry.page.text." + RUNE_OF_RIPENING_ENTRY + ".1",
                "把滋养仪式的力量拗转而导向 自身，使滋养生灵肉体的力量 改为强健我的精神。熟成符文 可以相对稳定的增加四分之一 经验获取。不多，但是够用。");
            add("malum.gui.book.entry." + VOID_RUNEWORKING_ENTRY, "虚空符文工艺：拾遗");
            add("malum.gui.book.entry." + VOID_RUNEWORKING_ENTRY + ".description", "拗转白镴");
            add("malum.gui.book.entry.page.headline." + VOID_RUNEWORKING_ENTRY, "虚空符文工艺：拾遗");
            add("malum.gui.book.entry.page.text." + VOID_RUNEWORKING_ENTRY + ".1",
                "作为与这本书原作者不同的魔 法施行者，将恶念白镴这一反 魔法金属拗转到能为魔法所用 是我一直以来的研究。基于虚 空符文的工艺，我引入了一些 变量来突出白镴的一面而拒绝 其另一面。用白镴施行魔法既 不实用又不安全，但这就是我 想要的。");
            add("malum.gui.book.entry." + RUNE_OF_STAGNANT_EVOLUTION_ENTRY, "演进凝滞符文");
            add("malum.gui.book.entry." + RUNE_OF_STAGNANT_EVOLUTION_ENTRY + ".description", "阻止演进");
            add("malum.gui.book.entry.page.headline." + RUNE_OF_STAGNANT_EVOLUTION_ENTRY, "演进凝滞符文");
            add("malum.gui.book.entry.page.text." + RUNE_OF_STAGNANT_EVOLUTION_ENTRY + ".1",
                "阻止演进，是白镴的核心要 素。与意识相配后，具有一定 的倾向性，能够阻止由外而来 的演进。");
            add("malum.gui.book.entry." + RUNE_OF_ROTTEN_BONE_ENTRY, "朽骨符文");
            add("malum.gui.book.entry." + RUNE_OF_ROTTEN_BONE_ENTRY + ".description", "拥抱枯骨");
            add("malum.gui.book.entry.page.headline." + RUNE_OF_ROTTEN_BONE_ENTRY, "朽骨符文");
            add("malum.gui.book.entry.page.text." + RUNE_OF_ROTTEN_BONE_ENTRY + ".1",
                "终有一天， 她将离开他们而独自离去， 她会厌倦天国天使们的歌声， 而更愿意去拥抱凡人的枯骨。");
            add("malum.gui.book.entry." + UMBRAL_EXPERIMENT_ENTRY, "幽影精魂的实验");
            add("malum.gui.book.entry." + UMBRAL_EXPERIMENT_ENTRY + ".description", "探求禁忌……有意义吗");
            add("malum.gui.book.entry.page.headline." + UMBRAL_EXPERIMENT_ENTRY, "幽影精魂的实验");
            add("malum.gui.book.entry.page.text." + UMBRAL_EXPERIMENT_ENTRY + ".1",
                "在获取了大量幽影精魂后，我 用已经证明过有效的符板进行 自由的创作。在一些弯路过后 ，我获得了以下的符板。");
            add("malum.gui.book.entry." + RUNE_OF_MELANCHOLIA_ENTRY, "抑郁符文");
            add("malum.gui.book.entry." + RUNE_OF_MELANCHOLIA_ENTRY + ".description", "谵妄的幻想，终归");
            add("malum.gui.book.entry.page.headline." + RUNE_OF_MELANCHOLIA_ENTRY, "抑郁符文");
            add("malum.gui.book.entry.page.text." + RUNE_OF_MELANCHOLIA_ENTRY + ".1",
                "我好忧愁。 我好忧愁。 我好忧愁。 我好忧愁。");
            addBlock(MaledictBlocks.MNEMONIC_OBELISK, "回忆方尖碑");
            addBlock(MaledictBlocks.SOULWOOD_OBELISK, "灵魂木方尖碑");
            addItem(MaledictItems.SACRED_SPIRIT_ARROW, "神圣精魂箭");
            addItem(MaledictItems.WICKED_SPIRIT_ARROW, "邪恶精魂箭");
            addItem(MaledictItems.ARCANE_SPIRIT_ARROW, "奥术精魂箭");
            addItem(MaledictItems.ELDRITCH_SPIRIT_ARROW, "邪术精魂箭");
            addItem(MaledictItems.AERIAL_SPIRIT_ARROW, "澄空精魂箭");
            addItem(MaledictItems.AQUEOUS_SPIRIT_ARROW, "碧水精魂箭");
            addItem(MaledictItems.EARTHEN_SPIRIT_ARROW, "大地精魂箭");
            addItem(MaledictItems.INFERNAL_SPIRIT_ARROW, "狱火精魂箭");
            add("attribute.name.maledict.powder_snow_damage", "冻结伤害");
            add("entity.maledict.first_vicissitude", "无常(WIP)");
            addItem(MaledictItems.AGE_OF_ENLIGHTENMENT, "启蒙之年");
            addItem(MaledictItems.CURIO_RETURN_TOKEN, "保管凭证");
            addItem(MaledictItems.MALIGNANT_PEWTER_TABLET, "恶念白镴符板");
            addItem(MaledictItems.FIRST_VICISSITUDE_PHASE_ONE_SPAWN_EGG, "无常刷怪蛋（一阶段）");
            addItem(MaledictItems.FIRST_VICISSITUDE_PHASE_TWO_SPAWN_EGG, "无常刷怪蛋（二阶段）");
            addItem(MaledictItems.RUNE_OF_SATIATION, "饱食符文");
            addItem(MaledictItems.RUNE_OF_DECAY, "衰朽符文");
            addItem(MaledictItems.RUNE_OF_THE_PACK, "兽群符文");
            addItem(MaledictItems.RUNE_OF_RIPENING, "熟成符文");
            addItem(MaledictItems.RUNE_OF_STAGNANT_EVOLUTION, "演进凝滞符文");
            addItem(MaledictItems.RUNE_OF_ROTTEN_BONE, "朽骨符文");
            addItem(MaledictItems.RUNE_OF_MELANCHOLIA, "抑郁符文");
            add("effect.maledict.age_of_enlightenment", "启蒙之年");
            add("effect.maledict.age_of_darkness", "黑暗时代");
            add("effect.maledict.age_of_enlightenment.description",
                "人们把蒙昧与黑暗错认为神圣，你的下一击必定造成暴击；每级额外给予 1 点法杖暂存弹数。");
            add("effect.maledict.age_of_darkness.description",
                "任由野蛮焚烧文明的黑暗时代啊，每级降低 20% 魔法抗性、灵魂护盾容量与灵魂护盾稳固度。");
            add("effect.maledict.blessing_of_life", "生灵之祝");
            add("effect.maledict.blessing_of_life.description",
                "生灵的祝祷随饱足流入血脉：自然回复生命时额外回复一份。");
            add("effect.maledict.decay", "衰朽之息");
            add("effect.maledict.decay.description",
                "佩戴者身边的敌对之物每两秒被磨去半颗心，永不致命。");
            add("effect.maledict.thinning", "汰余之令");
            add("effect.maledict.thinning.description",
                "佩戴者身边同种敌对生物超过八只时，每2秒对外围的敌人造成1.5的伤害。");
            add("effect.maledict.ripening", "熟成之赐");
            add("effect.maledict.ripening.description",
                "佩戴者获得的每一份经验都多出四分之一。");
            add("effect.maledict.sacrifice", "献祭");
            add("effect.maledict.sacrifice.description",
                "贤者献祭生命，因而获取智慧。然而并非每一次牺牲都会带来智慧，只有模仿先贤的仪式，才有可能获得先贤的智慧");
            add("curios.identifier.delusion", "眼");
            add("malum.gui.curio.effect.maledict.age_of_enlightenment.cooldown", "佩戴时冷却速度加倍");
            add("malum.gui.curio.effect.maledict.age_of_enlightenment.spirit_void", "攻击半生命值目标时触发收获精魂时的效果");
            add("malum.gui.curio.effect.maledict.age_of_enlightenment.enlightenment", "击杀敌人时获得启蒙之年，已有则延长时间");
            add("malum.gui.curio.effect.maledict.blessing_of_life", "自然回复生命时额外回复等量的一份。");
            add("malum.gui.curio.effect.maledict.decay", "身边敌对之物每两秒被磨去半颗心，永不致命。");
            add("malum.gui.curio.effect.maledict.ripening", "获得的经验多出四分之一。");
            add("malum.gui.curio.effect.maledict.pack_boon", "十六格内属于自己的随从获得抗性提升、力量与迅捷各 I 级。");
            add("malum.gui.curio.effect.maledict.rotten_bone",
                "若生命上限大于1点，则死亡时以25%最大生命值为代价逃离死亡");
            add("malum.gui.curio.effect.maledict.melancholia",
                "会逐渐获得同类伤害的抗性。治疗和伤害对你来说都很麻木。");
            add("tooltip.maledict.age_of_enlightenment.hold_shift", "按住 Shift 追问一个不该问的问题");
            add("tooltip.maledict.age_of_enlightenment.shift", "黑暗的时代曾经存在过吗");
            add("tooltip.maledict.age_of_enlightenment.shift.enlightened", "那无穷，无限，永动的启蒙之年啊");
            add("tooltip.maledict.curio_return_token", "右键领取被无常没收、尚未归还的饰品。全部交付后凭证才会消失。");
            add("message.maledict.first_vicissitude.curio_return_pending",
                "还有 %s 件饰品由无常保管：腾出背包或饰品栏空间后会自动归还");
            add("message.maledict.first_vicissitude.curio_return_complete", "被没收的饰品已全部归还");
            add("message.maledict.first_vicissitude.adaptation",
                "墨守成规，因循守旧，无常视之如粪土");
            add("message.maledict.first_vicissitude.attack", "命运总是会将人逼上悬崖，犹如恶客造访");
            add("message.maledict.first_vicissitude.phase_two",
                "所谓无常，就是圣人行走在正道上，依然会遇见的命运");
            add("sounds.maledict.music.vicissitude.phase_one", "无常：恶客造访");
            add("sounds.maledict.music.vicissitude.phase_two", "无常：不得已");
            add("sounds.maledict.item.incursus_blade.throw", "神侵恶刃：清脆的鸣叫 回旋而去");
            add("sounds.maledict.item.incursus_blade.recall", "神侵恶刃：撕风裂帛 金声玉振");
            add("sounds.maledict.item.incursus_blade.ascension", "神侵恶刃：振翅");
            add("sounds.maledict.item.incursus_blade.slash", "神侵恶刃：侵蚀");
            add("sounds.maledict.item.incursus_blade.crit", "神侵恶刃：狂喜咆哮");
            add("tooltip.maledict.incursus_blade.description", "吞噬精魂成长，向周身挥出锋刃的凶邪镰刀。");
            add("tooltip.maledict.incursus_blade.medium_unlock_hint", "精魂之力到达神圣之数会解锁更本征的力量");
            add("tooltip.maledict.incursus_blade.hold_shift", "按住 Shift 查看精魂详情");
            add("tooltip.maledict.incursus_blade.infusion", "在背包中拿精魂右键此物品以永久注入：");
            add("tooltip.maledict.incursus_blade.spirit.earthen", "大地 %s（%s/%s）：攻击力");
            add("tooltip.maledict.incursus_blade.spirit.aqueous", "碧水 %s（%s/%s）：冻结伤害");
            add("tooltip.maledict.incursus_blade.spirit.arcane", "奥术 %s（%s/%s）：魔法伤害");
            add("tooltip.maledict.incursus_blade.spirit.aerial", "澄空 %s%%（%s/%s）：净化随机负面并获得随机正面效果");
            add("tooltip.maledict.incursus_blade.spirit.sacred", "神圣 %s%%（%s/%s）：缩短敌方正面效果，延长或施加负面效果");
            add("tooltip.maledict.incursus_blade.spirit.infernal", "狱火 %s（%s/%s）：额外抢夺等级");
            add("tooltip.maledict.incursus_blade.spirit.eldritch", "邪术 %s%%（%s/%s）：伤害按比例转化为有上限的黄心，并立即恢复灵魂护盾");
            add("tooltip.maledict.incursus_blade.spirit.wicked", "邪恶 %s%%（%s/%s）：暴击伤害");
            add("malum.gui.book.entry.void.maledict.incursus_blade", "神侵恶刃");
            add("malum.gui.book.entry.void.maledict.incursus_blade.description", "永无止境的饥渴");
            add("malum.gui.book.entry.page.headline.void.maledict.incursus_blade", "神侵恶刃");
            add("malum.gui.book.entry.page.text.void.maledict.incursus_blade.1",
                "也许是坠井击碎了我对禁忌的 畏怖。恶念白镴对知识消退的 渴望铸造了挽魂锋镰。");
            add("malum.gui.book.entry.page.text.void.maledict.incursus_blade.2",
                "那么能否将这种排斥转化为一 种显性的魔法性质呢？之前的 研究提醒了我，如果能够构建 一个动态稳定的环境，在八种 精魂脉动的刺激下，也许第九 种奥术能量的脉动得以显现。");
            add("malum.gui.book.entry.page.text.void.maledict.incursus_blade.3",
                "最后我得到了这种武器，挽魂 锋镰本身的意志被虚空吞噬殆 尽，这片空无渴求着吞并一切 ，而八种精魂完全受其操控， 甚至脉动被扭曲，从而对外展 现出意料之外的能力。");
            add("malum.gui.book.entry.page.text.void.maledict.incursus_blade.4",
                "随着投入精魂数量的增加，这 柄武器也更加的难以捉摸。我 将之称为神侵恶刃，并希望这 片虚空能够满足于脉动。");
            add("malum.gui.book.entry." + OBELISKS_ENTRY, "异质方尖碑");
            add("malum.gui.book.entry." + OBELISKS_ENTRY + ".description", "排斥与回忆");
            add("malum.gui.book.entry.page.headline." + SOULWOOD_OBELISK_PAGE, "灵魂木方尖碑");
            add("malum.gui.book.entry.page.text." + SOULWOOD_OBELISK_PAGE + ".1",
                "巧妙的利用恶念金属对魔法的 排斥可使神圣金的力量更加集 中，从而得到二分之一的加速 效果。");
            add("malum.gui.book.entry.page.headline." + MNEMONIC_OBELISK_PAGE, "回忆方尖碑");
            add("malum.gui.book.entry.page.text." + MNEMONIC_OBELISK_PAGE + ".1",
                "经验晶已经证明了它的成功。 加入更多忆念残片中凝结的力 量则可以使附魔的力量更加辉 煌，达到惊人的十个书架的效 果。");
            add("malum.gui.rite." + VICISSITUDE_RITE_ID, "无常仪式");
            add("malum.gui.rite.corrupted_" + VICISSITUDE_RITE_ID, "无常仪式");
            add("malum.gui.rite." + GREATER_RITE_ID, "无常大仪式");
            add("malum.gui.rite.corrupted_" + GREATER_RITE_ID, "无常大仪式");
            add("malum.gui.rite." + ELDRITCH_RITE_ID, "邪术无常仪式(WIP!)");
            add("malum.gui.rite.corrupted_" + ELDRITCH_RITE_ID, "邪术无常仪式");
            add("malum.gui.rite." + GREATER_ELDRITCH_RITE_ID, "邪术无常大仪式");
            add("malum.gui.rite.corrupted_" + GREATER_ELDRITCH_RITE_ID, "邪术无常大仪式");
            add("malum.gui.book.entry." + RITE_ENTRY, "无常仪式");
            add("malum.gui.book.entry." + RITE_ENTRY + ".description", "唤来无法收回之物");
            add("malum.gui.book.entry.page.headline." + RITE_ENTRY, "无常仪式");
            add("malum.gui.book.entry.page.text." + RITE_ENTRY + ".1",
                "无常不是我能够创造的存在， 它只是从伤口侵入的影子。遵 从未知的法则，用三枚奥术精 魂 开启一扇微小的门扉，四枚推 得更开； 那顺门扉而来之物即是命运的 恶客。");
            add("malum.gui.book.entry.page.text." + RITE_ENTRY + ".2",
                "使用邪术精魂会扩大无常仪式 的影响力。更完整的影子照落 在此处，虽然影子很 难说完整。而两枚邪术精魂则 会召唤更加完整的一面。");
            add("malum.gui.rite.corrupted_" + SACRIFICE_RITE_ID, "牺牲仪式");
            add("malum.gui.book.entry.page.text.corrupt_" + SACRIFICE_RITE_ID + ".hover",
                "在撑过献祭后，获得一个额外 的符文槽位和一个眼槽位。");
            add("malum.gui.book.entry." + SACRIFICE_RITE_ENTRY, "牺牲仪式");
            add("malum.gui.book.entry." + SACRIFICE_RITE_ENTRY + ".description", "智慧的代价");
            add("malum.gui.book.entry.page.text." + SACRIFICE_RITE_PAGE,
                "智慧从何而来？于一场刻意的 仪式中，使用提尔锋伤害自己 ，妄想获得贤人的智慧，这能 够实现吗？");
            add("malum.gui.book.entry.page.text." + SACRIFICE_RITE_PAGE + ".2",
                "在九天九夜的折磨中，我确实 看到了些什么，也知道了些什 么。我的身体有额外的位置能 够刻画符文，而我的眼睛变成 了一处门扉。");
        }else {
            add("itemGroup.maledict", "Maledict");
            add("enchantment.maledict.ectoplasm", "Ectoplasm");
            add("enchantment.maledict.reminiscence", "Reminiscence");
            add("enchantment.maledict.aftertaste", "Aftertaste");
            add("enchantment.maledict.ectoplasm.desc",
                "Each level increases arrow speed by 8%. Each shot consumes (level + 1) / 2 spirits from your inventory; "
                + "the enchantment has no effect without enough spirits.");
            add("enchantment.maledict.reminiscence.desc",
                "Arrows can pass through blocks once, up to a thickness of 2 + level blocks.");
            add("enchantment.maledict.aftertaste.desc",
                "When a scythe strikes, you taste the hunger and saturation that the target's drops would restore, scaled "
                + "by the damage dealt as a fraction of its maximum health. Levels 1 to 3 restore 50%, 60% and 70% of that "
                + "amount, respectively. Any food effects from those drops also apply.");
            addItem(MaledictItems.INCURSUS_BLADE, "The Incursus Blade");
            addItem(MaledictItems.REMEMBRANCE_BOW, "Remembrance Bow");
            addItem(MaledictItems.ELEGY_BOW, "Elegy Bow");
            add("malum.gui.book.entry.maledict.remembrance_bow", "Remembrance Bow");
            add("malum.gui.book.entry.maledict.remembrance_bow.description", "Longing Beyond Barriers");
            add("malum.gui.book.entry.page.headline.maledict.remembrance_bow", "Remembrance Bow");
            add("malum.gui.book.entry.page.text.maledict.remembrance_bow.1",
                "Gathering the magic of endermen into astral weave can give soulwood wondrous properties. A bow made from "
                + "this wood takes half as long to draw as an ordinary bow. Its arrows fly faster and can pass through "
                + "blocks to a limited depth.");
            add("malum.gui.book.entry." + ELEGY_BOW_ENTRY, "Elegy Bow");
            add("malum.gui.book.entry." + ELEGY_BOW_ENTRY + ".description", "Remembrance Without Respite");
            add("malum.gui.book.entry.page.headline." + ELEGY_BOW_ENTRY, "Elegy Bow");
            add("malum.gui.book.entry.page.text." + ELEGY_BOW_ENTRY + ".1",
                "I cannot move on. I cannot move on. I cannot move on. I cannot move on. I cannot move on. I cannot move "
                + "on. I cannot move on. I cannot move on. I cannot move on. I cannot move on. I cannot move on. I poured my "
                + "unceasing elegy into the bow, allowing it to fire automatically at full draw.");
            add("malum.gui.book.entry." + TOTEMIC_RUNES_CONTINUED_ENTRY, "Totemic Runes: Continued");
            add("malum.gui.book.entry." + TOTEMIC_RUNES_CONTINUED_ENTRY + ".description", "Repeated Until Disproven");
            add("malum.gui.book.entry.page.headline." + TOTEMIC_RUNES_CONTINUED_ENTRY, "Totemic Runes: Continued");
            add("malum.gui.book.entry.page.text." + TOTEMIC_RUNES_CONTINUED_ENTRY + ".1",
                "After adjusting the pattern of the runic rite, I succeeded in inscribing other aura rites onto tablets. "
                + "My earlier conclusion that only rites of the basic elements would work was not entirely correct. These "
                + "pulses are simpler, so their effects differ somewhat from those of the full rites.");
            add("malum.gui.book.entry." + RUNE_OF_SATIATION_ENTRY, "Rune of Satiation");
            add("malum.gui.book.entry." + RUNE_OF_SATIATION_ENTRY + ".description", "A Second Share of Fullness");
            add("malum.gui.book.entry.page.headline." + RUNE_OF_SATIATION_ENTRY, "Rune of Satiation");
            add("malum.gui.book.entry.page.text." + RUNE_OF_SATIATION_ENTRY + ".1",
                "The Rune of Satiation is a weaker extension of the Rite of Healing. Without a sufficient field, it "
                + "instead refines the energy in food to restore health. Whenever nourishment restores health, the rune "
                + "restores an additional amount equal to what the food itself would provide.");
            add("malum.gui.book.entry." + RUNE_OF_DECAY_ENTRY, "Rune of Decay");
            add("malum.gui.book.entry." + RUNE_OF_DECAY_ENTRY + ".description", "Grinding Down the Flesh");
            add("malum.gui.book.entry.page.headline." + RUNE_OF_DECAY_ENTRY, "Rune of Decay");
            add("malum.gui.book.entry.page.text." + RUNE_OF_DECAY_ENTRY + ".1",
                "The pulse of the Rite of Decay grows sluggish when inscribed on a tablet. It slowly wears away the flesh "
                + "of hostile creatures near the wearer, but cannot kill them.");
            add("malum.gui.book.entry." + RUNE_OF_THE_PACK_ENTRY, "Rune of the Pack");
            add("malum.gui.book.entry." + RUNE_OF_THE_PACK_ENTRY + ".description", "The Fangs That Follow");
            add("malum.gui.book.entry.page.headline." + RUNE_OF_THE_PACK_ENTRY, "Rune of the Pack");
            add("malum.gui.book.entry.page.text." + RUNE_OF_THE_PACK_ENTRY + ".1",
                "The pulse of empowerment was once inscribed upon monsters. Now I have bent it back to recognize only my "
                + "own pack. Companions within sixteen blocks gain Resistance I, Strength I and Speed I.");
            add("malum.gui.book.entry." + RUNE_OF_RIPENING_ENTRY, "Rune of Ripening");
            add("malum.gui.book.entry." + RUNE_OF_RIPENING_ENTRY + ".description",
                "Little by Little");
            add("malum.gui.book.entry.page.headline." + RUNE_OF_RIPENING_ENTRY, "Rune of Ripening");
            add("malum.gui.book.entry.page.text." + RUNE_OF_RIPENING_ENTRY + ".1",
                "I have turned the power of the Rite of Nourishment inward, strengthening my mind with what once nourished "
                + "living flesh. The Rune of Ripening provides a fairly steady increase of one quarter to the experience I "
                + "gain. Not much, but enough.");
            add("malum.gui.book.entry." + VOID_RUNEWORKING_ENTRY, "Voidish Runecraft: Addenda");
            add("malum.gui.book.entry." + VOID_RUNEWORKING_ENTRY + ".description",
                "Bending Pewter to My Will");
            add("malum.gui.book.entry.page.headline." + VOID_RUNEWORKING_ENTRY,
                "Voidish Runecraft: Addenda");
            add("malum.gui.book.entry.page.text." + VOID_RUNEWORKING_ENTRY + ".1",
                "My practice differs from that of this book's original author. I have long sought to bend Malignant "
                + "Pewter, an anti-magic metal, to magical use. Building on Voidish Runecraft, I introduced variables to "
                + "emphasize one aspect of the pewter while suppressing the other. Working magic through pewter is neither "
                + "practical nor safe, but it is what I want.");
            add("malum.gui.book.entry." + RUNE_OF_STAGNANT_EVOLUTION_ENTRY, "Rune of Stagnant Evolution");
            add("malum.gui.book.entry." + RUNE_OF_STAGNANT_EVOLUTION_ENTRY + ".description",
                "To Halt Evolution");
            add("malum.gui.book.entry.page.headline." + RUNE_OF_STAGNANT_EVOLUTION_ENTRY,
                "Rune of Stagnant Evolution");
            add("malum.gui.book.entry.page.text." + RUNE_OF_STAGNANT_EVOLUTION_ENTRY + ".1",
                "Halting evolution is pewter's defining property. When paired with consciousness, it gains a measure of "
                + "direction, allowing it to halt evolution imposed from outside.");
            add("malum.gui.book.entry." + RUNE_OF_ROTTEN_BONE_ENTRY, "Rune of Rotten Bone");
            add("malum.gui.book.entry." + RUNE_OF_ROTTEN_BONE_ENTRY + ".description",
                "To Embrace Mortal Bone");
            add("malum.gui.book.entry.page.headline." + RUNE_OF_ROTTEN_BONE_ENTRY,
                "Rune of Rotten Bone");
            add("malum.gui.book.entry.page.text." + RUNE_OF_ROTTEN_BONE_ENTRY + ".1",
                "One day she will leave them and depart alone, weary of the songs of heaven's angels, preferring instead "
                + "to embrace the bones of mortals.");
            add("malum.gui.book.entry." + UMBRAL_EXPERIMENT_ENTRY, "The Umbral Spirit Experiment");
            add("malum.gui.book.entry." + UMBRAL_EXPERIMENT_ENTRY + ".description",
                "Seeking the Forbidden… Is There Any Point?");
            add("malum.gui.book.entry.page.headline." + UMBRAL_EXPERIMENT_ENTRY,
                "The Umbral Spirit Experiment");
            add("malum.gui.book.entry.page.text." + UMBRAL_EXPERIMENT_ENTRY + ".1",
                "Having gathered a great quantity of umbral spirits, I began experimenting freely with tablets already "
                + "proven to work. After a few wrong turns, I produced the following tablets.");
            add("malum.gui.book.entry." + RUNE_OF_MELANCHOLIA_ENTRY, "Rune of Melancholia");
            add("malum.gui.book.entry." + RUNE_OF_MELANCHOLIA_ENTRY + ".description", "Delirium's Fancy, After All");
            add("malum.gui.book.entry.page.headline." + RUNE_OF_MELANCHOLIA_ENTRY, "Rune of Melancholia");
            add("malum.gui.book.entry.page.text." + RUNE_OF_MELANCHOLIA_ENTRY + ".1",
                "I am so sorrowful. I am so sorrowful. I am so sorrowful. I am so sorrowful.");
            addBlock(MaledictBlocks.MNEMONIC_OBELISK, "Mnemonic Obelisk");
            addBlock(MaledictBlocks.SOULWOOD_OBELISK, "Soulwood Obelisk");
            addItem(MaledictItems.SACRED_SPIRIT_ARROW, "Sacred Spirit Arrow");
            addItem(MaledictItems.WICKED_SPIRIT_ARROW, "Wicked Spirit Arrow");
            addItem(MaledictItems.ARCANE_SPIRIT_ARROW, "Arcane Spirit Arrow");
            addItem(MaledictItems.ELDRITCH_SPIRIT_ARROW, "Eldritch Spirit Arrow");
            addItem(MaledictItems.AERIAL_SPIRIT_ARROW, "Aerial Spirit Arrow");
            addItem(MaledictItems.AQUEOUS_SPIRIT_ARROW, "Aqueous Spirit Arrow");
            addItem(MaledictItems.EARTHEN_SPIRIT_ARROW, "Earthen Spirit Arrow");
            addItem(MaledictItems.INFERNAL_SPIRIT_ARROW, "Infernal Spirit Arrow");
            add("attribute.name.maledict.powder_snow_damage", "Freezing Damage");
            add("entity.maledict.first_vicissitude", "Vicissitude(WIP)");
            addItem(MaledictItems.AGE_OF_ENLIGHTENMENT, "Age of Enlightenment");
            addItem(MaledictItems.CURIO_RETURN_TOKEN, "Custody Token");
            addItem(MaledictItems.MALIGNANT_PEWTER_TABLET, "Malignant Pewter Tablet");
            addItem(MaledictItems.FIRST_VICISSITUDE_PHASE_ONE_SPAWN_EGG, "Vicissitude Spawn Egg (Phase One)");
            addItem(MaledictItems.FIRST_VICISSITUDE_PHASE_TWO_SPAWN_EGG, "Vicissitude Spawn Egg (Phase Two)");
            addItem(MaledictItems.RUNE_OF_SATIATION, "Rune of Satiation");
            addItem(MaledictItems.RUNE_OF_DECAY, "Rune of Decay");
            addItem(MaledictItems.RUNE_OF_THE_PACK, "Rune of the Pack");
            addItem(MaledictItems.RUNE_OF_RIPENING, "Rune of Ripening");
            addItem(MaledictItems.RUNE_OF_STAGNANT_EVOLUTION, "Rune of Stagnant Evolution");
            addItem(MaledictItems.RUNE_OF_ROTTEN_BONE, "Rune of Rotten Bone");
            addItem(MaledictItems.RUNE_OF_MELANCHOLIA, "Rune of Melancholia");
            add("effect.maledict.age_of_enlightenment", "Age of Enlightenment");
            add("effect.maledict.age_of_darkness", "Age of Darkness");
            add("effect.maledict.age_of_enlightenment.description",
                "People mistake ignorance and darkness for the sacred. Your next strike is guaranteed to be a critical "
                + "hit; each level also grants 1 additional Reserve Staff Charge.");
            add("effect.maledict.age_of_darkness.description",
                "O dark age that lets barbarism burn civilization to ashes: each level reduces magic resistance, Soul Ward "
                + "capacity and Soul Ward integrity by 20%.");
            add("effect.maledict.blessing_of_life", "Blessing of Life");
            add("effect.maledict.blessing_of_life.description",
                "The prayers of the living flow through your veins with nourishment: natural health regeneration restores "
                + "an additional share.");
            add("effect.maledict.decay", "Breath of Decay");
            add("effect.maledict.decay.description",
                "Hostile creatures near the wearer lose half a heart every two seconds. This damage is never fatal.");
            add("effect.maledict.thinning", "Edict of Thinning");
            add("effect.maledict.thinning.description",
                "When more than eight hostile creatures of the same type surround the wearer, those on the outskirts take "
                + "1.5 damage every two seconds.");
            add("effect.maledict.ripening", "Boon of Ripening");
            add("effect.maledict.ripening.description",
                "The wearer gains an extra quarter of all experience earned.");
            add("effect.maledict.sacrifice", "Sacrifice");
            add("effect.maledict.sacrifice.description",
                "Sages gave their lives in sacrifice, and thereby gained wisdom. Yet not every sacrifice "
                + "brings wisdom: only by imitating the rites of the sages might one gain a sage's wisdom.");
            add("curios.identifier.delusion",
                "Eye");
            add("malum.gui.curio.effect.maledict.age_of_enlightenment.cooldown",
                "Cooldowns recover twice as fast while worn");
            add("malum.gui.curio.effect.maledict.age_of_enlightenment.spirit_void",
                "Attacking targets at half health triggers spirit-harvesting effects");
            add("malum.gui.curio.effect.maledict.age_of_enlightenment.enlightenment",
                "Slaying an enemy grants Age of Enlightenment, or extends its duration if already active");
            add("malum.gui.curio.effect.maledict.blessing_of_life",
                "Natural health regeneration restores an additional equal amount.");
            add("malum.gui.curio.effect.maledict.decay",
                "Nearby hostile creatures lose half a heart every two seconds. This damage is never fatal.");
            add("malum.gui.curio.effect.maledict.ripening",
                "Gain a quarter more experience.");
            add("malum.gui.curio.effect.maledict.pack_boon",
                "Your companions within sixteen blocks gain Resistance I, Strength I and Speed I.");
            add("malum.gui.curio.effect.maledict.rotten_bone",
                "If your maximum health is greater than 1, escape death at the cost of 25% of your maximum health.");
            add("malum.gui.curio.effect.maledict.melancholia",
                "You gradually become resistant to repeated damage of the same kind. Healing and harm alike leave you "
                + "numb.");
            add("tooltip.maledict.age_of_enlightenment.hold_shift", "Hold Shift to ask a question better left unasked");
            add("tooltip.maledict.age_of_enlightenment.shift",
                "Did an age of darkness ever exist?");
            add("tooltip.maledict.age_of_enlightenment.shift.enlightened",
                "O infinite, boundless, ever-turning Age of Enlightenment");
            add("tooltip.maledict.curio_return_token",
                "Right-click to reclaim confiscated curios that are still in custody. "
                + "The token only disappears once everything has been handed back.");
            add("message.maledict.first_vicissitude.curio_return_pending",
                "%s curio(s) are still in custody: free up inventory or curio slots and they "
                + "will be returned automatically");
            add("message.maledict.first_vicissitude.curio_return_complete",
                "Every confiscated curio has been returned");
            add("message.maledict.first_vicissitude.adaptation",
                "Rigid convention, blind adherence to the old ways: Vicissitude regards them as filth.");
            add("message.maledict.first_vicissitude.attack",
                "Fate always drives people to the edge of a cliff, like an unwelcome guest calling.");
            add("message.maledict.first_vicissitude.phase_two",
                "Vicissitude is the fate even sages meet while walking the righteous path.");
            add("sounds.maledict.music.vicissitude.phase_one",
                "Vicissitude: An Unwelcome Guest");
            add("sounds.maledict.music.vicissitude.phase_two",
                "Vicissitude: What Cannot Be Helped");
            add("sounds.maledict.item.incursus_blade.throw", "Incursus Blade: Keening, Whirling Away");
            add("sounds.maledict.item.incursus_blade.recall", "Incursus Blade: Rending Wind and Silk, Ringing Gold and Jade");
            add("sounds.maledict.item.incursus_blade.ascension", "Incursus Blade: Wings Beat");
            add("sounds.maledict.item.incursus_blade.slash", "Incursus Blade: Erosion");
            add("sounds.maledict.item.incursus_blade.crit", "Incursus Blade: Ecstatic Roar");
            add("tooltip.maledict.incursus_blade.description",
                "A sinister scythe that grows by devouring spirits and sweeps its blade around the wielder.");
            add("tooltip.maledict.incursus_blade.medium_unlock_hint",
                "When spirit power reaches the sacred number, a deeper power awakens.");
            add("tooltip.maledict.incursus_blade.hold_shift", "Hold Shift for spirit details");
            add("tooltip.maledict.incursus_blade.infusion", "Right-click this item with spirits in the inventory to infuse them permanently:");
            add("tooltip.maledict.incursus_blade.spirit.earthen", "Earthen %s (%s/%s): Attack damage");
            add("tooltip.maledict.incursus_blade.spirit.aqueous", "Aqueous %s (%s/%s): Freezing damage");
            add("tooltip.maledict.incursus_blade.spirit.arcane", "Arcane %s (%s/%s): Magic damage");
            add("tooltip.maledict.incursus_blade.spirit.aerial",
                "Aerial %s%% (%s/%s): Remove random harmful effects and gain random beneficial effects");
            add("tooltip.maledict.incursus_blade.spirit.sacred",
                "Sacred %s%% (%s/%s): Shorten enemies' beneficial effects; extend or inflict harmful effects");
            add("tooltip.maledict.incursus_blade.spirit.infernal", "Infernal %s (%s/%s): Additional looting level");
            add("tooltip.maledict.incursus_blade.spirit.eldritch",
                "Eldritch %s%% (%s/%s): Convert a portion of damage into absorption up to a cap, and immediately restore "
                + "Soul Ward");
            add("tooltip.maledict.incursus_blade.spirit.wicked", "Wicked %s%% (%s/%s): Critical damage");
            add("malum.gui.book.entry.void.maledict.incursus_blade", "The Incursus Blade");
            add("malum.gui.book.entry.void.maledict.incursus_blade.description", "A hunger without end");
            add("malum.gui.book.entry.page.headline.void.maledict.incursus_blade", "The Incursus Blade");
            add("malum.gui.book.entry.page.text.void.maledict.incursus_blade.1",
                "Perhaps falling into the well shattered my fear of the forbidden. "
                + "Malignant pewter's hunger for the erosion of knowledge forged the Edge of Deliverance.");
            add("malum.gui.book.entry.page.text.void.maledict.incursus_blade.2",
                "Could that rejection be transformed into an overt magical property? My earlier research reminded me "
                + "that, if I could construct a dynamically stable environment, the pulses of the eight spirits "
                + "might provoke the pulse of a ninth arcane energy to reveal itself.");
            add("malum.gui.book.entry.page.text.void.maledict.incursus_blade.3",
                "At last I obtained this weapon. The will within the Edge of Deliverance was utterly devoured by the "
                + "void. This emptiness hungers to consume everything, while the eight spirits submit entirely to its "
                + "control; even their pulses are distorted, manifesting unexpected abilities.");
            add("malum.gui.book.entry.page.text.void.maledict.incursus_blade.4",
                "As more spirits are offered, the weapon becomes ever more inscrutable. I have named it the Incursus "
                + "Blade, and hope this void can be sated by the pulses.");
            add("malum.gui.book.entry." + OBELISKS_ENTRY, "Esoteric Obelisks");
            add("malum.gui.book.entry." + OBELISKS_ENTRY + ".description", "Rejection and remembrance");
            add("malum.gui.book.entry.page.headline." + SOULWOOD_OBELISK_PAGE, "Soulwood Obelisk");
            add("malum.gui.book.entry.page.text." + SOULWOOD_OBELISK_PAGE + ".1",
                "By exploiting malignant metal's rejection of magic, I can focus the power of hallowed gold more "
                + "intensely, providing a 50% acceleration effect.");
            add("malum.gui.book.entry.page.headline." + MNEMONIC_OBELISK_PAGE, "Mnemonic Obelisk");
            add("malum.gui.book.entry.page.text." + MNEMONIC_OBELISK_PAGE + ".1",
                "Experience crystals have already proven their worth. Adding more of the power condensed in mnemonic "
                + "fragments makes their enchanting power shine brighter still, reaching the astonishing strength of ten "
                + "bookshelves.");
            add("malum.gui.rite." + VICISSITUDE_RITE_ID, "Rite of Vicissitude");
            add("malum.gui.rite.corrupted_" + VICISSITUDE_RITE_ID, "Rite of Vicissitude");
            add("malum.gui.rite." + GREATER_RITE_ID, "Greater Rite of Vicissitude");
            add("malum.gui.rite.corrupted_" + GREATER_RITE_ID, "Greater Rite of Vicissitude");
            add("malum.gui.rite." + ELDRITCH_RITE_ID, "Eldritch Rite of Vicissitude(WIP!)");
            add("malum.gui.rite.corrupted_" + ELDRITCH_RITE_ID, "Eldritch Rite of Vicissitude");
            add("malum.gui.rite." + GREATER_ELDRITCH_RITE_ID, "Greater Eldritch Rite of Vicissitude");
            add("malum.gui.rite.corrupted_" + GREATER_ELDRITCH_RITE_ID, "Greater Eldritch Rite of Vicissitude");
            add("malum.gui.book.entry." + RITE_ENTRY, "Rite of Vicissitude");
            add("malum.gui.book.entry." + RITE_ENTRY + ".description", "Calling what cannot be recalled");
            add("malum.gui.book.entry.page.headline." + RITE_ENTRY, "Rite of Vicissitude");
            add("malum.gui.book.entry.page.text." + RITE_ENTRY + ".1",
                "Vicissitude is not a being I can create; it is only a shadow that enters through the wound. "
                + "By laws I do not know, three arcane spirits pry open a crack and four pry it wider - and "
                + "what comes through that door is fate's own unwelcome guest.");
            add("malum.gui.book.entry.page.text." + RITE_ENTRY + ".2",
                "Using eldritch spirits extends the reach of the Rite of Vicissitude. A more complete shadow falls upon "
                + "this place, though a shadow can hardly be called complete. Two eldritch spirits summon a more complete "
                + "aspect still.");
            add("malum.gui.rite.corrupted_" + SACRIFICE_RITE_ID, "Rite of Sacrifice");
            add("malum.gui.book.entry.page.text.corrupt_" + SACRIFICE_RITE_ID + ".hover",
                "Survive the sacrifice to gain an additional rune slot and an eye slot.");
            add("malum.gui.book.entry." + SACRIFICE_RITE_ENTRY, "Rite of Sacrifice");
            add("malum.gui.book.entry." + SACRIFICE_RITE_ENTRY + ".description", "The Price of Wisdom");
            add("malum.gui.book.entry.page.text." + SACRIFICE_RITE_PAGE,
                "Where does wisdom come from? Could I truly gain a sage's wisdom by wounding myself with Tyrving in a "
                + "deliberate rite, as I so foolishly hope?");
            add("malum.gui.book.entry.page.text." + SACRIFICE_RITE_PAGE + ".2",
                "Through nine days and nine nights of torment I did see something, and came to know something "
                + "as well. My body has gained another place where runes may be carved, and my eyes have "
                + "become a doorway.");
        }
    }

    private static final String OBELISKS_ENTRY = "void.maledict.obelisks";
    private static final String ELEGY_BOW_ENTRY = "void.maledict.elegy_bow";
    private static final String SOULWOOD_OBELISK_PAGE = OBELISKS_ENTRY + ".soulwood_obelisk";
    private static final String MNEMONIC_OBELISK_PAGE = OBELISKS_ENTRY + ".mnemonic_obelisk";
    private static final String RITE_ENTRY = "void.maledict.vicissitude_rite";

    /**
     * 牺牲仪式的键名。键值（各语言的文案）是作者写的，别动；键名按 Malum 的灵魂木惯例带 {@code corrupt}，
     * 这样 {@code RiteEntryObject} 与 {@code SpiritRiteRecipePage} 会自动画灵魂木那一套。
     * 页面文本键是短名，{@code SpiritRiteTextPage} 与 {@code HeadlineTextItemPage} 共用它。
     */
    private static final String SACRIFICE_RITE_ENTRY = "maledict.corrupt_sacrifice_rite";
    private static final String SACRIFICE_RITE_PAGE = "corrupt_sacrifice_rite";
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
    private static final String VICISSITUDE_RITE_ID = "vicissitude_rite";
    private static final String SACRIFICE_RITE_ID = "sacrifice_rite";
    private static final String GREATER_RITE_ID = "greater_vicissitude_rite";
    private static final String ELDRITCH_RITE_ID = "eldritch_vicissitude_rite";
    private static final String GREATER_ELDRITCH_RITE_ID = "greater_eldritch_vicissitude_rite";
}
