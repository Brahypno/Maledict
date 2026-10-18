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

public final class MaledictBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Maledict.MODID);

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
