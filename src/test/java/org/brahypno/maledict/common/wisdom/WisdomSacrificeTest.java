package org.brahypno.maledict.common.wisdom;

import com.mojang.authlib.GameProfile;
import net.minecraft.advancements.Advancement;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.registry.MaledictMobEffects;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class WisdomSacrificeTest {

    @GameTest(template = "test_empty")
    public static void naturalExpiryRemovesSacrificeBeforeAddingSixMinuteSightLoss(GameTestHelper helper) {
        ServerPlayer player = createResilientPlayer(helper);
        float before = healthAndAbsorption(player);
        player.addEffect(new MobEffectInstance(MaledictMobEffects.SACRIFICE.get(), 1));
        WisdomSacrifice.markOffered(player);
        player.doTick();

        helper.assertTrue(before - healthAndAbsorption(player) == SacrificeSchedule.PULSES * SacrificeSchedule.DAMAGE,
                          "Even a one-tick offering must trigger 180 fixed hits");
        helper.assertTrue(WisdomSacrifice.hasEarned(player),
                          "A rite-origin effect expiring naturally must award without checking actual damage");
        helper.assertTrue(!player.hasEffect(MaledictMobEffects.SACRIFICE.get()),
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
    public static void acceleratedExpiryTriggersAllSkippedHitsAtSixDamageEach(GameTestHelper helper) {
        ServerPlayer player = createResilientPlayer(helper);
        float before = healthAndAbsorption(player);
        List<Float> hits = new ArrayList<>();
        Consumer<LivingAttackEvent> observe = event -> {
            if (event.getEntity() == player) {
                hits.add(event.getAmount());
            }
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, false, LivingAttackEvent.class, observe);
        player.addEffect(new MobEffectInstance(MaledictMobEffects.SACRIFICE.get(),
                SacrificeSchedule.DURATION_TICKS));
        WisdomSacrifice.markOffered(player);
        try {
            player.doTick();
            helper.assertTrue(before - healthAndAbsorption(player) == 6.0F, "Normal first hit must remain six");
            // Simulate a foreign countdown modifier jumping straight to the last tick.
            player.getActiveEffectsMap().put(MaledictMobEffects.SACRIFICE.get(),
                    new MobEffectInstance(MaledictMobEffects.SACRIFICE.get(), 1));
            player.doTick();
        } finally {
            MinecraftForge.EVENT_BUS.unregister(observe);
        }
        helper.assertTrue(hits.size() == 180 && hits.stream().allMatch(damage -> damage == 6.0F),
                          "Acceleration must trigger exactly 180 separate six-point hits");
        helper.assertTrue(before - healthAndAbsorption(player) == SacrificeSchedule.PULSES * SacrificeSchedule.DAMAGE,
                          "Accelerated hits must bypass each other's damage cooldown");
        helper.assertTrue(WisdomSacrifice.hasEarned(player), "Surviving natural expiry must complete the rite");
        helper.assertTrue(!player.getPersistentData().contains("maledict:sacrifice_schedule"),
                          "Natural expiry must clear timing progress");
        helper.succeed();
    }

    /** 已确认：有槽即免疫，不受渲染开关、饰品和夜视影响；旧黑暗也必须清掉。 */
    @GameTest(template = "test_empty")
    public static void visionSlotBlocksDarknessWithRenderingOffAndClearsAnExistingEffect(GameTestHelper helper) {
        ServerPlayer player = createPlayer(helper);
        helper.assertTrue(player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 1000)),
                          "Without a vision slot, vanilla darkness must still apply");
        Advancement wisdom = player.server.getAdvancements().getAdvancement(WisdomSacrifice.ID);
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
        player.setAbsorptionAmount(1000.0F);
        return player;
    }

    private static float healthAndAbsorption(ServerPlayer player) {
        return player.getHealth() + player.getAbsorptionAmount();
    }

    private static ServerPlayer createPlayer(GameTestHelper helper) {
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
