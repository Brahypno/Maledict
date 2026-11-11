package org.brahypno.maledict.common.soulbinding;

import com.mojang.logging.LogUtils;
import net.minecraft.advancements.Advancement;
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
 */
public final class Soulbinding {
    /** Advancement id, also the criterion name used by the data file. */
    public static final ResourceLocation ADVANCEMENT =
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "soulbinding_rite");

    private static final String CRITERION = "impossible";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static boolean knows(ServerPlayer player) {
        Advancement advancement = advancement(player.server);
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    /** Awards the advancement once; returns whether this call actually granted it. */
    public static boolean award(ServerPlayer player) {
        if (knows(player)) {
            return false;
        }
        Advancement advancement = advancement(player.server);
        if (advancement == null) {
            LOGGER.warn("Soulbinding: advancement {} is missing from this datapack", ADVANCEMENT);
            return false;
        }
        player.getAdvancements().award(advancement, CRITERION);
        // Forge refuses advancement awards for fake players; only report a grant when the progress
        // really is done, so the scroll is not consumed without teaching the rite.
        return player.getAdvancements().getOrStartProgress(advancement).isDone();
    }

    @Nullable
    private static Advancement advancement(MinecraftServer server) {
        return server.getAdvancements().getAdvancement(ADVANCEMENT);
    }

    private Soulbinding() {
    }
}
