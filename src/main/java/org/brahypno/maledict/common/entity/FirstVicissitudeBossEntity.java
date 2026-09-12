package org.brahypno.maledict.common.entity;

import com.sammy.malum.registry.common.DamageTypeRegistry;
import com.sammy.malum.registry.common.item.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import org.brahypno.maledict.common.item.IncursusBladeItem;
import org.brahypno.maledict.registry.MaledictItems;
import org.jetbrains.annotations.Nullable;
import team.lodestar.lodestone.helpers.DamageTypeHelper;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** The first Vicissitude encounter. */
public final class FirstVicissitudeBossEntity extends VicissitudeBossEntity {
    public static final int PHASE_ONE_DURATION_TICKS = 20 * 300;
    public static final int PHASE_ONE_WARMUP_TICKS = 20;
    public static final int LIGHT_ORB_INTERVAL_TICKS = 20 * 2;
    public static final String PHASE_TWO_MESSAGE_KEY =
            "message.maledict.first_vicissitude.phase_two";

    private static final String[] INCURSUS_STATS = {
            IncursusBladeItem.ATTACK_DAMAGE,
            IncursusBladeItem.POWDER_SNOW_DAMAGE,
            IncursusBladeItem.MAGIC_DAMAGE,
            IncursusBladeItem.AERIAL_PROGRESS,
            IncursusBladeItem.SACRED_POWER,
            IncursusBladeItem.INFERNAL_POWER,
            IncursusBladeItem.ELDRITCH_ABSORPTION,
            IncursusBladeItem.WICKED_CRITICAL_DAMAGE
    };
    private static final double PREFERRED_DISTANCE = 12.0D;
    private static final double HOVER_HEIGHT = 5.0D;
    private static final double PHASE_TWO_DISTANCE = 2.25D;
    private static final double PHASE_TWO_HOVER_HEIGHT = 1.35D;
    private static final double MAX_FLIGHT_SPEED = 0.38D;
    private static final double PHASE_TWO_MAX_FLIGHT_SPEED = 0.52D;
    private static final double FLIGHT_STEERING = 0.2D;
    private static final double ORBIT_STEP = 0.08D;
    private static final int PHASE_TWO_ATTACK_INTERVAL = 20;
    private static final int BLOCK_BREAK_INTERVAL = 10;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
    private final Set<UUID> phaseOneTargets = new LinkedHashSet<>();
    private final Set<UUID> phaseTwoPlayers = new LinkedHashSet<>();
    private final Map<UUID, Integer> targetPlayerDeaths = new HashMap<>();
    private final Map<UUID, Integer> announcedPlayerDeaths = new HashMap<>();
    private final Map<UUID, Integer> phaseTwoPlayerDeaths = new HashMap<>();
    private final Map<UUID, Deque<ConfiscatedCurio>> confiscatedCurios = new LinkedHashMap<>();
    private boolean phaseOneStarted;
    private boolean phaseTwoStarted;
    private int phaseOneTicks;
    private int targetCursor;
    private int curioReturnCursor;
    private double idleHoverY = Double.NaN;
    private BossDifficulty bossDifficulty = BossDifficulty.SIMPLE;

    public FirstVicissitudeBossEntity(EntityType<? extends FirstVicissitudeBossEntity> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
        xpReward = 100;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBossAttributes()
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FLYING_SPEED, 0.45D)
                .add(Attributes.FOLLOW_RANGE, 128.0D);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new PhaseOneCombatGoal(this));
        goalSelector.addGoal(0, new PhaseTwoCombatGoal(this));
    }

    public boolean isPhaseOne() {
        return phaseOneTicks < PHASE_ONE_DURATION_TICKS;
    }

    @Override
    protected boolean isDamageImmune(DamageSource source) {
        return isPhaseOne() || source.getEntity() == null;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return isDamageImmune(source) || super.isInvulnerableTo(source);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return effect.getEffect().getCategory() != MobEffectCategory.HARMFUL
               && super.canBeAffected(effect);
    }

    @Override
    protected void onIncomingAttack(DamageSource source, float amount) {
        if (!isPhaseOne()) {
            return;
        }
        LivingEntity attacker = resolveLivingAttacker(source);
        if (attacker != null && attacker != this && attacker.isAlive() && attacker.hasLineOfSight(this)) {
            phaseOneTargets.add(attacker.getUUID());
            if (attacker instanceof ServerPlayer player) {
                targetPlayerDeaths.put(player.getUUID(), getDeathCount(player));
            }
            phaseOneStarted = true;
            if (!isValidPhaseOneTarget(getTarget())) {
                setTarget(attacker);
            }
        }
    }

    @Override
    protected void onDamageAccepted(DamageSource source, float amount) {
        if (phaseTwoStarted && bossDifficulty.confiscatesCurios()) {
            returnOneCurio(resolveAttackingPlayer(source));
        }
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!level().isClientSide && Double.isNaN(idleHoverY)) {
            idleHoverY = Math.min(level().getMaxBuildHeight() - getBbHeight() - 1.0D,
                                  getY() + HOVER_HEIGHT);
        }
    }

    @Override
    public void tick() {
        setNoGravity(true);
        super.tick();
        if (level().isClientSide) {
            return;
        }
        bossEvent.setName(getDisplayName());
        bossEvent.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0.0F, 1.0F));
        if (isAlive() && isPhaseOne() && phaseOneStarted) {
            phaseOneTicks++;
            if (!isPhaseOne()) {
                beginPhaseTwo();
            }
        } else if (isAlive() && !isPhaseOne() && !phaseTwoStarted) {
            beginPhaseTwo();
        }
        if (isAlive() && phaseTwoStarted) {
            returnCuriosAfterPlayerDeaths();
            destroyBlockingEntities();
            if ((horizontalCollision || verticalCollision) && tickCount % BLOCK_BREAK_INTERVAL == 0) {
                destroyBlockingBlocks();
            }
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        bossEvent.removePlayer(player);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("PhaseOneStarted", phaseOneStarted);
        tag.putBoolean("PhaseTwoStarted", phaseTwoStarted);
        tag.putInt("PhaseOneTicks", phaseOneTicks);
        tag.putInt("PhaseOneTargetCursor", targetCursor);
        tag.putInt("CurioReturnCursor", curioReturnCursor);
        tag.putString("VicissitudeDifficulty", bossDifficulty.serializedName());
        if (!Double.isNaN(idleHoverY)) {
            tag.putDouble("PhaseOneHoverY", idleHoverY);
        }
        tag.put("PhaseOneTargets", saveUuidSet(phaseOneTargets));
        tag.put("PhaseTwoPlayers", saveUuidSet(phaseTwoPlayers));
        tag.put("PhaseOneTargetDeaths", savePlayerDeathMap(targetPlayerDeaths));
        tag.put("PhaseOneAnnouncements", savePlayerDeathMap(announcedPlayerDeaths));
        tag.put("PhaseTwoPlayerDeaths", savePlayerDeathMap(phaseTwoPlayerDeaths));
        tag.put("ConfiscatedCurios", saveConfiscatedCurios());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        boolean hasStartedState = tag.contains("PhaseOneStarted", Tag.TAG_BYTE);
        phaseOneTicks = hasStartedState
                        ? Mth.clamp(tag.getInt("PhaseOneTicks"), 0, PHASE_ONE_DURATION_TICKS)
                        : 0;
        phaseOneStarted = hasStartedState && tag.getBoolean("PhaseOneStarted");
        phaseTwoStarted = tag.getBoolean("PhaseTwoStarted");
        targetCursor = Math.max(0, tag.getInt("PhaseOneTargetCursor"));
        curioReturnCursor = Math.max(0, tag.getInt("CurioReturnCursor"));
        String savedDifficulty = tag.contains("VicissitudeDifficulty", Tag.TAG_STRING)
                                 ? tag.getString("VicissitudeDifficulty")
                                 : tag.getString("PhaseTwoMode");
        bossDifficulty = BossDifficulty.fromName(savedDifficulty);
        if (tag.contains("PhaseOneHoverY", Tag.TAG_DOUBLE)) {
            idleHoverY = tag.getDouble("PhaseOneHoverY");
        }
        loadUuidSet(tag.getList("PhaseOneTargets", Tag.TAG_INT_ARRAY), phaseOneTargets);
        loadUuidSet(tag.getList("PhaseTwoPlayers", Tag.TAG_INT_ARRAY), phaseTwoPlayers);
        loadPlayerDeathMap(tag.getList("PhaseOneTargetDeaths", Tag.TAG_COMPOUND), targetPlayerDeaths);
        loadPlayerDeathMap(tag.getList("PhaseOneAnnouncements", Tag.TAG_COMPOUND), announcedPlayerDeaths);
        loadPlayerDeathMap(tag.getList("PhaseTwoPlayerDeaths", Tag.TAG_COMPOUND), phaseTwoPlayerDeaths);
        loadConfiscatedCurios(tag.getList("ConfiscatedCurios", Tag.TAG_COMPOUND));
    }

    private void beginPhaseTwo() {
        if (!(level() instanceof ServerLevel serverLevel) || phaseTwoStarted) {
            return;
        }
        phaseTwoStarted = true;
        setTarget(null);
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        equipPhaseTwoWeapon();

        phaseTwoPlayers.clear();
        for (UUID identity : phaseOneTargets) {
            ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(identity);
            if (player != null && player.level() == level() && !player.isSpectator()) {
                phaseTwoPlayers.add(identity);
                phaseTwoPlayerDeaths.put(identity, getDeathCount(player));
                player.displayClientMessage(Component.translatable(PHASE_TWO_MESSAGE_KEY), true);
                if (bossDifficulty.confiscatesCurios()) {
                    confiscateEquippedCurios(player);
                }
            }
        }
    }

    private void equipPhaseTwoWeapon() {
        ItemStack weapon;
        if (bossDifficulty == BossDifficulty.SIMPLE) {
            weapon = new ItemStack(ItemRegistry.SOUL_STAINED_STEEL_SCYTHE.get());
        } else if (bossDifficulty == BossDifficulty.DIFFICULT) {
            weapon = new ItemStack(ItemRegistry.EDGE_OF_DELIVERANCE.get());
        } else {
            weapon = new ItemStack(MaledictItems.INCURSUS_BLADE.get());
            IncursusBladeItem.initializeStats(weapon);
            double level = bossDifficulty == BossDifficulty.EXTREME ? 9.0D : 3.0D;
            for (String stat : INCURSUS_STATS) {
                IncursusBladeItem.setStat(weapon, stat, level);
            }
        }
        setItemSlot(EquipmentSlot.MAINHAND, weapon);
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    public BossDifficulty getBossDifficulty() {
        return bossDifficulty;
    }

    public void setBossDifficulty(BossDifficulty difficulty) {
        bossDifficulty = difficulty == null ? BossDifficulty.SIMPLE : difficulty;
        if (phaseTwoStarted && !level().isClientSide) {
            equipPhaseTwoWeapon();
        }
    }

    private void confiscateEquippedCurios(ServerPlayer player) {
        ICuriosItemHandler inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inventory == null) {
            return;
        }
        Deque<ConfiscatedCurio> held = confiscatedCurios.computeIfAbsent(
                player.getUUID(), ignored -> new ArrayDeque<>());
        List<String> identifiers = new ArrayList<>(inventory.getCurios().keySet());
        Collections.sort(identifiers);
        for (String identifier : identifiers) {
            ICurioStacksHandler handler = inventory.getCurios().get(identifier);
            IDynamicStackHandler stacks = handler.getStacks();
            for (int slot = 0; slot < stacks.getSlots(); slot++) {
                ItemStack equipped = stacks.getStackInSlot(slot);
                if (!equipped.isEmpty()) {
                    held.addLast(new ConfiscatedCurio(identifier, slot, equipped.copy()));
                    inventory.setEquippedCurio(identifier, slot, ItemStack.EMPTY);
                }
            }
        }
        if (held.isEmpty()) {
            confiscatedCurios.remove(player.getUUID());
        }
    }

    private void returnOneCurio(@Nullable ServerPlayer preferredPlayer) {
        if (preferredPlayer != null && returnOneCurioTo(preferredPlayer)) {
            return;
        }
        if (!(level() instanceof ServerLevel serverLevel) || phaseTwoPlayers.isEmpty()) {
            return;
        }
        List<UUID> players = new ArrayList<>(phaseTwoPlayers);
        int start = Math.floorMod(curioReturnCursor, players.size());
        for (int offset = 0; offset < players.size(); offset++) {
            int index = (start + offset) % players.size();
            ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(players.get(index));
            if (player != null && returnOneCurioTo(player)) {
                curioReturnCursor = index + 1;
                return;
            }
        }
    }

    private boolean returnOneCurioTo(ServerPlayer player) {
        Deque<ConfiscatedCurio> held = confiscatedCurios.get(player.getUUID());
        if (held == null || held.isEmpty()) {
            return false;
        }
        ConfiscatedCurio curio = held.removeFirst();
        ItemStack stack = curio.stack().copy();
        if (!tryEquipCurio(player, curio, stack)) {
            player.getInventory().add(stack);
            if (!stack.isEmpty()) {
                player.drop(stack, false);
            }
        }
        if (held.isEmpty()) {
            confiscatedCurios.remove(player.getUUID());
        }
        return true;
    }

    private void returnCuriosAfterPlayerDeaths() {
        if (!(level() instanceof ServerLevel serverLevel) || confiscatedCurios.isEmpty()) {
            return;
        }
        for (UUID identity : phaseTwoPlayers) {
            ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(identity);
            if (player == null) {
                continue;
            }
            int deaths = getDeathCount(player);
            int recordedDeaths = phaseTwoPlayerDeaths.getOrDefault(identity, deaths);
            if (deaths != recordedDeaths && player.isAlive()) {
                while (returnOneCurioTo(player)) {
                    // Restore every curio still held by the boss after this player's death.
                }
                phaseTwoPlayerDeaths.put(identity, deaths);
            }
        }
    }

    private boolean tryEquipCurio(ServerPlayer player, ConfiscatedCurio curio, ItemStack stack) {
        ICuriosItemHandler inventory = CuriosApi.getCuriosInventory(player).orElse(null);
        if (inventory == null) {
            return false;
        }
        if (tryEquipCurio(inventory, curio.slotIdentifier(), curio.slotIndex(), stack)) {
            return true;
        }
        List<String> identifiers = new ArrayList<>(inventory.getCurios().keySet());
        Collections.sort(identifiers);
        for (String identifier : identifiers) {
            ICurioStacksHandler handler = inventory.getCurios().get(identifier);
            for (int slot = 0; slot < handler.getStacks().getSlots(); slot++) {
                if (tryEquipCurio(inventory, identifier, slot, stack)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean tryEquipCurio(
            ICuriosItemHandler inventory, String identifier, int slot, ItemStack stack) {
        ICurioStacksHandler handler = inventory.getCurios().get(identifier);
        if (handler == null || slot < 0 || slot >= handler.getStacks().getSlots()) {
            return false;
        }
        IDynamicStackHandler stacks = handler.getStacks();
        if (!stacks.getStackInSlot(slot).isEmpty() || !stacks.isItemValid(slot, stack)) {
            return false;
        }
        inventory.setEquippedCurio(identifier, slot, stack.copy());
        stack.setCount(0);
        return true;
    }

    private void updatePhaseOneMovement(@Nullable LivingEntity target) {
        if (target == null) {
            double wantedY = Double.isNaN(idleHoverY) ? getY() + HOVER_HEIGHT : idleHoverY;
            steerToward(new Vec3(getX(), wantedY, getZ()), MAX_FLIGHT_SPEED);
            return;
        }
        Vec3 away = position().subtract(target.position()).multiply(1.0D, 0.0D, 1.0D);
        if (away.lengthSqr() < 0.01D) {
            double angle = phaseOneTicks * ORBIT_STEP;
            away = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
        }
        double currentAngle = Math.atan2(away.z, away.x);
        double wantedAngle = currentAngle + ORBIT_STEP;
        Vec3 wanted = new Vec3(
                target.getX() + Math.cos(wantedAngle) * PREFERRED_DISTANCE,
                target.getY(),
                target.getZ() + Math.sin(wantedAngle) * PREFERRED_DISTANCE);
        double wantedY = Mth.clamp(target.getEyeY() + HOVER_HEIGHT,
                                   level().getMinBuildHeight() + 1.0D,
                                   level().getMaxBuildHeight() - getBbHeight() - 1.0D);
        steerToward(new Vec3(wanted.x, wantedY, wanted.z), MAX_FLIGHT_SPEED);
    }

    private void updatePhaseTwoMovement(@Nullable LivingEntity target) {
        if (target == null) {
            steerToward(position().add(0.0D, 0.15D, 0.0D), PHASE_TWO_MAX_FLIGHT_SPEED);
            return;
        }
        Vec3 away = position().subtract(target.position()).multiply(1.0D, 0.0D, 1.0D);
        if (away.lengthSqr() < 0.01D) {
            double angle = tickCount * ORBIT_STEP;
            away = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
        } else {
            away = away.normalize();
        }
        double wantedY = Mth.clamp(target.getY() + PHASE_TWO_HOVER_HEIGHT,
                                   level().getMinBuildHeight() + 1.0D,
                                   level().getMaxBuildHeight() - getBbHeight() - 1.0D);
        Vec3 destination = new Vec3(
                target.getX() + away.x * PHASE_TWO_DISTANCE,
                wantedY,
                target.getZ() + away.z * PHASE_TWO_DISTANCE);
        steerToward(destination, PHASE_TWO_MAX_FLIGHT_SPEED);
    }

    private void steerToward(Vec3 destination, double maximumSpeed) {
        Vec3 offset = destination.subtract(position());
        if (offset.lengthSqr() < 0.01D) {
            setDeltaMovement(getDeltaMovement().scale(0.7D));
            return;
        }
        double desiredSpeed = Math.min(maximumSpeed, offset.length() * 0.12D);
        Vec3 desiredMotion = offset.normalize().scale(desiredSpeed);
        Vec3 motion = getDeltaMovement().scale(1.0D - FLIGHT_STEERING)
                                        .add(desiredMotion.scale(FLIGHT_STEERING));
        if (motion.lengthSqr() > maximumSpeed * maximumSpeed) {
            motion = motion.normalize().scale(maximumSpeed);
        }
        setDeltaMovement(motion);
        hasImpulse = true;
    }

    @Nullable
    private LivingEntity currentPhaseOneTarget() {
        LivingEntity target = getTarget();
        return isValidPhaseOneTarget(target) ? target : selectNextPhaseOneTarget(false);
    }

    @Nullable
    private LivingEntity selectNextPhaseOneTarget(boolean advance) {
        if (!(level() instanceof ServerLevel serverLevel) || phaseOneTargets.isEmpty()) {
            setTarget(null);
            return null;
        }
        List<UUID> targets = new ArrayList<>(phaseOneTargets);
        int size = targets.size();
        int start = Math.floorMod(targetCursor, size);
        List<UUID> invalid = new ArrayList<>();
        for (int offset = 0; offset < size; offset++) {
            int index = (start + offset) % size;
            UUID identity = targets.get(index);
            Entity candidate = serverLevel.getEntity(identity);
            if (candidate instanceof LivingEntity living && isValidPhaseOneTarget(living)) {
                targetCursor = advance ? index + 1 : index;
                setTarget(living);
                removeInvalidTargets(invalid);
                return living;
            }
            if (candidate != null) {
                invalid.add(identity);
            }
        }
        removeInvalidTargets(invalid);
        setTarget(null);
        return null;
    }

    @Nullable
    private LivingEntity selectBalancedPhaseTwoTarget() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        LivingEntity selected = phaseTwoPlayers.stream()
                .map(identity -> serverLevel.getServer().getPlayerList().getPlayer(identity))
                .filter(player -> player != null && player.level() == level()
                                  && player.isAlive() && !player.isSpectator())
                .max(Comparator.comparingDouble(LivingEntity::getHealth))
                .orElse(null);
        if (selected == null) {
            selected = phaseOneTargets.stream()
                    .map(serverLevel::getEntity)
                    .filter(LivingEntity.class::isInstance)
                    .map(LivingEntity.class::cast)
                    .filter(target -> target != this && target.isAlive() && target.level() == level())
                    .max(Comparator.comparingDouble(LivingEntity::getHealth))
                    .orElse(null);
        }
        setTarget(selected);
        return selected;
    }

    private boolean isValidPhaseOneTarget(@Nullable LivingEntity target) {
        if (target == null || target == this || !target.isAlive() || target.level() != level()) {
            return false;
        }
        if (target instanceof ServerPlayer player) {
            Integer registeredDeathCount = targetPlayerDeaths.get(player.getUUID());
            return !player.isSpectator()
                   && (registeredDeathCount == null || registeredDeathCount == getDeathCount(player));
        }
        return !(target instanceof Player player) || !player.isSpectator();
    }

    private void fireLightOrb(LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Vec3 launch = new Vec3(getX(), getEyeY() - 0.1D, getZ());
        announceFirstAttack(target);
        VicissitudeLightOrbEntity orb = new VicissitudeLightOrbEntity(serverLevel, target, launch);
        serverLevel.addFreshEntity(orb);
        swing(InteractionHand.MAIN_HAND);
    }

    private boolean performPhaseTwoAttack(LivingEntity target) {
        ItemStack weapon = getMainHandItem();
        float damage = Math.max(1.0F, (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
        boolean damaged = target.hurt(
                DamageTypeHelper.create(level(), DamageTypeRegistry.SCYTHE_MELEE, this), damage);
        if (damaged) {
            swing(InteractionHand.MAIN_HAND, true);
            if (!weapon.isEmpty()) {
                weapon.hurtAndBreak(1, this,
                        entity -> entity.broadcastBreakEvent(EquipmentSlot.MAINHAND));
            }
        }
        return damaged;
    }

    private void announceFirstAttack(LivingEntity target) {
        if (!(target instanceof ServerPlayer player)) {
            return;
        }
        int deathCount = getDeathCount(player);
        if (announcedPlayerDeaths.getOrDefault(player.getUUID(), -1) != deathCount) {
            player.displayClientMessage(
                    Component.translatable(VicissitudeLightOrbEntity.ATTACK_MESSAGE_KEY), true);
            announcedPlayerDeaths.put(player.getUUID(), deathCount);
        }
    }

    private void destroyBlockingEntities() {
        List<Entity> obstacles = level().getEntities(
                this,
                getBoundingBox().expandTowards(getDeltaMovement()).inflate(0.25D),
                entity -> entity.isAlive() && entity.isAttackable()
                          && !(entity instanceof Player)
                          && entity != getTarget());
        if (obstacles.isEmpty()) {
            return;
        }
        float damage = Math.max(10.0F, (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
        boolean attacked = false;
        for (Entity obstacle : obstacles) {
            attacked |= obstacle.hurt(damageSources().mobAttack(this), damage);
        }
        if (attacked) {
            swing(InteractionHand.MAIN_HAND, true);
        }
    }

    private void destroyBlockingBlocks() {
        if (!ForgeEventFactory.getMobGriefingEvent(level(), this)) {
            return;
        }
        BlockPos center = blockPosition();
        boolean destroyed = false;
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = 0; y <= 3; y++) {
                    BlockPos position = center.offset(x, y, z);
                    BlockState state = level().getBlockState(position);
                    if (!state.isAir()
                        && state.canEntityDestroy(level(), position, this)
                        && ForgeEventFactory.onEntityDestroyBlock(this, position, state)) {
                        destroyed = level().destroyBlock(position, true, this) || destroyed;
                    }
                }
            }
        }
        if (destroyed) {
            level().levelEvent(null, 1022, center, 0);
        }
    }

    @Nullable
    private LivingEntity resolveLivingAttacker(DamageSource source) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity)) {
            attacker = source.getDirectEntity();
        }
        return attacker instanceof LivingEntity living ? living : null;
    }

    @Nullable
    private ServerPlayer resolveAttackingPlayer(DamageSource source) {
        LivingEntity attacker = resolveLivingAttacker(source);
        return attacker instanceof ServerPlayer player ? player : null;
    }

    private static int getDeathCount(ServerPlayer player) {
        return player.getStats().getValue(Stats.CUSTOM.get(Stats.DEATHS));
    }

    private void removeInvalidTargets(List<UUID> invalid) {
        phaseOneTargets.removeAll(invalid);
        for (UUID identity : invalid) {
            targetPlayerDeaths.remove(identity);
        }
    }

    private static ListTag saveUuidSet(Set<UUID> values) {
        ListTag entries = new ListTag();
        for (UUID value : values) {
            entries.add(NbtUtils.createUUID(value));
        }
        return entries;
    }

    private static void loadUuidSet(ListTag entries, Set<UUID> values) {
        values.clear();
        for (Tag entry : entries) {
            values.add(NbtUtils.loadUUID(entry));
        }
    }

    private static ListTag savePlayerDeathMap(Map<UUID, Integer> values) {
        ListTag entries = new ListTag();
        for (Map.Entry<UUID, Integer> entry : values.entrySet()) {
            CompoundTag value = new CompoundTag();
            value.putUUID("Player", entry.getKey());
            value.putInt("Deaths", entry.getValue());
            entries.add(value);
        }
        return entries;
    }

    private static void loadPlayerDeathMap(ListTag entries, Map<UUID, Integer> values) {
        values.clear();
        for (Tag entry : entries) {
            CompoundTag value = (CompoundTag) entry;
            if (value.hasUUID("Player")) {
                values.put(value.getUUID("Player"), value.getInt("Deaths"));
            }
        }
    }

    private ListTag saveConfiscatedCurios() {
        ListTag players = new ListTag();
        for (Map.Entry<UUID, Deque<ConfiscatedCurio>> entry : confiscatedCurios.entrySet()) {
            CompoundTag player = new CompoundTag();
            player.putUUID("Player", entry.getKey());
            ListTag items = new ListTag();
            for (ConfiscatedCurio curio : entry.getValue()) {
                CompoundTag item = new CompoundTag();
                item.putString("Slot", curio.slotIdentifier());
                item.putInt("Index", curio.slotIndex());
                item.put("Stack", curio.stack().save(new CompoundTag()));
                items.add(item);
            }
            player.put("Items", items);
            players.add(player);
        }
        return players;
    }

    private void loadConfiscatedCurios(ListTag players) {
        confiscatedCurios.clear();
        for (Tag playerTag : players) {
            CompoundTag player = (CompoundTag) playerTag;
            if (!player.hasUUID("Player")) {
                continue;
            }
            Deque<ConfiscatedCurio> items = new ArrayDeque<>();
            for (Tag itemTag : player.getList("Items", Tag.TAG_COMPOUND)) {
                CompoundTag item = (CompoundTag) itemTag;
                ItemStack stack = ItemStack.of(item.getCompound("Stack"));
                if (!stack.isEmpty()) {
                    items.addLast(new ConfiscatedCurio(
                            item.getString("Slot"), item.getInt("Index"), stack));
                }
            }
            if (!items.isEmpty()) {
                confiscatedCurios.put(player.getUUID("Player"), items);
            }
        }
    }

    public enum BossDifficulty {
        SIMPLE,
        DIFFICULT,
        COMPLETE,
        EXTREME;

        private boolean confiscatesCurios() {
            return this == COMPLETE || this == EXTREME;
        }

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        private static BossDifficulty fromName(String name) {
            try {
                return valueOf(name.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return SIMPLE;
            }
        }
    }

    private record ConfiscatedCurio(String slotIdentifier, int slotIndex, ItemStack stack) {
    }

    private static final class PhaseOneCombatGoal extends Goal {
        private final FirstVicissitudeBossEntity boss;

        private PhaseOneCombatGoal(FirstVicissitudeBossEntity boss) {
            this.boss = boss;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return boss.isAlive() && boss.isPhaseOne();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = boss.currentPhaseOneTarget();
            if (boss.phaseOneTicks >= PHASE_ONE_WARMUP_TICKS
                && (boss.phaseOneTicks - PHASE_ONE_WARMUP_TICKS) % LIGHT_ORB_INTERVAL_TICKS == 0) {
                target = boss.selectNextPhaseOneTarget(true);
                if (target != null) {
                    boss.fireLightOrb(target);
                }
            }
            if (target != null) {
                boss.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
            boss.updatePhaseOneMovement(target);
        }
    }

    private static final class PhaseTwoCombatGoal extends Goal {
        private final FirstVicissitudeBossEntity boss;
        private int attackCooldown;
        private int targetRefreshCooldown;

        private PhaseTwoCombatGoal(FirstVicissitudeBossEntity boss) {
            this.boss = boss;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return boss.isAlive() && boss.phaseTwoStarted && !boss.isPhaseOne();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (attackCooldown > 0) {
                attackCooldown--;
            }
            LivingEntity target = boss.getTarget();
            if (targetRefreshCooldown-- <= 0 || target == null || !target.isAlive()
                || target.level() != boss.level()) {
                target = boss.selectBalancedPhaseTwoTarget();
                targetRefreshCooldown = 10;
            }
            if (target != null) {
                boss.getLookControl().setLookAt(target, 40.0F, 40.0F);
                double reach = 2.5D + boss.getBbWidth() * 0.5D + target.getBbWidth() * 0.5D;
                if (attackCooldown <= 0 && boss.distanceToSqr(target) <= reach * reach
                    && boss.hasLineOfSight(target)) {
                    if (boss.performPhaseTwoAttack(target)) {
                        attackCooldown = PHASE_TWO_ATTACK_INTERVAL;
                        targetRefreshCooldown = 0;
                    }
                }
            }
            boss.updatePhaseTwoMovement(target);
        }
    }
}
