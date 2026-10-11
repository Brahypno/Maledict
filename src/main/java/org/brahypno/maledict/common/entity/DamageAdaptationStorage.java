package org.brahypno.maledict.common.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Saves remembered messages and their counters together, in most-recent-hit order. */
final class DamageAdaptationStorage {
    private DamageAdaptationStorage() {}

    static ListTag save(DamageAdaptation adaptation) {
        ListTag entries = new ListTag();
        Map<String, Integer> hits = adaptation.hitCounts();
        for (String message : adaptation.snapshot()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Message", message);
            entry.putInt("Hits", hits.get(message));
            entries.add(entry);
        }
        return entries;
    }

    static void load(DamageAdaptation adaptation, ListTag entries) {
        List<String> window = new ArrayList<>();
        Map<String, Integer> hits = new LinkedHashMap<>();
        for (Tag tag : entries) {
            if (tag instanceof CompoundTag entry) {
                String message = entry.getString("Message");
                window.add(message);
                hits.putIfAbsent(message, Math.max(1, entry.getInt("Hits")));
            }
        }
        // Legacy entries have no counter and retain the old one-hit fallback.
        adaptation.restore(window, hits);
    }
}
