package org.brahypno.maledict.registry;

import org.brahypno.maledict.common.block.RealmGroundcoverBlock;
import org.brahypno.maledict.common.block.WitherlightMushroomBlock;

import com.sammy.malum.common.block.curiosities.obelisk.ObeliskComponentBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.block.MnemonicObeliskCoreBlock;
import org.brahypno.maledict.common.block.SoulwoodObeliskCoreBlock;

public final class MaledictBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(BuiltInRegistries.BLOCK, Maledict.MODID);

    public static final DeferredHolder<Block, Block> WITHERED_LEAF_LITTER = BLOCKS.register("withered_leaf_litter",
            () -> new RealmGroundcoverBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.MOSS_CARPET)
                    .noCollission().sound(SoundType.AZALEA_LEAVES), 1));
    public static final DeferredHolder<Block, Block> EXPOSED_SOULWOOD_ROOTS = BLOCKS.register("exposed_soulwood_roots",
            () -> new RealmGroundcoverBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.HANGING_ROOTS)
                    .noCollission().sound(SoundType.ROOTS), 4));
    public static final DeferredHolder<Block, Block> FALLEN_SOULWOOD_BRANCHES = BLOCKS.register("fallen_soulwood_branches",
            () -> new RealmGroundcoverBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.HANGING_ROOTS)
                    .noCollission().sound(SoundType.WOOD), 3));
    public static final DeferredHolder<Block, Block> WITHERLIGHT_MUSHROOM = BLOCKS.register("witherlight_mushroom",
            () -> new WitherlightMushroomBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM)
                    .lightLevel(state -> 7).sound(SoundType.FUNGUS)));
    public static final DeferredHolder<Block, Block> WITHERLIGHT_LAMP = BLOCKS.register("witherlight_lamp",
            () -> new RealmGroundcoverBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM)
                    .noCollission().noOcclusion().strength(0.5F).lightLevel(state -> 14).sound(SoundType.WOOD), 13));

    public static final DeferredHolder<Block, Block> TAINTED_MALIGNANT_LEAD_ORE = BLOCKS.register("tainted_malignant_lead_ore",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_ORE)));
    public static final DeferredHolder<Block, Block> TWISTED_MALIGNANT_LEAD_ORE = BLOCKS.register("twisted_malignant_lead_ore",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_GOLD_ORE)));

    private static final BlockBehaviour.Properties OBELISK_PROPERTIES = BlockBehaviour.Properties.of()
                                                                                                 .strength(2.5f, 6.0f)
                                                                                                 .sound(SoundType.DEEPSLATE)
                                                                                                 .noOcclusion();

    public static final DeferredHolder<Block, Block> MNEMONIC_OBELISK = BLOCKS.register("mnemonic_obelisk", () ->
            new MnemonicObeliskCoreBlock(OBELISK_PROPERTIES));
    public static final DeferredHolder<Block, Block> MNEMONIC_OBELISK_COMPONENT = BLOCKS.register("mnemonic_obelisk_component", () ->
            new ObeliskComponentBlock(OBELISK_PROPERTIES, MaledictItems.MNEMONIC_OBELISK));

    public static final DeferredHolder<Block, Block> SOULWOOD_OBELISK =
            BLOCKS.register("soulwood_obelisk", () -> new SoulwoodObeliskCoreBlock(OBELISK_PROPERTIES));
    public static final DeferredHolder<Block, Block> SOULWOOD_OBELISK_COMPONENT =
            BLOCKS.register("soulwood_obelisk_component", () -> new ObeliskComponentBlock(OBELISK_PROPERTIES, MaledictItems.SOULWOOD_OBELISK));

    private MaledictBlocks() {
    }
}
