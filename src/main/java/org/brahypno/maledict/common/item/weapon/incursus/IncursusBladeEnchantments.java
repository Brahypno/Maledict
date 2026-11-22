package org.brahypno.maledict.common.item.weapon.incursus;

import com.sammy.malum.core.handlers.enchantment.AscensionHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import com.sammy.malum.common.item.spirit.ISpiritAffiliatedItem;
import com.sammy.malum.common.item.curiosities.TemporarilyDisabledItem;
import com.sammy.malum.common.item.curiosities.weapons.scythe.MalumScytheItem;
import com.sammy.malum.visual_effects.networked.MalumNetworkedWeaponParticleEffectType;
import com.sammy.malum.registry.common.MalumDamageTypes;
import com.sammy.malum.registry.common.MalumMobEffects;
import com.sammy.malum.registry.common.MalumParticleEffectTypes;
import com.sammy.malum.registry.common.MalumSoundEvents;
import com.sammy.malum.registry.common.enchantment.EnchantmentKeys;
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
import org.brahypno.maledict.common.item.weapon.incursus.IncursusScytheBoomerangEntity;
import org.brahypno.maledict.common.enchantment.DiplopiaThrows;
import org.brahypno.maledict.common.item.weapon.incursus.rift.RiftMode;
import org.brahypno.maledict.mixin.WeaponParticleEffectBuilderAccess;
import org.brahypno.maledict.network.MaledictNetwork;
import org.brahypno.maledict.registry.MaledictSounds;
import team.lodestar.lodestone.helpers.DamageTypeHelper;
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
                .damageChannels(player, stack)
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
        scythe.setNarrow(enhanced);
        scythe.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, velocity, 0.0f);
        level.addFreshEntity(scythe);
        SoundHelper.playSound(
                player,
                MaledictSounds.INCURSUS_BLADE_THROW.get(),
                1.2f,
                RandomHelper.randomBetween(level.getRandom(), 0.95f, 1.05f));
        TemporarilyDisabledItem.disable(serverPlayer, slot, com.sammy.malum.registry.common.item.MalumItems.SOUL_OF_A_SCYTHE);
        // 复视：与原投掷同点出发，两条常规通道已按强化倍率放大，幻影各自再砍半；
        // 碧水是属性，幻影命中时同样由 flush 按刀上的修饰符派发。
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
            AscensionHandler.triggerAscension(level, player, hand, stack);
        }else {
            performAscensionAttack(level, player, stack, enhanced);
        }

        if (!player.isCreative()){
            int levelOfAscension = org.brahypno.maledict.registry.MaledictEnchantments.level(stack, EnchantmentKeys.ASCENSION);
            if (levelOfAscension < 6){
                player.getCooldowns().addCooldown(stack.getItem(), 150 - 25 * (levelOfAscension - 1));
            }
        }
        player.swing(hand, false);
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
    }

    private static void performAscensionAttack(Level level, Player player, ItemStack stack, boolean enhanced) {
        IncursusBladeAttack.DamageChannels channels = IncursusBladeAttack
                .damageChannels(player, stack)
                .scaled(enhanced ? ASCENSION_DAMAGE_MULTIPLIER : 1.0f);
        AABB area = player.getBoundingBox().inflate(4.0, 1.0, 4.0);
        var sound = MalumSoundEvents.SCYTHE_SWEEP.get();
        MalumNetworkedWeaponParticleEffectType.MalumWeaponParticleEffectBuilder<?> particles = MalumParticleEffectTypes.SCYTHE_ASCENSION_SPIN.createEffect()
                .mirroredRandomly(level.getRandom());
        if (enhanced){
            area = area.move(player.getLookAngle().scale(2.0)).inflate(-2.0, 1.0, -2.0);
            sound = MalumSoundEvents.SCYTHE_CUT.get();
            particles = MalumParticleEffectTypes.SCYTHE_ASCENSION_UPPERCUT.createEffect()
                                      .verticalSlashRotation()
                                      .mirrored(true);
        }
        if (stack.getItem() instanceof ISpiritAffiliatedItem spiritItem){
            particles.color(spiritItem.getDefiningSpiritType());
        }

        boolean hitAnything = false;
        for (Entity target : IncursusBladeAttack.uniqueLivingTargets(
                level.getEntities(player, area, entity -> canHitEntity(player, entity)))) {
            // 飞升横扫按刀自己的精魂档结算；无忧符文的抬档由 dealSkillDamage 内部统一处理。
            // 命中判定与 1.20.1 一致用 instantEffective：只要这一记真的产生了效果（掉血、吸收、状态变化）
            // 就算命中；success() 还要求「打满预期数值」，被护甲或抗性吃掉一部分时会把命中后附魔整段跳过。
            boolean hit = IncursusBladeItem.dealSkillDamage(stack, target,
                                                           DamageTypeHelper.create(level, MalumDamageTypes.SCYTHE_SWEEP, player),
                                                           channels.attack())
                                           .instantEffective();
            if (hit){
                EnchantmentHelper.doPostAttackEffectsWithItemSource((ServerLevel) level, target,
                        DamageTypeHelper.create(level, MalumDamageTypes.SCYTHE_SWEEP, player), stack);
                int fireAspect = org.brahypno.maledict.registry.MaledictEnchantments.level(stack, Enchantments.FIRE_ASPECT);
                if (fireAspect > 0 && target instanceof LivingEntity livingTarget){
                    livingTarget.igniteForSeconds(fireAspect * 4);
                }
                // 奥术与碧水两条属性通道由这一记 sweep probe 的 flush 自己派发（MaledictAttributes），
                // 与 1.20.1 一样这里不再补发：补一发会多一笔魔法，还顺带多派一轮碧水。
                SoundHelper.playSound(
                        player, sound, 2.0f,
                        RandomHelper.randomBetween(level.getRandom(), 0.75f, 1.25f));
                hitAnything = true;
            }
        }
        if (hitAnything){
            player.addEffect(new MobEffectInstance(MalumMobEffects.ASCENSION, 80, 0));
        }

        Vec3 particlePosition = player.position().add(0.0, player.getBbHeight() * 0.75, 0.0);
        Vec3 particleDirection = player.getLookAngle().multiply(1.0, 0.0, 1.0);
        if (player instanceof ServerPlayer serverPlayer) {
            // 构建器仍然照建：mirroredRandomly 这一次抽样是上游的顺序，视觉改造不该挪动随机流。
            // 原版这两个量：普通旋斩默认 0，强化分支 verticalSlashRotation() = 1.57 且强制镜像。
            RiftMode mode = enhanced ? RiftMode.ASCENSION_UPPERCUT : RiftMode.ASCENSION_SPIN;
            float baseAngle = enhanced ? 1.57f : 0.0f;
            boolean mirrored = enhanced
                    || ((WeaponParticleEffectBuilderAccess) particles).maledict$isMirrored();
            MaledictNetwork.sendIncursusRiftSkill(
                    serverPlayer, mode, particlePosition, particleDirection, baseAngle, mirrored);
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

    /** 强化状态 = Malum 判定这把镰刀不能横扫（戴着把横扫换成单体强化的项链）。 */
    private static boolean isEnhanced(Player player) {
        return MalumScytheItem.isEnhanced(player);
    }

    private IncursusBladeEnchantments() {
    }
}
