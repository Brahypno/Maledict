package org.brahypno.maledict.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 手写的 Malum 精魂表（提尔锋按它算灵魂强度加成）：八种精魂各 6 枚，外加一枚幽影。
 * 写错键名或数量不会有编译错误，只会在游戏里静静地少掉奖励。
 */
class FirstVicissitudeSpiritDataTest {

    private static final String PATH =
            "/data/maledict/spirit_data/entity/first_vicissitude.json";

    private static final int PER_SPIRIT = 6;
    private static final int UMBRAL_COUNT = 1;

    private static final Set<String> EIGHT_SPIRITS = Set.of(
            "sacred", "wicked", "arcane", "eldritch",
            "aerial", "aqueous", "earthen", "infernal");

    private static final Set<String> NINE_SPIRITS = Set.of(
            "sacred", "wicked", "arcane", "eldritch",
            "aerial", "aqueous", "earthen", "infernal", "umbral");

    @Test
    void theBossIsRegisteredUnderItsOwnRegistryName() {
        JsonObject data = spiritData();
        assertEquals("maledict:first_vicissitude", data.get("registry_name").getAsString());
    }

    @Test
    void thePrimaryTypeIsARealSpirit() {
        assertTrue(NINE_SPIRITS.contains(spiritData().get("primary_type").getAsString()));
    }

    @Test
    void theTableIsEightSpiritsAtSixEachPlusOneUmbral() {
        Map<String, Integer> spirits = spirits();
        assertEquals(NINE_SPIRITS, spirits.keySet());
        for (Map.Entry<String, Integer> entry : spirits.entrySet()) {
            int expected = "umbral".equals(entry.getKey()) ? UMBRAL_COUNT : PER_SPIRIT;
            assertEquals(expected, entry.getValue(), entry.getKey());
        }
        int total = spirits.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(EIGHT_SPIRITS.size() * PER_SPIRIT + UMBRAL_COUNT, total);
    }

    private static Map<String, Integer> spirits() {
        JsonArray array = spiritData().getAsJsonArray("spirits");
        assertNotNull(array, "spirits 数组缺失");
        Map<String, Integer> spirits = new LinkedHashMap<>();
        for (JsonElement element : array) {
            JsonObject spirit = element.getAsJsonObject();
            String name = spirit.get("spirit").getAsString();
            assertFalse(spirits.containsKey(name), "精魂重复出现：" + name);
            spirits.put(name, spirit.get("count").getAsInt());
        }
        return spirits;
    }

    private static JsonObject spiritData() {
        InputStream stream = FirstVicissitudeSpiritDataTest.class.getResourceAsStream(PATH);
        assertNotNull(stream, "找不到资源 " + PATH);
        JsonElement parsed = JsonParser.parseReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8));
        return parsed.getAsJsonObject();
    }
}
