package org.brahypno.maledict.data.tags;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import com.sammy.malum.registry.common.MalumTags;
import com.sammy.malum.registry.common.block.MalumBlocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.MaledictTags;
import org.brahypno.maledict.registry.MaledictBlocks;

import java.util.concurrent.CompletableFuture;

public final class MaledictBlockTags extends BlockTagsProvider {
    public MaledictBlockTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFiles) {
        super(output, lookupProvider, Maledict.MODID, existingFiles);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // 哭泣石照 Malum 的污秽石/扭曲石登记：可镐采、并计入 Malum 自身的岩石标签。
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(MaledictBlocks.CRYING_TAINTED_ROCK.get(),
                MaledictBlocks.CRYING_TWISTED_ROCK.get());
        // 祭坛是深板岩砖的仿制品，用镐挖。
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(MaledictBlocks.RELIQUARY_ALTAR.get());
        tag(MalumTags.BlockTags.TAINTED_ROCK).add(MaledictBlocks.CRYING_TAINTED_ROCK.get());
        tag(MalumTags.BlockTags.TWISTED_ROCK).add(MaledictBlocks.CRYING_TWISTED_ROCK.get());
        tag(BlockTags.MINEABLE_WITH_HOE).add(MaledictBlocks.WITHERED_LEAF_LITTER.get());
        // These covers have no collision, so footsteps must explicitly select the block above the ground.
        tag(BlockTags.INSIDE_STEP_SOUND_BLOCKS).add(MaledictBlocks.WITHERED_LEAF_LITTER.get());
        tag(BlockTags.COMBINATION_STEP_SOUND_BLOCKS).add(MaledictBlocks.EXPOSED_SOULWOOD_ROOTS.get(),
                MaledictBlocks.FALLEN_SOULWOOD_BRANCHES.get());
        // Match the realm's upper/lower host layers to vanilla ore variants.
        tag(BlockTags.STONE_ORE_REPLACEABLES).add(MalumBlocks.TAINTED_ROCK.get());
        tag(BlockTags.DEEPSLATE_ORE_REPLACEABLES).add(MalumBlocks.TWISTED_ROCK.get());
        tag(BlockTags.BASE_STONE_OVERWORLD).add(MalumBlocks.TAINTED_ROCK.get(), MalumBlocks.TWISTED_ROCK.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(MaledictBlocks.TAINTED_MALIGNANT_LEAD_ORE.get(),
                MaledictBlocks.TWISTED_MALIGNANT_LEAD_ORE.get());
        tag(BlockTags.NEEDS_IRON_TOOL).add(MaledictBlocks.TAINTED_MALIGNANT_LEAD_ORE.get(),
                MaledictBlocks.TWISTED_MALIGNANT_LEAD_ORE.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(MaledictBlocks.LUMENFALL_BISMUTH_ORE.get(),
                MaledictBlocks.LUMENFALL_BISMUTH_BLOCK.get());
        tag(BlockTags.NEEDS_DIAMOND_TOOL).add(MaledictBlocks.LUMENFALL_BISMUTH_ORE.get(),
                MaledictBlocks.LUMENFALL_BISMUTH_BLOCK.get());
        tag(Tags.Blocks.ORES).add(MaledictBlocks.LUMENFALL_BISMUTH_ORE.get());
        tag(Tags.Blocks.STORAGE_BLOCKS).add(MaledictBlocks.LUMENFALL_BISMUTH_BLOCK.get());
        tag(TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores/lumenfall_bismuth")))
                .add(MaledictBlocks.LUMENFALL_BISMUTH_ORE.get());
        tag(TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "storage_blocks/lumenfall_bismuth")))
                .add(MaledictBlocks.LUMENFALL_BISMUTH_BLOCK.get());
        tag(MaledictTags.BLIGHTED_REALM_ORES).add(
                MaledictBlocks.TAINTED_MALIGNANT_LEAD_ORE.get(),
                MaledictBlocks.TWISTED_MALIGNANT_LEAD_ORE.get(),
                MaledictBlocks.LUMENFALL_BISMUTH_ORE.get());
        tag(BlockTags.MINEABLE_WITH_AXE)
                .add(MaledictBlocks.WITHERLIGHT_LAMP.get())
                .add(MaledictBlocks.EXPOSED_SOULWOOD_ROOTS.get(), MaledictBlocks.FALLEN_SOULWOOD_BRANCHES.get())
                .add(MaledictBlocks.MNEMONIC_OBELISK.get())
                .add(MaledictBlocks.MNEMONIC_OBELISK_COMPONENT.get())
                .add(MaledictBlocks.SOULWOOD_OBELISK.get())
                .add(MaledictBlocks.SOULWOOD_OBELISK_COMPONENT.get());
    }
}
