package org.brahypno.maledict.common.ritual.wisdom;

import com.sammy.malum.core.systems.registry.SpiritHolder;
import com.sammy.malum.core.systems.registry.rite.RiteEffectHolder;
import com.sammy.malum.core.systems.rite.SpiritRiteType;
import com.sammy.malum.core.systems.rite.effect.SpiritRiteEffect;
import com.sammy.malum.core.systems.spirit.type.SpiritArcanaType;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

/**
 * 牺牲仪式：图腾底座上立起五根柱子（自下而上 邪术 / 大地 / 碧水 / 澄空 / 狱火），凑齐即发动一次。
 *
 * <p>1.20.1 只有一条注册（{@code sacrifice_rite}），天然（符文木）与腐化（灵魂木）两半由同一个类型内部
 * 分派：符文木那一半是空效果，灵魂木那一半才真正挑人。1.8.2 把这道分派挪到了注册层——
 * {@code SpiritRiteType#matches} 先比 {@code totem.corrupted} 与本类型的 {@code isCorrupted}，
 * 再看精魂序列，所以同一条五精魂序列可以各注册一条，查表不会互相吃掉。
 * 两条在 {@code MaledictRites} 里成对注册：{@code sacrifice_rite}（自然，空效果）与
 * {@code corrupted_sacrifice_rite}（腐化，真身）。
 *
 * <p>显示文案两条都用腐化那条的键：1.20.1 只有一个类型，符文木上立柱子时看到的也是「腐化牺牲仪式」这组
 * 字符串，这里保持一致，不给自然半身另造一套没人写过的文案。
 *
 * <p>图标借 Malum 狱火仪式那张：Malum 按 identifier 拼图标路径（{@code sacrifice_rite} 会拼出一张不存在的图），
 * 而本仪式列表末尾的辨识精魂就是狱火。
 */
public final class SacrificeRiteType extends SpiritRiteType {
    public SacrificeRiteType(RiteEffectHolder<? extends SpiritRiteEffect> effect, boolean corrupted,
            List<SpiritHolder<SpiritArcanaType>> spirits) {
        super(effect, corrupted, List.copyOf(spirits));
    }

    @Override
    public ResourceLocation getIcon() {
        return com.sammy.malum.registry.common.magic.rite.MalumSpiritRiteTypes
                .RITE_OF_THE_BURNING_FERVOR.value().getIcon();
    }

    @Override
    public String getLangKey() { return "malum.gui.rite.corrupted_sacrifice_rite"; }

    @Override
    public String getCodexEntryLangKey() { return "malum.gui.book.entry.page.text.corrupt_sacrifice_rite"; }

    @Override
    public String getEffectLangKey() { return getCodexEntryLangKey() + ".hover"; }
}
