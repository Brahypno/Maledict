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
                "停不下的思念迫使我倾注在制 作上。于是，思念弓具有了满 蓄力自动发射的功能，我将其 称为哀歌之弓");
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
            add("effect.maledict.age_of_enlightenment", "启蒙之年");
            add("effect.maledict.age_of_darkness", "黑暗时代");
            add("effect.maledict.age_of_enlightenment.description",
                "人民把蒙昧与黑暗错认为神圣，你的下一击必定造成暴击；每级额外给予 1 点法杖暂存弹数。");
            add("effect.maledict.age_of_darkness.description",
                "任由野蛮焚烧文明的黑暗时代啊，每级降低 20% 魔法抗性、灵魂护盾容量与灵魂护盾稳固度。");
            add("malum.gui.curio.effect.maledict.age_of_enlightenment.cooldown", "佩戴时冷却速度加倍");
            add("malum.gui.curio.effect.maledict.age_of_enlightenment.spirit_void", "攻击半生命值目标时触发收获精魂时的效果");
            add("malum.gui.curio.effect.maledict.age_of_enlightenment.enlightenment", "击杀敌人时获得启蒙之年，已有则延长时间");
            add("tooltip.maledict.age_of_enlightenment.hold_shift", "按住 Shift 追问一个不该问的问题");
            add("tooltip.maledict.age_of_enlightenment.shift", "黑暗的时代曾经存在过吗");
            add("tooltip.maledict.age_of_enlightenment.shift.enlightened", "那无穷，无限，永动的启蒙之年啊");
            add("tooltip.maledict.curio_return_token", "右键领取被无常没收、尚未归还的饰品。全部交付后凭证才会消失。");
            add("message.maledict.first_vicissitude.curio_return_pending",
                "还有 %s 件饰品由无常保管：腾出背包或饰品栏空间后会自动归还");
            add("message.maledict.first_vicissitude.curio_return_complete", "被没收的饰品已全部归还");
            add("message.maledict.first_vicissitude.attack", "命运总是会将人逼上悬崖，犹如恶客造访");
            add("message.maledict.first_vicissitude.phase_two",
                "所谓无常，就是圣人行走在正道上，依然会遇见的命运");
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
                "也许是坠井击碎了我对禁忌的 畏怖。恶念白镴对知识消退的 渴望铸造了挽魂锋镰，那么能 否将这种排斥转化为一种显性 的魔法性质呢？之前的研究提 醒了我，如果能够构建一个动 态稳定的环境，在八种精魂脉 动的刺激下，也许第九种奥术 能量的脉动得以显现。");
            add("malum.gui.book.entry.page.text.void.maledict.incursus_blade.2",
                "最后我得到了这种武器，挽魂 锋镰本身的意志被虚空吞噬殆 尽，这片空无渴求着吞并一切 ，而八种精魂完全受其操控， 甚至脉动被扭曲，从而对外展 现出意料之外的能力。随着投 入精魂数量的增加，这柄武器 也更加的难以捉摸。我将之成 为神侵恶刃，并希望这片虚空 能够满足于脉动。");
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
            add("malum.gui.rite." + ELDRITCH_RITE_ID, "邪术无常仪式");
            add("malum.gui.rite.corrupted_" + ELDRITCH_RITE_ID, "邪术无常仪式");
            add("malum.gui.rite." + GREATER_ELDRITCH_RITE_ID, "邪术无常大仪式");
            add("malum.gui.rite.corrupted_" + GREATER_ELDRITCH_RITE_ID, "邪术无常大仪式");
            add("malum.gui.book.entry." + RITE_ENTRY, "无常仪式");
            add("malum.gui.book.entry." + RITE_ENTRY + ".description", "唤来无法收回之物");
            add("malum.gui.book.entry.page.headline." + RITE_ENTRY, "无常仪式");
            add("malum.gui.book.entry.page.text." + RITE_ENTRY + ".1",
                "无常不是我创造的东西，它只 是被叫来的。三枚奥术精魂推 开一条缝，四枚推得更开；同 一个存在，愿意带多少力气过 来却不同。配方页所画的顺序 ，就是自下而上的摆放顺序。");
            add("malum.gui.book.entry.page.text." + RITE_ENTRY + ".2",
                "把邪术精魂放在最底下，召唤 就会落到更深处：一枚邪术是 完整的它，两枚则是不再留手 的那一面。邪术不因我的谨慎 而收敛，它只把无常推到我原 本不敢伸手的地方。");
        }else {
            add("itemGroup.maledict", "Maledict");
            add("enchantment.maledict.ectoplasm", "Ectoplasm");
            add("enchantment.maledict.reminiscence", "Reminiscence");
            add("enchantment.maledict.aftertaste", "Aftertaste");
            addItem(MaledictItems.INCURSUS_BLADE, "The Incursus Blade");
            addItem(MaledictItems.REMEMBRANCE_BOW, "Remembrance Bow");
            addItem(MaledictItems.ELEGY_BOW, "Elegy Bow");
            add("malum.gui.book.entry.maledict.remembrance_bow", "Remembrance Bow");
            add("malum.gui.book.entry.maledict.remembrance_bow.description", "Longing Beyond Barriers");
            add("malum.gui.book.entry.page.headline.maledict.remembrance_bow", "Remembrance Bow");
            add("malum.gui.book.entry.page.text.maledict.remembrance_bow.1",
                "If the magic of endermen could be gathered and borne by astral weave, it might lend soulwood wondrous properties. A bow made from this material takes only half as long to draw as an ordinary bow; its arrows fly faster and can pass through blocks to a limited extent.");
            add("malum.gui.book.entry." + ELEGY_BOW_ENTRY, "Elegy Bow");
            add("malum.gui.book.entry." + ELEGY_BOW_ENTRY + ".description", "Remembrance Without Respite");
            add("malum.gui.book.entry.page.headline." + ELEGY_BOW_ENTRY, "Elegy Bow");
            add("malum.gui.book.entry.page.text." + ELEGY_BOW_ENTRY + ".1",
                "Longing that would not cease compelled me to pour myself into the work. Thus, the Remembrance Bow gained the ability to fire automatically at full draw. I call it the Elegy Bow.");
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
            add("effect.maledict.age_of_enlightenment", "Age of Enlightenment");
            add("effect.maledict.age_of_darkness", "Age of Darkness");
            add("effect.maledict.age_of_enlightenment.description",
                "Where ignorance and shadow were mistaken for divine, Your next strike is sealed as a critical blow. "
                + "Each level grants an additional Reserve Staff Charge.");
            add("effect.maledict.age_of_darkness.description",
                "Burned through civilization, stripping a fifth of your magic resistance, "
                + "Soul Ward capacity and Soul Ward integrity per level.");
            add("malum.gui.curio.effect.maledict.age_of_enlightenment.cooldown", "Doubles the speed of item cooldowns while worn");
            add("malum.gui.curio.effect.maledict.age_of_enlightenment.spirit_void", "Striking Half Health Targets Triggers Spirit Collection Effects");
            add("malum.gui.curio.effect.maledict.age_of_enlightenment.enlightenment",
                "Slaying an enemy grants the Age of Enlightenment, or extends it if already held");
            add("tooltip.maledict.age_of_enlightenment.hold_shift", "Hold Shift to ask a question better left unasked");
            add("tooltip.maledict.age_of_enlightenment.shift", "Was there ever a true age of darkness?");
            add("tooltip.maledict.age_of_enlightenment.shift.enlightened",
                "That infinite, boundless, perpetual \"Age of Enlightenment.\"");
            add("tooltip.maledict.curio_return_token",
                "Right-click to reclaim confiscated curios that are still in custody. "
                + "The token only disappears once everything has been handed back.");
            add("message.maledict.first_vicissitude.curio_return_pending",
                "%s curio(s) are still in custody: free up inventory or curio slots and they "
                + "will be returned automatically");
            add("message.maledict.first_vicissitude.curio_return_complete",
                "Every confiscated curio has been returned");
            add("message.maledict.first_vicissitude.attack",
                "Fate always drives people to the edge of a cliff, like an unwelcome guest calling.");
            add("message.maledict.first_vicissitude.phase_two",
                "Vicissitude is the fate even sages meet while walking the righteous path.");
            add("tooltip.maledict.incursus_blade.description", "A evil scythe that devours spirits and strikes all around its wielder.");
            add("tooltip.maledict.incursus_blade.medium_unlock_hint", "When every spirit reaches the sacred number, a more intrinsic power will awaken.");
            add("tooltip.maledict.incursus_blade.hold_shift", "Hold Shift for spirit details");
            add("tooltip.maledict.incursus_blade.infusion", "Right-click this item with spirits in the inventory to infuse them permanently:");
            add("tooltip.maledict.incursus_blade.spirit.earthen", "Earthen %s (%s/%s): Attack damage");
            add("tooltip.maledict.incursus_blade.spirit.aqueous", "Aqueous %s (%s/%s): Freezing damage");
            add("tooltip.maledict.incursus_blade.spirit.arcane", "Arcane %s (%s/%s): Magic damage");
            add("tooltip.maledict.incursus_blade.spirit.aerial", "Aerial %s%% (%s/%s): Cleanse random negatives and gain random beneficial effects");
            add("tooltip.maledict.incursus_blade.spirit.sacred", "Sacred %s%% (%s/%s): Shorten enemy benefits; extend or inflict harmful effects");
            add("tooltip.maledict.incursus_blade.spirit.infernal", "Infernal %s (%s/%s): Additional looting level");
            add("tooltip.maledict.incursus_blade.spirit.eldritch", "Eldritch %s%% (%s/%s): Damage grants capped absorption and immediately recovers Soul Ward");
            add("tooltip.maledict.incursus_blade.spirit.wicked", "Wicked %s%% (%s/%s): Critical damage");
            add("malum.gui.book.entry.void.maledict.incursus_blade", "The Incursus Blade");
            add("malum.gui.book.entry.void.maledict.incursus_blade.description", "A hunger without end");
            add("malum.gui.book.entry.page.headline.void.maledict.incursus_blade", "The Incursus Blade");
            add("malum.gui.book.entry.page.text.void.maledict.incursus_blade.1",
                "Perhaps falling into the well shattered my fear of the forbidden. Malignant pewter's hunger for the erosion of knowledge forged the Edge of Deliverance, so could that rejection be transformed into an overt magical property? My earlier research reminded me that, if I could construct a dynamically stable environment, the pulses of the eight spirits might provoke the pulse of a ninth arcane energy to reveal itself.");
            add("malum.gui.book.entry.page.text.void.maledict.incursus_blade.2",
                "At last I obtained this weapon. The will within the Edge of Deliverance was utterly devoured by the void. This emptiness hungers to consume everything, while the eight spirits submit entirely to its control; even their pulses are distorted, manifesting unexpected abilities. As more spirits are offered, the weapon becomes ever more inscrutable. I have named it the Incursus Blade, and hope this void can be sated by the pulses.");
            add("malum.gui.book.entry." + OBELISKS_ENTRY, "Esoteric Obelisks");
            add("malum.gui.book.entry." + OBELISKS_ENTRY + ".description", "Rejection and remembrance");
            add("malum.gui.book.entry.page.headline." + SOULWOOD_OBELISK_PAGE, "Soulwood Obelisk");
            add("malum.gui.book.entry.page.text." + SOULWOOD_OBELISK_PAGE + ".1",
                "By cleverly exploiting malignant metal's rejection of magic, the power of hallowed gold can be focused more intensely, producing an acceleration effect of one half.");
            add("malum.gui.book.entry.page.headline." + MNEMONIC_OBELISK_PAGE, "Mnemonic Obelisk");
            add("malum.gui.book.entry.page.text." + MNEMONIC_OBELISK_PAGE + ".1",
                "Experience crystals have already proven this approach successful. Adding more of the power condensed within mnemonic fragments can make enchanting power shine even more brilliantly, reaching the astonishing strength of ten bookshelves.");
            add("malum.gui.rite." + VICISSITUDE_RITE_ID, "Rite of Vicissitude");
            add("malum.gui.rite.corrupted_" + VICISSITUDE_RITE_ID, "Rite of Vicissitude");
            add("malum.gui.rite." + GREATER_RITE_ID, "Greater Rite of Vicissitude");
            add("malum.gui.rite.corrupted_" + GREATER_RITE_ID, "Greater Rite of Vicissitude");
            add("malum.gui.rite." + ELDRITCH_RITE_ID, "Eldritch Rite of Vicissitude");
            add("malum.gui.rite.corrupted_" + ELDRITCH_RITE_ID, "Eldritch Rite of Vicissitude");
            add("malum.gui.rite." + GREATER_ELDRITCH_RITE_ID, "Greater Eldritch Rite of Vicissitude");
            add("malum.gui.rite.corrupted_" + GREATER_ELDRITCH_RITE_ID, "Greater Eldritch Rite of Vicissitude");
            add("malum.gui.book.entry." + RITE_ENTRY, "Rite of Vicissitude");
            add("malum.gui.book.entry." + RITE_ENTRY + ".description", "Calling what cannot be recalled");
            add("malum.gui.book.entry.page.headline." + RITE_ENTRY, "Rite of Vicissitude");
            add("malum.gui.book.entry.page.text." + RITE_ENTRY + ".1",
                "Vicissitude is not something I created; it is only ever called. Three arcane spirits open a crack, four open it wider, and the same being decides how much of itself comes through. The order drawn on the recipe page is the order of the poles, bottom first.");
            add("malum.gui.book.entry.page.text." + RITE_ENTRY + ".2",
                "Set eldritch spirits at the very bottom and the call reaches deeper: one eldritch brings the complete thing, two bring the face that no longer holds back. The eldritch does not moderate itself for my caution; it only pushes Vicissitude further than I dared to reach.");
        }
    }

    private static final String OBELISKS_ENTRY = "void.maledict.obelisks";
    private static final String ELEGY_BOW_ENTRY = "void.maledict.elegy_bow";
    private static final String SOULWOOD_OBELISK_PAGE = OBELISKS_ENTRY + ".soulwood_obelisk";
    private static final String MNEMONIC_OBELISK_PAGE = OBELISKS_ENTRY + ".mnemonic_obelisk";
    private static final String RITE_ENTRY = "void.maledict.vicissitude_rite";
    private static final String VICISSITUDE_RITE_ID = "vicissitude_rite";
    private static final String GREATER_RITE_ID = "greater_vicissitude_rite";
    private static final String ELDRITCH_RITE_ID = "eldritch_vicissitude_rite";
    private static final String GREATER_ELDRITCH_RITE_ID = "greater_eldritch_vicissitude_rite";
}
