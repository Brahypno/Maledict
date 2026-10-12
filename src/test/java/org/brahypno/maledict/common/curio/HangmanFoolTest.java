package org.brahypno.maledict.common.curio;

import com.mojang.authlib.GameProfile;
import com.sammy.malum.common.entity.bolt.HexBoltEntity;
import com.sammy.malum.common.entity.bolt.AuricFlameBoltEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.*;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingUseTotemEvent;
import net.minecraftforge.event.entity.living.ShieldBlockEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.common.entity.HangmanTrapEntity;
import org.brahypno.maledict.common.entity.SpiritArrowEntity;
import org.brahypno.maledict.common.item.HangmanFoolItem;
import org.brahypno.maledict.common.item.SpiritArrowType;
import org.brahypno.maledict.mixin.ProjectileAccess;
import org.brahypno.maledict.registry.MaledictItems;
import team.lodestar.lodestone.registry.common.LodestoneAttributeRegistry;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurio;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.common.inventory.CurioStacksHandler;

import java.util.Map;
import java.util.UUID;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class HangmanFoolTest {
    @GameTest(template = "test_empty", batch = "hangman_fool")
    public static void stageSequencePreservesNbtAndRequiresEveryPreviousMarker(GameTestHelper helper) {
        ItemStack stack = new ItemStack(MaledictItems.HANGMAN_FOOL.get());
        helper.assertTrue(stack.getTag() == null && HangmanFoolItem.stage(stack) == 0,
                "A completely untagged item must already be stage zero");
        stack.setHoverName(Component.literal("preserved"));
        stack.enchant(Enchantments.UNBREAKING, 2);
        helper.assertTrue(HangmanFoolItem.stage(stack) == 0, "No stage NBT means stage zero");
        for (int stage = 1; stage <= 4; stage++) {
            helper.assertTrue(HangmanFoolItem.advance(stack), "Upgrade must add the next marker");
            stack = ItemStack.of(stack.save(new CompoundTag()));
            helper.assertTrue(HangmanFoolItem.stage(stack) == stage, "Stage sequence must be 0 -> 1 -> 2 -> 3 -> 4");
            helper.assertTrue(stack.getHoverName().getString().equals("preserved")
                    && stack.getEnchantmentLevel(Enchantments.UNBREAKING) == 2, "Upgrade must preserve other NBT");
        }
        helper.assertTrue(!HangmanFoolItem.advance(stack), "There is no stage five");
        stack.getOrCreateTag().getCompound(HangmanFoolItem.STAGES_TAG).remove("stage_1");
        helper.assertTrue(HangmanFoolItem.stage(stack) == 0, "Stage two cannot skip stage one");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "hangman_fool")
    public static void realCuriosEquipHalvesAttackAndMultipliesMagicThenUnequipRestores(GameTestHelper helper) {
        Fixture f = new Fixture(helper, 0);
        double attack = f.player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        double magic = f.player.getAttributeValue(LodestoneAttributeRegistry.MAGIC_PROFICIENCY.get());
        f.tickCurios();
        equal(helper, attack * 0.5D, f.player.getAttributeValue(Attributes.ATTACK_DAMAGE), "Attack total");
        equal(helper, magic * 1.5D, f.player.getAttributeValue(LodestoneAttributeRegistry.MAGIC_PROFICIENCY.get()), "Magic total");
        var item = (HangmanFoolItem) f.stack().getItem();
        var modifiers = item.getAttributeModifiers(new SlotContext("delusion", f.player, 0, false, true),
                UUID.randomUUID(), f.stack());
        helper.assertTrue(modifiers.values().stream().allMatch(m -> m.getOperation() == AttributeModifier.Operation.MULTIPLY_TOTAL),
                "Both modifiers must use MULTIPLY_TOTAL");
        helper.assertTrue(!item.canEquip(new SlotContext("charm", f.player, 0, false, true), f.stack()), "Vision slot only");
        f.equip(2);
        f.tickCurios();
        equal(helper, attack * 0.5D, f.player.getAttributeValue(Attributes.ATTACK_DAMAGE), "Upgrade must not double apply attack");
        equal(helper, magic * 1.5D, f.player.getAttributeValue(LodestoneAttributeRegistry.MAGIC_PROFICIENCY.get()), "Upgrade preserves magic");
        f.inventory.setEquippedCurio("delusion", 0, ItemStack.EMPTY);
        f.tickCurios();
        equal(helper, attack, f.player.getAttributeValue(Attributes.ATTACK_DAMAGE), "Unequip attack");
        equal(helper, magic, f.player.getAttributeValue(LodestoneAttributeRegistry.MAGIC_PROFICIENCY.get()), "Unequip magic");
        f.finish();
    }

    @GameTest(template = "test_empty", batch = "hangman_fool")
    public static void restrictionsStartAtTwoAndStopAnAlreadyRaisedShield(GameTestHelper helper) {
        Fixture f = new Fixture(helper, 1);
        assertRestrictions(helper, f, false);
        f.player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
        f.player.startUsingItem(InteractionHand.OFF_HAND);
        helper.assertTrue(f.player.isUsingItem(), "Stage one may raise a shield");
        f.equip(2);
        f.tickCurios();
        helper.assertTrue(!f.player.isUsingItem(), "Equipping stage two must stop an existing block");
        assertRestrictions(helper, f, true);
        f.player.startUsingItem(InteractionHand.OFF_HAND);
        helper.assertTrue(!f.player.isUsingItem(), "Actual shield start must be rejected");
        f.finish();
    }

    @GameTest(template = "test_empty", batch = "hangman_fool")
    public static void reflectedFastArrowKeepsSpeedFacingAndProducesTrap(GameTestHelper helper) {
        Fixture f = new Fixture(helper, 2);
        f.player.setHealth(4); // 80% missing -> capped at 100%.
        Arrow arrow = new Arrow(helper.getLevel(), 0, 0, 0);
        Vec3 incoming = new Vec3(8, -2, 3);
        arrow.setDeltaMovement(incoming);
        arrow.setYRot(-20);
        arrow.yRotO = -20;
        ProjectileImpactEvent hit = new ProjectileImpactEvent(arrow, new EntityHitResult(f.player));
        MinecraftForge.EVENT_BUS.post(hit);
        helper.assertTrue(hit.isCanceled(), "Reflection must precede the original damage branch");
        helper.assertTrue(arrow.getOwner() == f.player, "Reflection transfers ownership");
        equal(helper, incoming.length(), arrow.getDeltaMovement().length(), "Reflection preserves speed");
        equal(helper, 0, arrow.getDeltaMovement().add(incoming).length(), "Reflection reverses all components");
        double yaw = Math.toRadians(arrow.getYRot());
        double pitch = Math.toRadians(arrow.getXRot());
        Vec3 renderedForward = new Vec3(Math.sin(yaw) * Math.cos(pitch), Math.sin(pitch),
                Math.cos(yaw) * Math.cos(pitch));
        equal(helper, 1, renderedForward.dot(arrow.getDeltaMovement().normalize()), "Arrow tip follows reversed flight");
        equal(helper, arrow.getYRot(), arrow.yRotO, "Previous yaw cannot point the tail forward");
        equal(helper, arrow.getXRot(), arrow.xRotO, "Previous pitch cannot point the tail forward");
        var packet = org.brahypno.maledict.network.ProjectileReflectionPacket.of(arrow);
        var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            org.brahypno.maledict.network.ProjectileReflectionPacket.encode(packet, buffer);
            var decoded = org.brahypno.maledict.network.ProjectileReflectionPacket.decode(buffer);
            equal(helper, 0, decoded.velocity().subtract(incoming.reverse()).length(), "Network must not clamp to 3.9");
        } finally {
            buffer.release();
        }
        LivingEntity target = f.target();
        HangmanTrapEntity trap = f.trap(arrow, target.position());
        trap.tick();
        helper.assertTrue(trap.isRemoved() && target.getHealth() < target.getMaxHealth(), "Reflected arrows retain stage two");
        f.finish();
    }

    @GameTest(template = "test_empty", batch = "hangman_fool")
    public static void piercingArrowActuallyReversesBeforeOldRayHitsAnythingElse(GameTestHelper helper) {
        Fixture f = new Fixture(helper, 2);
        f.player.setHealth(4);
        helper.getLevel().addNewPlayer(f.player);
        Vec3 center = f.player.getBoundingBox().getCenter();
        LivingEntity behind = f.target();
        behind.setPos(center.add(2, -0.5D, 0));
        Arrow arrow = new Arrow(helper.getLevel(), center.x - 2, center.y, center.z);
        arrow.setDeltaMovement(4, 0, 0);
        arrow.setPierceLevel((byte) 3);
        helper.getLevel().addFreshEntity(arrow);
        arrow.tick();
        helper.assertTrue(HangmanFoolProjectiles.wasReflected(arrow), "Must reflect through the actual arrow tick hook");
        equal(helper, -4 * 0.99F, arrow.getDeltaMovement().x, "Reversed arrow retains normal vanilla drag");
        helper.assertTrue(arrow.getPierceLevel() == 3 && arrow.getX() < f.player.getX() - 2,
                "Cancellation ends the old ray and lets the reversed arrow finish its normal movement");
        helper.assertTrue(behind.getHealth() == behind.getMaxHealth(), "Old forward ray must not damage the entity behind wearer");
        helper.assertTrue(arrow.getYRot() == arrow.yRotO && Math.abs(arrow.getYRot() + 90) < 0.01,
                "Rendered arrow yaw must immediately follow reversed flight");
        arrow.discard();
        f.finish();
    }

    @GameTest(template = "test_empty", batch = "hangman_fool")
    public static void powerFlameAndPiercingSurviveTrapSaveReload(GameTestHelper helper) {
        Fixture f = new Fixture(helper, 1);
        ItemStack bow = new ItemStack(Items.BOW);
        bow.enchant(Enchantments.POWER_ARROWS, 3);
        bow.enchant(Enchantments.FLAMING_ARROWS, 1);
        f.player.setItemInHand(InteractionHand.MAIN_HAND, bow);
        Arrow arrow = new Arrow(helper.getLevel(), f.player);
        arrow.setEnchantmentEffectsFromEntity(f.player, 1.0F);
        arrow.setPierceLevel((byte) 2);
        arrow.setDeltaMovement(12, 0, 0); // Beyond Entity.load's component limit as well as vanilla network speed limit.
        float expectedDamage = (float) Math.ceil(12 * arrow.getBaseDamage());
        LivingEntity target = f.target();
        HangmanTrapEntity trap = f.trap(arrow, target.position());
        CompoundTag saved = new CompoundTag();
        helper.assertTrue(trap.save(saved), "Trap must save");
        trap.discard();
        saved.remove("UUID");
        var loaded = EntityType.loadEntityRecursive(saved, helper.getLevel(), entity -> entity);
        helper.getLevel().addFreshEntity(loaded);
        loaded.tick();
        equal(helper, expectedDamage, target.getMaxHealth() - target.getHealth(), "Power damage and high velocity survive reload");
        helper.assertTrue(target.isOnFire(), "Flame effect must run");
        helper.assertTrue(loaded.isRemoved(), "One-shot trap is consumed");
        f.finish();
    }

    @GameTest(template = "test_empty", batch = "hangman_fool")
    public static void tippedAndSpectralArrowsRunTheirPostHurtEffects(GameTestHelper helper) {
        Fixture f = new Fixture(helper, 1);
        LivingEntity target = f.target();
        Arrow tipped = new Arrow(helper.getLevel(), f.player);
        tipped.setEffectsFromItem(PotionUtils.setPotion(new ItemStack(Items.TIPPED_ARROW), Potions.POISON));
        tipped.setDeltaMovement(2, 0, 0);
        f.trap(tipped, target.position()).tick();
        helper.assertTrue(target.hasEffect(MobEffects.POISON), "Tipped arrow potion branch");
        target.invulnerableTime = 0;
        SpectralArrow spectral = new SpectralArrow(helper.getLevel(), f.player);
        spectral.setDeltaMovement(2, 0, 0);
        f.trap(spectral, target.position()).tick();
        helper.assertTrue(target.hasEffect(MobEffects.GLOWING), "Spectral arrow branch");
        f.finish();
    }

    @GameTest(template = "test_empty", batch = "hangman_fool")
    public static void splashAndLingeringPotionsRunTheirCompleteOnHitOverrides(GameTestHelper helper) {
        Fixture f = new Fixture(helper, 1);
        LivingEntity target = f.target();
        ThrownPotion splash = new ThrownPotion(helper.getLevel(), f.player);
        splash.setItem(PotionUtils.setPotion(new ItemStack(Items.SPLASH_POTION), Potions.POISON));
        splash.setDeltaMovement(2, 0, 0);
        f.trap(splash, target.position()).tick();
        helper.assertTrue(target.hasEffect(MobEffects.POISON), "Splash is implemented in onHit, not onHitEntity");
        ThrownPotion lingering = new ThrownPotion(helper.getLevel(), f.player);
        lingering.setItem(PotionUtils.setPotion(new ItemStack(Items.LINGERING_POTION), Potions.SLOWNESS));
        lingering.setDeltaMovement(2, 0, 0);
        f.trap(lingering, target.position()).tick();
        helper.assertTrue(!helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.AreaEffectCloud.class,
                target.getBoundingBox().inflate(2)).isEmpty(), "Lingering potion creates its cloud");
        f.finish();
    }

    @GameTest(template = "test_empty", batch = "hangman_fool")
    public static void fireAspectHexStaffAndAuricStaffEffectsSurviveCapture(GameTestHelper helper) {
        Fixture f = new Fixture(helper, 1);
        helper.getLevel().addNewPlayer(f.player); // Serialized owners resolve through ServerLevel.
        LivingEntity target = f.target();
        HexBoltEntity hex = new HexBoltEntity(helper.getLevel());
        hex.setData(f.player, 7, 0);
        ItemStack staff = new ItemStack(com.sammy.malum.registry.common.item.ItemRegistry.MNEMONIC_HEX_STAFF.get());
        staff.enchant(Enchantments.FIRE_ASPECT, 2);
        hex.setItem(staff);
        hex.setDeltaMovement(1, 0, 0);
        f.trap(hex, target.position()).tick();
        helper.assertTrue(target.getHealth() < target.getMaxHealth() && target.isOnFire(), "Hex magic and Fire Aspect branch");
        target.invulnerableTime = 0;
        target.clearFire();
        AuricFlameBoltEntity auric = new AuricFlameBoltEntity(helper.getLevel());
        auric.setData(f.player, 6, 0);
        auric.setDeltaMovement(1, 0, 0);
        float health = target.getHealth();
        f.trap(auric, target.position()).tick();
        helper.assertTrue(target.getHealth() < health, "Auric staff subclass must retain its hit behavior");
        f.finish();
    }

    @GameTest(template = "test_empty", batch = "hangman_fool")
    public static void allEightSpiritArrowChannelsSurviveTheTrap(GameTestHelper helper) {
        Fixture f = new Fixture(helper, 1);
        LivingEntity target = f.target();
        LivingEntity directTarget = f.target();
        directTarget.setPos(target.position().add(4, 0, 0));
        for (SpiritArrowType type : SpiritArrowType.values()) {
            target.setHealth(target.getMaxHealth());
            target.invulnerableTime = 0;
            target.removeAllEffects();
            directTarget.setHealth(directTarget.getMaxHealth());
            directTarget.invulnerableTime = 0;
            directTarget.removeAllEffects();
            SpiritArrowEntity direct = new SpiritArrowEntity(helper.getLevel(), f.player, type);
            direct.setBaseDamage(2);
            direct.setDeltaMovement(2, 0, 0);
            ((ProjectileAccess) direct).maledict$onHit(new EntityHitResult(directTarget));
            SpiritArrowEntity arrow = new SpiritArrowEntity(helper.getLevel(), f.player, type);
            arrow.setBaseDamage(2);
            arrow.setDeltaMovement(2, 0, 0);
            f.trap(arrow, target.position()).tick();
            float damage = target.getMaxHealth() - target.getHealth();
            equal(helper, directTarget.getMaxHealth() - directTarget.getHealth(), damage,
                    type + " trap damage must match the original direct-hit branch");
            if (type == SpiritArrowType.INFERNAL) {
                helper.assertTrue(target.hasEffect(MobEffects.GLOWING), "Infernal post-hurt effect");
            }
        }
        f.finish();
    }

    @GameTest(template = "test_empty", batch = "hangman_fool")
    public static void fireballAccelerationReversesAndTrappedFireballStillBurns(GameTestHelper helper) {
        Fixture f = new Fixture(helper, 2);
        f.player.setHealth(4);
        SmallFireball fireball = new SmallFireball(helper.getLevel(), f.player, 1, 0.2D, -0.3D);
        Vec3 power = new Vec3(fireball.xPower, fireball.yPower, fireball.zPower);
        LivingEntity source = f.target();
        source.setPos(f.player.position().add(5, 0, 0));
        fireball.setOwner(source);
        fireball.setDeltaMovement(2, 0, -1);
        MinecraftForge.EVENT_BUS.post(new ProjectileImpactEvent(fireball, new EntityHitResult(f.player)));
        equal(helper, 0, new Vec3(fireball.xPower, fireball.yPower, fireball.zPower).add(power).length(), "Fireball power reverses");
        LivingEntity target = f.target();
        f.trap(fireball, target.position()).tick();
        helper.assertTrue(target.isOnFire() && target.getHealth() < target.getMaxHealth(), "Small fireball original hit branch");
        f.finish();
    }

    @GameTest(template = "test_empty", batch = "hangman_fool")
    public static void fireworkExplosionsAndImpalingTridentsKeepTheirHitBranches(GameTestHelper helper) {
        Fixture f = new Fixture(helper, 1);
        LivingEntity target = f.target();
        ItemStack rocketItem = new ItemStack(Items.FIREWORK_ROCKET);
        CompoundTag explosion = new CompoundTag();
        explosion.putByte("Type", (byte) 0);
        explosion.putIntArray("Colors", new int[]{0xFFFFFF});
        ListTag explosions = new ListTag();
        explosions.add(explosion);
        rocketItem.getOrCreateTagElement("Fireworks").put("Explosions", explosions);
        FireworkRocketEntity rocket = new FireworkRocketEntity(helper.getLevel(), f.player,
                target.getX(), target.getY(), target.getZ(), rocketItem);
        rocket.setDeltaMovement(1, 0, 0);
        f.trap(rocket, target.position()).tick();
        helper.assertTrue(target.getHealth() < target.getMaxHealth(), "Firework entity hit must explode and deal damage");
        target.discard();
        var guardian = helper.spawn(EntityType.GUARDIAN, new BlockPos(3, 2, 3));
        guardian.setNoAi(true);
        ItemStack tridentItem = new ItemStack(Items.TRIDENT);
        tridentItem.enchant(Enchantments.IMPALING, 3);
        ThrownTrident trident = new ThrownTrident(helper.getLevel(), f.player, tridentItem);
        trident.setDeltaMovement(2, 0, 0);
        float health = guardian.getHealth();
        f.trap(trident, guardian.position()).tick();
        equal(helper, 15.5D, health - guardian.getHealth(), "Impaling III adds 7.5 to trident's 8 against aquatic mobs");
        f.finish();
    }

    @GameTest(template = "test_empty", batch = "hangman_fool")
    public static void trapIgnoresCasterAndAlliesExpiresAndOnlyHitsOneTarget(GameTestHelper helper) {
        Fixture f = new Fixture(helper, 1);
        Arrow arrow = new Arrow(helper.getLevel(), f.player);
        arrow.setDeltaMovement(2, 0, 0);
        HangmanTrapEntity trap = f.trap(arrow, f.player.position());
        trap.tick();
        helper.assertTrue(!trap.isRemoved(), "Caster cannot consume their own trap");
        var pet = helper.spawn(EntityType.WOLF, new BlockPos(1, 2, 1));
        pet.setTame(true);
        pet.setOwnerUUID(f.player.getUUID());
        pet.setNoAi(true);
        pet.setPos(trap.position());
        trap.tick();
        helper.assertTrue(!trap.isRemoved(), "Owner's tame animals cannot consume the trap");
        LivingEntity first = f.target();
        LivingEntity second = f.target();
        first.setPos(trap.position());
        second.setPos(trap.position());
        trap.tick();
        int damaged = (first.getHealth() < first.getMaxHealth() ? 1 : 0)
                + (second.getHealth() < second.getMaxHealth() ? 1 : 0);
        helper.assertTrue(damaged == 1 && trap.isRemoved(), "Contact consumes one shot for one target");
        Arrow expiryArrow = new Arrow(helper.getLevel(), f.player);
        expiryArrow.setDeltaMovement(2, 0, 0);
        HangmanTrapEntity expiring = f.trap(expiryArrow, f.player.position().add(6, 0, 0));
        CompoundTag saved = expiring.saveWithoutId(new CompoundTag());
        saved.putInt("RemainingTicks", 1);
        expiring.load(saved);
        expiring.tick();
        helper.assertTrue(expiring.isRemoved(), "Lifetime ends without a hit");
        f.finish();
    }

    @GameTest(template = "test_empty", batch = "hangman_fool")
    public static void highestPriorityTrapTakesPrecedenceOverReminiscenceBlockPhasing(GameTestHelper helper) {
        Fixture f = new Fixture(helper, 1);
        Arrow arrow = new Arrow(helper.getLevel(), f.player);
        arrow.setDeltaMovement(2, 0, 0);
        org.brahypno.maledict.common.combat.RemembranceBowProjectileEffect.markForPhasing(arrow, 5);
        LivingEntity target = f.target();
        HangmanTrapEntity trap = f.trap(arrow, target.position());
        helper.assertTrue(arrow.isRemoved() && !trap.isRemoved(), "Trap wins before normal-priority enchantment impact handlers");
        f.finish();
    }

    @GameTest(template = "test_empty", batch = "hangman_fool")
    public static void healthChanceHasConfirmedBoundaries(GameTestHelper helper) {
        equal(helper, 0.25D, HangmanFoolProjectiles.reflectionChance(20, 20, 0.25), "Full health");
        equal(helper, 0.75D, HangmanFoolProjectiles.reflectionChance(10, 20, 0.25), "Half health");
        equal(helper, 1, HangmanFoolProjectiles.reflectionChance(5, 20, 0.25), "Quarter health");
        equal(helper, 1, HangmanFoolProjectiles.reflectionChance(1, 20, 0.25), "Clamped chance");
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "hangman_fool")
    public static void forcedNonVisionEquipmentHasNoAttributesPassivesOrRestrictions(GameTestHelper helper) {
        Fixture f = new Fixture(helper, 0);
        var charm = new CurioStacksHandler(f.inventory, "charm", 1, true, true, true, ICurio.DropRule.DEFAULT);
        var vision = new CurioStacksHandler(f.inventory, "delusion", 1, true, true, true, ICurio.DropRule.DEFAULT);
        f.inventory.setCurios(Map.of("charm", charm, "delusion", vision));
        ItemStack stack = HangmanFoolItem.create(2);
        // Bypass slot validation to exercise effects independently of canEquip().
        charm.getStacks().setStackInSlot(0, stack);
        var context = new SlotContext("charm", f.player, 0, false, true);
        var item = (HangmanFoolItem) stack.getItem();
        helper.assertTrue(!item.canEquip(context, stack), "Only vision slots permit equip");
        helper.assertTrue(item.getAttributeModifiers(context, UUID.randomUUID(), stack).isEmpty(),
                "Forced equipment in another slot must grant neither attack nor magic modifiers");
        helper.assertTrue(HangmanFoolItem.equippedStage(f.player) == -1,
                "Combat passives must ignore this item outside vision slots");
        assertRestrictions(helper, f, false);
        f.player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
        f.player.startUsingItem(InteractionHand.OFF_HAND);
        item.curioTick(context, stack);
        helper.assertTrue(f.player.isUsingItem(), "Non-vision curio ticks cannot stop shield use");
        f.finish();
    }

    private static void assertRestrictions(GameTestHelper helper, Fixture f, boolean canceled) {
        var start = new LivingEntityUseItemEvent.Start(f.player, new ItemStack(Items.SHIELD), 72000);
        var shield = new ShieldBlockEvent(f.player, f.player.damageSources().generic(), 4);
        var totem = new LivingUseTotemEvent(f.player, f.player.damageSources().generic(),
                new ItemStack(Items.TOTEM_OF_UNDYING), InteractionHand.OFF_HAND);
        MinecraftForge.EVENT_BUS.post(start);
        MinecraftForge.EVENT_BUS.post(shield);
        MinecraftForge.EVENT_BUS.post(totem);
        helper.assertTrue(start.isCanceled() == canceled && shield.isCanceled() == canceled && totem.isCanceled() == canceled,
                "Shield start, block, and totem restrictions must begin at stage two");
    }

    private static void equal(GameTestHelper helper, double expected, double actual, String message) {
        helper.assertTrue(Math.abs(expected - actual) < 0.001D, message + ": expected " + expected + ", got " + actual);
    }

    private static final class Fixture {
        final GameTestHelper helper;
        final FakePlayer player;
        final ICuriosItemHandler inventory;

        Fixture(GameTestHelper helper, int stage) {
            this.helper = helper;
            player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "HangmanTest"));
            player.setPos(helper.absolutePos(new BlockPos(1, 2, 1)).getCenter());
            inventory = CuriosApi.getCuriosInventory(player).orElseThrow(() -> new IllegalStateException("Missing Curios capability"));
            inventory.setCurios(Map.of("delusion", new CurioStacksHandler(inventory, "delusion", 1,
                    true, true, true, ICurio.DropRule.DEFAULT)));
            equip(stage);
        }

        void equip(int stage) {
            inventory.setEquippedCurio("delusion", 0, HangmanFoolItem.create(stage));
        }

        ItemStack stack() {
            return inventory.getCurios().get("delusion").getStacks().getStackInSlot(0);
        }

        void tickCurios() {
            MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingTickEvent(player));
        }

        LivingEntity target() {
            var pig = helper.spawn(EntityType.PIG, new BlockPos(3, 2, 3));
            pig.setNoAi(true);
            pig.getAttribute(Attributes.MAX_HEALTH).setBaseValue(200);
            pig.setHealth(200);
            return pig;
        }

        HangmanTrapEntity trap(Projectile projectile, Vec3 position) {
            projectile.setOwner(player);
            var event = new ProjectileImpactEvent(projectile,
                    new BlockHitResult(position, Direction.UP, BlockPos.containing(position), false));
            MinecraftForge.EVENT_BUS.post(event);
            helper.assertTrue(event.isCanceled() && projectile.isRemoved(), "Block impact must capture exactly one projectile");
            return helper.getLevel().getEntitiesOfClass(HangmanTrapEntity.class,
                    new AABB(position, position).inflate(0.1D)).stream().findFirst().orElseThrow();
        }

        void finish() {
            player.discard();
            helper.succeed();
        }
    }
}
