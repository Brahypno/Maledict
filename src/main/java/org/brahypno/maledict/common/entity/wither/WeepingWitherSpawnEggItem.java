package org.brahypno.maledict.common.entity.wither;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;

import java.util.function.Supplier;

/**
 * 腐枯凋灵的刷怪蛋：和平难度下拒绝出现，与召唤仪式（{@code WeepingWitherSummoning#trySpawn}）
 * 及 Demiurge 的刷怪蛋保持一致。
 *
 * <p>1.20.1 侧这条规则长在通用类 {@code StatedSpawnEggItem} 上，但 1.21 只需要「拒绝和平难度」
 * 这一件事——状态本来就由 {@code DeferredSpawnEggItem#getType(ItemStack)} 的数据组件携带——
 * 所以不再移植那个通用类。
 */
public class WeepingWitherSpawnEggItem extends DeferredSpawnEggItem {
    private final boolean refusesPeaceful;

    public WeepingWitherSpawnEggItem(
            Supplier<? extends EntityType<? extends Mob>> type,
            int backgroundColor, int highlightColor, Properties properties,
            boolean refusesPeaceful) {
        super(type, backgroundColor, highlightColor, properties);
        this.refusesPeaceful = refusesPeaceful;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (refusesPeaceful && level.getDifficulty() == Difficulty.PEACEFUL) {
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        return super.use(level, player, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (refusesPeaceful && context.getLevel() instanceof ServerLevel level
            && level.getDifficulty() == Difficulty.PEACEFUL) {
            return InteractionResult.FAIL;
        }
        return super.useOn(context);
    }
}
