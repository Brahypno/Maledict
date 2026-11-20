package org.brahypno.maledict.common.ritual.invocation;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import org.brahypno.maledict.registry.MaledictRecipeTypes;

import java.util.stream.IntStream;
import java.util.stream.Stream;

/** Bucket data also contains health, age and other fields; only Variant determines eligibility. */
public final class OrdinaryAxolotlBucketIngredient implements ICustomIngredient {
    public static final OrdinaryAxolotlBucketIngredient INSTANCE = new OrdinaryAxolotlBucketIngredient();
    public static final MapCodec<OrdinaryAxolotlBucketIngredient> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public boolean test(ItemStack stack) {
        return stack.is(Items.AXOLOTL_BUCKET)
                && stack.getOrDefault(DataComponents.BUCKET_ENTITY_DATA, CustomData.EMPTY).copyTag().getInt("Variant") != 4;
    }

    @Override
    public Stream<ItemStack> getItems() {
        return IntStream.range(0, 4).mapToObj(variant -> {
            ItemStack bucket = new ItemStack(Items.AXOLOTL_BUCKET);
            CompoundTag data = new CompoundTag();
            data.putInt("Variant", variant);
            bucket.set(DataComponents.BUCKET_ENTITY_DATA, CustomData.of(data));
            return bucket;
        });
    }

    @Override public boolean isSimple() { return false; }
    @Override public IngredientType<?> getType() { return MaledictRecipeTypes.ORDINARY_AXOLOTL_BUCKET.get(); }

    private OrdinaryAxolotlBucketIngredient() {}
}
