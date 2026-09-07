package org.brahypno.maledict.data;

import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;
import org.brahypno.maledict.Maledict;
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
            addItem(MaledictItems.INCURSUS_BLADE, "神侵恶刃");
            add("attribute.name.maledict.powder_snow_damage", "冻结伤害");
            add("tooltip.maledict.incursus_blade.description", "吞噬精魂成长，向周身挥出镰刃的魔法镰刀。");
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
        }else {
            add("itemGroup.maledict", "Maledict");
            addItem(MaledictItems.INCURSUS_BLADE, "The Incursus Blade");
            add("attribute.name.maledict.powder_snow_damage", "Freezing Damage");
            add("tooltip.maledict.incursus_blade.description", "A magic scythe that devours spirits and strikes all around its wielder.");
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
        }
    }
}
