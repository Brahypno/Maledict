package org.brahypno.maledict.common.rite;

import com.mojang.authlib.GameProfile;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.registry.MaledictMobEffects;
import org.brahypno.maledict.registry.MaledictItems;
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
        ServerPlayer player = createPlayer(helper);
        ICuriosItemHandler inventory = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing real Curios capability"));

        AdvancementHolder unrelated = player.server.getAdvancements().get(
                ResourceLocation.fromNamespaceAndPath("minecraft", "story/root"));
        helper.assertTrue(unrelated != null, "Vanilla advancement must be loaded");
        unrelated.value().criteria().keySet().forEach(criterion -> player.getAdvancements().award(unrelated, criterion));
        assertSlots(helper, inventory, 1, 0);
        helper.assertTrue(!player.hasEffect(MobEffects.BLINDNESS) && !player.hasEffect(MobEffects.DARKNESS),
                          "Unrelated advancements must not cause sight loss");
        AdvancementHolder wisdom = player.server.getAdvancements().get(WisdomSacrifice.ID);
        helper.assertTrue(wisdom != null, "Wisdom's Sacrifice must be loaded");
        helper.assertTrue(wisdom.value().display().orElseThrow() != null
                          && wisdom.value().display().orElseThrow().getIcon().is(MaledictItems.BLEEDING_EYE.get())
                          && wisdom.value().display().orElseThrow().shouldShowToast(),
                          "Wisdom's Sacrifice must show a toast with the Bleeding Eye icon");
        helper.assertTrue(!wisdom.value().display().orElseThrow().shouldAnnounceChat(), "The toast must not announce in chat");
        helper.assertTrue(wisdom.value().parent().isPresent()
                          && player.server.getAdvancements().get(wisdom.value().parent().orElseThrow()).value().display().isEmpty(),
                          "The advancement must belong to a separate root without a display or UI tab");
        // The same advancement API used by /advancement grant, without running the sacrifice rite.
        helper.assertTrue(player.getAdvancements().award(wisdom, "impossible"), "Must award Wisdom's Sacrifice");
        helper.assertTrue(WisdomSacrifice.hasEarned(player), "Wisdom's Sacrifice must be complete");
        assertSlots(helper, inventory, 2, 1);

        helper.runAfterDelay(1, () -> {
            assertSightLoss(helper, player, 7200);
            player.getAdvancements().revoke(wisdom, "impossible");
            player.getAdvancements().award(wisdom, "impossible");
            assertSlots(helper, inventory, 2, 1);
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty")
    public static void naturalExpiryRemovesSacrificeBeforeAddingSixMinuteSightLoss(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper);
        player.addEffect(new MobEffectInstance(MaledictMobEffects.SACRIFICE, 1));
        WisdomSacrifice.markOffered(player);
        // Reproduce the final tick of an offering whose full ritual deadline has been reached.
        player.getPersistentData().putLong("maledict:sacrifice_offered", player.level().getGameTime());
        player.doTick();

        helper.assertTrue(WisdomSacrifice.hasEarned(player), "Natural expiry must award the advancement");
        helper.assertTrue(!player.hasEffect(MaledictMobEffects.SACRIFICE),
                          "Expired sacrifice must leave the effect map without interrupting its iterator");
        helper.assertTrue(!player.hasEffect(MobEffects.BLINDNESS) && !player.hasEffect(MobEffects.DARKNESS),
                          "New effects must wait until the server tick ends");
        helper.runAfterDelay(1, () -> {
            assertSightLoss(helper, player, 7200);
            for (int tick = 0; tick < 60; tick++) {
                player.doTick();
            }
            assertSightLoss(helper, player, 7140);
            assertSlots(helper, CuriosApi.getCuriosInventory(player)
                    .orElseThrow(() -> new IllegalStateException("Missing real Curios capability")), 2, 1);
            helper.succeed();
        });
    }

    /** 已确认：有槽即免疫，不受渲染开关、饰品和夜视影响；旧黑暗也必须清掉。 */
    @GameTest(template = "test_empty")
    public static void visionSlotBlocksDarknessWithRenderingOffAndClearsAnExistingEffect(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper);
        helper.assertTrue(player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 1000)),
                          "Without a vision slot, vanilla darkness must still apply");
        AdvancementHolder wisdom = player.server.getAdvancements().get(WisdomSacrifice.ID);
        helper.assertTrue(wisdom != null, "Wisdom's Sacrifice must be loaded");
        player.getAdvancements().award(wisdom, "impossible");
        ICuriosItemHandler inventory = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing real Curios capability"));
        var vision = inventory.getCurios().get(WisdomSacrifice.DELUSION_SLOT);
        vision.getRenders().set(0, false);
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1000));
        helper.assertTrue(!player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 2000)),
                          "An empty vision slot with rendering off must reject new darkness");
        player.doTick();
        helper.assertTrue(!player.hasEffect(MobEffects.DARKNESS), "The player tick must remove existing darkness");
        helper.assertTrue(player.hasEffect(MobEffects.NIGHT_VISION), "Darkness immunity must preserve night vision");
        helper.assertTrue(player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 1000)),
                          "The vision slot must not grant immunity to blindness");
        helper.succeed();
    }

    private static ServerPlayer createPlayer(GameTestHelper helper) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), "WisdomSlotTest");
        // Forge deliberately refuses to award advancements to FakePlayer.
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), profile, net.minecraft.server.level.ClientInformation.createDefault());
        player.connection = new FakePlayer(helper.getLevel(), profile).connection;
        ICuriosItemHandler inventory = CuriosApi.getCuriosInventory(player)
                .orElseThrow(() -> new IllegalStateException("Missing real Curios capability"));
        Map<String, ICurioStacksHandler> slots = new LinkedHashMap<>();
        slots.put(WisdomSacrifice.RUNE_SLOT, new CurioStacksHandler(inventory,
                WisdomSacrifice.RUNE_SLOT, 1, true, true, true, ICurio.DropRule.DEFAULT));
        slots.put(WisdomSacrifice.DELUSION_SLOT, new CurioStacksHandler(inventory,
                WisdomSacrifice.DELUSION_SLOT, 0, true, true, true, ICurio.DropRule.DEFAULT));
        inventory.setCurios(slots);
        return player;
    }

    private static void assertSightLoss(GameTestHelper helper, ServerPlayer player, int duration) {
        MobEffectInstance blindness = player.getEffect(MobEffects.BLINDNESS);
        helper.assertTrue(blindness != null && blindness.getDuration() == duration,
                          "Blindness must last " + duration + " ticks");
        helper.assertTrue(!player.hasEffect(MobEffects.DARKNESS), "The awarded vision slot must block darkness");
    }

    private static void assertSlots(GameTestHelper helper, ICuriosItemHandler inventory,
                                    int runes, int visions) {
        helper.assertTrue(inventory.getCurios().get(WisdomSacrifice.RUNE_SLOT).getStacks().getSlots() == runes,
                "Rune slot count must be " + runes);
        helper.assertTrue(inventory.getCurios().get(WisdomSacrifice.DELUSION_SLOT).getStacks().getSlots() == visions,
                "Vision slot count must be " + visions);
    }
}
