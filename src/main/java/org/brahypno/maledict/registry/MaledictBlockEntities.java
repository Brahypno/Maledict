package org.brahypno.maledict.registry;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.block.MnemonicObeliskBlockEntity;
import org.brahypno.maledict.common.block.SoulwoodObeliskBlockEntity;

public final class MaledictBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Maledict.MODID);

    public static final RegistryObject<BlockEntityType<MnemonicObeliskBlockEntity>> MNEMONIC_OBELISK =
            BLOCK_ENTITY_TYPES.register("mnemonic_obelisk", () -> BlockEntityType.Builder.of(
                    MnemonicObeliskBlockEntity::new, MaledictBlocks.MNEMONIC_OBELISK.get()).build(null));

    public static final RegistryObject<BlockEntityType<SoulwoodObeliskBlockEntity>> SOULWOOD_OBELISK =
            BLOCK_ENTITY_TYPES.register("soulwood_obelisk", () -> BlockEntityType.Builder.of(
                    SoulwoodObeliskBlockEntity::new, MaledictBlocks.SOULWOOD_OBELISK.get()).build(null));

    private MaledictBlockEntities() {
    }
}
