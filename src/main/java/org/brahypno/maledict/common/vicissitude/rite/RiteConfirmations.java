package org.brahypno.maledict.common.vicissitude.rite;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Short-lived server session state; targets contain values, never world or player references. */
final class RiteConfirmations {
    record Target(String dimension, long position, String rite) {}
    private record Pending(Target target, long firstTick) {}
    enum Result { WARN, DUPLICATE, CONFIRMED }

    private final Map<UUID, Pending> pending = new HashMap<>();

    Result click(UUID player, Target target, long now) {
        Pending previous = pending.get(player);
        if (previous != null && previous.target.equals(target) && now < previous.firstTick + 200) {
            if (now <= previous.firstTick) return Result.DUPLICATE;
            pending.remove(player);
            return Result.CONFIRMED;
        }
        pending.put(player, new Pending(target, now));
        return Result.WARN;
    }

    void remove(UUID player) {
        pending.remove(player);
    }

    void expire(long now) {
        pending.values().removeIf(record -> now >= record.firstTick + 200);
    }

    void clear() {
        pending.clear();
    }
}
