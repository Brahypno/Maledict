package org.brahypno.maledict.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/** Schema verified against Malum 6646111's spirit_data/entity examples and reload listener. */
public final class MaledictSpiritData implements DataProvider {
    private final Path directory;

    public MaledictSpiritData(PackOutput output) {
        directory = output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("maledict/spirit_data/entity");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        return CompletableFuture.allOf(
                DataProvider.saveStable(output, data("weeping_wither", "umbral", spirit("umbral", 2)),
                        directory.resolve("weeping_wither.json")),
                DataProvider.saveStable(output, data("demiurge_statue", "eldritch", spirit("sacred", 6),
                                spirit("wicked", 7), spirit("eldritch", 8), spirit("umbral", 3)),
                        directory.resolve("demiurge_statue.json")));
    }

    private static JsonObject spirit(String type, int count) {
        JsonObject spirit = new JsonObject();
        spirit.addProperty("spirit", type);
        spirit.addProperty("count", count);
        return spirit;
    }

    private static JsonObject data(String entity, String primary, JsonObject... entries) {
        JsonArray spirits = new JsonArray();
        for (var entry : entries) spirits.add(entry);
        JsonObject data = new JsonObject();
        data.addProperty("registry_name", "maledict:" + entity);
        data.addProperty("primary_type", primary);
        data.add("spirits", spirits);
        return data;
    }

    @Override
    public String getName() {
        return "Maledict entity spirit data";
    }
}
