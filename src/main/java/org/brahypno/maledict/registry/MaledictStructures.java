package org.brahypno.maledict.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.realm.reliquary.InvertedReliquaryPiece;
import org.brahypno.maledict.common.realm.reliquary.InvertedReliquaryStructure;

/**
 * 结构类型与 piece 类型的注册表句柄（1.20.1 的 {@code MaledictStructures} 在 1.21 原本缺席，随第 11 组补上）。
 * <p>
 * 两张注册表在 1.21 里都还在原位置：{@code minecraft:structure_type} 登记 {@link StructureType}，
 * 结构 JSON 的 {@code "type"} 字段读它；{@code minecraft:structure_piece} 登记 {@link StructurePieceType}，
 * 存档里 piece 的反序列化靠它找构造器。
 */
public final class MaledictStructures {
    public static final DeferredRegister<StructureType<?>> TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, Maledict.MODID);
    public static final DeferredRegister<StructurePieceType> PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, Maledict.MODID);

    /** 1.21 的 {@code StructureType} 是单方法接口（{@code MapCodec codec()}），所以两层 lambda 照旧成立。 */
    public static final DeferredHolder<StructureType<?>, StructureType<InvertedReliquaryStructure>> INVERTED_RELIQUARY =
            TYPES.register("inverted_reliquary", () -> () -> InvertedReliquaryStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> RELIQUARY_PIECE =
            PIECES.register("inverted_reliquary", () -> InvertedReliquaryPiece::new);

    private MaledictStructures() {
    }
}
