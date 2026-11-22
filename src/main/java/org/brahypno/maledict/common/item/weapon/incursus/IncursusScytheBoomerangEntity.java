package org.brahypno.maledict.common.item.weapon.incursus;

import com.sammy.malum.common.entity.scythe.ScytheBoomerangEntity;
import com.sammy.malum.registry.common.MalumDamageTypes;
import com.sammy.malum.registry.common.MalumSoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.brahypno.changelib.DamageHelper.LivingTargetHelper;
import org.brahypno.changelib.DamageHelper.attribute.AttackItemProvider;
import org.brahypno.maledict.common.enchantment.DiplopiaThrows;
import org.brahypno.maledict.registry.MaledictSounds;
import team.lodestar.lodestone.helpers.DamageTypeHelper;
import team.lodestar.lodestone.helpers.RandomHelper;
import team.lodestar.lodestone.helpers.SoundHelper;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Malum's rebound projectile with the Incursus Blade's sweep channel. The arcane and freezing
 * channels are attributes ({@code lodestone:magic_damage} / {@code maledict:powder_snow_damage}),
 * dispatched by ChangeLib after this projectile's own probe lands — exactly like 1.20.1, where the
 * boomerang deals the sweep hit and nothing else.
 */
public final class IncursusScytheBoomerangEntity extends ScytheBoomerangEntity
        implements AttackItemProvider {

    /** 复视幻影：伤害通道照常结算，但绝不碰掷出者的物品栏，返程到家门口自行消散。 */
    private final boolean echo;

    /** 「收回」只响一次：飞镰掉头（{@code returnTimer} 落到 0）的那一 tick。 */
    private boolean recallAnnounced;

    /** 这一次投掷里已经探过的活体根：去程与回程共用，每个根只吃一记。 */
    private final Set<UUID> hitLivingTargets = new HashSet<>();

    public IncursusScytheBoomerangEntity(Level level, double x, double y, double z) {
        this(level, x, y, z, false);
    }

    public IncursusScytheBoomerangEntity(
            Level level, double x, double y, double z, boolean echo) {
        super(level, x, y, z);
        this.echo = echo;
    }

    /**
     * 投掷中的刀就是权威物品快照：属性派发器按它取碧水与奥术两条修饰符，
     * 不依赖这一刻主手还拿着什么。
     */
    @Override
    public ItemStack getAttackItem() {
        return getItem();
    }

    /**
     * 投掷与接住之间，玩家听到的只有 Malum 的旋转声（{@code SCYTHE_SPINS}）+ 接住时的
     * {@code SCYTHE_CATCH}；本模组补的是「掉头回手」这一下。判定用 {@code returnTimer} 而不是
     * {@code flyBack()}：后者只在撞墙和强化命中时被调用，普通投掷的返程不会走它。
     */
    @Override
    public void tick() {
        if (echo) {
            // 幻影必须在基类 {@code super.tick()} 移动它之前判断接取半径，否则这一 tick 就会
            // 走进 ReboundHandler#pickupScythe，把一把幻影镰刀塞回玩家物品栏。
            if (!level().isClientSide() && DiplopiaThrows.reachedOwnerOnReturn(this)) {
                discard();
                return;
            }
            super.tick();
            return;
        }
        super.tick();
        if (recallAnnounced || level().isClientSide() || returnTimer > 0) {
            return;
        }
        recallAnnounced = true;
        SoundHelper.playSound(
                this,
                MaledictSounds.INCURSUS_BLADE_RECALL.get(),
                1.2f,
                RandomHelper.randomBetween(level().getRandom(), 0.95f, 1.05f));
    }

    /**
     * 幻影只活在这一趟投掷里：万一半空中被区块存盘，读回来会变成一把普通回旋镰刀。
     */
    @Override
    public boolean shouldBeSaved() {
        return !echo && super.shouldBeSaved();
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        if (level().isClientSide()){
            return;
        }

        Entity ownerEntity = getOwner();
        if (ownerEntity instanceof LivingEntity owner){
            Entity hitEntity = hitResult.getEntity();
            // 多部位实体（末影龙、凋灵风暴那类）的每个 PartEntity 都指向同一个活体根：
            // 一次投掷对同一个根只探一次，否则回程会把同一只怪再探一轮、多部件 boss 每个 part 各探一次
            // （每次都还会重设主手与刷属性）。与近战路径 IncursusBladeAttack#uniqueLivingTargets 同一口径。
            LivingEntity livingRoot = LivingTargetHelper.getLivingTarget(hitEntity);
            Entity target = livingRoot != null ? livingRoot : hitEntity;
            if (livingRoot != null && !hitLivingTargets.add(livingRoot.getUUID())){
                return;
            }
            ItemStack heldItem = owner.getMainHandItem();
            ItemStack scythe = getItem();
            owner.setItemInHand(InteractionHand.MAIN_HAND, scythe);
            
            // 回旋按刀自己的精魂档结算；无忧符文的抬档由 dealSkillDamage 内部统一处理。
            // 命中判定与 1.20.1 一致用 instantEffective：部分落地（护甲/抗性/减伤）也算命中，
            // 否则火焰附加、命中计数与返程加速都会在部分减伤时长上什么都不做。
            boolean hit = IncursusBladeItem.dealSkillDamage(scythe, target,
                                                           DamageTypeHelper.create(level(), MalumDamageTypes.SCYTHE_SWEEP, this, owner),
                                                           damage)
                                           .instantEffective();
            if (hit){
                net.minecraft.world.item.enchantment.EnchantmentHelper.doPostAttackEffectsWithItemSource(
                        (net.minecraft.server.level.ServerLevel) level(), target, DamageTypeHelper.create(level(), MalumDamageTypes.SCYTHE_SWEEP, this, owner), scythe);
                int fireAspect = org.brahypno.maledict.registry.MaledictEnchantments.level(scythe, Enchantments.FIRE_ASPECT);
                if (fireAspect > 0 && target instanceof LivingEntity livingTarget){
                    livingTarget.igniteForSeconds(fireAspect * 4);
                }
                // 奥术与碧水两条属性通道由这一记 sweep probe 的 flush 自己派发（MaledictAttributes），
                // 这里不再显式补发：补一发反而会多派一轮（属性源本身也会再触发一次 flush）。
                enemiesHit++;
                returnTimer += 2;
            }

            owner.setItemInHand(InteractionHand.MAIN_HAND, heldItem);
            SoundHelper.playSound(
                    this,
                    MalumSoundEvents.SCYTHE_SWEEP.get(),
                    1.0f,
                    RandomHelper.randomBetween(level().getRandom(), 0.75f, 1.25f));
        }

        if (isNarrow()){
            returnTimer = 0;
            Entity owner = getOwner();
            if (owner instanceof LivingEntity){
                flyBack(owner);
            }
        }
    }
}
