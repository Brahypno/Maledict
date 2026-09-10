package org.brahypno.maledict.common.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * World-scoped authority, stored in data/maledict_vicissitude.dat in the overworld.
 * Entity.load (including ForgeCaps) cannot deserialize this independent file.
 * Updates mark the data dirty; Minecraft writes it during world saves, not per hit.
 * Dead UUID records are retained so loading an older entity copy cannot revive them.
 */
final class VicissitudeVitalityLedger extends SavedData {
    private static final String FILE_NAME = "maledict_vicissitude";
    private final Map<UUID, VicissitudeVitality> entries = new HashMap<>();

    static VicissitudeVitalityLedger get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                VicissitudeVitalityLedger::load, VicissitudeVitalityLedger::new, FILE_NAME);
    }

    VicissitudeVitality find(UUID identity) {
        return entries.get(identity);
    }

    void commit(UUID identity, VicissitudeVitality value) {
        if (!value.equals(entries.put(identity, value))) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag records = new ListTag();
        entries.forEach((identity, value) -> {
            CompoundTag record = encode(value);
            record.putUUID("Identity", identity);
            records.add(record);
        });
        tag.put("Entries", records);
        return tag;
    }

    static VicissitudeVitalityLedger load(CompoundTag tag) {
        VicissitudeVitalityLedger ledger = new VicissitudeVitalityLedger();
        for (Tag entry : tag.getList("Entries", Tag.TAG_COMPOUND)) {
            CompoundTag record = (CompoundTag) entry;
            ledger.entries.put(record.getUUID("Identity"), decode(record));
        }
        return ledger;
    }

    static CompoundTag encode(VicissitudeVitality value) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("Current", value.current());
        tag.putFloat("Maximum", value.maximum());
        tag.putLong("NextHit", value.nextHit());
        tag.putInt("KillAttempts", value.killAttempts());
        tag.putLong("LastKillTick", value.lastKillTick());
        return tag;
    }

    static VicissitudeVitality decode(CompoundTag tag) {
        return new VicissitudeVitality(tag.getFloat("Current"), tag.getFloat("Maximum"),
                tag.getLong("NextHit"), tag.getInt("KillAttempts"),
                tag.contains("LastKillTick", Tag.TAG_LONG) ? tag.getLong("LastKillTick") : Long.MIN_VALUE);
    }
}
