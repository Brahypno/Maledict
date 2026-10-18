package org.brahypno.maledict;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.UUID;

/** Registry context shared by actual game scenarios; pure NBT fixtures need no dynamic entries. */
public final class TestSupport {
    public static RegistryAccess registries() {
        var server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? RegistryAccess.EMPTY : server.registryAccess();
    }

    public static Holder<Enchantment> enchantment(ResourceKey<Enchantment> key) {
        return registries().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
    }

    public static ResourceLocation modifierId(UUID id) { return ResourceLocation.parse("maledict:test/" + id); }
    public static ResourceLocation modifierId(ResourceLocation id) { return id; }

    /** Configure a projectile fixture through vanilla's persisted public state. */
    public static void piercing(net.minecraft.world.entity.projectile.AbstractArrow arrow, byte level) {
        var owner = arrow.getOwner();
        var tag = new net.minecraft.nbt.CompoundTag();
        arrow.saveWithoutId(tag);
        tag.putByte("PierceLevel", level);
        arrow.load(tag);
        arrow.setOwner(owner);
    }

    private TestSupport() {}
}
