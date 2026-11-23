package org.brahypno.maledict.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Malum 的 {@code spirit_data/entity} 掉落表：决定打死一只生物会散出哪些精魂。
 * <p>两个首领（腐枯凋灵、亚大伯斯之石像）原本都没有条目，进了 Malum 的精魂系统也只是按
 * {@code DEFAULT_BOSS_SPIRIT_DATA} 走。这里补上设计值。
 * <p>格式对齐 Malum 1.21.1-1.8.2 自带的 {@code data/malum/spirit_data/entity/*.json}：精魂类型写
 * <b>全限定名</b>（{@code malum:umbral}），不写裸名——裸名会被当成 {@code minecraft:} 命名空间。
 */
public final class MaledictSpiritData implements DataProvider {
    private final Path directory;

    public MaledictSpiritData(PackOutput output) {
        directory = output.getOutputFolder(PackOutput.Target.DATA_PACK).resolve("maledict/spirit_data/entity");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        return CompletableFuture.allOf(
                DataProvider.saveStable(output, data("weeping_wither", "malum:umbral", spirit("malum:umbral", 2)),
                        directory.resolve("weeping_wither.json")),
                DataProvider.saveStable(output, data("demiurge_statue", "malum:eldritch", spirit("malum:sacred", 6),
                                spirit("malum:wicked", 7), spirit("malum:eldritch", 8), spirit("malum:umbral", 3)),
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
