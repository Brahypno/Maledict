package org.brahypno.maledict.common.rite;

import com.sammy.malum.common.block.curiosities.totem.TotemBaseBlockEntity;
import com.sammy.malum.common.spiritrite.TotemicRiteEffect;
import com.sammy.malum.common.spiritrite.TotemicRiteType;
import com.sammy.malum.core.systems.spirit.MalumSpiritType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

/**
 * 牺牲仪式：图腾底座上立起五根柱子（自下而上 邪术 / 大地 / 碧水 / 澄空 / 狱火），凑齐即发动一次——
 * <b>但只有灵魂木那一半会兑现</b>。
 *
 * <p>注意这不是两条仪式：注册表里只有一条 {@code sacrifice_rite}（见 {@link SacrificeRite#install()}）。
 * 下面两个钩子是 Malum 强制的天然（符文木）/ 腐化（灵魂木）变体，材质只决定走哪一个：符文木那一半是
 * {@link RefusedRiteEffect}，什么都不做。Malum 查表用 {@code SpiritRiteRegistry.getRite(spirits)}，
 * 只看精魂顺序、不看材质，所以做不到"符文木不认识这套排布"，只能做到"符文木上不发生任何事"。
 *
 * <p>与 {@link VicissitudeRiteType} 的差别只在图标：Malum 按 identifier 拼图标路径（{@code sacrifice_rite}
 * 会拼出一张不存在的图），故这里借它现成的一张——本仪式列表末尾的辨识精魂是狱火，Malum 自己的狱火仪式
 * 用的就是它。那边要拿 {@code this.difficulty}，所以效果对象反过来持有仪式；这边不需要，于是各存一份。
 */
public final class SacrificeRiteType extends TotemicRiteType {
    /** 符文木上的那一半：空效果。 */
    private static final TotemicRiteEffect REFUSED = new RefusedRiteEffect();

    /** 灵魂木上的那一半：真正挑人、按印记。 */
    private static final TotemicRiteEffect OFFERING = new OfferingRiteEffect();

    public SacrificeRiteType(String identifier, MalumSpiritType... spirits) {
        super(identifier, spirits);
    }

    @Override
    protected TotemicRiteEffect getNaturalRiteEffect() {
        return REFUSED;
    }

    @Override
    protected TotemicRiteEffect getCorruptedEffect() {
        return OFFERING;
    }

    @Override
    public ResourceLocation getIcon() {
        return ResourceLocation.fromNamespaceAndPath("malum", "textures/vfx/rite/infernal.png");
    }

    /**
     * 符文木上的空效果，类别与真身一致（单次触发），于是符文木塔凑齐五柱会照常点亮、随即熄回闲置，
     * 不掉血、不按印记、也不留凭据——看上去就是"这套排布在符文木上不应验"。
     */
    private static final class RefusedRiteEffect extends TotemicRiteEffect {
        private RefusedRiteEffect() {
            super(MalumRiteEffectCategory.ONE_TIME_EFFECT);
        }

        @Override
        protected void doRiteEffect(TotemBaseBlockEntity totem, ServerLevel level) {
            // 有意留空：牺牲仪式只在灵魂木上兑现。
        }
    }

    private static final class OfferingRiteEffect extends TotemicRiteEffect {
        private OfferingRiteEffect() {
            super(MalumRiteEffectCategory.ONE_TIME_EFFECT);
        }

        @Override
        protected void doRiteEffect(TotemBaseBlockEntity totem, ServerLevel level) {
            SacrificeRite.offer(totem, level);
        }
    }
}
