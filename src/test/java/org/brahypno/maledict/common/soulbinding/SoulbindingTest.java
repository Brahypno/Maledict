package org.brahypno.maledict.common.soulbinding;

import com.mojang.authlib.GameProfile;
import com.sammy.malum.common.block.curiosities.totem.TotemBaseBlockEntity;
import com.sammy.malum.common.block.curiosities.totem.TotemPoleBlockEntity;
import com.sammy.malum.registry.common.block.BlockRegistry;
import net.minecraft.advancements.Advancement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.common.MaledictTags;
import org.brahypno.maledict.registry.MaledictItems;
import team.lodestar.lodestone.helpers.DamageTypeHelper;

import java.util.UUID;

/**
 * End-to-end soulbinding flow: Soulwood arms the main-hand tool and spends a seal, the one-time rite
 * resets, the first melee kill records the victim snapshot and exposes it as a main-hand attribute,
 * and Runewood releases the spirit again.
 *
 * <p>Forge refuses normal advancement awards for fake players, so the test grants the progress
 * directly; the ritual's own {@link Soulbinding#knows} gate is still exercised.
 */
@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class SoulbindingTest {
    @GameTest(template = "test_empty", batch = "soulbinding", timeoutTicks = 300)
    public static void soulwoodArmsBindsFirstKillAndRunewoodReleases(GameTestHelper helper) {
        helper.assertTrue(SoulbindingRite.get() != null, "the soulbinding rite must be installed");
        TotemBaseBlockEntity soulwood = arena(helper, true, 18);
        final String[] feedback = {""};
        FakePlayer player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "Soulbinding")) {
            @Override
            public void displayClientMessage(Component message, boolean overlay) {
                feedback[0] = message.getString();
            }
        };
        player.moveTo(soulwood.getBlockPos().getX() + 5.5D, soulwood.getBlockPos().getY(),
                soulwood.getBlockPos().getZ() + .5D);

        Advancement advancement = helper.getLevel().getServer().getAdvancements()
                .getAdvancement(Soulbinding.ADVANCEMENT);
        helper.assertTrue(advancement != null, "the soulbinding advancement must be loaded");
        // Forge deliberately refuses PlayerAdvancements#award for fake players.
        player.getAdvancements().getOrStartProgress(advancement).grantProgress("impossible");
        helper.assertTrue(Soulbinding.knows(player), "the advancement must read back as known");
        helper.assertTrue(SoulbindingRite.matches(helper.getLevel(), soulwood),
                "the built poles must match the rite");

        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        helper.assertTrue(sword.is(MaledictTags.SOUL_BINDABLE),
                "iron swords must be in the soul_bindable tag");
        player.setItemInHand(InteractionHand.MAIN_HAND, sword);
        player.setItemInHand(InteractionHand.OFF_HAND,
                new ItemStack(MaledictItems.SOULBINDING_SEAL.get(), 2));

        click(helper, player, soulwood);
        helper.assertTrue(SoulbindingData.state(sword) == SoulbindingData.State.ARMED,
                "Soulwood must arm the main-hand tool; state=" + SoulbindingData.state(sword)
                        + " feedback='" + feedback[0] + "' base=" + soulwood.state
                        + " assembling=" + soulwood.isActiveOrAssembling());
        helper.assertTrue(player.getOffhandItem().getCount() == 1,
                "Arming must consume exactly one seal");
        helper.assertTrue(soulwood.isActiveOrAssembling(), "the totem must start assembling");

        helper.runAfterDelay(120, () -> {
            helper.assertTrue(soulwood.state == TotemBaseBlockEntity.TotemRiteState.IDLE,
                    "the one-time dormant rite must reset the totem");
            helper.assertTrue(SoulbindingData.state(sword) == SoulbindingData.State.ARMED,
                    "the rite effect must not arm or bind a second time");

            Cow actionTarget = EntityType.COW.create(helper.getLevel());
            helper.assertTrue(actionTarget != null, "action target must exist");
            actionTarget.moveTo(player.getX() + 1.0D, player.getY(), player.getZ());
            helper.getLevel().addFreshEntity(actionTarget);
            player.attack(actionTarget);

            Zombie zombie = EntityType.ZOMBIE.create(helper.getLevel());
            helper.assertTrue(zombie != null, "zombie must exist");
            zombie.moveTo(player.getX() + 2.0D, player.getY(), player.getZ());
            zombie.setHealth(0.01F);
            helper.getLevel().addFreshEntity(zombie);
            zombie.hurt(DamageTypeHelper.create(helper.getLevel(), DamageTypes.MAGIC, player), 1.0F);
            helper.assertTrue(SoulbindingData.state(sword) == SoulbindingData.State.BOUND,
                    "the first valid kill must bind the spirit even when the killing damage type is not melee");
            SoulbindingData.Snapshot snapshot = SoulbindingData.snapshot(sword);
            helper.assertTrue(snapshot != null
                            && BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.ZOMBIE)
                                    .equals(snapshot.entityType()),
                    "the snapshot must record the killed entity type");
            helper.assertTrue(sword.getAttributeModifiers(EquipmentSlot.MAINHAND)
                                     .get(Attributes.ATTACK_DAMAGE).stream()
                                     .anyMatch(modifier -> modifier.getId()
                                             .equals(SoulbindingData.BONUSES.get(0).uuid())),
                    "the bound snapshot must surface as a main-hand attribute modifier");

            TotemBaseBlockEntity runewood = arena(helper, false, 19);
            player.moveTo(runewood.getBlockPos().getX() + 5.5D, runewood.getBlockPos().getY(),
                    runewood.getBlockPos().getZ() + .5D);
            click(helper, player, runewood);
            helper.assertTrue(SoulbindingData.state(sword) == SoulbindingData.State.EMPTY,
                    "Runewood must release the bound spirit");
            helper.assertTrue(sword.getAttributeModifiers(EquipmentSlot.MAINHAND)
                                     .get(Attributes.ATTACK_DAMAGE).stream()
                                     .noneMatch(modifier -> modifier.getId()
                                             .equals(SoulbindingData.BONUSES.get(0).uuid())),
                    "releasing must remove the attribute modifier");
            helper.succeed();
        });
    }

    private static TotemBaseBlockEntity arena(GameTestHelper helper, boolean soulwood, int layer) {
        BlockPos pos = helper.absolutePos(new BlockPos(2, 65 + layer * 12, 2));
        for (var entity : helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.Entity.class,
                new AABB(pos).inflate(7, 5, 7), entity -> entity instanceof net.minecraft.world.entity.Mob)) {
            entity.discard();
        }
        for (BlockPos clear : BlockPos.betweenClosed(pos.offset(-6, 0, -6), pos.offset(6, 6, 6))) {
            helper.getLevel().setBlockAndUpdate(clear, Blocks.AIR.defaultBlockState());
        }
        for (BlockPos floor : BlockPos.betweenClosed(pos.offset(-6, -1, -6), pos.offset(6, -1, 6))) {
            helper.getLevel().setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
        }
        helper.getLevel().setBlockAndUpdate(pos,
                (soulwood ? BlockRegistry.SOULWOOD_TOTEM_BASE : BlockRegistry.RUNEWOOD_TOTEM_BASE)
                        .get().defaultBlockState());
        TotemBaseBlockEntity base = (TotemBaseBlockEntity) helper.getLevel().getBlockEntity(pos);
        for (int index = 0; index < SoulbindingRite.get().spirits.size(); index++) {
            BlockPos at = pos.above(index + 1);
            helper.getLevel().setBlockAndUpdate(at,
                    (soulwood ? BlockRegistry.SOULWOOD_TOTEM_POLE : BlockRegistry.RUNEWOOD_TOTEM_POLE)
                            .get().defaultBlockState()
                            .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
            ((TotemPoleBlockEntity) helper.getLevel().getBlockEntity(at)).type =
                    SoulbindingRite.get().spirits.get(index);
        }
        return base;
    }

    private static void click(GameTestHelper helper, FakePlayer player, TotemBaseBlockEntity base) {
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(base.getBlockPos()), Direction.NORTH,
                base.getBlockPos(), false);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getMainHandItem(),
                InteractionHand.MAIN_HAND, hit);
    }

    private SoulbindingTest() {
    }
}
