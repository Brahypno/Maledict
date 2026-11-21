package org.brahypno.maledict.common.ritual.soulbinding;

import com.mojang.logging.LogUtils;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.brahypno.maledict.Maledict;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

/**
 * Permission gate for the Rite of Soulbinding.
 *
 * <p>The advancement is a technical one: it has no display and no parent, so learning the rite
 * never opens an empty advancement tab. The scroll is the only in-game way to earn it, and the
 * rite checks it again on the server before every arm or release.
 *
 * <p>1.21 的 {@code ServerAdvancementManager} 按 id 返回 {@link AdvancementHolder}（1.20.1 返回
 * {@code Advancement}），{@code PlayerAdvancements} 的 award/查询也都收 holder。
 */
public final class Soulbinding {
    /**
     * Advancement id, also the criterion name used by the data file.
     */
    public static final ResourceLocation ADVANCEMENT =
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "soulbinding_rite");

    private static final String CRITERION = "impossible";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static boolean knows(ServerPlayer player) {
        AdvancementHolder advancement = advancement(player.server);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    /**
     * Awards the advancement once; returns whether this call actually granted it.
     */
    public static boolean award(ServerPlayer player) {
        if (knows(player)){
            return false;
        }
        AdvancementHolder advancement = advancement(player.server);
        if (advancement == null){
            LOGGER.warn("Soulbinding: advancement {} is missing from this datapack", ADVANCEMENT);
            return false;
        }
        player.getAdvancements().award(advancement, CRITERION);
        // Fake players never get advancement progress; only report a grant when the progress really
        // is done, so the scroll is not consumed without teaching the rite.
        return player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    @Nullable
    private static AdvancementHolder advancement(MinecraftServer server) {
        return server.getAdvancements().get(ADVANCEMENT);
    }

    private Soulbinding() {
    }
}
