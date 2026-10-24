package org.brahypno.maledict.common.invocation;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.brahypno.maledict.registry.MaledictRecipeTypes;

/** A synced data-pack definition, not a crafting-table recipe. */
public record InvocationDefinition(ResourceLocation id, ResourceLocation entity, String variant,
                                   boolean soulwood, Profile profile, int tint) implements Recipe<Container> {
    public enum Profile { GROUND, FLYING, WATER, ATTACHED }

    public static InvocationDefinition find(Level level, ItemStack sigil) {
        ResourceLocation id = InvocationSigilItem.definitionId(sigil);
        if (id == null || level == null) return null;
        return level.getRecipeManager().byKey(id)
                .filter(InvocationDefinition.class::isInstance).map(InvocationDefinition.class::cast).orElse(null);
    }

    public static InvocationDefinition fromJson(ResourceLocation id, JsonObject json) {
        String medium = GsonHelper.getAsString(json, "medium");
        if (!medium.equals("runewood") && !medium.equals("soulwood")) {
            throw new JsonSyntaxException("Unknown invocation medium: " + medium);
        }
        Profile profile;
        try {
            profile = Profile.valueOf(GsonHelper.getAsString(json, "spawn_profile").toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new JsonSyntaxException("Unknown invocation spawn_profile", exception);
        }
        ResourceLocation entity = ResourceLocation.tryParse(GsonHelper.getAsString(json, "entity"));
        if (entity == null) throw new JsonSyntaxException("Invalid invocation entity ID");
        String tint = GsonHelper.getAsString(json, "tint", "#FFFFFF");
        if (!tint.matches("#[0-9a-fA-F]{6}")) throw new JsonSyntaxException("Invocation tint must be #RRGGBB");
        return new InvocationDefinition(id, entity,
                GsonHelper.getAsString(json, "variant", "default"), medium.equals("soulwood"), profile,
                Integer.parseInt(tint.substring(1), 16));
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "maledict:invocation");
        json.addProperty("entity", entity.toString());
        json.addProperty("variant", variant);
        json.addProperty("medium", soulwood ? "soulwood" : "runewood");
        json.addProperty("spawn_profile", profile.name().toLowerCase(java.util.Locale.ROOT));
        json.addProperty("tint", String.format(java.util.Locale.ROOT, "#%06X", tint));
        return json;
    }

    @Override public ResourceLocation getId() { return id; }
    @Override public boolean matches(Container inventory, Level level) { return false; }
    @Override public boolean canCraftInDimensions(int width, int height) { return false; }
    @Override public boolean isSpecial() { return true; }
    @Override public ItemStack assemble(Container inventory, RegistryAccess registries) { return ItemStack.EMPTY; }
    @Override public ItemStack getResultItem(RegistryAccess registries) { return InvocationSigilItem.create(id); }
    @Override public RecipeSerializer<?> getSerializer() { return MaledictRecipeTypes.INVOCATION_SERIALIZER.get(); }
    @Override public RecipeType<?> getType() { return MaledictRecipeTypes.INVOCATION.get(); }

    public static final class Serializer implements RecipeSerializer<InvocationDefinition> {
        @Override
        public InvocationDefinition fromJson(ResourceLocation id, JsonObject json) {
            return InvocationDefinition.fromJson(id, json);
        }

        @Override
        public InvocationDefinition fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return new InvocationDefinition(id, buffer.readResourceLocation(), buffer.readUtf(),
                    buffer.readBoolean(), buffer.readEnum(Profile.class), buffer.readInt());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, InvocationDefinition definition) {
            buffer.writeResourceLocation(definition.entity());
            buffer.writeUtf(definition.variant());
            buffer.writeBoolean(definition.soulwood());
            buffer.writeEnum(definition.profile());
            buffer.writeInt(definition.tint());
        }
    }
}
