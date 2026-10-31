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
    private final Path path;

    public MaledictSpiritData(PackOutput output) {
        path = output.getOutputFolder(PackOutput.Target.DATA_PACK)
                .resolve("maledict/spirit_data/entity/weeping_wither.json");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        JsonObject spirit = new JsonObject();
        spirit.addProperty("spirit", "umbral");
        spirit.addProperty("count", 2);
        JsonArray spirits = new JsonArray();
        spirits.add(spirit);
        JsonObject data = new JsonObject();
        data.addProperty("registry_name", "maledict:weeping_wither");
        data.addProperty("primary_type", "umbral");
        data.add("spirits", spirits);
        return DataProvider.saveStable(output, data, path);
    }

    @Override
    public String getName() {
        return "Maledict entity spirit data";
    }
}
