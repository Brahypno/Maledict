package org.brahypno.maledict.common.item.equipment.lightward;

import com.sammy.malum.common.data.attachment.SoulWardData;
import com.sammy.malum.registry.common.MalumAttachmentTypes;
import com.sammy.malum.registry.common.MalumAttributes;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.enchanting.GetEnchantmentLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.combat.refulgence.RefulgenceChargeService;
import org.brahypno.maledict.config.MaledictConfig;

import java.util.List;

/**
 * 2 级弃形、3 级以魂代形与 4 级未竟飞升。
 *
 * <p>两件套只要灵魂护盾还有一点存量就加 15% 移速；三件套在灵魂护盾尚存时，对最终伤害再抵消一份，
 * 被抵消的部分照 Malum 的规矩从护盾里扣；四件套按护盾充盈度把套装件上已有的附魔抬 0..3 级。
 *
 * <p>1.21 侧的四处差异：{@code TickEvent.PlayerTickEvent} → {@link PlayerTickEvent.Pre} /
 * {@code LivingDamageEvent} → {@link LivingDamageEvent.Pre}（{@code getNewDamage/setNewDamage}）；
 * 修饰符 id 由 UUID 换成 {@link ResourceLocation}、{@code MULTIPLY_TOTAL} → {@code ADD_MULTIPLIED_TOTAL}；
 * 灵魂护盾从 capability 换成 {@link SoulWardData} 附件，改完要自己 {@code syncData}。
 *
 * <p>1.20.1 这里还会把「以魂代形」的减免登记给反照的第二个充能点（{@code RefulgencePending} 暂存 +
 * {@code MONITOR} 确认）。1.21 没有暂存区，改由执行减免的一方直接调
 * {@link org.brahypno.maledict.common.combat.refulgence.RefulgenceChargeService#charge}——
 * 这正是那份服务文档写下的口径。反照要求主手载体在 {@code maledict:refulgence_carriers} 标签里，
 * 当前标签为空，所以这笔充能与 1.20.1 一样只在数据包填了载体时才真的发生。
 */
@EventBusSubscriber(modid = Maledict.MODID)
public final class LightwardSetEvents {
    /**
     * 固定 id：每 tick 都按它查重，换 id 会叠出一堆修饰符。
     */
    private static final ResourceLocation SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(Maledict.MODID, "aufheben_lightward/abnegation_speed");
    private static final double SPEED_BONUS = 0.15D;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        // 只在服务端挂：属性值本来就会同步给本人，客户端再挂一次会被同步包清掉又补回来，
        // 来回之间本地移速会短暂算两遍。
        if (player.level().isClientSide()) {
            return;
        }
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        boolean wanted = LightwardSet.grantsSpeedBonus(player);
        boolean present = speed.getModifier(SPEED_MODIFIER_ID) != null;
        if (wanted && !present) {
            speed.addTransientModifier(new AttributeModifier(
                    SPEED_MODIFIER_ID, SPEED_BONUS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else if (!wanted && present) {
            speed.removeModifier(SPEED_MODIFIER_ID);
        }
    }

    /**
     * <p>挂 {@code LivingDamageEvent.Pre} 而不是 {@code LivingIncomingDamageEvent}：这个事件里的量已经过了护甲与
     * 附魔保护，但<b>还没扣吸收</b>（NeoForge 的 Pre 在吸收之前；1.20.1 的 {@code LivingDamageEvent} 是扣过之后）。
     * 所以这里自己先把吸收摘出去，再算减免：1.20.1 的算例是 10 点伤害、4 点吸收 → 净 6 → 减免 1.5 → 掉血 4.5。
     * Malum 的灵魂护盾在前面的受伤事件里已经跑完，所以这里不再有物理 30% / 魔法 90% 的分流可重复，只补一份统一比例的抵消。
     * {@link EventPriority#LOWEST} 是为了排在其它同类修改之后。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof Player player) || !LightwardSet.grantsAbnegation(player)) {
            return;
        }
        double reduction = MaledictConfig.LIGHTWARD_ABNEGATION_REDUCTION.get();
        float current = event.getNewDamage();
        if (current <= 0.0F) {
            return;
        }
        // Pre 在吸收之前：先把会被吸收吃掉的那段摘出去，按真正会掉的血算 25%，再把吸收放回去。
        // 放回去之后 actuallyHurt 仍先吃吸收，生命掉的是 net - absorbed，与 1.20.1 的扣法一致。
        float absorption = Math.max(0.0F, player.getAbsorptionAmount());
        float net = Math.max(0.0F, current - absorption);
        if (net <= 0.0F) {
            return;
        }
        float absorbed = (float) (net * reduction);
        if (absorbed <= 0.0F) {
            return;
        }
        var type = MalumAttachmentTypes.SOUL_WARD.get();
        if (!player.hasData(type)) {
            return;
        }
        SoulWardData ward = player.getData(type);
        double integrity = player.getAttributeValue(MalumAttributes.SOUL_WARD_INTEGRITY);
        if (integrity != 0.0D) {
            ward.setSoulWard(Math.max(0.0D, ward.getSoulWard() - absorbed / integrity));
        } else {
            ward.setSoulWard(0.0D);
        }
        player.syncData(type);
        event.setNewDamage(absorption + (net - absorbed));
        // 反照的第二个充能点：这一份减免同样是「已经执行的减免」，直接充能。
        // 1.20.1 走的是 RefulgencePending 暂存 + MONITOR 确认；1.21 没有暂存区，而本处理器已经在 LOWEST，
        // 下面不会有人再改写这份量，所以按 RefulgenceChargeService 文档的口径由执行方直接调用。
        if (player instanceof ServerPlayer serverPlayer) {
            RefulgenceChargeService.charge(serverPlayer, event.getSource(), absorbed);
        }
    }

    /**
     * 4 级「未竟飞升」：按灵魂护盾充盈度把套装件上已有的附魔抬 0..3 级。
     *
     * <p>入口必须是 {@link GetEnchantmentLevelEvent}。1.21 里 {@code ItemStack#getEnchantmentLevel} /
     * {@code getAllEnchantments} 的流程是「先问物品自身的声明（{@code IItemExtension} 的两个钩子），
     * 再把整张表交给这个事件」——游戏的附魔读取（{@code EnchantmentHelper.runIterationOnItem} 等）
     * 全走这两条路，所以监听事件一次，四件护甲、神侵恶刃与两把斧就都算上了，不用逐件覆写物品钩子，
     * 也不会出现某一件漏掉的空洞。上游神化（Apotheosis）的 {@code EnchantmentAffix} /
     * {@code EnchantmentBonus} 就是同一个写法。
     *
     * <p>只抬表里已有的正等级：单个附魔查询那条路（{@code getTargetEnchant != null}）传进来的表里
     * 只有该附魔，等级为 0 时表是空的，所以「没有的附魔不会凭空长出来」——正是「II+II → IV」的语义。
     * 客户端与服务端都会触发，持有者反查由 {@link LightwardSet#enchantBonus} 自己分线程。
     */
    @SubscribeEvent
    public static void onGetEnchantmentLevel(GetEnchantmentLevelEvent event) {
        int bonus = LightwardSet.enchantBonus(event.getStack());
        if (bonus <= 0) {
            return;
        }
        ItemEnchantments.Mutable enchantments = event.getEnchantments();
        for (Holder<Enchantment> enchantment : List.copyOf(enchantments.keySet())) {
            int level = enchantments.getLevel(enchantment);
            if (level > 0) {
                enchantments.set(enchantment, level + bonus);
            }
        }
    }

    private LightwardSetEvents() {
    }
}
