package org.brahypno.maledict.data.tags;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.MaledictDamageTypes;

import java.util.concurrent.CompletableFuture;

/**
 * 两种自有伤害类型的标签镜像。
 *
 * <p>{@code c:is_magic} 是 Lodestone 判定魔法伤害的入口（{@code LodestoneDamageTypeTags.IS_MAGIC}，
 * 1.8.2 的 jar 里那份文件本身只列了 {@code minecraft:magic} 与 {@code minecraft:indirect_magic}），
 * 沉辉与反照既然算魔法，就得自己挂上去 —— 它们是自有的 {@link DamageType}，不会沾原版那两个名字的光。
 *
 * <p>生成物是不带 {@code replace} 的合并片段：Minecraft 会把不同数据包里同名 tag 的值并起来，
 * 所以这里不需要、也不该重抄 Lodestone 的成员。
 */
public final class MaledictDamageTypeTags extends TagsProvider<DamageType> {
    /** Lodestone 判定魔法伤害用的通用标签。 */
    private static final TagKey<DamageType> C_IS_MAGIC = TagKey.create(Registries.DAMAGE_TYPE,
                                                                       ResourceLocation.fromNamespaceAndPath("c", "is_magic"));

    public MaledictDamageTypeTags(PackOutput output,
                                  CompletableFuture<HolderLookup.Provider> lookupProvider,
                                  ExistingFileHelper existingFiles) {
        super(output, Registries.DAMAGE_TYPE, lookupProvider, Maledict.MODID, existingFiles);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(C_IS_MAGIC).add(MaledictDamageTypes.LUMENFALL_RELEASE, MaledictDamageTypes.REFULGENCE);
    }

    @Override
    public String getName() {
        return "Maledict damage type tags";
    }
}
