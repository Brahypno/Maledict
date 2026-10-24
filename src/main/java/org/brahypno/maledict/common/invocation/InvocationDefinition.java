package org.brahypno.maledict.common.invocation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.brahypno.maledict.registry.MaledictRecipeTypes;

import java.util.Locale;

/** A synced data-pack definition, not a crafting-table recipe. Its ID lives in RecipeHolder. */
public record InvocationDefinition(ResourceLocation entity, String variant,
                                   boolean soulwood, Profile profile, int tint) implements Recipe<RecipeInput> {
    public enum Profile { GROUND, FLYING, WATER, ATTACHED }

    private static final Codec<Boolean> MEDIUM = Codec.STRING.comapFlatMap(
            value -> switch (value) {
                case "runewood" -> DataResult.success(false);
                case "soulwood" -> DataResult.success(true);
                default -> DataResult.error(() -> "Unknown invocation medium: " + value);
            }, value -> value ? "soulwood" : "runewood");
    private static final Codec<Profile> PROFILE = Codec.STRING.comapFlatMap(value -> {
        try {
            return DataResult.success(Profile.valueOf(value.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            return DataResult.error(() -> "Unknown invocation spawn_profile: " + value);
        }
    }, value -> value.name().toLowerCase(Locale.ROOT));
    private static final Codec<Integer> TINT = Codec.STRING.comapFlatMap(
            value -> value.matches("#[0-9a-fA-F]{6}")
                    ? DataResult.success(Integer.parseInt(value.substring(1), 16))
                    : DataResult.error(() -> "Invocation tint must be #RRGGBB"),
            value -> String.format(Locale.ROOT, "#%06X", value));
    public static final MapCodec<InvocationDefinition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("entity").forGetter(InvocationDefinition::entity),
            Codec.STRING.optionalFieldOf("variant", "default").forGetter(InvocationDefinition::variant),
            MEDIUM.fieldOf("medium").forGetter(InvocationDefinition::soulwood),
            PROFILE.fieldOf("spawn_profile").forGetter(InvocationDefinition::profile),
            TINT.optionalFieldOf("tint", 0xFFFFFF).forGetter(InvocationDefinition::tint)
    ).apply(instance, InvocationDefinition::new));

    public static InvocationDefinition find(Level level, ItemStack sigil) {
        ResourceLocation id = InvocationSigilItem.definitionId(sigil);
        if (id == null || level == null) return null;
        return level.getRecipeManager().byKey(id).map(RecipeHolder::value)
                .filter(InvocationDefinition.class::isInstance).map(InvocationDefinition.class::cast).orElse(null);
    }

    @Override public boolean matches(RecipeInput inventory, Level level) { return false; }
    @Override public boolean canCraftInDimensions(int width, int height) { return false; }
    @Override public boolean isSpecial() { return true; }
    @Override public ItemStack assemble(RecipeInput inventory, HolderLookup.Provider registries) { return ItemStack.EMPTY; }
    @Override public ItemStack getResultItem(HolderLookup.Provider registries) { return ItemStack.EMPTY; }
    @Override public RecipeSerializer<?> getSerializer() { return MaledictRecipeTypes.INVOCATION_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return MaledictRecipeTypes.INVOCATION.get(); }

    public static final class Serializer implements RecipeSerializer<InvocationDefinition> {
        public static final StreamCodec<RegistryFriendlyByteBuf, InvocationDefinition> STREAM_CODEC = StreamCodec.of(
                (buffer, definition) -> {
                    buffer.writeResourceLocation(definition.entity());
                    buffer.writeUtf(definition.variant());
                    buffer.writeBoolean(definition.soulwood());
                    buffer.writeEnum(definition.profile());
                    buffer.writeInt(definition.tint());
                }, buffer -> new InvocationDefinition(buffer.readResourceLocation(), buffer.readUtf(),
                        buffer.readBoolean(), buffer.readEnum(Profile.class), buffer.readInt()));

        @Override public MapCodec<InvocationDefinition> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, InvocationDefinition> streamCodec() { return STREAM_CODEC; }
    }
}
