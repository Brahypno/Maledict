package org.brahypno.maledict.common.ritual.soulbinding;

import com.sammy.malum.common.block.curiosities.totem.TotemBaseBlockEntity;
import com.sammy.malum.common.block.curiosities.totem.TotemPoleBlockEntity;
import com.sammy.malum.core.systems.registry.SpiritHolder;
import com.sammy.malum.core.systems.registry.rite.RiteEffectHolder;
import com.sammy.malum.core.systems.rite.SpiritRiteType;
import com.sammy.malum.core.systems.rite.effect.SpiritRiteEffect;
import com.sammy.malum.core.systems.spirit.type.SpiritArcanaType;
import com.sammy.malum.registry.common.magic.MalumSpiritTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictRites;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The one Rite of Soulbinding. Malum registers a single rite with a natural (Runewood) and a
 * corrupted (Soulwood) effect, so both halves live here instead of being two separate rites.
 *
 * <p>The rite's actual work is performed by {@link SoulbindingRitual} when the totem base is
 * activated: that is the only place Malum exposes the player who started the rite. The two effects
 * are therefore deliberately empty (a {@code OneShotRiteEffect} that only lets the totem reset);
 * the totem still assembles, lights up and resets as a normal one-time rite.
 *
 * <p>Spirit order is bottom-to-top: Eldritch, Wicked, Arcane. 1.20.1 用 {@code SpiritRiteRegistry}
 * 手工放进静态表并自己查重；1.8.2 换成 {@code DeferredRiteTypes}，所以注册落在
 * {@link MaledictRites}，查重保留在下面的 {@code setup} 里——序列撞车要当场炸掉，不能悄悄覆盖别家的仪式。
 */
@EventBusSubscriber(modid = Maledict.MODID)
public final class SoulbindingRite extends SpiritRiteType {
    public static final String IDENTIFIER = "maledict_soulbinding_rite";
    public static final String CORRUPTED_IDENTIFIER = "corrupted_maledict_soulbinding_rite";

    private static final List<SpiritHolder<SpiritArcanaType>> SPIRITS = List.of(
            MalumSpiritTypes.ELDRITCH_SPIRIT,
            MalumSpiritTypes.WICKED_SPIRIT,
            MalumSpiritTypes.ARCANE_SPIRIT
    );

    public SoulbindingRite(RiteEffectHolder<? extends SpiritRiteEffect> effect, boolean corrupted) {
        super(effect, corrupted, SPIRITS);
    }

    @SubscribeEvent
    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            for (var holder : MaledictRites.RITES.getEntries()) {
                SpiritRiteType other = holder.get();
                if (!(other instanceof SoulbindingRite) && other.getSpirits().equals(SPIRITS)){
                    throw new IllegalStateException("Soulbinding spirit sequence is already used by " + holder.getId());
                }
            }
        });
    }

    /**
     * The rite that owns the given base's wood; {@code null} before the deferred registers are bound.
     */
    @Nullable
    public static SoulbindingRite of(TotemBaseBlockEntity base) {
        var holder = base.corrupted ? MaledictRites.CORRUPTED_SOULBINDING : MaledictRites.SOULBINDING;
        return holder == null ? null : holder.get();
    }

    public record Structure(TotemBaseBlockEntity base, List<TotemPoleBlockEntity> poles, Direction direction) {
    }

    /**
     * Whether the poles above this base are exactly the Soulbinding arrangement: same wood and
     * facing as the base, no missing spirits, no extra poles. Reading the blocks directly is
     * necessary before assembly, because the base only learns its pole positions while assembling.
     */
    @Nullable
    public static Structure inspect(Level level, TotemBaseBlockEntity base) {
        SoulbindingRite rite = of(base);
        if (rite == null) return null;
        if (level.getBlockEntity(base.getBlockPos()) != base) return null;
        List<TotemPoleBlockEntity> poles = new ArrayList<>();
        List<SpiritArcanaType> spirits = new ArrayList<>();
        Direction direction = null;
        BlockPos pos = base.getBlockPos().above();
        while (pos.getY() < level.getMaxBuildHeight()) {
            if (!level.isLoaded(pos)) return null;
            if (!(level.getBlockEntity(pos) instanceof TotemPoleBlockEntity pole)) break;
            Direction facing = pole.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
            if (direction == null) direction = facing;
            if (pole.isSoulwood() != base.corrupted || facing != direction || pole.getSpirit() == null) return null;
            poles.add(pole);
            spirits.add(pole.getSpirit());
            if (poles.size() > rite.getSpirits().size()) return null;
            pos = pos.above();
        }
        return matchesSpirits(rite.getSpirits(), spirits) ? new Structure(base, List.copyOf(poles), direction) : null;
    }

    private static boolean matchesSpirits(List<SpiritHolder<SpiritArcanaType>> expected,
                                          List<SpiritArcanaType> actual) {
        if (expected.size() != actual.size()) return false;
        for (int i = 0; i < expected.size(); i++) if (!expected.get(i).is(actual.get(i))) return false;
        return true;
    }

    @Override
    public ResourceLocation getIcon() {
        // The identifying spirit is the top one (Arcane); Malum's own rite icons are per spirit.
        return ResourceLocation.fromNamespaceAndPath("malum", "textures/vfx/rite/arcane.png");
    }

    @Override
    public String getLangKey() {
        return "malum.gui.rite." + getName();
    }

    @Override
    public String getEffectLangKey() {
        return "malum.gui.book.entry.page.text." + (isCorrupted() ? "corrupt_" : "") + IDENTIFIER + ".hover";
    }

    /**
     * 法典仪式页的正文键。
     *
     * <p>1.8.2 的 {@code SpiritRiteTextPage} 只收仪式本身，正文键由
     * {@link SpiritRiteType#getCodexEntryLangKey()} 给出（基类是
     * {@code malum.gui.book.entry.page.text.<name>}）；1.20.1 那条「同一项仪式两套木头、正文键当参数传」
     * 的写法没有对应物了。腐化那一半的注册名是 {@code corrupted_...}，而语言文件里这两条正文叫
     * {@code maledict_soulbinding_rite} / {@code corrupt_maledict_soulbinding_rite}（沿用 1.20.1 的键名），
     * 所以这里跟 {@link #getEffectLangKey()} 用同一套前缀规则对齐。
     */
    @Override
    public String getCodexEntryLangKey() {
        return "malum.gui.book.entry.page.text." + (isCorrupted() ? "corrupt_" : "") + IDENTIFIER;
    }
}
