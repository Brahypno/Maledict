package org.brahypno.maledict.common.effect;

import com.sammy.malum.registry.common.DamageTypeRegistry;
import com.sammy.malum.registry.common.ParticleEffectTypeRegistry;
import com.sammy.malum.registry.common.SpiritTypeRegistry;
import com.sammy.malum.visual_effects.networked.data.ColorEffectData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import team.lodestar.lodestone.helpers.DamageTypeHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 汰余之令：给佩戴者的药水效果，每 40 tick 打一只最外围的过量同种敌人；目前没有物品发放它。
 * 伤害走玩家源的 {@code malum:voodoo}（算佩戴者击杀），该类型穿甲/穿抗性/穿无敌，故自行判 {@code isInvulnerable}。
 */
public final class ThinningEffect extends MobEffect {
    /** Malum 邪恶精魂的次色 {@code #4815FF}。 */
    public static final int COLOR = 0x4815FF;

    /** 每级每次的伤害，{@code 3.0} = 一颗半心。 */
    public static final float DAMAGE_PER_LEVEL = 3.0F;

    /** 同种敌对生物挤到超过这个数才算「过量」。 */
    public static final int CROWD_LIMIT = 8;

    /** 以佩戴者所在方块为中心的水平与垂直半径，{@code 2} 即 5×5×5。 */
    public static final int RADIUS = 2;

    /** 出手间隔，40 tick。 */
    public static final int INTERVAL_TICKS = 40;

    public ThinningEffect() {
        super(MobEffectCategory.BENEFICIAL, COLOR);
    }

    /** 原版按 {@code duration % n == 0} 判定；符文每 40 tick 把时长刷回 200，故恰好每 40 tick 出手一次。 */
    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % INTERVAL_TICKS == 0;
    }

    @Override
    public void applyEffectTick(LivingEntity wearer, int amplifier) {
        if (wearer.level().isClientSide){
            return;
        }

        DamageSource source = DamageTypeHelper.create(wearer.level(), DamageTypeRegistry.VOODOO, wearer);
        Map<Class<? extends LivingEntity>, List<LivingEntity>> crowds = new LinkedHashMap<>();
        for (LivingEntity candidate : wearer.level().getEntitiesOfClass(LivingEntity.class, crowdArea(wearer))) {
            if (!isSurplus(wearer, candidate)){
                continue;
            }
            crowds.computeIfAbsent(candidate.getClass(), kind -> new ArrayList<>()).add(candidate);
        }

        float damage = DAMAGE_PER_LEVEL * (amplifier + 1);
        for (List<LivingEntity> crowd : crowds.values()) {
            if (crowd.size() <= CROWD_LIMIT){
                continue;
            }
            LivingEntity victim = outermost(wearer, crowd);
            ParticleEffectTypeRegistry.MAJOR_HEXING_SMOKE.createEntityEffect(victim,
                                                                             new ColorEffectData(SpiritTypeRegistry.WICKED_SPIRIT.getPrimaryColor()));
            victim.hurt(source, damage);
            return;
        }
    }

    private static AABB crowdArea(LivingEntity wearer) {
        return new AABB(wearer.blockPosition()).inflate(RADIUS);
    }

    private static boolean isSurplus(LivingEntity wearer, LivingEntity candidate) {
        if (candidate == wearer || candidate instanceof Player){
            return false;
        }
        return candidate instanceof Enemy && candidate.isAlive() && !candidate.isInvulnerable();
    }

    private static LivingEntity outermost(LivingEntity wearer, List<LivingEntity> crowd) {
        LivingEntity outermost = crowd.get(0);
        for (LivingEntity candidate : crowd) {
            if (candidate.distanceToSqr(wearer) > outermost.distanceToSqr(wearer)){
                outermost = candidate;
            }
        }
        return outermost;
    }
}
