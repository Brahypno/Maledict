package org.brahypno.maledict.common.realm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/** World-owned, bidirectional well pairs; no player's journey can overwrite another's route. */
public final class WeepingWellLinks extends SavedData {
    private static final String FILE_NAME = "maledict_weeping_wells";
    private final Map<GlobalPos, Link> links = new HashMap<>();

    public record Endpoint(GlobalPos core, Vec3 landing) {
        public Endpoint(ResourceKey<Level> dimension, BlockPos core, Vec3 landing) {
            this(GlobalPos.of(dimension, core.immutable()), landing);
        }
    }

    public record Link(Endpoint overworld, Endpoint realm) {
        public Endpoint other(GlobalPos source) {
            if (overworld.core().equals(source)) return realm;
            if (realm.core().equals(source)) return overworld;
            throw new IllegalArgumentException("Well is not part of this link: " + source);
        }
    }

    public static WeepingWellLinks get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(WeepingWellLinks::new, WeepingWellLinks::load, null), FILE_NAME);
    }

    public Link find(GlobalPos core) {
        return links.get(core);
    }

    public boolean hasWellNear(ResourceKey<Level> dimension, BlockPos center, int separation) {
        long squared = (long) separation * separation;
        return links.keySet().stream().anyMatch(pos -> pos.dimension().equals(dimension)
                && horizontalDistanceSquared(pos.pos(), center) < squared);
    }

    public void replaceEndpoint(Link old, Endpoint replacement) {
        Endpoint overworld = old.overworld();
        Endpoint realm = old.realm();
        if (replacement.core().dimension().equals(Level.OVERWORLD)) overworld = replacement;
        else if (replacement.core().dimension().equals(BlightedRealm.LEVEL)) realm = replacement;
        else throw new IllegalArgumentException("Replacement well must be in one of the paired dimensions");
        Link claimed = links.get(replacement.core());
        if (claimed != null && !claimed.equals(old)) throw new IllegalStateException("Cannot replace another pair's well");
        links.remove(old.overworld().core());
        links.remove(old.realm().core());
        pair(overworld, realm);
    }

    private static long horizontalDistanceSquared(BlockPos first, BlockPos second) {
        long x = (long) first.getX() - second.getX();
        long z = (long) first.getZ() - second.getZ();
        return x * x + z * z;
    }

    public void pair(Endpoint overworld, Endpoint realm) {
        if (!overworld.core().dimension().equals(Level.OVERWORLD)
                || !realm.core().dimension().equals(BlightedRealm.LEVEL)) {
            throw new IllegalArgumentException("A well pair must connect overworld and blighted realm");
        }
        if (links.containsKey(overworld.core()) || links.containsKey(realm.core())) {
            throw new IllegalStateException("A paired well cannot be rebound");
        }
        Link link = new Link(overworld, realm);
        links.put(overworld.core(), link);
        links.put(realm.core(), link);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ListTag pairs = new ListTag();
        for (Link link : new HashSet<>(links.values())) {
            CompoundTag entry = new CompoundTag();
            entry.put("Overworld", encode(link.overworld()));
            entry.put("Realm", encode(link.realm()));
            pairs.add(entry);
        }
        tag.put("Pairs", pairs);
        return tag;
    }

    public static WeepingWellLinks load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        WeepingWellLinks data = new WeepingWellLinks();
        for (Tag value : tag.getList("Pairs", Tag.TAG_COMPOUND)) {
            CompoundTag pair = (CompoundTag) value;
            Endpoint overworld = decode(pair.getCompound("Overworld"));
            Endpoint realm = decode(pair.getCompound("Realm"));
            data.pair(overworld, realm);
        }
        return data;
    }

    private static CompoundTag encode(Endpoint endpoint) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Dimension", endpoint.core().dimension().location().toString());
        tag.putLong("Core", endpoint.core().pos().asLong());
        tag.putDouble("X", endpoint.landing().x);
        tag.putDouble("Y", endpoint.landing().y);
        tag.putDouble("Z", endpoint.landing().z);
        return tag;
    }

    private static Endpoint decode(CompoundTag tag) {
        return new Endpoint(ResourceKey.create(Registries.DIMENSION,
                ResourceLocation.parse(tag.getString("Dimension"))), BlockPos.of(tag.getLong("Core")),
                new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z")));
    }
}
