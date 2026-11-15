package org.brahypno.maledict.common;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;

/**
 * 全服共用的 {@code gameTime}。
 *
 * <p>{@code gameTime} 出自 {@code ServerLevelData}，各维度是同步推进的；拿「谁在哪个维度挨的打」
 * 去当时间戳，一旦同一笔账前后落在不同维度就会对不上，所以这里统一取主世界那一份。
 * 两个跨 tick 的能力（沉辉的 80 tick 倒数、反照的 120 tick 有效期）都只认这一个时钟。
 */
public final class MaledictClock {

    public static long now(Entity entity) {
        MinecraftServer server = entity.getServer();
        return server == null ? entity.level().getGameTime() : now(server);
    }

    public static long now(MinecraftServer server) {
        return server.overworld().getGameTime();
    }

    private MaledictClock() {
    }
}
