package org.brahypno.maledict.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import org.brahypno.maledict.Maledict;

import java.util.concurrent.CompletableFuture;

/** Raven spawning in the Crimson Forest and Nether Wastes, without light or difficulty restrictions. */
public final class MaledictBiomeModifiers implements DataProvider {
    private final PackOutput.PathProvider output;

    public MaledictBiomeModifiers(PackOutput output) {
        this.output = output.createPathProvider(PackOutput.Target.DATA_PACK, "forge/biome_modifier");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        JsonObject modifier = new JsonObject();
        modifier.addProperty("type", "forge:add_spawns");
        JsonArray biomes = new JsonArray();
        biomes.add("minecraft:crimson_forest");
        biomes.add("minecraft:nether_wastes");
        modifier.add("biomes", biomes);
        JsonArray spawns = new JsonArray();
        JsonObject raven = new JsonObject();
        raven.addProperty("type", "maledict:raven");
        raven.addProperty("weight", 12);
        raven.addProperty("minCount", 4);
        raven.addProperty("maxCount", 6);
        spawns.add(raven);
        modifier.add("spawners", spawns);
        return DataProvider.saveStable(cache, modifier, output.json(
                ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "raven_spawns")));
    }

    @Override
    public String getName() {
        return "Maledict biome modifiers";
    }
}
