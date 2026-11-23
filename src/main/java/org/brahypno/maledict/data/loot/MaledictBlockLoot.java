package org.brahypno.maledict.data.loot;

import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import com.sammy.malum.registry.common.block.MalumBlocks;
import org.brahypno.maledict.registry.MaledictBlocks;
import org.brahypno.maledict.registry.MaledictItems;

import java.util.Set;
import java.util.List;

public final class MaledictBlockLoot extends BlockLootSubProvider {
    public MaledictBlockLoot(net.minecraft.core.HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        dropSelf(MaledictBlocks.CRYING_TAINTED_ROCK.get());
        dropSelf(MaledictBlocks.CRYING_TWISTED_ROCK.get());
        dropSelf(MaledictBlocks.BLIGHTED_WITHER_SKULL.get());
        // 祭坛本身没有方块物品：用掉或挖掉之后掉的是 Malum 的符文污秽石砖，与 1.20.1 一致。
        dropOther(MaledictBlocks.RELIQUARY_ALTAR.get(), MalumBlocks.RUNIC_TAINTED_ROCK_BRICKS.get());
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

    /**
     * 恶念之铅矿沿用 1.20.1 的写法（照抄 Malum 的阴金矿）：精准采集掉方块本身，否则均匀 3~4 个碎片
     * 并按爆炸衰减，<b>不吃时运</b>。原版的 {@code createOreDrop} 是 1 个 + {@code minecraft:ore_drops}
     * 函数，基础产量会从 3~4 掉到 1，碎片合成锭的成本随之翻几倍。
     */
    private void malignantLeadOre(Block block) {
        add(block, ore -> createSilkTouchDispatchTable(ore, (LootPoolEntryContainer.Builder<?>) applyExplosionDecay(block,
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
