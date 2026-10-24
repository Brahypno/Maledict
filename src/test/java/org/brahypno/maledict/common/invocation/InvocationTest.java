package org.brahypno.maledict.common.invocation;

import com.mojang.authlib.GameProfile;
import com.sammy.malum.common.block.curiosities.spirit_altar.SpiritAltarBlockEntity;
import com.sammy.malum.common.block.curiosities.totem.TotemBaseBlockEntity;
import com.sammy.malum.common.block.curiosities.totem.TotemPoleBlockEntity;
import com.sammy.malum.common.recipe.SpiritInfusionRecipe;
import com.sammy.malum.registry.common.block.BlockRegistry;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.registry.MaledictItems;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class InvocationTest {
    @Test
    void definitionRoundTripPreservesItsMediumAndRejectsUnknownPolicies() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("maledict", "invocation/test");
        for (var profile : InvocationDefinition.Profile.values()) {
            var definition = new InvocationDefinition(id, ResourceLocation.fromNamespaceAndPath("minecraft", "shulker"),
                    "default", true, profile, 0xB28DD1);
            assertEquals(definition, InvocationDefinition.fromJson(id, definition.toJson()));
            var serializer = new InvocationDefinition.Serializer();
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                serializer.toNetwork(buffer, definition);
                assertEquals(definition, serializer.fromNetwork(id, buffer));
                assertEquals(0, buffer.readableBytes());
            } finally {
                buffer.release();
            }
        }
        assertThrows(JsonSyntaxException.class, () -> InvocationDefinition.fromJson(id, JsonParser.parseString(
                "{\"entity\":\"minecraft:shulker\",\"medium\":\"any_wood\",\"spawn_profile\":\"ground\"}").getAsJsonObject()));
        assertThrows(JsonSyntaxException.class, () -> InvocationDefinition.fromJson(id, JsonParser.parseString(
                "{\"entity\":\"minecraft:shulker\",\"medium\":\"soulwood\",\"spawn_profile\":\"arbitrary_nbt\"}").getAsJsonObject()));
    }

    @Test
    void tintIsOptionalAndRejectsMalformedRgbValues() {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("maledict", "invocation/test");
        var json = JsonParser.parseString(
                "{\"entity\":\"minecraft:axolotl\",\"variant\":\"blue\",\"medium\":\"runewood\",\"spawn_profile\":\"water\"}")
                .getAsJsonObject();
        assertEquals(0xFFFFFF, InvocationDefinition.fromJson(id, json).tint());
        json.addProperty("tint", "#718be8");
        assertEquals(0x718BE8, InvocationDefinition.fromJson(id, json).tint());
        for (String invalid : new String[]{"blue", "#123", "#FF718BE8", "#GGGGGG"}) {
            json.addProperty("tint", invalid);
            assertThrows(JsonSyntaxException.class, () -> InvocationDefinition.fromJson(id, json));
        }
    }

    @GameTest(template = "test_empty", batch = "invocation", timeoutTicks = 160)
    public static void paysOnRightClickAndCompletesOnceAfterChangingHands(GameTestHelper helper) {
        var base = arena(helper, false, 0);
        FakePlayer player = player(helper, base, "sniffer", 2);
        click(helper, player, base, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().getCount() == 1 && base.isActiveOrAssembling(),
                "The initial right-click must consume one sigil immediately");
        click(helper, player, base, InteractionHand.MAIN_HAND);
        player.setItemInHand(InteractionHand.OFF_HAND, InvocationSigilItem.create("sniffer"));
        click(helper, player, base, InteractionHand.OFF_HAND);
        helper.assertTrue(player.getMainHandItem().getCount() == 1 && player.getOffhandItem().getCount() == 1,
                "Repeated and offhand interactions must not pay again");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        List<Mob> summoned = new java.util.ArrayList<>();
        Consumer<EntityJoinLevelEvent> observe = event -> {
            if (!event.isCanceled() && event.getEntity() instanceof Mob mob && mob.getType() == EntityType.SNIFFER
                    && area(base).contains(mob.position())) summoned.add(mob);
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, observe);
        helper.runAfterDelay(110, () -> {
            try {
            helper.assertTrue(summoned.size() == 1 && summoned.get(0).isAddedToWorld(),
                    "Real Malum assembly must create exactly one sniffer without requiring a held sigil");
            InvocationLedger.get(helper.getLevel()).complete(helper.getLevel(), base);
            helper.assertTrue(summoned.size() == 1
                    && !InvocationLedger.get(helper.getLevel()).contains(base.getBlockPos()), "A completed rite cannot pay out again");
            helper.assertTrue(helper.getLevel().getBlockEntity(base.getBlockPos()) == base
                    && helper.getLevel().getBlockEntity(base.getBlockPos().above(4)) instanceof TotemPoleBlockEntity,
                    "Ordinary summons must preserve the totem");
            helper.succeed();
            } finally {
                MinecraftForge.EVENT_BUS.unregister(observe);
            }
        });
    }

    @GameTest(template = "test_empty", batch = "invocation", timeoutTicks = 160)
    public static void changingThePolesStopsAssemblyAndAnUnpaidRestartCannotSummon(GameTestHelper helper) {
        var base = arena(helper, false, 11);
        FakePlayer player = player(helper, base, "allay", 1);
        click(helper, player, base, InteractionHand.MAIN_HAND);
        var pole = (TotemPoleBlockEntity) helper.getLevel().getBlockEntity(base.getBlockPos().above());
        pole.type = com.sammy.malum.registry.common.SpiritTypeRegistry.WICKED_SPIRIT;
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(!base.isActiveOrAssembling() && drops(helper, base).size() == 1,
                    "Changing spirits must stop the original assembly as well as refunding payment");
            pole.type = InvocationRite.get().spirits.get(0);
            click(helper, player, base, InteractionHand.MAIN_HAND);
        });
        helper.runAfterDelay(125, () -> {
            helper.assertTrue(mobs(helper, base, EntityType.ALLAY).isEmpty() && drops(helper, base).size() == 1,
                    "An empty-hand restart after refund must not produce a creature or another refund");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", batch = "invocation")
    public static void wrongWoodInvalidSigilAndProtectedInteractionNeverPay(GameTestHelper helper) {
        var base = arena(helper, false, 1);
        FakePlayer player = player(helper, base, "shulker", 2);
        click(helper, player, base, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().getCount() == 2 && !base.isActiveOrAssembling(),
                "A hostile sigil on Runewood must not pay or activate");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(MaledictItems.INVOCATION_SIGIL.get(), 2));
        click(helper, player, base, InteractionHand.MAIN_HAND);
        helper.assertTrue(player.getMainHandItem().getCount() == 2 && !base.isActiveOrAssembling(), "Invalid NBT must not pay");
        player.setItemInHand(InteractionHand.MAIN_HAND, InvocationSigilItem.create("sniffer"));
        Consumer<PlayerInteractEvent.RightClickBlock> deny = event -> {
            if (event.getEntity() == player) event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, deny);
        try {
            click(helper, player, base, InteractionHand.MAIN_HAND);
        } finally {
            MinecraftForge.EVENT_BUS.unregister(deny);
        }
        helper.assertTrue(player.getMainHandItem().getCount() == 1 && !base.isActiveOrAssembling(),
                "Earlier protection cancellations must remain authoritative");
        var soulwood = arena(helper, true, 2);
        FakePlayer other = player(helper, soulwood, "sniffer", 1);
        click(helper, other, soulwood, InteractionHand.MAIN_HAND);
        helper.assertTrue(other.getMainHandItem().getCount() == 1 && !soulwood.isActiveOrAssembling(),
                "A friendly sigil must reject Soulwood");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "invocation")
    public static void brokenTotemDropsTheOriginalSigilExactlyOnce(GameTestHelper helper) {
        var base = arena(helper, false, 3);
        FakePlayer player = player(helper, base, "allay", 2);
        player.getMainHandItem().setHoverName(Component.literal("Remember this creature"));
        player.getMainHandItem().getOrCreateTag().putInt("TestMarker", 1234);
        ItemStack paid = player.getMainHandItem().copyWithCount(1);
        click(helper, player, base, InteractionHand.MAIN_HAND);
        helper.getLevel().setBlockAndUpdate(base.getBlockPos(), Blocks.AIR.defaultBlockState());
        helper.runAfterDelay(15, () -> {
            var drops = drops(helper, base);
            helper.assertTrue(drops.size() == 1 && drops.get(0).getItem().getCount() == 1
                    && ItemStack.isSameItemSameTags(paid, drops.get(0).getItem()), "A broken base must return the exact paid stack once");
            InvocationLedger.get(helper.getLevel()).complete(helper.getLevel(), base);
            InvocationLedger.get(helper.getLevel()).tick(helper.getLevel());
            helper.assertTrue(drops(helper, base).size() == 1 && mobs(helper, base, EntityType.ALLAY).isEmpty(),
                    "The failure cannot later create another refund or a mob");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", batch = "invocation", timeoutTicks = 160)
    public static void cancelledMobJoinRefundsAndCancelledDropWaitsForRetry(GameTestHelper helper) {
        var base = arena(helper, false, 4);
        FakePlayer player = player(helper, base, "sniffer", 1);
        AtomicBoolean allowDrop = new AtomicBoolean(false);
        Consumer<EntityJoinLevelEvent> deny = event -> {
            if (!area(base).contains(event.getEntity().position())) return;
            if (event.getEntity().getType() == EntityType.SNIFFER
                    || (event.getEntity() instanceof ItemEntity item
                    && item.getItem().is(MaledictItems.INVOCATION_SIGIL.get()) && !allowDrop.get())) event.setCanceled(true);
        };
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, deny);
        click(helper, player, base, InteractionHand.MAIN_HAND);
        helper.runAfterDelay(110, () -> {
            try {
                helper.assertTrue(mobs(helper, base, EntityType.SNIFFER).isEmpty() && drops(helper, base).isEmpty()
                        && InvocationLedger.get(helper.getLevel()).contains(base.getBlockPos()),
                        "Cancelled mob and item joins must leave a pending refund");
            } catch (Throwable failure) {
                MinecraftForge.EVENT_BUS.unregister(deny);
                throw failure;
            }
            allowDrop.set(true);
        });
        helper.runAfterDelay(130, () -> {
            MinecraftForge.EVENT_BUS.unregister(deny);
            helper.assertTrue(drops(helper, base).size() == 1 && mobs(helper, base, EntityType.SNIFFER).isEmpty()
                    && !InvocationLedger.get(helper.getLevel()).contains(base.getBlockPos()),
                    "Once the item join is allowed, exactly one physical refund must complete");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", batch = "invocation", timeoutTicks = 170)
    public static void variantsAndHostileMediumSurviveRealInitialization(GameTestHelper helper) {
        var cowBase = arena(helper, false, 5);
        var axolotlBase = arena(helper, false, 6);
        var shulkerBase = arena(helper, true, 7);
        var horseBase = arena(helper, false, 8);
        BlockPos center = axolotlBase.getBlockPos().relative(Direction.NORTH, 3);
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-2, 0, -2), center.offset(2, 0, 2))) {
            helper.getLevel().setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
        }
        click(helper, player(helper, cowBase, "brown_mooshroom", 1), cowBase, InteractionHand.MAIN_HAND);
        click(helper, player(helper, axolotlBase, "blue_axolotl", 1), axolotlBase, InteractionHand.MAIN_HAND);
        click(helper, player(helper, shulkerBase, "shulker", 1), shulkerBase, InteractionHand.MAIN_HAND);
        click(helper, player(helper, horseBase, "skeleton_horse", 1), horseBase, InteractionHand.MAIN_HAND);
        helper.runAfterDelay(110, () -> {
            var cows = mobs(helper, cowBase, EntityType.MOOSHROOM);
            var axolotls = mobs(helper, axolotlBase, EntityType.AXOLOTL);
            var horses = mobs(helper, horseBase, EntityType.SKELETON_HORSE);
            helper.assertTrue(cows.size() == 1 && ((MushroomCow) cows.get(0)).getVariant() == MushroomCow.MushroomType.BROWN,
                    "Mooshroom initialization must preserve the brown variant");
            helper.assertTrue(axolotls.size() == 1 && ((Axolotl) axolotls.get(0)).getVariant() == Axolotl.Variant.BLUE,
                    "Axolotl initialization must preserve the blue variant");
            helper.assertTrue(mobs(helper, shulkerBase, EntityType.SHULKER).size() == 1,
                    "Soulwood must summon a shulker on its supported floor");
            helper.assertTrue(horses.size() == 1 && !((SkeletonHorse) horses.get(0)).isTrap(),
                    "A non-hostile skeleton horse must use Runewood without starting a trap");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", batch = "invocation")
    public static void savedPendingPaymentRestoresAsOnePhysicalRefund(GameTestHelper helper) {
        var base = arena(helper, false, 9);
        ItemStack sigil = InvocationSigilItem.create("allay");
        sigil.getOrCreateTag().putString("RefundMarker", "saved");
        InvocationLedger original = new InvocationLedger();
        original.begin(helper.getLevel(), InvocationRite.inspect(helper.getLevel(), base),
                InvocationDefinition.find(helper.getLevel(), sigil), sigil);
        InvocationLedger restored = InvocationLedger.load(original.save(new CompoundTag()));
        // Exercises production save/load and physical delivery, rather than claiming a full process restart.
        for (int tick = 1; tick <= 20; tick++) helper.runAfterDelay(tick, () -> restored.tick(helper.getLevel()));
        helper.runAfterDelay(22, () -> {
            helper.assertTrue(drops(helper, base).size() == 1
                    && ItemStack.isSameItemSameTags(sigil, drops(helper, base).get(0).getItem())
                    && !restored.contains(base.getBlockPos()), "Restored payments must return the original sigil only once");
            helper.succeed();
        });
    }

    @GameTest(template = "test_empty", batch = "invocation")
    public static void actualAltarCompletionConsumesOrdinaryBucketWithoutReturningAnEmptyOne(GameTestHelper helper) {
        var base = arena(helper, false, 10);
        var level = helper.getLevel();
        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath("maledict", "spirit_infusion/invocation_blue_axolotl");
        SpiritInfusionRecipe recipe = (SpiritInfusionRecipe) level.getRecipeManager().byKey(recipeId).orElseThrow();
        for (int variant = 0; variant <= 4; variant++) {
            ItemStack bucket = new ItemStack(Items.AXOLOTL_BUCKET);
            bucket.getOrCreateTag().putInt("Variant", variant);
            helper.assertTrue(recipe.doesInputMatch(bucket) == (variant != 4), "Only ordinary axolotl variants may be consumed");
        }
        BlockPos altarPos = base.getBlockPos().east(3);
        level.setBlockAndUpdate(altarPos, BlockRegistry.SPIRIT_ALTAR.get().defaultBlockState());
        SpiritAltarBlockEntity altar = (SpiritAltarBlockEntity) level.getBlockEntity(altarPos);
        ItemStack bucket = new ItemStack(Items.AXOLOTL_BUCKET);
        bucket.getOrCreateTag().putInt("Variant", 2);
        bucket.setHoverName(Component.literal("Do not copy me"));
        altar.inventory.setStackInSlot(0, bucket);
        altar.recipe = recipe;
        // Runs Malum's actual completion path; pedestal collection/animation are not simulated here.
        altar.craft();
        helper.assertTrue(altar.inventory.getStackInSlot(0).isEmpty(), "The entire bucket item must be consumed");
        var outputs = level.getEntitiesOfClass(ItemEntity.class, area(base));
        helper.assertTrue(outputs.size() == 1 && outputs.get(0).getItem().is(MaledictItems.INVOCATION_SIGIL.get())
                && !outputs.get(0).getItem().hasCustomHoverName(), "Only the recipe's sigil NBT must remain; no empty bucket is returned");
        InvocationDefinition definition = InvocationDefinition.find(level, outputs.get(0).getItem());
        helper.assertTrue(definition != null && definition.variant().equals("blue"), "Altar output must retain the target definition");
        var serializer = new InvocationDefinition.Serializer();
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            serializer.toNetwork(buffer, definition);
            helper.assertTrue(serializer.fromNetwork(definition.id(), buffer).equals(definition), "Definition sync must preserve medium and variant");
        } finally {
            buffer.release();
        }
        helper.succeed();
    }

    private static TotemBaseBlockEntity arena(GameTestHelper helper, boolean soulwood, int layer) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 65 + layer * 12, 2));
        // GameTest's world is reused between Gradle runs; our raised arenas are outside the template's cleanup box.
        for (var entity : helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Entity.class,
                new AABB(pos).inflate(7, 5, 7), entity -> entity instanceof Mob || entity instanceof ItemEntity)) {
            entity.discard();
        }
        for (BlockPos clear : BlockPos.betweenClosed(pos.offset(-6, 0, -6), pos.offset(6, 6, 6))) {
            helper.getLevel().setBlockAndUpdate(clear, Blocks.AIR.defaultBlockState());
        }
        for (BlockPos floor : BlockPos.betweenClosed(pos.offset(-6, -1, -6), pos.offset(6, -1, 6))) {
            helper.getLevel().setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        }
        helper.getLevel().setBlockAndUpdate(pos, (soulwood ? BlockRegistry.SOULWOOD_TOTEM_BASE : BlockRegistry.RUNEWOOD_TOTEM_BASE)
                .get().defaultBlockState());
        TotemBaseBlockEntity base = (TotemBaseBlockEntity) helper.getLevel().getBlockEntity(pos);
        for (int index = 0; index < 4; index++) {
            BlockPos at = pos.above(index + 1);
            helper.getLevel().setBlockAndUpdate(at, (soulwood ? BlockRegistry.SOULWOOD_TOTEM_POLE : BlockRegistry.RUNEWOOD_TOTEM_POLE)
                    .get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
            ((TotemPoleBlockEntity) helper.getLevel().getBlockEntity(at)).type = InvocationRite.get().spirits.get(index);
        }
        return base;
    }

    private static FakePlayer player(GameTestHelper helper, TotemBaseBlockEntity base, String target, int count) {
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "Invocation")) {
            @Override public void displayClientMessage(Component message, boolean overlay) { }
        };
        player.moveTo(base.getBlockPos().getX() + 5.5D, base.getBlockPos().getY(), base.getBlockPos().getZ() + .5D);
        player.setItemInHand(InteractionHand.MAIN_HAND, InvocationSigilItem.create(target).copyWithCount(count));
        return player;
    }

    private static void click(GameTestHelper helper, FakePlayer player, TotemBaseBlockEntity base, InteractionHand hand) {
        var hit = new BlockHitResult(Vec3.atCenterOf(base.getBlockPos()), Direction.NORTH, base.getBlockPos(), false);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getItemInHand(hand), hand, hit);
    }

    private static AABB area(TotemBaseBlockEntity base) {
        return new AABB(base.getBlockPos()).inflate(7, 5, 7);
    }

    private static List<Mob> mobs(GameTestHelper helper, TotemBaseBlockEntity base, EntityType<?> type) {
        return helper.getLevel().getEntitiesOfClass(Mob.class, area(base), mob -> mob.getType() == type);
    }

    private static List<ItemEntity> drops(GameTestHelper helper, TotemBaseBlockEntity base) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area(base), item -> item.getItem().is(MaledictItems.INVOCATION_SIGIL.get()));
    }
}
