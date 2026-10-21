package org.brahypno.maledict.common.rite;

import com.mojang.authlib.GameProfile;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.common.effect.SacrificeSchedule;
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
import java.util.function.Consumer;

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
        ServerPlayer player = createResilientPlayer(helper);
        float before = healthAndAbsorption(player);
        player.addEffect(new MobEffectInstance(MaledictMobEffects.SACRIFICE, 1));
        WisdomSacrifice.markOffered(player);
        player.doTick();

        helper.assertTrue(before - healthAndAbsorption(player) == SacrificeSchedule.TOTAL_DAMAGE,
                          "Even a one-tick offering must charge all 1080 damage");
        helper.assertTrue(WisdomSacrifice.hasEarned(player),
                          "Full damage and natural expiry must award without nine days of server time");
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

    @GameTest(template = "test_empty")
    public static void halfDurationHalvesPulseIntervalAndRemovalInvalidatesTheOffering(GameTestHelper helper) {
        ServerPlayer player = createResilientPlayer(helper);
        float before = healthAndAbsorption(player);
        player.addEffect(new MobEffectInstance(MaledictMobEffects.SACRIFICE,
                SacrificeSchedule.DURATION_TICKS / 2));
        WisdomSacrifice.markOffered(player);
        player.doTick();
        helper.assertTrue(before - healthAndAbsorption(player) == 6.0F,
                          "Half duration must preserve the six-point first hit");
        int initial = SacrificeSchedule.DURATION_TICKS / 2;
        player.getActiveEffectsMap().put(MaledictMobEffects.SACRIFICE,
                new MobEffectInstance(MaledictMobEffects.SACRIFICE, initial - 599));
        player.doTick();
        helper.assertTrue(before - healthAndAbsorption(player) == 6.0F,
                          "No second pulse before 600 effect ticks have elapsed");
        player.getActiveEffectsMap().put(MaledictMobEffects.SACRIFICE,
                new MobEffectInstance(MaledictMobEffects.SACRIFICE, initial - 600));
        player.doTick();
        helper.assertTrue(before - healthAndAbsorption(player) == 12.0F,
                          "Half duration must deliver another six-point hit at 600 effect ticks");
        player.removeEffect(MaledictMobEffects.SACRIFICE);
        helper.assertTrue(!player.getPersistentData().contains("maledict:sacrifice_offered")
                          && !player.getPersistentData().contains("maledict:sacrifice_damage"),
                          "Removal must discard both provenance and damage progress");
        player.addEffect(new MobEffectInstance(MaledictMobEffects.SACRIFICE, 1));
        player.doTick();
        helper.assertTrue(!WisdomSacrifice.hasEarned(player),
                          "A replacement effect without a rite must not inherit eligibility");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void resistanceDoesNotIncreaseLaterPulsesOrPreventAcceleratedCompletion(GameTestHelper helper) {
        ServerPlayer player = createResilientPlayer(helper);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1000, 3));
        float before = healthAndAbsorption(player);
        player.addEffect(new MobEffectInstance(MaledictMobEffects.SACRIFICE,
                SacrificeSchedule.DURATION_TICKS));
        WisdomSacrifice.markOffered(player);
        player.doTick();
        // Confirmed sequence: each scheduled hit is six raw damage, even when resistance reduces it.
        helper.assertTrue(Math.abs(before - healthAndAbsorption(player) - 1.2F) < 0.001F,
                          "Resistance IV must reduce the actual first hit to 1.2");
        var progress = player.getPersistentData().getCompound("maledict:sacrifice_damage");
        helper.assertTrue(progress.getInt("pulses") == 1,
                          "Only the scheduled pulse count is recorded, regardless of damage taken");
        player.getActiveEffectsMap().put(MaledictMobEffects.SACRIFICE,
                new MobEffectInstance(MaledictMobEffects.SACRIFICE,
                        SacrificeSchedule.DURATION_TICKS - SacrificeSchedule.INTERVAL_TICKS));
        player.doTick();
        helper.assertTrue(progress.getInt("pulses") == 2
                          && Math.abs(before - healthAndAbsorption(player) - 2.4F) < 0.001F,
                          "The second six-point pulse must not collect the damage resisted on the first");
        // Simulate a foreign countdown modifier jumping straight to the last tick.
        player.getActiveEffectsMap().put(MaledictMobEffects.SACRIFICE,
                new MobEffectInstance(MaledictMobEffects.SACRIFICE, 1));
        player.doTick();
        helper.assertTrue(Math.abs(before - healthAndAbsorption(player) - 216.0F) < 0.05F,
                          "1080 raw damage with Resistance IV must cost only 216 actual damage");
        helper.assertTrue(WisdomSacrifice.hasEarned(player),
                          "Resistance must not prevent accelerated completion of the raw damage budget");
        helper.assertTrue(!player.getPersistentData().contains("maledict:sacrifice_damage"),
                          "Natural expiry must clear the finished ledger");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void cancelledAndImmuneDamageStillCompleteTheRite(GameTestHelper helper) {
        ServerPlayer player = createResilientPlayer(helper);
        float before = healthAndAbsorption(player);
        int[] hits = {0};
        Consumer<LivingIncomingDamageEvent> cancel = event -> {
            if (event.getEntity() == player) {
                helper.assertTrue(event.getAmount() == 6.0F, "Every hit must stay at six even when time is compressed");
                hits[0]++;
                event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, false, LivingIncomingDamageEvent.class, cancel);
        try {
            player.addEffect(new MobEffectInstance(MaledictMobEffects.SACRIFICE, 1));
            WisdomSacrifice.markOffered(player);
            player.doTick();
        } finally {
            NeoForge.EVENT_BUS.unregister(cancel);
        }
        helper.assertTrue(healthAndAbsorption(player) == before, "Cancelled damage must not be charged");
        helper.assertTrue(hits[0] == 180, "A one-tick offering must issue 180 separate six-point hits");
        helper.assertTrue(WisdomSacrifice.hasEarned(player),
                          "Cancelling the hit must not prevent completion of the raw damage budget");
        ServerPlayer immune = createPlayer(helper);
        immune.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 1000, 4));
        float immuneBefore = healthAndAbsorption(immune);
        immune.addEffect(new MobEffectInstance(MaledictMobEffects.SACRIFICE, 1));
        WisdomSacrifice.markOffered(immune);
        immune.doTick();
        helper.assertTrue(healthAndAbsorption(immune) == immuneBefore && WisdomSacrifice.hasEarned(immune),
                          "Full damage immunity must preserve health and still complete the offering");
        helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void dyingDuringCompressedDamageDoesNotAward(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper);
        player.addEffect(new MobEffectInstance(MaledictMobEffects.SACRIFICE, 1));
        WisdomSacrifice.markOffered(player);
        player.doTick();
        helper.assertTrue(!player.isAlive(), "An ordinary player must not survive 1080 damage");
        helper.assertTrue(!WisdomSacrifice.hasEarned(player), "A lethal final payment must not award");
        helper.assertTrue(!player.getPersistentData().contains("maledict:sacrifice_offered")
                          && !player.getPersistentData().contains("maledict:sacrifice_damage"),
                          "Death must clear provenance and the damage ledger");
        helper.succeed();
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

    private static ServerPlayer createResilientPlayer(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper);
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1024.0D);
        player.setHealth(player.getMaxHealth());
        player.getAttribute(Attributes.MAX_ABSORPTION).setBaseValue(1000.0D);
        player.setAbsorptionAmount(1000.0F);
        return player;
    }

    private static float healthAndAbsorption(ServerPlayer player) {
        return player.getHealth() + player.getAbsorptionAmount();
    }

    private static ServerPlayer createPlayer(GameTestHelper helper) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), "WisdomSlotTest");
        // Forge deliberately refuses to award advancements to FakePlayer.
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(), profile, net.minecraft.server.level.ClientInformation.createDefault());
        player.connection = new FakePlayer(helper.getLevel(), profile).connection;
        // Malum 1.21 voodoo respects the player's initial 60-tick spawn protection.
        for (int tick = 0; tick < 60; tick++) {
            player.tick();
        }
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
