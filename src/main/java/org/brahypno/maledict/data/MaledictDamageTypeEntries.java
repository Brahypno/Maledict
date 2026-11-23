package org.brahypno.maledict.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.MaledictDamageTypes;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * {@code data/maledict/damage_type/} 的唯一来源；标签由 {@link MaledictDamageTypeTags} 生成，
 * 数值与消息键写在这里。
 *
 * <p>只有沉辉与反照两种：造物主的四类伤害继续用原版的 {@code mob_attack} / {@code generic} /
 * {@code magic} / {@code indirect_magic}。1.20.1 之所以另起类型，是因为它在代码里先乘一遍配置倍率、
 * 再挡住原版难度曲线；那套倍率已删（见 {@code DemiurgeDifficulty}），原版四类自带的
 * {@code when_caused_by_living_non_player} 就是唯一的口径，再克隆一份只会多出一叠必须跟着原版同步的标签。
 */
public final class MaledictDamageTypeEntries extends DatapackBuiltinEntriesProvider {
    public MaledictDamageTypeEntries(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, new RegistrySetBuilder()
                .add(Registries.DAMAGE_TYPE, MaledictDamageTypeEntries::damageTypes), Set.of(Maledict.MODID));
    }

    private static void damageTypes(BootstrapContext<DamageType> context) {
        // 两种都按世界难度缩放，与原版魔法伤害同一口径。
        context.register(MaledictDamageTypes.LUMENFALL_RELEASE,
                new DamageType("lumenfall_release", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.0F));
        context.register(MaledictDamageTypes.REFULGENCE,
                new DamageType("refulgence", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.0F));
    }

    @Override
    public String getName() {
        return "Maledict damage type entries";
    }
}
