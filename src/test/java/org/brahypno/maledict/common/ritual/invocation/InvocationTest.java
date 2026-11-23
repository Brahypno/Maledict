package org.brahypno.maledict.common.ritual.invocation;

import com.mojang.authlib.GameProfile;
import com.sammy.malum.common.block.curiosities.totem.TotemBaseBlockEntity;
import com.sammy.malum.common.block.curiosities.totem.TotemPoleBlockEntity;
import com.sammy.malum.registry.common.block.MalumBlocks;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.brahypno.maledict.TestSupport;
import com.sammy.malum.core.systems.spirit.SpiritTypeProperty;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.registry.MaledictItems;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class InvocationTest {
    @Test
    void definitionRoundTripPreservesItsMediumAndRejectsUnknownPolicies() {
        for (var profile : InvocationDefinition.Profile.values()) {
            var definition = new InvocationDefinition(ResourceLocation.fromNamespaceAndPath("minecraft", "shulker"),
                    "default", true, profile, 0xB28DD1);
            assertEquals(definition, InvocationDefinition.CODEC.codec().parse(JsonOps.INSTANCE, InvocationDefinition.CODEC.codec().encodeStart(JsonOps.INSTANCE, definition).getOrThrow()).getOrThrow());
            var serializer = new InvocationDefinition.Serializer();
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), TestSupport.registries(), ConnectionType.NEOFORGE);
            try {
                serializer.streamCodec().encode(buffer, definition);
                assertEquals(definition, serializer.streamCodec().decode(buffer));
                assertEquals(0, buffer.readableBytes());
            } finally {
                buffer.release();
            }
        }
        assertThrows(IllegalStateException.class, () -> InvocationDefinition.CODEC.codec().parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"entity\":\"minecraft:shulker\",\"medium\":\"any_wood\",\"spawn_profile\":\"ground\"}").getAsJsonObject()).getOrThrow());
        assertThrows(IllegalStateException.class, () -> InvocationDefinition.CODEC.codec().parse(JsonOps.INSTANCE, JsonParser.parseString(
                "{\"entity\":\"minecraft:shulker\",\"medium\":\"soulwood\",\"spawn_profile\":\"arbitrary_nbt\"}").getAsJsonObject()).getOrThrow());
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
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, observe);
        helper.runAfterDelay(110, () -> {
            try {
            helper.assertTrue(summoned.size() == 1 && helper.getLevel().getEntity(summoned.get(0).getUUID()) == summoned.get(0),
                    "Real Malum assembly must create exactly one sniffer without requiring a held sigil");
            InvocationLedger.get(helper.getLevel()).complete(helper.getLevel(), base);
            helper.assertTrue(summoned.size() == 1
                    && !InvocationLedger.get(helper.getLevel()).contains(base.getBlockPos()), "A completed rite cannot pay out again");
            helper.assertTrue(helper.getLevel().getBlockEntity(base.getBlockPos()) == base
                    && helper.getLevel().getBlockEntity(base.getBlockPos().above(4)) instanceof TotemPoleBlockEntity,
                    "Ordinary summons must preserve the totem");
            helper.succeed();
            } finally {
                NeoForge.EVENT_BUS.unregister(observe);
            }
        });
    }

    @GameTest(template = "test_empty", batch = "invocation", timeoutTicks = 160)
    public static void changingThePolesStopsAssemblyAndAnUnpaidRestartCannotSummon(GameTestHelper helper) {
        var base = arena(helper, false, 11);
        FakePlayer player = player(helper, base, "allay", 1);
        click(helper, player, base, InteractionHand.MAIN_HAND);
        var pole = (TotemPoleBlockEntity) helper.getLevel().getBlockEntity(base.getBlockPos().above());
        var original = pole.getBlockState();
        // The spirit is fixed at block-entity construction in 1.21; replace the carved pole.
        helper.getLevel().setBlockAndUpdate(pole.getBlockPos(), Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(pole.getBlockPos(), SpiritTypeProperty.setSpiritType(original, com.sammy.malum.registry.common.magic.MalumSpiritTypes.WICKED_SPIRIT.get()));
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(!base.isActiveOrAssembling() && drops(helper, base).size() == 1,
                    "Changing spirits must stop the original assembly as well as refunding payment");
            helper.getLevel().setBlockAndUpdate(pole.getBlockPos(), Blocks.AIR.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(pole.getBlockPos(), original);
            click(helper, player, base, InteractionHand.MAIN_HAND);
        });
        helper.runAfterDelay(125, () -> {
            helper.assertTrue(mobs(helper, base, EntityType.ALLAY).isEmpty() && drops(helper, base).size() == 1,
                    "An empty-hand restart after refund must not produce a creature or another refund");
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
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, deny);
        click(helper, player, base, InteractionHand.MAIN_HAND);
        helper.runAfterDelay(110, () -> {
            try {
                helper.assertTrue(mobs(helper, base, EntityType.SNIFFER).isEmpty() && drops(helper, base).isEmpty()
                        && InvocationLedger.get(helper.getLevel()).contains(base.getBlockPos()),
                        "Cancelled mob and item joins must leave a pending refund");
            } catch (Throwable failure) {
                NeoForge.EVENT_BUS.unregister(deny);
                throw failure;
            }
            allowDrop.set(true);
        });
        helper.runAfterDelay(130, () -> {
            NeoForge.EVENT_BUS.unregister(deny);
            helper.assertTrue(drops(helper, base).size() == 1 && mobs(helper, base, EntityType.SNIFFER).isEmpty()
                    && !InvocationLedger.get(helper.getLevel()).contains(base.getBlockPos()),
                    "Once the item join is allowed, exactly one physical refund must complete");
            helper.succeed();
        });
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
        helper.getLevel().setBlockAndUpdate(pos, (soulwood ? MalumBlocks.SOULWOOD_TOTEM_BASE : MalumBlocks.RUNEWOOD_TOTEM_BASE)
                .get().defaultBlockState());
        TotemBaseBlockEntity base = (TotemBaseBlockEntity) helper.getLevel().getBlockEntity(pos);
        for (int index = 0; index < 4; index++) {
            BlockPos at = pos.above(index + 1);
            var state = (soulwood ? MalumBlocks.SOULWOOD_TOTEM_POLE : MalumBlocks.RUNEWOOD_TOTEM_POLE)
                    .get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
            helper.getLevel().setBlockAndUpdate(at, SpiritTypeProperty.setSpiritType(state, InvocationRite.get().getSpirits().get(index).get()));
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
