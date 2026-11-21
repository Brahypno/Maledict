package org.brahypno.maledict.common.entity.demiurge;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 带状态的刷怪蛋：一颗蛋代表石像的某个状态，落地时把状态写进实体。
 *
 * <p>写状态必须赶在 {@code onAddedToLevel} <b>之前</b>（血量池在那里按当时的难度捕获），所以用
 * {@code EntityType#create} 定完状态再自己 {@code addFreshEntityWithPassengers}。
 *
 * <p>与 1.20.1 的 {@code StatedSpawnEggItem} 相比，这里不需要自己解析 NBT：1.21 的刷怪蛋走
 * {@code DeferredSpawnEggItem#getType(ItemStack)} 的数据组件，状态由子类字段直接携带。
 * {@code refusesPeaceful} 用于「和平难度下不该出现」的领袖：召唤仪式本身也拒绝和平难度，刷怪蛋与之一致。
 */
public class DemiurgeSpawnEggItem extends DeferredSpawnEggItem {
    private final boolean refusesPeaceful;
    private final boolean awakened;

    public DemiurgeSpawnEggItem(
            Supplier<? extends EntityType<? extends DemiurgeStatueEntity>> type,
            int backgroundColor, int highlightColor, Properties properties,
            boolean refusesPeaceful, boolean awakened) {
        super(type, backgroundColor, highlightColor, properties);
        this.refusesPeaceful = refusesPeaceful;
        this.awakened = awakened;
    }

    /**
     * 只有沉睡那颗蛋印提示：1.20.1 也是给未激活的蛋覆盖 `appendHoverText`，已激活的不说唤醒方式。
     * 提示文案仍是 {@code tooltip.maledict.demiurge_statue_spawn_egg}，否则这个键就是死的。
     */
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (!awakened) {
            tooltip.add(Component.translatable("tooltip.maledict.demiurge_statue_spawn_egg"));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        if (refusesPeaceful && level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) {
            return InteractionResult.FAIL;
        }
        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockState state = level.getBlockState(clicked);
        if (state.is(Blocks.SPAWNER)
            && level.getBlockEntity(clicked) instanceof SpawnerBlockEntity spawner) {
            // 刷怪笼走原版那一支：笼子只记实体类型，状态由笼子自己的生成流程决定。
            spawner.setEntityId(getType(stack), level.getRandom());
            spawner.setChanged();
            level.sendBlockUpdated(clicked, state, state, 3);
            level.gameEvent(context.getPlayer(), GameEvent.BLOCK_CHANGE, clicked);
            stack.shrink(1);
            return InteractionResult.CONSUME;
        }
        BlockPos spawnAt = state.getCollisionShape(level, clicked).isEmpty()
                           ? clicked : clicked.relative(face);
        Player player = context.getPlayer();
        Entity spawned = getType(stack)
                .create(level, EntityType.createDefaultStackConfig(level, stack, player), spawnAt,
                        MobSpawnType.SPAWN_EGG, true, !Objects.equals(clicked, spawnAt) && face == Direction.UP);
        if (awakened && spawned instanceof DemiurgeStatueEntity statue) {
            statue.activateOnSpawn();
        }
        if (spawned != null) {
            level.addFreshEntityWithPassengers(spawned);
            stack.shrink(1);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, clicked);
        }
        return InteractionResult.CONSUME;
    }
}
