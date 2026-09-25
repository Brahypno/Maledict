package org.brahypno.maledict.common.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.RegistryObject;
import org.brahypno.maledict.common.entity.FirstVicissitudeBossEntity;
import org.brahypno.maledict.common.entity.FirstVicissitudeBossEntity.BossDifficulty;
import org.brahypno.maledict.common.entity.FirstVicissitudeBossEntity.BossSpawnPhase;

import java.util.Objects;

/**
 * 无常的刷怪蛋，一个阶段一个：落地的同时就把这一只安排到那个阶段。
 * {@code setSpawnPhase} 必须赶在 {@code onAddedToWorld} <b>之前</b>（血量池在那里按当时的难度捕获），
 * 所以用 {@code EntityType#create} 定完阶段再自己 {@code addFreshEntityWithPassengers}。
 */
public class VicissitudeSpawnEggItem extends ForgeSpawnEggItem {
    private final BossDifficulty difficulty;
    private final BossSpawnPhase spawnPhase;

    public VicissitudeSpawnEggItem(
            RegistryObject<? extends EntityType<? extends FirstVicissitudeBossEntity>> type,
            BossDifficulty difficulty, BossSpawnPhase spawnPhase,
            int backgroundColor, int highlightColor, Properties properties) {
        super(type, backgroundColor, highlightColor, properties);
        this.difficulty = difficulty;
        this.spawnPhase = spawnPhase;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockState state = level.getBlockState(clicked);
        if (state.is(Blocks.SPAWNER)
            && level.getBlockEntity(clicked) instanceof SpawnerBlockEntity spawner) {
            // 刷怪笼走原版那一支：笼子只记实体类型，阶段由笼子自己的生成流程决定。
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
        if (spawned instanceof FirstVicissitudeBossEntity boss) {
            boss.setSpawnPhase(difficulty, spawnPhase);
        }
        if (spawned != null) {
            level.addFreshEntityWithPassengers(spawned);
            stack.shrink(1);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, clicked);
        }
        return InteractionResult.CONSUME;
    }
}
