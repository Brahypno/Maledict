package org.brahypno.maledict.data.tags;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.MaledictTags;
import org.brahypno.maledict.registry.MaledictEntities;

import java.util.concurrent.CompletableFuture;

/**
 * Entity-type tags for the Soulbinding rite. The blacklist is emitted empty on purpose: packs and
 * modpacks extend it, and the base mod excludes nothing by default.
 */
public final class MaledictEntityTypeTags extends TagsProvider<EntityType<?>> {
    public MaledictEntityTypeTags(PackOutput output,
                                  CompletableFuture<HolderLookup.Provider> lookupProvider,
                                  ExistingFileHelper existingFiles) {
        super(output, Registries.ENTITY_TYPE, lookupProvider, Maledict.MODID, existingFiles);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(MaledictTags.SOUL_BINDING_BLACKLIST);
        // 1.20.1 用 MobType.UNDEAD 判定亡灵，1.21 换成实体类型标签，而且标签是静态的——没有"重塑之后
        // 才变亡灵"这种动态类型。本模组的凋灵与凋灵骷髅都属于凋灵族，按已确认口径两个都常驻
        // #minecraft:undead（腐枯凋灵在 1.20.1 里只有重塑后才算亡灵，这里从一阶段起就算）。
        // 少了这一条，乌鸦不再把它们当亡灵（不再追、扑击与羽毛的 x2 失效）、腐枯凋灵的侧头会打自己
        // 召唤的骷髅，inverted_healing_and_harm（直接引用 #undead）与 Smite 也一并失效。
        tag(EntityTypeTags.UNDEAD).add(
                ResourceKey.create(Registries.ENTITY_TYPE, MaledictEntities.WEEPING_WITHER.getId()),
                ResourceKey.create(Registries.ENTITY_TYPE, MaledictEntities.WEEPING_WITHER_SKELETON.getId()));
    }
}
