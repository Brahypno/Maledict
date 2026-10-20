package org.brahypno.maledict.data.tags;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import com.sammy.malum.registry.common.block.MalumBlocks;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictBlocks;

import java.util.concurrent.CompletableFuture;

public final class MaledictBlockTags extends BlockTagsProvider {
    public MaledictBlockTags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFiles) {
        super(output, lookupProvider, Maledict.MODID, existingFiles);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // Match the realm's upper/lower host layers to vanilla ore variants.
        tag(BlockTags.STONE_ORE_REPLACEABLES).add(MalumBlocks.TAINTED_ROCK.get());
        tag(BlockTags.DEEPSLATE_ORE_REPLACEABLES).add(MalumBlocks.TWISTED_ROCK.get());
        tag(BlockTags.BASE_STONE_OVERWORLD).add(MalumBlocks.TAINTED_ROCK.get(), MalumBlocks.TWISTED_ROCK.get());
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(MaledictBlocks.TAINTED_MALIGNANT_LEAD_ORE.get(),
                MaledictBlocks.TWISTED_MALIGNANT_LEAD_ORE.get());
        tag(BlockTags.NEEDS_IRON_TOOL).add(MaledictBlocks.TAINTED_MALIGNANT_LEAD_ORE.get(),
                MaledictBlocks.TWISTED_MALIGNANT_LEAD_ORE.get());
        tag(BlockTags.MINEABLE_WITH_AXE)
                .add(MaledictBlocks.MNEMONIC_OBELISK.get())
                .add(MaledictBlocks.MNEMONIC_OBELISK_COMPONENT.get())
                .add(MaledictBlocks.SOULWOOD_OBELISK.get())
                .add(MaledictBlocks.SOULWOOD_OBELISK_COMPONENT.get());
    }
}
