package org.brahypno.maledict.data;

import com.sammy.malum.registry.common.block.BlockRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.MaledictTags;
import org.brahypno.maledict.registry.MaledictBlocks;

import java.util.concurrent.CompletableFuture;

public final class MaledictBlockTags extends BlockTagsProvider {
    private static final TagKey<Block> FORGE_ORES_LUMENFALL_BISMUTH = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("forge", "ores/lumenfall_bismuth"));
    private static final TagKey<Block> FORGE_STORAGE_BLOCKS_LUMENFALL_BISMUTH = TagKey.create(Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("forge", "storage_blocks/lumenfall_bismuth"));

    public MaledictBlockTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFiles) {
        super(output, lookupProvider, Maledict.MODID, existingFiles);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(MaledictBlocks.CRYING_TAINTED_ROCK.get(),
                MaledictBlocks.CRYING_TWISTED_ROCK.get());
        tag(com.sammy.malum.registry.common.block.BlockTagRegistry.TAINTED_ROCK)
                .add(MaledictBlocks.CRYING_TAINTED_ROCK.get());
        tag(com.sammy.malum.registry.common.block.BlockTagRegistry.TWISTED_ROCK)
                .add(MaledictBlocks.CRYING_TWISTED_ROCK.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(MaledictBlocks.RELIQUARY_ALTAR.get());
        tag(BlockTags.MINEABLE_WITH_HOE).add(MaledictBlocks.WITHERED_LEAF_LITTER.get());
        // These covers have no collision, so footsteps must explicitly select the block above the ground.
        tag(BlockTags.INSIDE_STEP_SOUND_BLOCKS).add(MaledictBlocks.WITHERED_LEAF_LITTER.get());
        tag(BlockTags.COMBINATION_STEP_SOUND_BLOCKS).add(MaledictBlocks.EXPOSED_SOULWOOD_ROOTS.get(),
                MaledictBlocks.FALLEN_SOULWOOD_BRANCHES.get());
        // Match the realm's upper/lower host layers to vanilla ore variants.
        tag(BlockTags.STONE_ORE_REPLACEABLES).add(BlockRegistry.TAINTED_ROCK.get());
        tag(BlockTags.DEEPSLATE_ORE_REPLACEABLES).add(BlockRegistry.TWISTED_ROCK.get());
        tag(BlockTags.BASE_STONE_OVERWORLD).add(BlockRegistry.TAINTED_ROCK.get(), BlockRegistry.TWISTED_ROCK.get());
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
        tag(FORGE_ORES_LUMENFALL_BISMUTH).add(MaledictBlocks.LUMENFALL_BISMUTH_ORE.get());
        tag(FORGE_STORAGE_BLOCKS_LUMENFALL_BISMUTH).add(MaledictBlocks.LUMENFALL_BISMUTH_BLOCK.get());
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
