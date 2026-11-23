package org.brahypno.maledict.registry;

import org.brahypno.maledict.common.realm.RealmGroundcoverBlock;
import org.brahypno.maledict.common.realm.WitherlightMushroomBlock;

import com.sammy.malum.common.block.curiosities.obelisk.ObeliskComponentBlock;
import com.sammy.malum.registry.common.block.MalumBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CryingObsidianBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.block.MnemonicObeliskCoreBlock;
import org.brahypno.maledict.common.block.SoulwoodObeliskCoreBlock;
import org.brahypno.maledict.common.entity.wither.BlightedWitherSkullBlock;
import org.brahypno.maledict.common.realm.reliquary.ReliquaryAltarBlock;

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

    public static final DeferredHolder<Block, Block> LUMENFALL_BISMUTH_ORE = BLOCKS.register("lumenfall_bismuth_ore",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.ANCIENT_DEBRIS)
                    .mapColor(MapColor.STONE)));
    public static final DeferredHolder<Block, Block> LUMENFALL_BISMUTH_BLOCK = BLOCKS.register("lumenfall_bismuth_block",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK)
                    .mapColor(MapColor.METAL)));

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

    // 哭泣石与腐枯头颅构成腐枯凋灵的召唤结构；属性照抄 Malum 的污秽石/扭曲石与权当原版头颅。
    public static final DeferredHolder<Block, Block> CRYING_TAINTED_ROCK = BLOCKS.register("crying_tainted_rock",
            () -> new CryingObsidianBlock(BlockBehaviour.Properties.ofFullCopy(MalumBlocks.TAINTED_ROCK.get())));
    public static final DeferredHolder<Block, Block> CRYING_TWISTED_ROCK = BLOCKS.register("crying_twisted_rock",
            () -> new CryingObsidianBlock(BlockBehaviour.Properties.ofFullCopy(MalumBlocks.TWISTED_ROCK.get())));
    public static final DeferredHolder<Block, Block> BLIGHTED_WITHER_SKULL = BLOCKS.register("blighted_wither_skull",
            () -> new BlightedWitherSkullBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.WITHER_SKELETON_SKULL)
                    .noOcclusion()));
    // 倒置圣物匣中央的祭坛。1.20.1 就没有给它注册方块物品（只能从结构里遇到），这里保持同构：
    // 因此也没有 models/item，战利品表照旧只掉 Malum 的符文污秽石砖。
    public static final DeferredHolder<Block, Block> RELIQUARY_ALTAR = BLOCKS.register("reliquary_altar",
            () -> new ReliquaryAltarBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_BRICKS)
                    .lightLevel(state -> 8)));

    private MaledictBlocks() {
    }
}
