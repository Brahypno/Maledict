package org.brahypno.maledict.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import com.sammy.malum.registry.common.block.BlockRegistry;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictBlocks;

import java.util.concurrent.CompletableFuture;

public final class MaledictBlockTags extends BlockTagsProvider {
    public MaledictBlockTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFiles) {
        super(output, lookupProvider, Maledict.MODID, existingFiles);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
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
        tag(BlockTags.MINEABLE_WITH_AXE)
                .add(MaledictBlocks.WITHERLIGHT_LAMP.get())
                .add(MaledictBlocks.EXPOSED_SOULWOOD_ROOTS.get(), MaledictBlocks.FALLEN_SOULWOOD_BRANCHES.get())
                .add(MaledictBlocks.MNEMONIC_OBELISK.get())
                .add(MaledictBlocks.MNEMONIC_OBELISK_COMPONENT.get())
                .add(MaledictBlocks.SOULWOOD_OBELISK.get())
                .add(MaledictBlocks.SOULWOOD_OBELISK_COMPONENT.get());
    }
}
