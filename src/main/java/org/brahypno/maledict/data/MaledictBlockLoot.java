package org.brahypno.maledict.data;

import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.brahypno.maledict.registry.MaledictBlocks;
import org.brahypno.maledict.registry.MaledictItems;

import java.util.Set;
import java.util.List;

public final class MaledictBlockLoot extends BlockLootSubProvider {
    public MaledictBlockLoot() {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }

    @Override
    protected void generate() {
        dropSelf(MaledictBlocks.CRYING_TAINTED_ROCK.get());
        dropSelf(MaledictBlocks.CRYING_TWISTED_ROCK.get());
        dropSelf(MaledictBlocks.BLIGHTED_WITHER_SKULL.get());
        dropOther(MaledictBlocks.RELIQUARY_ALTAR.get(), com.sammy.malum.registry.common.block.BlockRegistry.RUNIC_TAINTED_ROCK_BRICKS.get());
        dropSelf(MaledictBlocks.MNEMONIC_OBELISK.get());
        dropSelf(MaledictBlocks.SOULWOOD_OBELISK.get());
        dropSelf(MaledictBlocks.WITHERED_LEAF_LITTER.get());
        dropSelf(MaledictBlocks.EXPOSED_SOULWOOD_ROOTS.get());
        dropSelf(MaledictBlocks.FALLEN_SOULWOOD_BRANCHES.get());
        dropSelf(MaledictBlocks.WITHERLIGHT_MUSHROOM.get());
        dropSelf(MaledictBlocks.WITHERLIGHT_LAMP.get());
        malignantLeadOre(MaledictBlocks.TAINTED_MALIGNANT_LEAD_ORE.get());
        malignantLeadOre(MaledictBlocks.TWISTED_MALIGNANT_LEAD_ORE.get());
        dropSelf(MaledictBlocks.LUMENFALL_BISMUTH_ORE.get());
        dropSelf(MaledictBlocks.LUMENFALL_BISMUTH_BLOCK.get());
    }

    /** Malum's Cthonic Gold Ore drops a uniform three or four fragments and has no Fortune bonus. */
    private void malignantLeadOre(Block block) {
        add(block, ore -> createSilkTouchDispatchTable(ore, applyExplosionDecay(block,
                LootItem.lootTableItem(MaledictItems.MALIGNANT_LEAD_FRAGMENT.get())
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 4.0F))))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return List.of(
                MaledictBlocks.CRYING_TAINTED_ROCK.get(),
                MaledictBlocks.CRYING_TWISTED_ROCK.get(),
                MaledictBlocks.BLIGHTED_WITHER_SKULL.get(),
                MaledictBlocks.RELIQUARY_ALTAR.get(),
                MaledictBlocks.MNEMONIC_OBELISK.get(),
                MaledictBlocks.SOULWOOD_OBELISK.get(),
                MaledictBlocks.WITHERED_LEAF_LITTER.get(),
                MaledictBlocks.EXPOSED_SOULWOOD_ROOTS.get(),
                MaledictBlocks.FALLEN_SOULWOOD_BRANCHES.get(),
                MaledictBlocks.WITHERLIGHT_MUSHROOM.get(),
                MaledictBlocks.WITHERLIGHT_LAMP.get(),
                MaledictBlocks.TAINTED_MALIGNANT_LEAD_ORE.get(),
                MaledictBlocks.TWISTED_MALIGNANT_LEAD_ORE.get(),
                MaledictBlocks.LUMENFALL_BISMUTH_ORE.get(),
                MaledictBlocks.LUMENFALL_BISMUTH_BLOCK.get());
    }
}
