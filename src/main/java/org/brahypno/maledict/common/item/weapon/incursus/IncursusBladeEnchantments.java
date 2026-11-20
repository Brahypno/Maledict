package org.brahypno.maledict.common.item.weapon.incursus;

import com.sammy.malum.common.enchantment.scythe.AscensionEnchantment;
import com.sammy.malum.common.item.curiosities.TemporarilyDisabledItem;
import com.sammy.malum.common.item.curiosities.weapons.scythe.MalumScytheItem;
import com.sammy.malum.registry.common.DamageTypeRegistry;
import com.sammy.malum.registry.common.MobEffectRegistry;
import com.sammy.malum.registry.common.SoundRegistry;
import com.sammy.malum.registry.common.item.EnchantmentRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.brahypno.maledict.common.enchantment.DiplopiaThrows;
import org.brahypno.maledict.common.item.weapon.incursus.rift.RiftMode;
import org.brahypno.maledict.network.MaledictNetwork;
import org.brahypno.maledict.registry.MaledictSounds;
import team.lodestar.lodestone.helpers.DamageTypeHelper;
import team.lodestar.lodestone.helpers.ItemHelper;
import team.lodestar.lodestone.helpers.RandomHelper;
import team.lodestar.lodestone.helpers.SoundHelper;

public final class IncursusBladeEnchantments {
    private static final float THROWN_DAMAGE_MULTIPLIER = 1.5f;
    private static final float ASCENSION_DAMAGE_MULTIPLIER = 1.25f;

    public static void throwScythe(Level level, Player player, InteractionHand hand, ItemStack stack) {
        int slot = hand == InteractionHand.OFF_HAND
                   ? player.getInventory().getContainerSize() - 1
                   : player.getInventory().selected;
        if (!(player instanceof ServerPlayer serverPlayer)){
            return;
        }

        boolean enhanced = isEnhanced(player);
        IncursusBladeAttack.DamageChannels channels = IncursusBladeAttack
                .damageChannels(player)
                .scaled(enhanced ? THROWN_DAMAGE_MULTIPLIER : 1.0f);
        float velocity = enhanced ? 3.0f : 1.75f;
        Vec3 position = player.position().add(0.0, player.getBbHeight() * 0.5f, 0.0);
        if (enhanced){
            int angle = hand == InteractionHand.MAIN_HAND ? 225 : 90;
            double radians = Math.toRadians(angle - player.yHeadRot);
            position = player.position().add(player.getLookAngle().scale(0.5)).add(
                    0.75 * Math.sin(radians), player.getBbHeight() * 0.9f, 0.75 * Math.cos(radians));
        }

        IncursusScytheBoomerangEntity scythe = new IncursusScytheBoomerangEntity(
                level, position.x, position.y, position.z);
        scythe.setData(player, channels.attack(), channels.magic(), slot, 8);
        scythe.setItem(stack);
        scythe.setEnhanced(enhanced);
        scythe.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, velocity, 0.0f);
        level.addFreshEntity(scythe);
        SoundHelper.playSound(
                player,
                MaledictSounds.INCURSUS_BLADE_THROW.get(),
                1.2f,
                RandomHelper.randomBetween(level.getRandom(), 0.95f, 1.05f));
        TemporarilyDisabledItem.disable(serverPlayer, slot);
        DiplopiaThrows.spawnEchoes(
                level, serverPlayer, hand, stack,
                channels.attack(), channels.magic(),
                enhanced, position, velocity);
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
        player.swing(hand, true);
    }

    public static void triggerAscension(Level level, Player player, InteractionHand hand, ItemStack stack) {
        boolean enhanced = isEnhanced(player);
        player.resetFallDistance();
        if (level.isClientSide()){
            AscensionEnchantment.triggerAscension(level, player, hand, stack);
        }else {
            performAscensionAttack(level, player, stack, enhanced);
        }

        if (!player.isCreative()){
            int levelOfAscension = stack.getEnchantmentLevel(EnchantmentRegistry.ASCENSION.get());
            if (levelOfAscension < 6){
                player.getCooldowns().addCooldown(stack.getItem(), 150 - 25 * (levelOfAscension - 1));
            }
        }
        player.swing(hand, false);
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
    }

    private static void performAscensionAttack(Level level, Player player, ItemStack stack, boolean enhanced) {
        IncursusBladeAttack.DamageChannels channels = IncursusBladeAttack
                .damageChannels(player)
                .scaled(enhanced ? ASCENSION_DAMAGE_MULTIPLIER : 1.0f);
        AABB area = player.getBoundingBox().inflate(4.0, 1.0, 4.0);
        var sound = SoundRegistry.SCYTHE_SWEEP.get();
        // 原版在创建 Malum 粒子构建器时抽一次镜像，强化分支会把这个构建器整个丢掉、改传 setMirrored(true)。
        // 这里保留同一次抽样，让世界随机流的消耗顺序与改造前一致——视觉改造不该挪动其它逻辑的随机数。
        boolean randomMirror = level.getRandom().nextBoolean();
        if (enhanced){
            area = area.move(player.getLookAngle().scale(2.0)).inflate(-2.0, 1.0, -2.0);
            sound = SoundRegistry.SCYTHE_CUT.get();
        }

        boolean hitAnything = false;
        for (Entity target : IncursusBladeAttack.uniqueLivingTargets(
                level.getEntities(player, area, entity -> canHitEntity(player, entity)))) {
            // 飞升横扫写死 medium（不随精魂成长）；无忧符文的抬档由 DamageRequestEvent 统一处理。
            boolean hit = IncursusBladeItem.dealSkillDamage(target,
                                                            DamageTypeHelper.create(level, DamageTypeRegistry.SCYTHE_SWEEP, player), channels.attack())
                                           .instantEffective();
            if (hit){
                ItemHelper.applyEnchantments(player, target, stack);
                int fireAspect = stack.getEnchantmentLevel(Enchantments.FIRE_ASPECT);
                if (fireAspect > 0 && target instanceof LivingEntity livingTarget){
                    livingTarget.setSecondsOnFire(fireAspect * 4);
                }
                // 魔法属性由 AttributeDamageRegistry 在 scythe_sweep 命中后独立派发。
                SoundHelper.playSound(
                        player, sound, 2.0f,
                        RandomHelper.randomBetween(level.getRandom(), 0.75f, 1.25f));
                hitAnything = true;
            }
        }
        if (hitAnything){
            player.addEffect(new MobEffectInstance(MobEffectRegistry.ASCENSION.get(), 80, 0));
        }

        Vec3 particlePosition = player.position().add(0.0, player.getBbHeight() * 0.75, 0.0);
        Vec3 particleDirection = player.getLookAngle().multiply(1.0, 0.0, 1.0);
        if (player instanceof ServerPlayer serverPlayer){
            // 原版这两个量：普通旋斩默认 0，强化分支 setVerticalSlashAngle() = 1.57 且强制镜像。
            RiftMode mode = enhanced ? RiftMode.ASCENSION_UPPERCUT : RiftMode.ASCENSION_SPIN;
            float baseAngle = enhanced ? 1.57f : 0.0f;
            MaledictNetwork.sendIncursusRiftSkill(
                    serverPlayer, mode, particlePosition, particleDirection, baseAngle, enhanced || randomMirror);
        }
        for (int i = 0; i < 3; i++) {
            SoundHelper.playSound(
                    player, sound, 1.0f,
                    RandomHelper.randomBetween(level.getRandom(), 1.25f, 1.75f));
        }
        SoundHelper.playSound(
                player,
                MaledictSounds.INCURSUS_BLADE_ASCENSION.get(),
                1.2f,
                RandomHelper.randomBetween(level.getRandom(), 0.95f, 1.05f));
    }

    private static boolean canHitEntity(Player player, Entity entity) {
        return entity.canBeHitByProjectile()
               && entity != player
               && !player.isPassengerOfSameVehicle(entity);
    }

    /**
     * 强化状态 = Malum 判定这把镰刀不能横扫（戴着把横扫换成单体强化的项链）。
     */
    private static boolean isEnhanced(Player player) {
        return !MalumScytheItem.canSweep(player);
    }

    private IncursusBladeEnchantments() {
    }
}
