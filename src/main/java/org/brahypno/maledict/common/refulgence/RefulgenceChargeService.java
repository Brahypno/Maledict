package org.brahypno.maledict.common.refulgence;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.brahypno.maledict.common.MaledictAbilities;
import org.brahypno.maledict.common.MaledictClock;
import org.brahypno.maledict.common.MaledictDamageTypes;

/**
 * 把「护盾实际减免掉的量」折算进当前主手载体的储能。规范第 3.1 与 5.2 节的 {@code ChargeService}。
 *
 * <p>只有主手那一件带反照的载体武器会充能：副手不替主手充，免得能量归属说不清。
 * 上限取武器自身基础攻击伤害的 80%，见 {@link RefulgenceCharge}。
 */
final class RefulgenceChargeService {

    static void charge(RefulgencePending.Absorbed absorbed) {
        ServerPlayer player = absorbed.player();
        if (player.level().isClientSide() || !eligible(absorbed.source(), player)) {
            return;
        }
        ItemStack weapon = player.getMainHandItem();
        if (!RefulgenceAbility.holds(player)) {
            return;
        }
        double baseAttack = MaledictAbilities.weaponAttackDamage(
                weapon, player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE));
        if (baseAttack <= 0.0D) {
            return;
        }
        long tick = MaledictClock.now(player);
        RefulgenceCharge charge = RefulgenceStore.read(weapon);
        if (charge.isExpired(tick)) {
            // 过期不明确的储能直接清零，不跨窗口累积。
            charge = RefulgenceCharge.empty();
        }
        charge.add(absorbed.amount(), baseAttack, tick);
        RefulgenceStore.write(weapon, charge);
    }

    /**
     * 充能资格：被打的一方是玩家，打过来的得是另一名活着的攻击者。
     *
     * <p>环境伤害、自己打自己、{@code /kill} 这类技术性伤害都不充能；反照伤害本身也不充能 ——
     * 否则两个都带反照的玩家可以互相喂能量。
     */
    private static boolean eligible(DamageSource source, Player victim) {
        if (MaledictDamageTypes.is(source, MaledictDamageTypes.REFULGENCE)) {
            return false;
        }
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        if (!(source.getEntity() instanceof LivingEntity attacker)) {
            return false;
        }
        return attacker != victim;
    }

    private RefulgenceChargeService() {
    }
}
