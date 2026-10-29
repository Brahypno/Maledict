package org.brahypno.maledict.registry;

import com.sammy.malum.common.block.curiosities.obelisk.ObeliskComponentBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.block.MnemonicObeliskCoreBlock;
import org.brahypno.maledict.common.block.SoulwoodObeliskCoreBlock;
import org.brahypno.maledict.common.realm.RealmGroundcoverBlock;
import org.brahypno.maledict.common.realm.WitherlightMushroomBlock;

public final class MaledictBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Maledict.MODID);

    public static final RegistryObject<Block> WITHERED_LEAF_LITTER = BLOCKS.register("withered_leaf_litter",
            () -> new RealmGroundcoverBlock(BlockBehaviour.Properties.copy(Blocks.MOSS_CARPET)
                    .noCollission().sound(SoundType.AZALEA_LEAVES), 1));
    public static final RegistryObject<Block> EXPOSED_SOULWOOD_ROOTS = BLOCKS.register("exposed_soulwood_roots",
            () -> new RealmGroundcoverBlock(BlockBehaviour.Properties.copy(Blocks.HANGING_ROOTS)
                    .noCollission().sound(SoundType.ROOTS), 4));
    public static final RegistryObject<Block> FALLEN_SOULWOOD_BRANCHES = BLOCKS.register("fallen_soulwood_branches",
            () -> new RealmGroundcoverBlock(BlockBehaviour.Properties.copy(Blocks.HANGING_ROOTS)
                    .noCollission().sound(SoundType.WOOD), 3));
    public static final RegistryObject<Block> WITHERLIGHT_MUSHROOM = BLOCKS.register("witherlight_mushroom",
            () -> new WitherlightMushroomBlock(BlockBehaviour.Properties.copy(Blocks.BROWN_MUSHROOM)
                    .lightLevel(state -> 7).sound(SoundType.FUNGUS)));

    public static final RegistryObject<Block> TAINTED_MALIGNANT_LEAD_ORE = BLOCKS.register("tainted_malignant_lead_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.GOLD_ORE)));
    public static final RegistryObject<Block> TWISTED_MALIGNANT_LEAD_ORE = BLOCKS.register("twisted_malignant_lead_ore",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_GOLD_ORE)));

    private static final BlockBehaviour.Properties OBELISK_PROPERTIES = BlockBehaviour.Properties.of()
                                                                                                 .strength(2.5f, 6.0f)
                                                                                                 .sound(SoundType.DEEPSLATE)
                                                                                                 .noOcclusion();

    public static final RegistryObject<Block> MNEMONIC_OBELISK = BLOCKS.register("mnemonic_obelisk", () ->
            new MnemonicObeliskCoreBlock(OBELISK_PROPERTIES));
    public static final RegistryObject<Block> MNEMONIC_OBELISK_COMPONENT = BLOCKS.register("mnemonic_obelisk_component", () ->
            new ObeliskComponentBlock(OBELISK_PROPERTIES, MaledictItems.MNEMONIC_OBELISK));

    public static final RegistryObject<Block> SOULWOOD_OBELISK = BLOCKS.register("soulwood_obelisk", () -> new SoulwoodObeliskCoreBlock(OBELISK_PROPERTIES));
    public static final RegistryObject<Block> SOULWOOD_OBELISK_COMPONENT =
            BLOCKS.register("soulwood_obelisk_component", () -> new ObeliskComponentBlock(OBELISK_PROPERTIES, MaledictItems.SOULWOOD_OBELISK));

    private MaledictBlocks() {
    }
}
