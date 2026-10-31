package org.brahypno.maledict.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.realm.reliquary.InvertedReliquaryPiece;
import org.brahypno.maledict.common.realm.reliquary.InvertedReliquaryStructure;

public final class MaledictStructures {
    public static final DeferredRegister<StructureType<?>> TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, Maledict.MODID);
    public static final DeferredRegister<StructurePieceType> PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, Maledict.MODID);

    public static final RegistryObject<StructureType<InvertedReliquaryStructure>> INVERTED_RELIQUARY =
            TYPES.register("inverted_reliquary", () -> () -> InvertedReliquaryStructure.CODEC);
    public static final RegistryObject<StructurePieceType> RELIQUARY_PIECE =
            PIECES.register("inverted_reliquary", () -> InvertedReliquaryPiece::new);

    private MaledictStructures() {
    }
}
