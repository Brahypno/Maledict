package org.brahypno.maledict.common.rite;

import com.mojang.authlib.GameProfile;
import net.minecraft.advancements.Advancement;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class WisdomSacrificeTest {
    @GameTest(template = "test_empty")
    public static void earningAdvancementGrantsBothSlotsWithoutSacrifice(GameTestHelper helper) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), "WisdomSlotTest");
        // Forge deliberately refuses to award advancements to FakePlayer.
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), profile);
        player.connection = new FakePlayer(helper.getLevel(), profile).connection;
        ICuriosItemHandler inventory = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing real Curios capability"));
        Map<String, ICurioStacksHandler> slots = new LinkedHashMap<>();
        slots.put(WisdomSacrifice.RUNE_SLOT, new CurioStacksHandler(inventory,
                WisdomSacrifice.RUNE_SLOT, 1, true, true, true, ICurio.DropRule.DEFAULT));
        slots.put(WisdomSacrifice.DELUSION_SLOT, new CurioStacksHandler(inventory,
                WisdomSacrifice.DELUSION_SLOT, 0, true, true, true, ICurio.DropRule.DEFAULT));
        inventory.setCurios(slots);

        Advancement unrelated = player.server.getAdvancements().getAdvancement(
                ResourceLocation.fromNamespaceAndPath("minecraft", "story/root"));
        helper.assertTrue(unrelated != null, "Vanilla advancement must be loaded");
        unrelated.getCriteria().keySet().forEach(criterion -> player.getAdvancements().award(unrelated, criterion));
        assertSlots(helper, inventory, 1, 0);
        Advancement wisdom = player.server.getAdvancements().getAdvancement(WisdomSacrifice.ID);
        helper.assertTrue(wisdom != null, "Wisdom's Sacrifice must be loaded");
        // The same advancement API used by /advancement grant, without running the sacrifice rite.
        helper.assertTrue(player.getAdvancements().award(wisdom, "impossible"), "Must award Wisdom's Sacrifice");
        helper.assertTrue(WisdomSacrifice.hasEarned(player), "Wisdom's Sacrifice must be complete");
        assertSlots(helper, inventory, 2, 1);

        player.getAdvancements().revoke(wisdom, "impossible");
        player.getAdvancements().award(wisdom, "impossible");
        assertSlots(helper, inventory, 2, 1);
        helper.succeed();
    }

    private static void assertSlots(GameTestHelper helper, ICuriosItemHandler inventory,
                                    int runes, int visions) {
        helper.assertTrue(inventory.getCurios().get(WisdomSacrifice.RUNE_SLOT).getStacks().getSlots() == runes,
                "Rune slot count must be " + runes);
        helper.assertTrue(inventory.getCurios().get(WisdomSacrifice.DELUSION_SLOT).getStacks().getSlots() == visions,
                "Vision slot count must be " + visions);
    }
}
