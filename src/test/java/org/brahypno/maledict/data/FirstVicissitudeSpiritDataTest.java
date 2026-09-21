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
 * 无常常规掉落之外那份「Malum 认得的」精魂表：{@code malum} 的
 * {@code spirit_data/entity} 是逐实体的精魂掉落数据，提尔锋按它算灵魂强度加成
 * （{@code totalSpirits * 2}），灵魂暴露后击杀也按它放出精魂。
 *
 * <p>这里钉住的是本轮需求本身：<b>八种精魂各 6 枚</b>，也就是 48 点灵魂强度。
 * 这个文件是手写的资源，不是 runData 产物，键名或数量写错不会有编译错误，
 * 只会在游戏里静静地少掉一半奖励，所以值得一个测试看着。
 *
 * <p>Malum 的读取器只认 {@code registry_name}、{@code primary_type} 与
 * {@code spirits[].spirit/count}；文件放在哪个命名空间都行（Malum 扫的是所有命名空间），
 * 这里跟着资源一起放在 {@code maledict} 下。
 */
class FirstVicissitudeSpiritDataTest {

    /** 与 {@code src/main/resources} 下的实际路径一致。 */
    private static final String PATH =
            "/data/maledict/spirit_data/entity/first_vicissitude.json";

    /** 需求里的「各 6」。 */
    private static final int PER_SPIRIT = 6;

    /** 无常要掉的八种精魂，用 Malum 的 identifier 拼写；幽影（umbral）不在其中。 */
    private static final Set<String> EIGHT_SPIRITS = Set.of(
            "sacred", "wicked", "arcane", "eldritch",
            "aerial", "aqueous", "earthen", "infernal");

    @Test
    void theBossIsRegisteredUnderItsOwnRegistryName() {
        JsonObject data = spiritData();
        assertEquals("maledict:first_vicissitude", data.get("registry_name").getAsString());
    }

    /** 主类型必须是一种真精魂：Malum 读不到就退回神圣精魂，属于静默走样。 */
    @Test
    void thePrimaryTypeIsOneOfTheEight() {
        assertTrue(EIGHT_SPIRITS.contains(spiritData().get("primary_type").getAsString()));
    }

    /** 八种精魂，各 6 枚，一种不多一种不少。 */
    @Test
    void eightSpiritsAtSixEach() {
        Map<String, Integer> spirits = spirits();
        assertEquals(EIGHT_SPIRITS, spirits.keySet());
        for (Map.Entry<String, Integer> entry : spirits.entrySet()) {
            assertEquals(PER_SPIRIT, entry.getValue(), entry.getKey());
        }
    }

    /** 总数就是提尔锋那条公式的输入，48 点也就是能触发魔法的每一下额外 96 点伤害。 */
    @Test
    void theTotalSpiritCountIsFortyEight() {
        int total = spirits().values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(EIGHT_SPIRITS.size() * PER_SPIRIT, total);
    }

    /** 幽影不在无常的精魂表里：九种灵里它不在本模组的「八种」之内。 */
    @Test
    void umbralIsNotHandedOut() {
        assertFalse(spirits().containsKey("umbral"));
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
