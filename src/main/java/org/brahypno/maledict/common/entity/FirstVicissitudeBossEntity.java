package org.brahypno.maledict.common.entity;

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
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * The first Vicissitude encounter. Only its timed opening phase is implemented here.
 */
public final class FirstVicissitudeBossEntity extends VicissitudeBossEntity {
    public static final int PHASE_ONE_DURATION_TICKS = 20 * 300;
    public static final int PHASE_ONE_WARMUP_TICKS = 20;
    public static final int LIGHT_ORB_INTERVAL_TICKS = 20 * 2;
    private static final double PREFERRED_DISTANCE = 12.0D;
    private static final double HOVER_HEIGHT = 5.0D;
    private static final double MAX_FLIGHT_SPEED = 0.38D;
    private static final double FLIGHT_STEERING = 0.2D;
    private static final double ORBIT_STEP = 0.08D;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
    private final Set<UUID> phaseOneTargets = new LinkedHashSet<>();
    private final Map<UUID, Integer> targetPlayerDeaths = new HashMap<>();
    private final Map<UUID, Integer> announcedPlayerDeaths = new HashMap<>();
    private boolean phaseOneStarted;
    private int phaseOneTicks;
    private int targetCursor;
    private double idleHoverY = Double.NaN;

    public FirstVicissitudeBossEntity(EntityType<? extends FirstVicissitudeBossEntity> type, Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
        xpReward = 100;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBossAttributes()
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
        if (!isPhaseOne()){
            return;
        }
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity)){
            attacker = source.getDirectEntity();
        }
        if (attacker instanceof LivingEntity livingAttacker
            && livingAttacker != this
            && livingAttacker.isAlive()
            && livingAttacker.hasLineOfSight(this)){
            phaseOneTargets.add(livingAttacker.getUUID());
            if (livingAttacker instanceof ServerPlayer player){
                targetPlayerDeaths.put(player.getUUID(), getDeathCount(player));
            }
            phaseOneStarted = true;
            if (!isValidTarget(getTarget())){
                setTarget(livingAttacker);
            }
        }
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();
        if (!level().isClientSide && Double.isNaN(idleHoverY)){
            idleHoverY = Math.min(level().getMaxBuildHeight() - getBbHeight() - 1.0D,
                                  getY() + HOVER_HEIGHT);
        }
    }

    @Override
    public void tick() {
        setNoGravity(true);
        super.tick();
        if (!level().isClientSide){
            bossEvent.setName(getDisplayName());
            bossEvent.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0.0F, 1.0F));
            if (isAlive() && isPhaseOne() && phaseOneStarted){
                phaseOneTicks++;
                if (!isPhaseOne()){
                    setTarget(null);
                    getNavigation().stop();
                    setDeltaMovement(Vec3.ZERO);
                }
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
        tag.putInt("PhaseOneTicks", phaseOneTicks);
        tag.putInt("PhaseOneTargetCursor", targetCursor);
        if (!Double.isNaN(idleHoverY)){
            tag.putDouble("PhaseOneHoverY", idleHoverY);
        }
        ListTag targets = new ListTag();
        for (UUID target : phaseOneTargets) {
            targets.add(NbtUtils.createUUID(target));
        }
        tag.put("PhaseOneTargets", targets);
        tag.put("PhaseOneTargetDeaths", savePlayerDeathMap(targetPlayerDeaths));
        tag.put("PhaseOneAnnouncements", savePlayerDeathMap(announcedPlayerDeaths));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        boolean hasStartedState = tag.contains("PhaseOneStarted", Tag.TAG_BYTE);
        phaseOneTicks = hasStartedState
                        ? Mth.clamp(tag.getInt("PhaseOneTicks"), 0, PHASE_ONE_DURATION_TICKS)
                        : 0;
        phaseOneStarted = hasStartedState && tag.getBoolean("PhaseOneStarted");
        targetCursor = Math.max(0, tag.getInt("PhaseOneTargetCursor"));
        if (tag.contains("PhaseOneHoverY", Tag.TAG_DOUBLE)){
            idleHoverY = tag.getDouble("PhaseOneHoverY");
        }
        phaseOneTargets.clear();
        ListTag targets = tag.getList("PhaseOneTargets", Tag.TAG_INT_ARRAY);
        for (Tag target : targets) {
            phaseOneTargets.add(NbtUtils.loadUUID(target));
        }
        loadPlayerDeathMap(tag.getList("PhaseOneTargetDeaths", Tag.TAG_COMPOUND), targetPlayerDeaths);
        loadPlayerDeathMap(tag.getList("PhaseOneAnnouncements", Tag.TAG_COMPOUND), announcedPlayerDeaths);
    }

    private void updatePhaseOneMovement(@Nullable LivingEntity target) {
        if (target == null){
            double wantedY = Double.isNaN(idleHoverY) ? getY() + HOVER_HEIGHT : idleHoverY;
            steerToward(new Vec3(getX(), wantedY, getZ()));
            return;
        }

        Vec3 away = position().subtract(target.position()).multiply(1.0D, 0.0D, 1.0D);
        if (away.lengthSqr() < 0.01D){
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
        steerToward(new Vec3(wanted.x, wantedY, wanted.z));
    }

    private void steerToward(Vec3 destination) {
        Vec3 offset = destination.subtract(position());
        if (offset.lengthSqr() < 0.01D){
            setDeltaMovement(getDeltaMovement().scale(0.7D));
            return;
        }
        double desiredSpeed = Math.min(MAX_FLIGHT_SPEED, offset.length() * 0.12D);
        Vec3 desiredMotion = offset.normalize().scale(desiredSpeed);
        Vec3 motion = getDeltaMovement().scale(1.0D - FLIGHT_STEERING)
                                        .add(desiredMotion.scale(FLIGHT_STEERING));
        if (motion.lengthSqr() > MAX_FLIGHT_SPEED * MAX_FLIGHT_SPEED){
            motion = motion.normalize().scale(MAX_FLIGHT_SPEED);
        }
        setDeltaMovement(motion);
        hasImpulse = true;
    }

    @Nullable
    private LivingEntity currentTarget() {
        LivingEntity target = getTarget();
        return isValidTarget(target) ? target : selectNextTarget(false);
    }

    @Nullable
    private LivingEntity selectNextTarget(boolean advance) {
        if (!(level() instanceof ServerLevel serverLevel) || phaseOneTargets.isEmpty()){
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
            if (candidate instanceof LivingEntity living && isValidTarget(living)){
                targetCursor = advance ? index + 1 : index;
                setTarget(living);
                removeInvalidTargets(invalid);
                return living;
            }
            if (candidate != null){
                invalid.add(identity);
            }
        }
        removeInvalidTargets(invalid);
        setTarget(null);
        return null;
    }

    private boolean isValidTarget(@Nullable LivingEntity target) {
        if (target == null || target == this || !target.isAlive() || target.level() != level()){
            return false;
        }
        if (target instanceof ServerPlayer player){
            Integer registeredDeathCount = targetPlayerDeaths.get(player.getUUID());
            return !player.isSpectator()
                   && (registeredDeathCount == null || registeredDeathCount == getDeathCount(player));
        }
        return !(target instanceof Player player) || !player.isSpectator();
    }

    private void fireLightOrb(LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel)){
            return;
        }
        Vec3 launch = new Vec3(getX(), getEyeY() - 0.1D, getZ());
        announceFirstAttack(target);
        VicissitudeLightOrbEntity orb = new VicissitudeLightOrbEntity(serverLevel, target, launch);
        serverLevel.addFreshEntity(orb);
        swing(getUsedItemHand());
    }

    private void announceFirstAttack(LivingEntity target) {
        if (!(target instanceof ServerPlayer player)){
            return;
        }
        int deathCount = getDeathCount(player);
        if (announcedPlayerDeaths.getOrDefault(player.getUUID(), -1) != deathCount){
            player.displayClientMessage(
                    Component.translatable(VicissitudeLightOrbEntity.ATTACK_MESSAGE_KEY), true);
            announcedPlayerDeaths.put(player.getUUID(), deathCount);
        }
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
            if (value.hasUUID("Player")){
                values.put(value.getUUID("Player"), value.getInt("Deaths"));
            }
        }
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
            LivingEntity target = boss.currentTarget();
            if (boss.phaseOneTicks >= PHASE_ONE_WARMUP_TICKS
                && (boss.phaseOneTicks - PHASE_ONE_WARMUP_TICKS) % LIGHT_ORB_INTERVAL_TICKS == 0){
                target = boss.selectNextTarget(true);
                if (target != null){
                    boss.fireLightOrb(target);
                }
            }
            if (target != null){
                boss.getLookControl().setLookAt(target, 30.0F, 30.0F);
            }
            boss.updatePhaseOneMovement(target);
        }
    }
}
