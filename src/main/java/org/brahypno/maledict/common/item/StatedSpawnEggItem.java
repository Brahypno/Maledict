package org.brahypno.maledict.common.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.ForgeSpawnEggItem;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 带状态的刷怪蛋：一枚刷怪蛋代表某个生物的某个状态，落地时把状态写进实体。
 * <p>
 * {@code onSpawn} 在实体已经创建、还没进入世界时调用。这一点是刻意的：血量池、属性上限这类东西由实体自己的
 * {@code onAddedToWorld} 捕获，刷怪蛋只负责写 {@code EntityType#create} 之后、{@code addFreshEntity} 之前
 * 的那一格状态，不能抢在实体之前拍板。见 {@code VicissitudeSpawnEggItem} 与石像的激活蛋。
 * <p>
 * {@code refusesPeaceful} 用于那些在和平难度下不该出现的首领：其召唤仪式本身也拒绝和平难度
 * （{@code WeepingWitherSummoning#trySpawn}、石像的腐枯之星），刷怪蛋与之保持一致。
 */
public class StatedSpawnEggItem extends ForgeSpawnEggItem {
    private final boolean refusesPeaceful;
    @Nullable
    private final Consumer<Entity> onSpawn;

    public StatedSpawnEggItem(
            Supplier<? extends EntityType<? extends Mob>> type, int backgroundColor, int highlightColor,
            Properties properties, boolean refusesPeaceful) {
        this(type, backgroundColor, highlightColor, properties, refusesPeaceful, null);
    }

    /**
     * @param onSpawn 落地时对这一只实体的状态写入；实体创建失败时不会调用
     */
    public StatedSpawnEggItem(
            Supplier<? extends EntityType<? extends Mob>> type, int backgroundColor, int highlightColor,
            Properties properties, boolean refusesPeaceful, @Nullable Consumer<Entity> onSpawn) {
        super(type, backgroundColor, highlightColor, properties);
        this.refusesPeaceful = refusesPeaceful;
        this.onSpawn = onSpawn;
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
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        if (refusesPeaceful && level.getDifficulty() == Difficulty.PEACEFUL) {
            return InteractionResult.FAIL;
        }
        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockState state = level.getBlockState(clicked);
        if (state.is(Blocks.SPAWNER)
            && level.getBlockEntity(clicked) instanceof SpawnerBlockEntity spawner) {
            // 刷怪笼走原版那一支：笼子只记实体类型，状态由笼子自己的生成流程决定。
            spawner.setEntityId(getType(stack.getTag()), level.getRandom());
            spawner.setChanged();
            level.sendBlockUpdated(clicked, state, state, 3);
            level.gameEvent(context.getPlayer(), GameEvent.BLOCK_CHANGE, clicked);
            stack.shrink(1);
            return InteractionResult.CONSUME;
        }
        BlockPos spawnAt = state.getCollisionShape(level, clicked).isEmpty()
                           ? clicked : clicked.relative(face);
        Player player = context.getPlayer();
        Entity spawned = getType(stack.getTag())
                .create(level, stack.getTag(), null, spawnAt, MobSpawnType.SPAWN_EGG,
                        true, !Objects.equals(clicked, spawnAt) && face == Direction.UP);
        if (spawned != null && onSpawn != null) {
            onSpawn.accept(spawned);
        }
        if (spawned != null) {
            level.addFreshEntityWithPassengers(spawned);
            stack.shrink(1);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, clicked);
        }
        return InteractionResult.CONSUME;
    }
}
