package org.brahypno.maledict.common.realm;

import com.sammy.malum.common.block.curiosities.weeping_well.PrimordialSoupBlock;
import com.sammy.malum.common.block.curiosities.weeping_well.VoidConduitBlock;
import com.sammy.malum.registry.common.block.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.Optional;

final class WellPlacement {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final ResourceLocation TEMPLATE = ResourceLocation.fromNamespaceAndPath("malum", "weeping_well");
    private static final int[][] SITES = {{0, 0}, {32, 0}, {0, 32}, {-32, 0}, {0, -32},
            {32, 32}, {-32, 32}, {32, -32}, {-32, -32}, {64, 0}, {0, 64}, {-64, 0}, {0, -64}};

    static boolean isWell(BlockState state) {
        return state.getBlock() instanceof PrimordialSoupBlock || state.getBlock() instanceof VoidConduitBlock;
    }

    static BlockPos source(ServerLevel level, LivingEntity player) {
        BlockPos contact = player.blockPosition();
        if (!isWell(level.getBlockState(contact))) contact = contact.above();
        BlockPos nearest = null;
        double distance = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(contact.offset(-8, -8, -8), contact.offset(8, 8, 8))) {
            if (level.hasChunkAt(pos) && level.getBlockState(pos).getBlock() instanceof VoidConduitBlock
                    && pos.distSqr(contact) < distance) {
                nearest = pos.immutable();
                distance = pos.distSqr(contact);
            }
        }
        if (nearest != null) return nearest;
        // Rejection itself is the entrance condition: a manually placed pool also works without a full structure.
        while (contact.getY() > level.getMinBuildHeight() && isWell(level.getBlockState(contact.below()))) {
            contact = contact.below();
        }
        return contact.immutable();
    }

    static Optional<Vec3> near(ServerLevel level, LivingEntity player, BlockPos center, int radius) {
        for (int ring = 0; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    for (int dy = 6; dy >= -8; dy--) {
                        BlockPos ground = center.offset(dx, dy, dz);
                        if (!level.hasChunkAt(ground) || !level.isInWorldBounds(ground)) continue;
                        BlockState state = level.getBlockState(ground);
                        if (hazard(state) || !state.getFluidState().isEmpty()) continue;
                        var shape = state.getCollisionShape(level, ground, CollisionContext.of(player));
                        if (shape.isEmpty()) continue;
                        Vec3 feet = new Vec3(ground.getX() + 0.5D,
                                ground.getY() + shape.max(Direction.Axis.Y), ground.getZ() + 0.5D);
                        if (safe(level, player, feet)) return Optional.of(feet);
                    }
                }
            }
        }
        return Optional.empty();
    }

    static boolean safe(ServerLevel level, LivingEntity player, Vec3 feet) {
        var box = player.getBoundingBox().move(feet.subtract(player.position()));
        if (!clear(level, player, feet)) return false;
        BlockPos support = BlockPos.containing(feet.x, feet.y - 0.01D, feet.z);
        BlockState ground = level.getBlockState(support);
        var shape = ground.getCollisionShape(level, support, CollisionContext.of(player));
        if (hazard(ground) || !ground.getFluidState().isEmpty() || shape.isEmpty()
                || Math.abs(support.getY() + shape.max(Direction.Axis.Y) - feet.y) > 0.02D) return false;
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            if (hazard(level.getBlockState(pos))) return false;
        }
        return true;
    }

    static boolean clear(ServerLevel level, LivingEntity player, Vec3 feet) {
        var box = player.getBoundingBox().move(feet.subtract(player.position()));
        return level.getWorldBorder().isWithinBounds(box) && box.minY >= level.getMinBuildHeight()
                && box.maxY < level.getMaxBuildHeight() && level.noCollision(player, box)
                && !level.containsAnyLiquid(box);
    }

    static Optional<Vec3> exit(ServerLevel level, LivingEntity player, BlockPos core) {
        BlockPos mouth = core.above();
        for (int height = 0; height < 16 && isWell(level.getBlockState(mouth)); height++) mouth = mouth.above();
        Vec3 feet = Vec3.atBottomCenterOf(mouth);
        return clear(level, player, feet) && clear(level, player, feet.add(0, 1, 0))
                ? Optional.of(feet) : Optional.empty();
    }

    private static boolean hazard(BlockState state) {
        return isWell(state) || state.is(BlockTags.FIRE) || state.is(Blocks.CACTUS)
                || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE)
                || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.POINTED_DRIPSTONE);
    }

    static Optional<WeepingWellLinks.Endpoint> build(ServerLevel level, LivingEntity player,
                                                   BlockPos target, WeepingWellLinks links) {
        var template = level.getStructureManager().get(TEMPLATE);
        if (template.isEmpty()) return Optional.empty();
        var settings = new StructurePlaceSettings().setIgnoreEntities(true).setKeepLiquids(false);
        var cores = template.get().filterBlocks(BlockPos.ZERO, settings, BlockRegistry.VOID_CONDUIT.get());
        if (cores.size() != 1) return Optional.empty();
        var coreOffset = cores.get(0).pos();
        var size = template.get().getSize();
        for (int[] offset : SITES) {
            int x = target.getX() + offset[0];
            int z = target.getZ() + offset[1];
            BlockPos center = new BlockPos(x, 0, z);
            if (!level.getWorldBorder().isWithinBounds(center.offset(-size.getX(), 0, -size.getZ()))
                    || !level.getWorldBorder().isWithinBounds(center.offset(size.getX(), 0, size.getZ()))
                    || links.hasWellNear(level.dimension(), center, 20)) continue;
            // Level#getHeight returns min build height for an unloaded chunk; generate before sampling it.
            level.getChunk(x >> 4, z >> 4);
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            while (surface > level.getMinBuildHeight() + 16
                    && level.getBlockState(new BlockPos(x, surface - 1, z)).is(BlockTags.LOGS)) surface--;
            // The native template is 13x15x13; bury its basin and leave its upper pillars above ground.
            int floorY = surface - coreOffset.getY() - 1;
            // A shallow/flat world cannot bury the native basin through bedrock; raise the whole well instead.
            for (int depth = 1; depth <= coreOffset.getY() + 1; depth++) {
                if (level.getBlockState(new BlockPos(x, surface - depth, z)).is(Blocks.BEDROCK)) {
                    floorY = Math.max(floorY, surface - depth + 1);
                }
            }
            BlockPos origin = new BlockPos(x - coreOffset.getX(), floorY,
                    z - coreOffset.getZ());
            if (origin.getY() <= level.getMinBuildHeight() || origin.getY() + size.getY() >= level.getMaxBuildHeight()) continue;
            boolean free = true;
            for (BlockPos pos : BlockPos.betweenClosed(origin, origin.offset(size.getX() - 1, size.getY() - 1, size.getZ() - 1))) {
                if (!natural(level.getBlockState(pos)) || level.getBlockEntity(pos) != null) {
                    LOGGER.debug("Well site {} blocked by {} at {}", center, level.getBlockState(pos), pos.immutable());
                    free = false;
                    break;
                }
            }
            if (!free) continue;
            if (!template.get().placeInWorld(level, origin, origin, settings, level.random, 3)) continue;
            BlockPos core = origin.offset(coreOffset);
            var landing = near(level, player, core.above(2), 8);
            if (landing.isPresent()) return Optional.of(new WeepingWellLinks.Endpoint(level.dimension(), core, landing.get()));
            LOGGER.debug("Well at {} has no safe rim for {}", core, player.getUUID());
        }
        return Optional.empty();
    }

    private static boolean natural(BlockState state) {
        return state.isAir() || state.canBeReplaced() || !state.getFluidState().isEmpty()
                || state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.DIRT) || state.is(BlockTags.SAND)
                || state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES) || state.is(Blocks.GRAVEL)
                || state.is(net.minecraftforge.common.Tags.Blocks.ORES)
                || state.is(Blocks.CLAY) || state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.ICE)
                || state.is(Blocks.PACKED_ICE) || state.is(BlockRegistry.BLIGHTED_EARTH.get())
                || state.is(BlockRegistry.BLIGHTED_SOIL.get()) || state.is(BlockRegistry.BLIGHTED_GROWTH.get())
                || state.is(BlockRegistry.CLINGING_BLIGHT.get()) || state.is(BlockRegistry.TAINTED_ROCK.get())
                || state.is(BlockRegistry.TWISTED_ROCK.get())
                || state.is(BlockRegistry.BLIGHTED_SOULWOOD.get());
    }

    private WellPlacement() {
    }
}
