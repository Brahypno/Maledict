package org.brahypno.maledict.mixin;

import com.sammy.malum.core.handlers.enchantment.ReboundHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.brahypno.maledict.common.enchantment.DiplopiaThrows;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 复视：在 Malum 的回旋投掷里补出幻影镰刀。
 *
 * <p><b>为什么 1.21 仍然必须是 mixin。</b>1.8.2 的投掷入口是
 * {@code ReboundHandler#throwScythe(Level, Player, InteractionHand, ItemStack)} 这个静态方法，
 * 上游没有给「镰刀投出去之后」留事件：{@code core/systems/events} 里只有精魂收集、恶性暴击、
 * 暴食/灵魂护盾属性、精魂战利品与法典条目五类，投掷本身一发不落；{@code IMalumEventResponder}
 * 只是回调饰品自己。NeoForge 侧最近的钩子是 {@code PlayerInteractEvent.RightClickItem}，但在
 * {@code Item#use} 之前、且看不出这一下到底有没有真的投出去（冷却、别的监听器取消、别的物品
 * 覆写 {@code use}），也拿不到 Malum 刚算好的伤害与出膛点。所以按 AGENTS.md「mixin 是最后手段」
 * 的要求查完上游后，只能落在这一个调用点。
 *
 * <p><b>为什么锚在 {@code TemporarilyDisabledItem.disable} 之前。</b>1.8.2 的 {@code disable}
 * 不再只是打个标记：它会把物品栏这一格换成装着原镰刀的「禁用」占位堆叠（javap 实读），
 * 之后再读 {@code ATTACK_DAMAGE} 就没有镰刀自己的属性修饰符了，手里也不再是那把镰刀。
 * 这一行之前是「主镰刀实体已经生成、玩家属性仍是持镰状态」的唯一一点，于是幻影在这里出发。
 * （1.20.1 的 {@code ReboundEnchantmentMixin} 锚的是同一个语义位置，只是当时目标类叫
 * {@code ReboundEnchantment}、{@code disable} 是两参版。）
 *
 * <p>注入只做加法：Malum 自己的生成、音效、禁用与统计全部原样执行，复视等级为 0 时本方法立即返回。
 * mixin 注册在公共 {@code mixins} 数组而不是 {@code client}：这段逻辑跑在服务端，独立服务器必须能加载。
 */
@Mixin(value = ReboundHandler.class, remap = false)
public abstract class ReboundHandlerMixin {
    @Inject(
            method = "throwScythe",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/sammy/malum/common/item/curiosities/TemporarilyDisabledItem;"
                             + "disable(Lnet/minecraft/server/level/ServerPlayer;ILjava/util/function/Supplier;)V",
                    shift = At.Shift.BEFORE),
            remap = false)
    private static void maledict$spawnDiplopiaEchoes(
            Level level, Player player, InteractionHand hand, ItemStack scythe,
            CallbackInfo callback) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer){
            DiplopiaThrows.afterReboundThrow(level, serverPlayer, hand, scythe);
        }
    }
}
