package org.brahypno.maledict.common.curio;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.common.entity.FirstVicissitudeBossEntity;
import org.brahypno.maledict.common.entity.VicissitudeBossStage;
import org.brahypno.maledict.registry.MaledictEntities;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.event.CurioCanEquipEvent;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/** Uses real Curios inventories, validation, equipment events and slot modifiers in Forge. */
@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class VicissitudeCurioReturnsTest {
    @GameTest(template = "test_empty")
    public static void manyFullSlotsReturnEveryStackToItsExactPosition(GameTestHelper helper) {
        Fixture f = new Fixture(helper, Map.of("charm", 32, "head", 32, "belt", 32,
                "ring", 64, "necklace", 32, "hands", 32, "bracelet", 32));
        f.fillEquipment();
        f.fillBackpack();
        List<VicissitudeCurioLedger.Entry> original = f.snapshot();
        helper.assertTrue(original.size() == 256, "Fixture must actually equip 256 distinct curios");
        var cosmetic = marked("cosmetic");
        f.inventory.getCurios().get("head").getCosmeticStacks().setStackInSlot(17, cosmetic.copy());
        List<VicissitudeCurioLedger.Entry> taken = VicissitudeCurioReturns.confiscate(f.player);
        f.equalEntries(original, taken);
        f.assertEquipmentEmpty();
        f.ledger.addAll(f.player.getUUID(), taken);
        helper.assertTrue(VicissitudeCurioReturns.deliverAll(f.player), "Full backpack must not prevent return to original empty slots");
        f.assertOriginalPositions(original);
        helper.assertTrue(ItemStack.matches(cosmetic, f.inventory.getCurios().get("head").getCosmeticStacks().getStackInSlot(17)),
                "Cosmetic slot must stay untouched");
        helper.assertTrue(VicissitudeCurioReturns.deliverAll(f.player), "Repeated delivery must remain empty");
        f.assertOriginalPositions(original);
        f.assertBackpackStillFull();
        f.assertNoDrops();
        helper.succeed();
    }

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void nestedProvidersRebuildSlotsBeforeTheirOriginalContentsReturn(GameTestHelper helper) {
        // Dependents are visited before providers, so this cannot pass by relying on map order.
        LinkedHashMap<String, Integer> slots = new LinkedHashMap<>();
        slots.put("belt", 1); slots.put("head", 1); slots.put("charm", 8);
        Fixture f = new Fixture(helper, slots);
        f.equip("charm", 6, provider("charm-provider", "charm", "head", 12));
        f.tickCurios();
        helper.assertTrue(f.size("head") == 13, "Equipped charm must create twelve real head slots");
        f.equip("head", 11, provider("head-provider", "head", "belt", 9));
        f.tickCurios();
        helper.assertTrue(f.size("belt") == 10, "Equipped head curio must create nine real belt slots");
        f.fillEquipment();
        f.tickCurios();
        f.fillBackpack();
        List<VicissitudeCurioLedger.Entry> original = f.snapshot();
        helper.assertTrue(original.size() == 31, "All base and nested slots must be filled");
        List<VicissitudeCurioLedger.Entry> taken = VicissitudeCurioReturns.confiscate(f.player);
        f.equalEntries(original, taken);
        f.assertEquipmentEmpty();
        f.tickCurios();
        helper.assertTrue(f.size("head") == 1 && f.size("belt") == 1, "Unequipping providers must actually remove their slots");
        // A saved ledger can contain dependent items before the providers they need.
        Collections.reverse(taken);
        f.ledger.addAll(f.player.getUUID(), taken);
        f.roundTripLedger();
        VicissitudeCurioReturns.deliverAll(f.player);
        helper.runAtTickTime(2, () -> {
            f.tickCurios();
            VicissitudeCurioReturns.deliverAll(f.player);
        });
        helper.runAtTickTime(4, () -> {
            f.tickCurios();
            helper.assertTrue(VicissitudeCurioReturns.deliverAll(f.player), "Providers and dependents must all return after equipment settles");
            f.tickCurios();
            helper.assertTrue(f.size("head") == 13 && f.size("belt") == 10, "Nested slot counts must be rebuilt exactly once");
            f.assertOriginalPositions(original);
            f.assertBackpackStillFull();
            f.assertNoDrops();
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void providersWaitForActualCuriosEquipmentTickRatherThanWorldTimeAlone(GameTestHelper helper) {
        Fixture f = new Fixture(helper, Map.of("head", 1, "charm", 1));
        ItemStack dependent = marked("delayed-head-8"), provider = provider("delayed-provider", "charm", "head", 12);
        f.ledger.addAll(f.player.getUUID(), List.of(new VicissitudeCurioLedger.Entry("head", 8, dependent),
                new VicissitudeCurioLedger.Entry("charm", 0, provider)));
        helper.assertTrue(!VicissitudeCurioReturns.deliverAll(f.player), "Restoring a provider must defer its dependent");
        helper.runAtTickTime(2, () -> {
            // World time has advanced but FakePlayer has not received its native equipment tick.
            helper.assertTrue(!VicissitudeCurioReturns.deliverAll(f.player) && f.size("head") == 1,
                    "Time alone must not declare the missing dependent slot irrecoverable");
            helper.assertTrue(f.player.getInventory().items.stream().noneMatch(stack -> ItemStack.matches(stack, dependent)),
                    "A pending provider must not cause premature backpack fallback");
            f.tickCurios();
            helper.assertTrue(f.size("head") == 13 && VicissitudeCurioReturns.deliverAll(f.player), "Actual native equipment update must unlock the original slot");
            helper.assertTrue(ItemStack.matches(dependent, f.stack("head", 8)), "Dependent must return to the exact original index");
            helper.assertTrue(f.player.getInventory().isEmpty(), "Successful exact return must remove the temporary claim token");
            f.assertNoDrops(); helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void occupiedOriginalSlotRetriesBeforeEncounterEnds(GameTestHelper helper) {
        Fixture f = new Fixture(helper, Map.of("head", 3));
        ItemStack occupant = marked("new-occupant"), owed = marked("owed-head-0"), other = marked("owed-head-2");
        f.equip("head", 0, occupant);
        f.fillBackpack();
        f.ledger.addAll(f.player.getUUID(), List.of(new VicissitudeCurioLedger.Entry("head", 0, owed),
                new VicissitudeCurioLedger.Entry("head", 2, other)));
        helper.assertTrue(!VicissitudeCurioReturns.deliverAll(f.player), "Occupied slot must leave a pending balance");
        helper.assertTrue(ItemStack.matches(other, f.stack("head", 2)), "Other restorable entries must not wait behind a blocked entry");
        helper.runAtTickTime(2, () -> {
            f.tickCurios();
            helper.assertTrue(!VicissitudeCurioReturns.deliverAll(f.player), "Full backpack during combat must retain the blocked item");
            helper.assertTrue(ItemStack.matches(occupant, f.stack("head", 0)) && f.stack("head", 1).isEmpty(),
                    "Never displace the occupant or use another empty Curios slot");
            helper.assertTrue(f.ledger.entries(f.player.getUUID()).size() == 1, "Only the blocked original item must remain");
            f.inventory.setEquippedCurio("head", 0, ItemStack.EMPTY);
            helper.assertTrue(VicissitudeCurioReturns.deliverAll(f.player), "Freed original slot must restore immediately, before boss death");
            helper.assertTrue(ItemStack.matches(owed, f.stack("head", 0)), "Return must preserve the exact original index and NBT");
            f.assertBackpackStillFull();
            f.assertNoDrops();
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 20)
    public static void unavailableOriginalSlotUsesBackpackSpaceInsteadOfAnotherCurioSlot(GameTestHelper helper) {
        Fixture f = new Fixture(helper, Map.of("head", 3));
        ItemStack occupant = marked("occupied"), owed = marked("backpack-return");
        f.equip("head", 0, occupant);
        f.fillBackpack();
        f.player.getInventory().setItem(35, ItemStack.EMPTY);
        f.ledger.addAll(f.player.getUUID(), List.of(new VicissitudeCurioLedger.Entry("head", 0, owed)));
        VicissitudeCurioReturns.deliverAll(f.player);
        helper.runAtTickTime(2, () -> {
            f.tickCurios();
            helper.assertTrue(VicissitudeCurioReturns.deliverAll(f.player), "The claim token must release the last backpack slot to the owed item");
            helper.assertTrue(ItemStack.matches(owed, f.player.getInventory().getItem(35)), "Backpack return must preserve complete NBT");
            helper.assertTrue(ItemStack.matches(occupant, f.stack("head", 0)) && f.stack("head", 1).isEmpty()
                    && f.stack("head", 2).isEmpty(), "Fallback must never equip a different Curios index");
            f.assertNoDrops();
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty")
    public static void nativeValidationReentryCannotDuplicateOrOverwriteAnOccupant(GameTestHelper helper) {
        Fixture f = new Fixture(helper, Map.of("head", 2));
        ItemStack owed = marked("reentrant-return"), occupant = marked("event-occupant");
        f.fillBackpack();
        f.ledger.addAll(f.player.getUUID(), List.of(new VicissitudeCurioLedger.Entry("head", 0, owed)));
        EquipEvents listener = new EquipEvents(event -> {
            if (event.getEntity() != f.player) return;
            VicissitudeCurioReturns.claim(f.player);
            f.inventory.setEquippedCurio("head", 0, occupant.copy());
        });
        NeoForge.EVENT_BUS.register(listener);
        try {
            helper.assertTrue(!VicissitudeCurioReturns.deliverAll(f.player), "Validation callback must prevent this attempt from overwriting its occupant");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        helper.assertTrue(ItemStack.matches(occupant, f.stack("head", 0)) && f.stack("head", 1).isEmpty(), "Callback's equipped stack must survive");
        helper.assertTrue(f.ledger.entries(f.player.getUUID()).size() == 1, "Reentrant claim must neither deliver nor duplicate the debt");
        f.inventory.setEquippedCurio("head", 0, ItemStack.EMPTY);
        helper.assertTrue(VicissitudeCurioReturns.deliverAll(f.player), "Retry after validation must deliver exactly once");
        helper.assertTrue(ItemStack.matches(owed, f.stack("head", 0)), "Original return must be preserved");
        f.assertBackpackStillFull();
        f.assertNoDrops();
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "curio_settlement", timeoutTicks = 20)
    public static void settlementRestoresOriginalSlotsThenBoxesMultipleOwnersOverflow(GameTestHelper helper) {
        Fixture first = new Fixture(helper, Map.of("head", 1)), second = new Fixture(helper, Map.of("head", 1));
        first.fillBackpack(); second.fillBackpack();
        UUID encounter = UUID.randomUUID();
        BlockPos site = helper.absolutePos(new BlockPos(2, 2, 2));
        first.player.setPos(site.offset(30, 0, 0).getCenter());
        second.player.setPos(site.offset(-30, 0, 0).getCenter());
        List<VicissitudeCurioLedger.Entry> a = entries("first", 61), b = entries("second", 31);
        VicissitudeCurioReturns.retain(helper.getLevel(), first.player.getUUID(), a, encounter);
        VicissitudeCurioReturns.retain(helper.getLevel(), second.player.getUUID(), b, encounter);
        VicissitudeCurioReturns.finishEncounter(helper.getLevel(), encounter, site);
        first.roundTripLedger(); second.roundTripLedger();
        helper.assertTrue(first.ledger.entries(first.player.getUUID()).get(0).returnSite().position().equals(site),
                "Final location must survive saved-data serialization");
        VicissitudeCurioReturns.deliverAll(first.player); VicissitudeCurioReturns.deliverAll(second.player);
        helper.runAtTickTime(2, () -> {
            first.tickCurios(); second.tickCurios();
            helper.assertTrue(VicissitudeCurioReturns.deliverAll(first.player) && VicissitudeCurioReturns.deliverAll(second.player), "Multiple chests must hold all overflow");
            helper.assertTrue(ItemStack.matches(a.get(0).stack(), first.stack("head", 0))
                    && ItemStack.matches(b.get(0).stack(), second.stack("head", 0)), "Settlement must still prioritize the original slot");
            List<ItemStack> boxed = chestContents(helper, site);
            helper.assertTrue(boxed.size() == 90, "Sixty plus thirty overflow entries must require five real chests");
            for (var entry : a.subList(1, a.size())) helper.assertTrue(boxed.stream().filter(stack -> ItemStack.matches(stack, entry.stack())).count() == 1, "First owner's overflow must occur exactly once");
            for (var entry : b.subList(1, b.size())) helper.assertTrue(boxed.stream().filter(stack -> ItemStack.matches(stack, entry.stack())).count() == 1, "Second owner's overflow must occur exactly once");
            helper.assertTrue(!first.ledger.hasPending(first.player.getUUID()) && !second.ledger.hasPending(second.player.getUUID()), "Successful physical delivery must clear both balances");
            first.assertBackpackStillFull(); second.assertBackpackStillFull();
            helper.assertTrue(VicissitudeCurioReturns.deliverAll(first.player) && chestContents(helper, site).size() == 90, "Repeated settlement must not create extra contents");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", batch = "curio_blocked", timeoutTicks = 20)
    public static void blockedChestPlacementKeepsSavedDebtUntilSpaceBecomesAvailable(GameTestHelper helper) {
        Fixture f = new Fixture(helper, Map.of("head", 1));
        f.equip("head", 0, marked("occupant")); f.fillBackpack();
        UUID encounter = UUID.randomUUID();
        BlockPos site = helper.absolutePos(new BlockPos(2, 2, 2));
        List<VicissitudeCurioLedger.Entry> owed = entries("blocked-box", 28);
        VicissitudeCurioReturns.retain(helper.getLevel(), f.player.getUUID(), owed, encounter);
        VicissitudeCurioReturns.finishEncounter(helper.getLevel(), encounter, site);
        Map<BlockPos, BlockState> restore = new LinkedHashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(site.offset(-4, -1, -4), site.offset(4, 1, 4))) {
            restore.put(pos.immutable(), helper.getLevel().getBlockState(pos));
            helper.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
        }
        VicissitudeCurioReturns.deliverAll(f.player);
        helper.runAtTickTime(2, () -> {
            f.tickCurios();
            helper.assertTrue(!VicissitudeCurioReturns.deliverAll(f.player), "No available chest location must retain every item");
            helper.assertTrue(f.ledger.entries(f.player.getUUID()).size() == 28 && chestContents(helper, site).isEmpty(), "Failed placement must not remove debt or overwrite blocks");
            f.roundTripLedger();
            helper.getLevel().setBlock(site, Blocks.AIR.defaultBlockState(), 3);
            helper.getLevel().setBlock(site.above(), Blocks.AIR.defaultBlockState(), 3);
            helper.assertTrue(VicissitudeCurioReturns.deliverAll(f.player), "Freeing two locations must deliver twenty-eight entries into two chests");
            List<ItemStack> boxed = chestContents(helper, site);
            helper.assertTrue(boxed.size() == 28, "Every saved entry must survive placement failure and retry");
            for (var entry : owed) helper.assertTrue(boxed.stream().filter(stack -> ItemStack.matches(stack, entry.stack())).count() == 1, "Retry must retain each entry's full NBT exactly once");
            f.assertBackpackStillFull();
            restore.forEach((pos, state) -> {
                if (helper.getLevel().getBlockEntity(pos) instanceof ChestBlockEntity chest) chest.clearContent();
                helper.getLevel().setBlock(pos, state, 3);
            });
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void oneDamageBatchReleasesOneItemAndNextBatchRestoresItsProvider(GameTestHelper helper) {
        Fixture f = new Fixture(helper, Map.of("head", 1, "charm", 1));
        f.fillBackpack();
        ItemStack dependent = marked("earned-head-4"), provider = provider("earned-provider", "charm", "head", 8);
        FirstVicissitudeBossEntity boss = spawnWithHeld(helper, f.player.getUUID(), List.of(
                new VicissitudeCurioLedger.Entry("head", 4, dependent), new VicissitudeCurioLedger.Entry("charm", 0, provider)), "extreme");
        DamageSource source = boss.damageSources().indirectMagic(null, f.player);
        DamageEvents events = new DamageEvents(boss);
        int[] channels = {0};
        helper.assertTrue(boss.getHealth() == 1500, "Registered extreme encounter must really have its authored health pool");
        float[] finalDamage = {6, 14, 14};
        events.damage = event -> event.setNewDamage(finalDamage[channels[0]++]);
        float initialHealth = boss.getHealth();
        NeoForge.EVENT_BUS.register(events);
        try {
            helper.assertTrue(boss.hurt(source, 6), "First channel must reach actual damage resolution");
            helper.assertTrue(Math.abs(boss.getHealth() - (initialHealth - 6)) < 0.001F, "First channel must debit six health");
            helper.assertTrue(boss.hurt(source, 14), "Larger same-tick channel must be accepted");
            helper.assertTrue(Math.abs(boss.getHealth() - (initialHealth - 14)) < 0.001F,
                    "Larger channel must top the batch up to fourteen; actual loss=" + (initialHealth - boss.getHealth())
                            + ", maximum health=" + boss.getMaxHealth());
            helper.assertTrue(boss.hurt(source, 14), "Repeated equal channel must also resolve");
        } finally {
            NeoForge.EVENT_BUS.unregister(events);
        }
        helper.assertTrue(channels[0] == 3 && Math.abs(boss.getHealth() - (initialHealth - 14)) < 0.001F,
                "Three actual final-damage events must cost fourteen health in total");
        helper.assertTrue(f.ledger.entries(f.player.getUUID()).size() == 1
                && heldCount(boss) == 1 && f.stack("charm", 0).isEmpty(),
                "The whole batch must release only its first item, leaving the provider held");
        helper.runAtTickTime(2, () -> {
            f.tickCurios();
            helper.assertTrue(!VicissitudeCurioReturns.deliverAll(f.player) && f.size("head") == 1,
                    "Equipment updates must not release the still boss-held provider");
        });
        helper.runAtTickTime(22, () -> {
            f.player.setPos(boss.position());
            helper.assertTrue(boss.hurt(source, 6), "Next batch must become eligible after the real damage cooldown");
            helper.assertTrue(heldCount(boss) == 0 && ItemStack.matches(provider, f.stack("charm", 0)),
                    "Only the next batch may release the provider into its original charm slot");
        });
        helper.runAtTickTime(24, () -> {
            f.tickCurios();
            helper.assertTrue(f.size("head") == 9 && VicissitudeCurioReturns.deliverAll(f.player), "Earned provider must permit the previously blocked item to return during combat");
            helper.assertTrue(ItemStack.matches(dependent, f.stack("head", 4)), "Dependent must return to head[4], never another slot");
            f.assertBackpackStillFull(); f.assertNoDrops();
            boss.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", timeoutTicks = 40)
    public static void nestedHurtReturnsOneItemForEitherDamageOrder(GameTestHelper helper) {
        Fixture f = new Fixture(helper, Map.of("head", 4));
        List<VicissitudeCurioLedger.Entry> held = entries("nested-batch", 4);
        FirstVicissitudeBossEntity boss = spawnWithHeld(helper, f.player.getUUID(), held, "extreme");
        DamageSource outer = boss.damageSources().playerAttack(f.player);
        DamageSource inner = boss.damageSources().indirectMagic(null, f.player);
        DamageEvents events = new DamageEvents(boss);
        int[] amounts = {2, 10};
        int[] nestedCalls = {0};
        events.hurt = event -> {
            if (event.getSource() == outer) {
                helper.assertTrue(boss.hurt(inner, amounts[1]), "LivingDamageEvent.Pre's nested hit must be accepted");
                nestedCalls[0]++;
            }
        };
        events.damage = event -> event.setNewDamage(event.getSource() == outer ? amounts[0] : amounts[1]);
        float initialHealth = boss.getHealth();
        NeoForge.EVENT_BUS.register(events);
        try {
            helper.assertTrue(boss.hurt(outer, 2), "Outer hit must finish after the nested hit");
        } finally {
            NeoForge.EVENT_BUS.unregister(events);
        }
        helper.assertTrue(nestedCalls[0] == 1 && Math.abs(boss.getHealth() - (initialHealth - 10)) < 0.001F,
                "Outer two and inner ten must cost ten health");
        helper.assertTrue(heldCount(boss) == 3 && ItemStack.matches(held.get(0).stack(), f.stack("head", 0))
                && f.stack("head", 1).isEmpty(), "Nested resolution must return only one original item");
        helper.runAtTickTime(22, () -> {
            f.player.setPos(boss.position());
            amounts[0] = 10; amounts[1] = 2;
            float before = boss.getHealth();
            NeoForge.EVENT_BUS.register(events);
            try {
                helper.assertTrue(boss.hurt(outer, 10), "Reverse-order batch must be accepted after cooldown: stage="
                        + boss.getStage() + ", health=" + boss.getHealth() + ", iframe=" + boss.invulnerableTime
                        + ", nested calls=" + nestedCalls[0] + ", world tick=" + helper.getLevel().getGameTime());
            } finally {
                NeoForge.EVENT_BUS.unregister(events);
            }
            helper.assertTrue(nestedCalls[0] == 2 && Math.abs(boss.getHealth() - (before - 10)) < 0.001F,
                    "Outer ten and inner two must also cost ten health");
            helper.assertTrue(heldCount(boss) == 2 && ItemStack.matches(held.get(1).stack(), f.stack("head", 1))
                    && f.stack("head", 2).isEmpty(), "A second nested batch must return exactly one further item");
            helper.assertTrue(!f.ledger.hasPending(f.player.getUUID()), "Both exact returns must clear their transferred debts");
            f.assertNoDrops(); boss.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK); helper.succeed();
        });
    }

    @GameTest(template = "test_empty")
    public static void cancelledAndFullyAbsorbedHitsDoNotSpendTheBatchReturn(GameTestHelper helper) {
        Fixture f = new Fixture(helper, Map.of("head", 3));
        List<VicissitudeCurioLedger.Entry> held = entries("cancelled-batch", 3);
        FirstVicissitudeBossEntity boss = spawnWithHeld(helper, f.player.getUUID(), held, "extreme");
        DamageSource source = boss.damageSources().indirectMagic(null, f.player);
        float initialHealth = boss.getHealth();
        DamageEvents events = new DamageEvents(boss);
        events.damage = event -> event.setNewDamage(0.0F);
        NeoForge.EVENT_BUS.register(events);
        try {
            helper.assertTrue(!boss.hurt(source, 6), "Cancelled final-damage event must reject the hit");
        } finally {
            NeoForge.EVENT_BUS.unregister(events);
        }
        boss.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION).setBaseValue(1000);
        boss.setAbsorptionAmount(100);
        helper.assertTrue(!boss.hurt(source, 6) && boss.getAbsorptionAmount() < 100,
                "Fully absorbed hit must consume real absorption without accepting health damage");
        helper.assertTrue(boss.getHealth() == initialHealth && heldCount(boss) == 3
                && f.stack("head", 0).isEmpty() && !f.ledger.hasPending(f.player.getUUID()),
                "Cancellation and full absorption must neither debit health nor release an item");
        boss.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION).setBaseValue(1000);
        boss.setAbsorptionAmount(0);
        helper.assertTrue(boss.hurt(source, 6) && boss.hurt(source, 14), "Later positive channels in the same tick must still resolve");
        helper.assertTrue(boss.getHealth() < initialHealth && heldCount(boss) == 2
                && ItemStack.matches(held.get(0).stack(), f.stack("head", 0)) && f.stack("head", 1).isEmpty(),
                "First actual health damage must earn one return; its top-up must not earn another");
        boss.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK); helper.succeed();
    }

    @GameTest(template = "test_empty")
    public static void curioEquipCallbackDamageCannotReleaseASecondItem(GameTestHelper helper) {
        Fixture f = new Fixture(helper, Map.of("head", 3));
        List<VicissitudeCurioLedger.Entry> held = entries("equip-hit-batch", 3);
        FirstVicissitudeBossEntity boss = spawnWithHeld(helper, f.player.getUUID(), held, "extreme");
        DamageSource outer = boss.damageSources().playerAttack(f.player), inner = boss.damageSources().indirectMagic(null, f.player);
        DamageEvents damage = new DamageEvents(boss);
        damage.damage = event -> event.setNewDamage(event.getSource() == outer ? 6 : 14);
        int[] callbacks = {0};
        EquipEvents equip = new EquipEvents(event -> {
            if (event.getEntity() == f.player && callbacks[0]++ == 0)
                helper.assertTrue(boss.hurt(inner, 14), "Real Curios validation must be able to trigger a nested hit");
        });
        float initialHealth = boss.getHealth();
        NeoForge.EVENT_BUS.register(damage);
        NeoForge.EVENT_BUS.register(equip);
        try {
            helper.assertTrue(boss.hurt(outer, 6), "Original hit must finish its return callback");
        } finally {
            NeoForge.EVENT_BUS.unregister(equip);
            NeoForge.EVENT_BUS.unregister(damage);
        }
        helper.assertTrue(callbacks[0] > 0 && Math.abs(boss.getHealth() - (initialHealth - 14)) < 0.001F,
                "Validation callback's larger hit must top up the same batch to fourteen health");
        helper.assertTrue(heldCount(boss) == 2 && ItemStack.matches(held.get(0).stack(), f.stack("head", 0))
                && f.stack("head", 1).isEmpty() && !f.ledger.hasPending(f.player.getUUID()),
                "Return must be reserved before the callback, with only one ownership transfer");
        f.assertNoDrops(); boss.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK); helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "curio_lifecycle", timeoutTicks = 20)
    public static void unloadKeepsBossOwnershipButActualDeletionSettlesOfflineOwnerThroughLoginHandler(GameTestHelper helper) {
        Fixture f = new Fixture(helper, Map.of("head", 1));
        f.equip("head", 0, marked("occupied")); f.fillBackpack();
        List<VicissitudeCurioLedger.Entry> held = entries("deleted-boss", 2);
        FirstVicissitudeBossEntity boss = spawnWithHeld(helper, f.player.getUUID(), held);
        CompoundTag saved = boss.saveWithoutId(new CompoundTag());
        boss.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        helper.assertTrue(!f.ledger.hasPending(f.player.getUUID()), "Chunk unload must not settle a living encounter");
        FirstVicissitudeBossEntity loaded = MaledictEntities.FIRST_VICISSITUDE.get().create(helper.getLevel());
        loaded.load(saved);
        helper.assertTrue(helper.getLevel().addFreshEntity(loaded), "Unloaded encounter must reload with held items");
        BlockPos site = loaded.blockPosition();
        // Exercise a genuine world deletion callback, including mods that call setRemoved directly.
        loaded.setRemoved(Entity.RemovalReason.DISCARDED);
        helper.assertTrue(f.ledger.entries(f.player.getUUID()).size() == 2, "Actual deletion must hand over every held item exactly once");
        helper.assertTrue(f.ledger.entries(f.player.getUUID()).stream().allMatch(entry -> entry.returnSite() != null
                && entry.returnSite().position().equals(site)), "Offline owner must keep the removed boss's final position");
        f.roundTripLedger();
        // FakePlayer has no network channel, so invoke our real login subscriber directly;
        // this tests the return handler, not Forge's unrelated network handshake listeners.
        VicissitudeCurioReturns.onLoggedIn(new PlayerEvent.PlayerLoggedInEvent(f.player));
        helper.runAtTickTime(2, () -> {
            f.tickCurios();
            helper.assertTrue(VicissitudeCurioReturns.deliverAll(f.player), "Login after deletion must still try slots/backpack before final chest delivery");
            List<ItemStack> boxed = chestContents(helper, site);
            helper.assertTrue(boxed.size() == 2, "Offline handoff must deliver both saved curios without duplicating them");
            for (var entry : held) helper.assertTrue(boxed.stream().filter(stack -> ItemStack.matches(stack, entry.stack())).count() == 1, "Deleted boss must preserve full curio NBT");
            f.assertBackpackStillFull(); f.assertNoDrops();
            helper.succeed();
        });
    }

    private static FirstVicissitudeBossEntity spawnWithHeld(GameTestHelper helper, UUID owner,
            List<VicissitudeCurioLedger.Entry> held) {
        return spawnWithHeld(helper, owner, held, "complete");
    }

    private static FirstVicissitudeBossEntity spawnWithHeld(GameTestHelper helper, UUID owner,
            List<VicissitudeCurioLedger.Entry> held, String difficulty) {
        CompoundTag setup = new CompoundTag();
        setup.putByte("VicissitudeStage", (byte) VicissitudeBossStage.PHASE_TWO.ordinal());
        setup.putBoolean("PhaseTwoReached", true);
        setup.putString("VicissitudeDifficulty", difficulty);
        ListTag items = new ListTag();
        for (var entry : held) {
            CompoundTag item = new CompoundTag();
            item.putString("Slot", entry.slotIdentifier()); item.putInt("Index", entry.slotIndex());
            item.put("Stack", entry.stack().save(org.brahypno.maledict.TestSupport.registries())); items.add(item);
        }
        CompoundTag player = new CompoundTag(); player.putUUID("Player", owner); player.put("Items", items);
        ListTag players = new ListTag(); players.add(player); setup.put("ConfiscatedCurios", players);
        FirstVicissitudeBossEntity boss = MaledictEntities.FIRST_VICISSITUDE.get().create(helper.getLevel());
        boss.readAdditionalSaveData(setup);
        boss.setPos(helper.absolutePos(new BlockPos(1, 2, 1)).getCenter());
        helper.assertTrue(helper.getLevel().addFreshEntity(boss), "Registered encounter must join the real level");
        return boss;
    }

    private static int heldCount(FirstVicissitudeBossEntity boss) {
        ListTag players = boss.saveWithoutId(new CompoundTag()).getList("ConfiscatedCurios", 10);
        int count = 0;
        for (int i = 0; i < players.size(); i++) count += players.getCompound(i).getList("Items", 10).size();
        return count;
    }

    private static List<VicissitudeCurioLedger.Entry> entries(String prefix, int count) {
        List<VicissitudeCurioLedger.Entry> entries = new ArrayList<>();
        for (int index = 0; index < count; index++) entries.add(new VicissitudeCurioLedger.Entry("head", index, marked(prefix + ":" + index)));
        return entries;
    }

    private static List<ItemStack> chestContents(GameTestHelper helper, BlockPos site) {
        List<ItemStack> contents = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(site.offset(-4, -1, -4), site.offset(4, 1, 4)))
            if (helper.getLevel().getBlockEntity(pos) instanceof ChestBlockEntity chest)
                for (int index = 0; index < chest.getContainerSize(); index++)
                    if (!chest.getItem(index).isEmpty()) contents.add(chest.getItem(index).copy());
        return contents;
    }

    public static final class EquipEvents {
        private final Consumer<CurioCanEquipEvent> action;
        EquipEvents(Consumer<CurioCanEquipEvent> action) { this.action = action; }
        @SubscribeEvent public void equip(CurioCanEquipEvent event) { action.accept(event); }
    }

    public static final class DamageEvents {
        private final FirstVicissitudeBossEntity boss;
        Consumer<LivingDamageEvent.Pre> hurt = event -> {};
        Consumer<LivingDamageEvent.Pre> damage = event -> {};
        DamageEvents(FirstVicissitudeBossEntity boss) { this.boss = boss; }
        @SubscribeEvent public void damage(LivingDamageEvent.Pre event) { if (event.getEntity() == boss) { hurt.accept(event); damage.accept(event); } }
    }

    private static ItemStack marked(String identity) {
        ItemStack stack = new ItemStack(Items.DIAMOND);
        org.brahypno.maledict.common.item.MaledictItemData.update(stack, data -> data.putString("ReturnIdentity", identity));
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal(identity));
        stack.enchant(org.brahypno.maledict.TestSupport.enchantment(Enchantments.UNBREAKING), 2);
        return stack;
    }

    private static ItemStack provider(String identity, String equippedIn, String grants, int amount) {
        ItemStack stack = marked(identity);
        CuriosApi.addSlotModifier(stack, grants, org.brahypno.maledict.TestSupport.modifierId(UUID.randomUUID()), amount,
                AttributeModifier.Operation.ADD_VALUE, equippedIn);
        return stack;
    }

    private static final class Fixture {
        final GameTestHelper helper;
        final FakePlayer player;
        final ICuriosItemHandler inventory;
        final VicissitudeCurioLedger ledger;

        Fixture(GameTestHelper helper, Map<String, Integer> sizes) {
            this.helper = helper;
            player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "CurioReturnTest"));
            player.setPos(helper.absolutePos(new BlockPos(2, 2, 2)).getCenter());
            inventory = CuriosApi.getCuriosInventory(player).orElseThrow(() -> new IllegalStateException("Missing real Curios capability"));
            Map<String, ICurioStacksHandler> handlers = new LinkedHashMap<>();
            sizes.forEach((id, size) -> handlers.put(id, new CurioStacksHandler(inventory, id, size,
                    true, true, true, ICurio.DropRule.DEFAULT)));
            inventory.setCurios(handlers);
            ledger = VicissitudeCurioLedger.get(helper.getLevel());
        }

        int size(String id) { return inventory.getCurios().get(id).getStacks().getSlots(); }
        ItemStack stack(String id, int index) { return inventory.getCurios().get(id).getStacks().getStackInSlot(index); }

        void equip(String id, int index, ItemStack stack) {
            helper.assertTrue(inventory.getCurios().get(id).getStacks().isItemValid(index, stack), "Fixture item must pass real Curios validation: " + id);
            inventory.setEquippedCurio(id, index, stack.copy());
        }

        void tickCurios() {
            // FakePlayer.tick is a no-op; this is the actual Forge event dispatched by living entity ticks.
            NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.EntityTickEvent.Pre(player));
            NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.EntityTickEvent.Post(player));
        }

        void fillEquipment() {
            for (String id : inventory.getCurios().keySet())
                for (int index = 0; index < size(id); index++)
                    if (inventory.getCurios().get(id).getStacks().getStackInSlot(index).isEmpty())
                        equip(id, index, marked(id + ":" + index));
        }

        void fillBackpack() {
            for (int index = 0; index < player.getInventory().getContainerSize(); index++)
                player.getInventory().setItem(index, new ItemStack(Items.DIRT, 64));
        }

        List<VicissitudeCurioLedger.Entry> snapshot() {
            List<VicissitudeCurioLedger.Entry> result = new ArrayList<>();
            inventory.getCurios().forEach((id, handler) -> {
                for (int index = 0; index < handler.getStacks().getSlots(); index++) {
                    ItemStack stack = handler.getStacks().getStackInSlot(index);
                    if (!stack.isEmpty()) result.add(new VicissitudeCurioLedger.Entry(id, index, stack.copy()));
                }
            });
            return result;
        }

        void equalEntries(List<VicissitudeCurioLedger.Entry> expected, List<VicissitudeCurioLedger.Entry> actual) {
            helper.assertTrue(expected.size() == actual.size(), "Confiscation must capture every equipped stack");
            for (var entry : expected)
                helper.assertTrue(actual.stream().anyMatch(other -> other.slotIdentifier().equals(entry.slotIdentifier())
                        && other.slotIndex() == entry.slotIndex() && ItemStack.matches(entry.stack(), other.stack())), "Lost original entry: " + entry);
        }

        void assertOriginalPositions(List<VicissitudeCurioLedger.Entry> expected) {
            equalEntries(expected, snapshot());
            helper.assertTrue(!ledger.hasPending(player.getUUID()), "Only completed returns may clear the ledger");
        }

        void assertEquipmentEmpty() { helper.assertTrue(snapshot().isEmpty(), "All equipped stacks must be detached"); }

        void assertBackpackStillFull() {
            for (int index = 0; index < player.getInventory().getContainerSize(); index++) {
                ItemStack stack = player.getInventory().getItem(index);
                helper.assertTrue(stack.is(Items.DIRT) && stack.getCount() == 64, "Return must not move curios into the backpack");
            }
        }

        void assertNoDrops() {
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(ItemEntity.class,
                    player.getBoundingBox().inflate(2)).isEmpty(), "Slot shrink/return must not drop confiscated curios");
        }

        void roundTripLedger() {
            var loaded = VicissitudeCurioLedger.load(ledger.save(new CompoundTag(), org.brahypno.maledict.TestSupport.registries()), org.brahypno.maledict.TestSupport.registries());
            ledger.replace(player.getUUID(), loaded.entries(player.getUUID()));
        }
    }
}
