package org.brahypno.maledict.common.invocation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.animal.horse.SkeletonHorse;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.registries.ForgeRegistries;

/** Explicit adapters for the six supported entities; item NBT is never loaded onto a mob. */
public final class InvocationSpawns {
    public static boolean supports(InvocationDefinition definition) {
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(definition.entity());
        if (type == null) return false;
        if (type == EntityType.SHULKER) return definition.soulwood()
                && definition.profile() == InvocationDefinition.Profile.ATTACHED && definition.variant().equals("default");
        if (definition.soulwood()) return false;
        if (type == EntityType.AXOLOTL) return definition.profile() == InvocationDefinition.Profile.WATER
                && definition.variant().equals("blue");
        if (type == EntityType.MOOSHROOM) return definition.profile() == InvocationDefinition.Profile.GROUND
                && definition.variant().equals("brown");
        if (!definition.variant().equals("default")) return false;
        if (type == EntityType.ALLAY) return definition.profile() == InvocationDefinition.Profile.FLYING;
        return (type == EntityType.SNIFFER || type == EntityType.SKELETON_HORSE)
                && definition.profile() == InvocationDefinition.Profile.GROUND;
    }

    public static Mob create(ServerLevel level, InvocationDefinition definition) {
        if (!supports(definition) || (definition.soulwood() && level.getDifficulty() == Difficulty.PEACEFUL)) return null;
        return ForgeRegistries.ENTITY_TYPES.getValue(definition.entity()).create(level) instanceof Mob mob ? mob : null;
    }

    public static Vec3 findSpace(ServerLevel level, BlockPos base, Direction direction,
                                 InvocationDefinition definition, Mob mob) {
        BlockPos center = base.relative(direction, 3);
        for (int radius = 0; radius <= 2; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
                    for (int dy : new int[]{0, 1, -1}) {
                        BlockPos candidate = center.offset(dx, dy, dz);
                        mob.moveTo(candidate.getX() + .5D, candidate.getY(), candidate.getZ() + .5D,
                                direction.toYRot(), 0);
                        if (validSpace(level, definition, mob)) return mob.position();
                    }
                }
            }
        }
        return null;
    }

    static boolean validSpace(ServerLevel level, InvocationDefinition definition, Mob mob) {
        AABB box = mob.getBoundingBox();
        BlockPos first = BlockPos.containing(box.minX, box.minY, box.minZ);
        BlockPos last = BlockPos.containing(Math.nextDown(box.maxX), Math.nextDown(box.maxY), Math.nextDown(box.maxZ));
        if (box.minY < level.getMinBuildHeight() || box.maxY > level.getMaxBuildHeight()
                || !level.getWorldBorder().isWithinBounds(box) || !level.hasChunksAt(first.below(), last.above())
                || !level.noCollision(mob, box)) return false;
        for (BlockPos pos : BlockPos.betweenClosed(first, last)) {
            BlockState state = level.getBlockState(pos);
            if (harmful(state)) return false;
            if (definition.profile() == InvocationDefinition.Profile.WATER) {
                if (!state.getFluidState().is(FluidTags.WATER)) return false;
            } else if (!state.getFluidState().isEmpty()) return false;
        }
        if (definition.profile() == InvocationDefinition.Profile.GROUND
                || definition.profile() == InvocationDefinition.Profile.ATTACHED) {
            BlockPos floor = mob.blockPosition().below();
            if (harmful(level.getBlockState(floor))
                    || !level.loadedAndEntityCanStandOnFace(floor, mob, Direction.UP)) return false;
        }
        // Shulkers start attached downwards. Reserve their full upward opening volume too.
        if (definition.profile() == InvocationDefinition.Profile.ATTACHED) {
            AABB opened = new AABB(mob.blockPosition()).expandTowards(0, 1, 0).deflate(1.0E-6D);
            return level.isEmptyBlock(mob.blockPosition()) && level.noCollision(mob, opened);
        }
        return true;
    }

    private static boolean harmful(BlockState state) {
        return state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE) || state.is(Blocks.CACTUS)
                || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE) || state.is(Blocks.POWDER_SNOW);
    }

    public static boolean summon(ServerLevel level, InvocationDefinition definition, Mob mob) {
        ForgeEventFactory.onFinalizeSpawn(mob, level, level.getCurrentDifficultyAt(mob.blockPosition()),
                MobSpawnType.MOB_SUMMONED, null, null);
        if (mob.isSpawnCancelled()) return false;
        if (mob instanceof AgeableMob ageable) ageable.setAge(0);
        if (mob instanceof Axolotl axolotl) axolotl.setVariant(Axolotl.Variant.BLUE);
        if (mob instanceof MushroomCow cow) cow.setVariant(MushroomCow.MushroomType.BROWN);
        if (mob instanceof SkeletonHorse horse) horse.setTrap(false);
        mob.setPersistenceRequired();
        return validSpace(level, definition, mob) && level.addFreshEntity(mob);
    }

    private InvocationSpawns() {
    }
}
