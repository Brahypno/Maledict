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
        if ("zh_cn".equals(locale)) {
            addItem(MaledictItems.INCURSUS_BLADE, "神侵恶刃");
            add("attribute.name.maledict.powder_snow_damage", "细雪伤害");
        } else {
            addItem(MaledictItems.INCURSUS_BLADE, "The Incursus Blade");
            add("attribute.name.maledict.powder_snow_damage", "Powder Snow Damage");
        }
    }
}
