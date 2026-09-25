package org.brahypno.maledict.common.entity;

import com.sammy.malum.core.helpers.ParticleHelper;
import com.sammy.malum.registry.common.DamageTypeRegistry;
import com.sammy.malum.registry.common.ParticleEffectTypeRegistry;
import com.sammy.malum.registry.common.SoundRegistry;
import com.sammy.malum.registry.common.SpiritTypeRegistry;
import com.sammy.malum.registry.common.item.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;
import org.brahypno.changelib.DamageHelper.DamageProbe;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.common.curio.VicissitudeCurioLedger;
import org.brahypno.maledict.common.curio.VicissitudeCurioReturns;
import org.brahypno.maledict.common.item.AgeOfEnlightenmentItem;
import org.brahypno.maledict.common.item.IncursusBladeItem;
import org.brahypno.maledict.config.MaledictConfig;
import org.brahypno.maledict.network.MaledictNetwork;
import org.brahypno.maledict.network.VicissitudeEffectPacket;
import org.brahypno.maledict.registry.MaledictItems;
import org.brahypno.maledict.rig.VicissitudeRig;
import org.brahypno.maledict.rig.VicissitudeRigData;
import org.jetbrains.annotations.Nullable;
import team.lodestar.lodestone.helpers.DamageTypeHelper;
import team.lodestar.lodestone.helpers.RandomHelper;
import team.lodestar.lodestone.helpers.SoundHelper;

import java.util.*;

/** The first Vicissitude encounter; stage, action and weapon state are server-authored and synced. */
public final class FirstVicissitudeBossEntity extends VicissitudeBossEntity {
    public static final int PHASE_ONE_WARMUP_TICKS = 20;
    public static final int TRANSITION_TICKS = 60;
    public static final int DEATH_TICKS = 80;
    public static final int BASE_ATTACK_INTERVAL_TICKS = 30;
    public static final int WING_BARRAGE_COOLDOWN = 70;
    public static final int CHEST_CAST_COOLDOWN = 90;
    public static final int HALO_CAST_COOLDOWN = 80;
    public static final int SLASH_COOLDOWN = 20;
    public static final int VERTICAL_SLASH_COOLDOWN = 30;
    public static final int HEAVY_ATTACK_COOLDOWN = 120;
    public static final int DASH_COOLDOWN = 160;
    public static final int THROW_COOLDOWN = 120;
    public static final int RANGED_FALLBACK_COOLDOWN = 60;
    /** Ticks with no living opponent before phase one returns to DORMANT and clears the roster. */
    public static final int EMPTY_ENCOUNTER_RESET_TICKS = 100;
    public static final int MAX_NON_HOMING_BOLTS = 48;
    public static final int MAX_HOMING_ORBS = 2;
    public static final double MELEE_COMMIT_RANGE = 5.75D;
    /** Reference value mirrored by the rig test; the blade decides real reach. */
    public static final double MELEE_REACH = 5.0D;
    private static final double MELEE_APPROACH_DISTANCE = 7.0D;
    /** The blade's swath, in blocks: the tolerance the melee test allows around the edge's centreline. */
    private static final double BLADE_HIT_RADIUS = 0.6D;
    public static final double THROW_MIN_RANGE = 6.0D;
    public static final double THROW_MAX_RANGE = 24.0D;
    public static final double DASH_MIN_RANGE = 8.0D;
    public static final double DASH_MAX_RANGE = 20.0D;
    public static final double DASH_DISTANCE = 10.0D;
    public static final double NO_FIRE_RANGE = 64.0D;
    public static final String PHASE_ONE_MESSAGE_KEY = VicissitudeLightOrbEntity.ATTACK_MESSAGE_KEY;
    public static final String PHASE_TWO_MESSAGE_KEY =
            "message.maledict.first_vicissitude.phase_two";
    public static final String UNSTICK_MESSAGE_KEY =
            "message.maledict.first_vicissitude.unstick";
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

    private static final EntityDataAccessor<Byte> DATA_STAGE =
            SynchedEntityData.defineId(FirstVicissitudeBossEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_ACTION =
            SynchedEntityData.defineId(FirstVicissitudeBossEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_ACTIVE_SIDE =
            SynchedEntityData.defineId(FirstVicissitudeBossEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> DATA_WEAPON_STATE =
            SynchedEntityData.defineId(FirstVicissitudeBossEntity.class, EntityDataSerializers.BYTE);
    /** Weapon tier of the current difficulty; the client draws from this, never from the hand. */
    private static final EntityDataAccessor<Byte> DATA_WEAPON_TIER =
            SynchedEntityData.defineId(FirstVicissitudeBossEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_ACTION_SEQUENCE =
            SynchedEntityData.defineId(FirstVicissitudeBossEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DATA_ACTION_START =
            SynchedEntityData.defineId(FirstVicissitudeBossEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DATA_HURT_TICKS =
            SynchedEntityData.defineId(FirstVicissitudeBossEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TRANSITION_TICKS =
            SynchedEntityData.defineId(FirstVicissitudeBossEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_PHASE_TWO_REACHED =
            SynchedEntityData.defineId(FirstVicissitudeBossEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> DATA_WING_FOLD =
            SynchedEntityData.defineId(FirstVicissitudeBossEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_CORE_GLOW =
            SynchedEntityData.defineId(FirstVicissitudeBossEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_GROUND_X =
            SynchedEntityData.defineId(FirstVicissitudeBossEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_GROUND_Y =
            SynchedEntityData.defineId(FirstVicissitudeBossEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_GROUND_Z =
            SynchedEntityData.defineId(FirstVicissitudeBossEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_GROUND_INNER =
            SynchedEntityData.defineId(FirstVicissitudeBossEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_GROUND_OUTER =
            SynchedEntityData.defineId(FirstVicissitudeBossEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_GROUND_GAP_CENTER =
            SynchedEntityData.defineId(FirstVicissitudeBossEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_GROUND_GAP_WIDTH =
            SynchedEntityData.defineId(FirstVicissitudeBossEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Long> DATA_GROUND_START =
            SynchedEntityData.defineId(FirstVicissitudeBossEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DATA_GROUND_DURATION =
            SynchedEntityData.defineId(FirstVicissitudeBossEntity.class, EntityDataSerializers.INT);

    private static final double PREFERRED_DISTANCE = 7.0D;
    private static final double HOVER_HEIGHT = 2.5D;
    private static final double PHASE_TWO_DISTANCE = 2.25D;
    private static final double PHASE_TWO_HOVER_HEIGHT = 1.35D;
    private static final double MAX_FLIGHT_SPEED = 0.38D;
    private static final double PHASE_TWO_MAX_FLIGHT_SPEED = 0.52D;
    private static final double FLIGHT_STEERING = 0.2D;
    private static final double ORBIT_STEP = 0.03D;
    private static final int BLOCK_BREAK_INTERVAL = 10;
    private static final int STUCK_WINDOW_TICKS = 40;
    private static final double STUCK_MIN_TRAVEL = 0.25D;
    private static final int UNSTICK_SUCCESS_COOLDOWN = 100;
    private static final int UNSTICK_MAX_CANDIDATES = 64;
    private static final int[] UNSTICK_RADII = {4, 8, 12, 16};
    /** Fixed id so the difficulty health modifier is rewritten instead of stacking. */
    private static final UUID MAX_HEALTH_MODIFIER_ID =
            UUID.fromString("2c4b1d5a-9f34-4b1c-9a37-6d1f4c0a51e2");
    /** Base id for baked weapon modifiers; each slot gets the next least significant value. */
    private static final UUID WEAPON_ATTRIBUTE_ID =
            UUID.fromString("8a17c3d2-5e64-4d0b-9c31-7b2f5a0e6d44");
    private static final int WEAPON_GUARD_INTERVAL = 20;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
    private final Set<UUID> phaseOneTargets = new LinkedHashSet<>();
    /** Save key stays {@code PhaseTwoPlayers}; the set holds any living entity, not only players. */
    private final Set<UUID> phaseTwoParticipants = new LinkedHashSet<>();
    private final Map<UUID, Integer> targetPlayerDeaths = new HashMap<>();
    private final Map<UUID, Integer> announcedPlayerDeaths = new HashMap<>();
    private final Map<UUID, Integer> phaseTwoPlayerDeaths = new HashMap<>();
    private final Map<UUID, Deque<VicissitudeCurioLedger.Entry>> confiscated = new LinkedHashMap<>();
    private final VicissitudeRig.Pose serverPose = VicissitudeRig.newPose();
    private final VicissitudeRig.Pose renderPose = VicissitudeRig.newPose();
    private final Set<UUID> roundDamagedTargets = new HashSet<>();

    private VicissitudeBossStage stage = VicissitudeBossStage.DORMANT;
    private VicissitudeRig.Action action = VicissitudeRig.Action.NONE;
    private boolean actionLeft;
    private boolean actionReleased;
    private boolean actionHitLanded;
    @Nullable
    private LivingEntity committedTarget;
    private Vec3 committedAim = Vec3.ZERO;
    private float committedYaw;
    private float committedPitch;
    private boolean scytheRecoveryPending;
    private int actionSequence;
    private int hurtTicks;
    private int transitionTicks;
    private int phaseOneTicks;
    private int phaseOneDurationTicks;
    private boolean phaseOneDurationLocked;
    /** Set by the spawn egg to run phase one out at once, see {@link #setSpawnPhase}. */
    private boolean forcedPhaseTwo;
    private int baseSlotIndex;
    private int targetCursor;
    private int curioReturnCursor;
    private int barrageCooldown;
    private int chestCooldown;
    private int haloCooldown;
    private int slashCooldown;
    private int verticalSlashCooldown;
    private int heavyCooldown;
    private int dashCooldown;
    private int throwCooldown;
    private int rangedCooldown;
    /** Recently hit damage messages, see {@link DamageAdaptation}. */
    private final DamageAdaptation damageAdaptation = new DamageAdaptation();
    private int emptyEncounterTicks;
    private int meleeAlternator;
    private int currentBaseSlot;
    private int fanCount;
    private boolean phaseTwoReached;
    private int specialRotator;
    private double idleHoverY = Double.NaN;
    private float wingFold;
    private boolean wingsBlockedLastTick;
    private double stuckReferenceX;
    private double stuckReferenceY;
    private double stuckReferenceZ;
    private int stuckTicks;
    private int unstickCooldown;
    private int caveInTicks;
    private boolean dashDirectionLocked;
    private Vec3 dashDirection = Vec3.ZERO;
    private int dashTicks;
    private final Set<UUID> dashHitTargets = new HashSet<>();
    private UUID scytheToken;
    private ItemStack stashedWeapon = ItemStack.EMPTY;
    private int pendingWeaponTier = -1;
    private BossDifficulty bossDifficulty = BossDifficulty.SIMPLE;
    /** Set once the vitality ledger has captured the pool; a mode change may not alter it after. */
    private boolean difficultyLocked;
    private ItemStack displayWeapon = ItemStack.EMPTY;
    private int displayWeaponTier = -1;
    private final List<AppliedAttribute> appliedWeaponAttributes = new ArrayList<>();

    public FirstVicissitudeBossEntity(
            EntityType<? extends FirstVicissitudeBossEntity> type,
            Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
        xpReward = 1000;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBossAttributes()
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FLYING_SPEED, 0.45D)
                .add(Attributes.FOLLOW_RANGE, 96.0D);
    }

    private void applyDifficultyAttributes() {
        AttributeInstance attribute = getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null){
            return;
        }
        attribute.removeModifier(MAX_HEALTH_MODIFIER_ID);
        double delta = bossDifficulty.maxHealth() - VicissitudeBossEntity.BASE_MAX_HEALTH;
        if (delta != 0.0D){
            attribute.addPermanentModifier(new AttributeModifier(MAX_HEALTH_MODIFIER_ID,
                                                                 "Vicissitude difficulty health", delta, AttributeModifier.Operation.ADDITION));
        }
    }

    /**
     * Bakes the weapon's attribute modifiers into the entity under our own ids; the item's own
     * copies are stripped so nothing counts twice.
     */
    private void applyWeaponAttributes() {
        for (AppliedAttribute applied : appliedWeaponAttributes) {
            AttributeInstance instance = getAttribute(applied.attribute());
            if (instance != null){
                instance.removeModifier(applied.id());
            }
        }
        appliedWeaponAttributes.clear();
        int slot = 0;
        for (var entry : createWeaponForDifficulty(bossDifficulty)
                .getAttributeModifiers(EquipmentSlot.MAINHAND).entries()) {
            UUID id = weaponModifierId(slot++);
            AttributeInstance instance = getAttribute(entry.getKey());
            if (instance == null){
                continue;
            }
            instance.removeModifier(id);
            AttributeModifier source = entry.getValue();
            instance.addPermanentModifier(new AttributeModifier(id,
                                                                "Vicissitude weapon " + entry.getKey().getDescriptionId(), source.getAmount(),
                                                                source.getOperation()));
            appliedWeaponAttributes.add(new AppliedAttribute(entry.getKey(), id));
        }
    }

    private static UUID weaponModifierId(int slot) {
        return new UUID(WEAPON_ATTRIBUTE_ID.getMostSignificantBits(),
                        WEAPON_ATTRIBUTE_ID.getLeastSignificantBits() + slot);
    }

    private void suppressHeldItemAttributes() {
        ItemStack held = getMainHandItem();
        if (held.isEmpty()){
            return;
        }
        getAttributes().removeAttributeModifiers(
                held.getAttributeModifiers(EquipmentSlot.MAINHAND));
    }

    private record AppliedAttribute(Attribute attribute, UUID id) {
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_STAGE, (byte) VicissitudeBossStage.DORMANT.ordinal());
        entityData.define(DATA_ACTION, (byte) VicissitudeRig.Action.NONE.ordinal());
        entityData.define(DATA_ACTIVE_SIDE, (byte) 0);
        entityData.define(DATA_WEAPON_STATE, (byte) 0);
        // defineSynchedData runs before field initializers, so the tier starts as SIMPLE (0).
        entityData.define(DATA_WEAPON_TIER, (byte) 0);
        entityData.define(DATA_ACTION_SEQUENCE, 0);
        entityData.define(DATA_ACTION_START, 0L);
        entityData.define(DATA_HURT_TICKS, 0);
        entityData.define(DATA_TRANSITION_TICKS, 0);
        entityData.define(DATA_PHASE_TWO_REACHED, false);
        entityData.define(DATA_WING_FOLD, 0.0F);
        entityData.define(DATA_CORE_GLOW, 0.55F);
        entityData.define(DATA_GROUND_X, 0.0F);
        entityData.define(DATA_GROUND_Y, 0.0F);
        entityData.define(DATA_GROUND_Z, 0.0F);
        entityData.define(DATA_GROUND_INNER, 0.0F);
        entityData.define(DATA_GROUND_OUTER, 0.0F);
        entityData.define(DATA_GROUND_GAP_CENTER, 0.0F);
        entityData.define(DATA_GROUND_GAP_WIDTH, 0.0F);
        entityData.define(DATA_GROUND_START, 0L);
        entityData.define(DATA_GROUND_DURATION, 0);
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
        goalSelector.addGoal(0, new CombatGoal(this));
    }

    public VicissitudeBossStage getStage() {
        return VicissitudeBossStage.byId(entityData.get(DATA_STAGE));
    }

    private void setStage(VicissitudeBossStage value) {
        stage = value;
        entityData.set(DATA_STAGE, (byte) value.ordinal());
    }

    public enum BossSpawnPhase {
        /** Idol until the first hit starts phase one. */
        DORMANT,
        PHASE_ONE,
        /** Runs phase one out at once, so the full transition still plays. */
        PHASE_TWO;
    }

    /**
     * Must run before the entity joins the world: the health pool is captured in
     * {@link #onAddedToWorld()} and locks the difficulty.
     */
    public void setSpawnPhase(BossDifficulty difficulty, BossSpawnPhase phase) {
        setBossDifficulty(difficulty);
        if (phase == null || phase == BossSpawnPhase.DORMANT) {
            return;
        }
        setStage(VicissitudeBossStage.PHASE_ONE);
        phaseOneTicks = 0;
        if (phase == BossSpawnPhase.PHASE_TWO) {
            forcedPhaseTwo = true;
            return;
        }
        lockPhaseOneDuration();
    }

    public VicissitudeRig.Action getRenderAction() {
        return VicissitudeRig.Action.byId(entityData.get(DATA_ACTION));
    }

    public boolean isActionLeft() {
        return entityData.get(DATA_ACTIVE_SIDE) == 1;
    }

    public boolean isPhaseTwoVisual() {
        return phaseTwoReached || entityData.get(DATA_PHASE_TWO_REACHED);
    }

    public float getPhaseTwoBlend() {
        if (isPhaseTwoVisual()){
            return 1.0F;
        }
        if (getStage() == VicissitudeBossStage.TRANSITION){
            return Mth.clamp(entityData.get(DATA_TRANSITION_TICKS) / (float) TRANSITION_TICKS, 0.0F, 1.0F);
        }
        return 0.0F;
    }

    public float getHurtTicks() {
        return entityData.get(DATA_HURT_TICKS);
    }

    public float getWingFold() {
        return entityData.get(DATA_WING_FOLD);
    }

    public float getDeathTicks() {
        if (getHealth() > 0.0F || deathTime <= 0){
            return -1.0F;
        }
        return deathTime;
    }

    public float getCoreGlow() {
        return entityData.get(DATA_CORE_GLOW);
    }

    /** Action clock in ticks, or -1 when no action is running. */
    public float getActionTicks(float partialTick) {
        VicissitudeRig.Action current = getRenderAction();
        if (current == VicissitudeRig.Action.NONE){
            return -1.0F;
        }
        long start = entityData.get(DATA_ACTION_START);
        return (float) (level().getGameTime() - start) + partialTick;
    }

    public int getActionSequence() {
        return entityData.get(DATA_ACTION_SEQUENCE);
    }

    public boolean isPhaseOneStage() {
        VicissitudeBossStage current = getStage();
        return current == VicissitudeBossStage.DORMANT || current == VicissitudeBossStage.PHASE_ONE;
    }

    public boolean isChargingRanged() {
        VicissitudeRig.Action current = getRenderAction();
        return current == VicissitudeRig.Action.WING_RANGED
               || current == VicissitudeRig.Action.WING_BARRAGE
               || current == VicissitudeRig.Action.RANGED_FALLBACK
               || current == VicissitudeRig.Action.SCYTHE_THROW;
    }

    public boolean shouldRenderHeldWeapon() {
        return entityData.get(DATA_WEAPON_STATE) == 1;
    }

    /** The stack the renderer draws, rebuilt from the synced tier rather than read from the hand. */
    public ItemStack getDisplayWeapon() {
        int tier = entityData.get(DATA_WEAPON_TIER);
        if (tier != displayWeaponTier || displayWeapon.isEmpty()){
            displayWeaponTier = tier;
            displayWeapon = createWeaponForDifficulty(BossDifficulty.byId(tier));
        }
        return displayWeapon;
    }

    private void setWeaponState(int state) {
        entityData.set(DATA_WEAPON_STATE, (byte) state);
    }

    private void setWeaponTier(BossDifficulty difficulty) {
        entityData.set(DATA_WEAPON_TIER, (byte) difficulty.ordinal());
        displayWeaponTier = -1;
    }

    private int actionTicks() {
        return (int) (level().getGameTime() - entityData.get(DATA_ACTION_START));
    }

    /** Starts a new action; the release frame is executed once, from {@link #tickAction()}. */
    private void startAction(VicissitudeRig.Action next, boolean left) {
        action = next;
        actionLeft = left;
        actionReleased = false;
        actionHitLanded = false;
        committedTarget = getTarget();
        committedAim = committedTarget == null ? position().add(getLookAngle().scale(8))
                                               : aimPoint(committedTarget);
        committedYaw = getYRot();
        committedPitch = getXRot();
        dashDirectionLocked = false;
        dashTicks = 0;
        barrageWaveTick = 0L;
        lastBarrageWave = 0;
        actionSequence++;
        roundDamagedTargets.clear();
        entityData.set(DATA_ACTION, (byte) next.ordinal());
        entityData.set(DATA_ACTIVE_SIDE, (byte) (left ? 1 : 0));
        entityData.set(DATA_ACTION_SEQUENCE, actionSequence);
        entityData.set(DATA_ACTION_START, level().getGameTime());
    }

    private void endAction() {
        action = VicissitudeRig.Action.NONE;
        actionReleased = false;
        committedTarget = null;
        barrageWaveTick = 0L;
        entityData.set(DATA_ACTION, (byte) VicissitudeRig.Action.NONE.ordinal());
        entityData.set(DATA_ACTION_START, level().getGameTime());
        actionSequence++;
        entityData.set(DATA_ACTION_SEQUENCE, actionSequence);
        clearGroundMarker();
    }

    @Override
    public void onAddedToWorld() {
        if (!level().isClientSide){
            // The base class captures the health pool here, so the modifier must be in place first.
            applyDifficultyAttributes();
            difficultyLocked = true;
            if (entityData.get(DATA_WEAPON_STATE) == 1){
                // A loaded save never runs the equip step again, so the attributes are rebuilt here.
                applyWeaponAttributes();
            }
        }
        super.onAddedToWorld();
        if (!level().isClientSide && Double.isNaN(idleHoverY)){
            idleHoverY = Math.min(level().getMaxBuildHeight() - getBbHeight() - 1.0D,
                                  getY() + HOVER_HEIGHT);
            if (phaseOneDurationTicks <= 0){
                phaseOneDurationTicks = bossDifficulty.phaseOneDurationTicks();
            }
            if (forcedPhaseTwo && !phaseOneDurationLocked){
                phaseOneDurationTicks = 0;
                phaseOneDurationLocked = true;
            }
            stuckReferenceX = getX();
            stuckReferenceY = getY();
            stuckReferenceZ = getZ();
        }
    }

    @Override
    public void tick() {
        setNoGravity(true);
        super.tick();
        if (level().isClientSide){
            return;
        }
        syncDerivedState();
        bossEvent.setName(getDisplayName());
        bossEvent.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0.0F, 1.0F));
        if (getHealth() <= 0.0F){
            return;
        }
        tickFacing();
        if (tickCount % 10 == 0){
            forgetDeadParticipants();
        }
        if (tickCount % WEAPON_GUARD_INTERVAL == 0){
            tickWeaponGuard(true);
        }
        tickWeaponGuard(false);
        suppressHeldItemAttributes();
        tickCooldowns();
        tickStage();
        tickAction();
        refreshPose();
        tickWingCollision();
        tickUnstick();
    }

    private record FacingAim(Vec3 point, double eyeY) {
    }

    @Nullable
    private FacingAim blendedFacingAim() {
        if (!(level() instanceof ServerLevel serverLevel)){
            return null;
        }
        Set<UUID> roster = isPhaseTwo() ? phaseTwoParticipants : phaseOneTargets;
        if (roster.isEmpty()){
            return null;
        }
        double range = engagementRange();
        Vec3 direction = Vec3.ZERO;
        double eyeSum = 0.0D;
        double weightSum = 0.0D;
        for (UUID identity : roster) {
            LivingEntity member = resolveParticipant(serverLevel, identity);
            if (member == null || !member.isAlive() || member.level() != level()
                || isIgnoredPlayer(member)){
                continue;
            }
            Vec3 offset = member.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D);
            double distance = offset.length();
            if (distance < 0.05D || distance > range){
                continue;
            }
            double weight = 1.0D / (1.0D + distance * 0.15D);
            direction = direction.add(offset.normalize().scale(weight));
            eyeSum += member.getEyeY() * weight;
            weightSum += weight;
        }
        if (weightSum <= 0.0D || direction.lengthSqr() < 1.0E-4D){
            return null;
        }
        return new FacingAim(position().add(direction.normalize().scale(4.0D)), eyeSum / weightSum);
    }

    @Nullable
    private LivingEntity resolveParticipant(ServerLevel serverLevel, UUID identity) {
        ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(identity);
        if (player != null){
            return player;
        }
        Entity entity = serverLevel.getEntity(identity);
        return entity instanceof LivingEntity living ? living : null;
    }

    /** Configurable aggro radius; the boss never starts or keeps a fight beyond it. */
    private static double engagementRange() {
        try {
            return MaledictConfig.VICISSITUDE_ENGAGEMENT_RANGE.get();
        }
        catch (IllegalStateException notLoaded) {
            return 12.0D;
        }
    }

    private double engagementRangeSqr() {
        double range = engagementRange();
        return range * range;
    }

    private static double damageRange() {
        try {
            return MaledictConfig.VICISSITUDE_DAMAGE_RANGE.get();
        }
        catch (IllegalStateException notLoaded) {
            return 48.0D;
        }
    }

    /** Drops anyone not present (unresolvable, wrong dimension, dead) from the rosters. */
    private void forgetDeadParticipants() {
        if (!(level() instanceof ServerLevel serverLevel)){
            return;
        }
        boolean forgive = level().getGameRules().getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS);
        var playerList = serverLevel.getServer().getPlayerList();
        for (UUID identity : new ArrayList<>(phaseOneTargets)) {
            if (updateParticipant(playerList.getPlayer(identity), identity, forgive,
                                  targetPlayerDeaths, serverLevel)){
                phaseOneTargets.remove(identity);
                announcedPlayerDeaths.remove(identity);
            }
        }
        for (UUID identity : new ArrayList<>(phaseTwoParticipants)) {
            if (updateParticipant(playerList.getPlayer(identity), identity, forgive,
                                  phaseTwoPlayerDeaths, serverLevel)){
                phaseTwoParticipants.remove(identity);
            }
        }
    }

    private boolean updateParticipant(
            @Nullable ServerPlayer player, UUID identity, boolean forgive,
            Map<UUID, Integer> deathRecords, ServerLevel serverLevel) {
        if (player == null){
            return dropAbsentParticipant(identity, serverLevel, deathRecords);
        }
        if (player.level() != level()){
            deathRecords.remove(identity);
            clearTargetIf(identity);
            return true;
        }
        Integer recorded = deathRecords.get(identity);
        if (recorded != null && recorded != getDeathCount(player)){
            if (forgive){
                deathRecords.remove(identity);
                clearTargetIf(identity);
                return true;
            }
            // Without forgiveness the boss keeps hunting the same player after respawn.
            deathRecords.put(identity, getDeathCount(player));
            return false;
        }
        if (!player.isAlive()){
            clearTargetIf(identity);
        }
        return false;
    }

    private boolean dropAbsentParticipant(
            UUID identity, ServerLevel serverLevel,
            Map<UUID, Integer> deathRecords) {
        Entity entity = serverLevel.getEntity(identity);
        if (entity instanceof LivingEntity living && living.isAlive() && living.level() == level()){
            return false;
        }
        deathRecords.remove(identity);
        clearTargetIf(identity);
        return true;
    }

    private void clearTargetIf(UUID identity) {
        LivingEntity target = getTarget();
        if (target != null && identity.equals(target.getUUID())){
            setTarget(null);
        }
    }

    private static final float YAW_RATE_TRACKING = 20.0F;
    private static final float YAW_RATE_WINDUP = 30.0F;
    private static final float YAW_CATCH_UP_ARC = 90.0F;
    private static final float YAW_CATCH_UP_BOOST = 1.8F;

    /** The boss owns its facing; runs after {@code super.tick()} so look control cannot fight it. */
    private void tickFacing() {
        if (stage == VicissitudeBossStage.DORMANT || getHealth() <= 0.0F){
            return;
        }
        if (action != VicissitudeRig.Action.NONE && actionTicks() >= action.aimLockTick()){
            setYRot(committedYaw);
            setYHeadRot(committedYaw);
            yBodyRot = committedYaw;
            setXRot(committedPitch);
            return;
        }
        FacingAim facing = action != VicissitudeRig.Action.NONE && committedTarget != null
                           ? new FacingAim(aimPoint(committedTarget), committedTarget.getEyeY())
                           : blendedFacingAim();
        if (facing == null){
            return;
        }
        Vec3 aim = facing.point();
        if (action != VicissitudeRig.Action.NONE){
            committedAim = aim;
        }
        double aimEyeY = facing.eyeY();
        float rate = YAW_RATE_TRACKING;
        if (action != VicissitudeRig.Action.NONE){
            rate = YAW_RATE_WINDUP;
        }
        float wanted = yawTo(aim);
        float delta = Mth.wrapDegrees(wanted - getYRot());
        float allowed = Math.abs(delta) > YAW_CATCH_UP_ARC ? rate * YAW_CATCH_UP_BOOST : rate;
        float yaw = getYRot() + Mth.clamp(delta, -allowed, allowed);
        setYRot(yaw);
        setYHeadRot(yaw);
        yBodyRot = yaw;
        double dy = aimEyeY - getEyeY();
        double horizontal = Math.sqrt(distanceToSqr(aim.x, getEyeY(), aim.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.max(0.25D, horizontal)));
        setXRot(getXRot() + Mth.clamp(Mth.clamp(pitch, -35.0F, 35.0F) - getXRot(), -allowed, allowed));
        committedYaw = yaw;
        committedPitch = getXRot();
    }


    private void syncDerivedState() {
        if (hurtTicks > 0){
            hurtTicks--;
        }
        entityData.set(DATA_HURT_TICKS, hurtTicks);
        entityData.set(DATA_WING_FOLD, wingFold);
        entityData.set(DATA_CORE_GLOW, computeCoreGlow());
    }

    private float computeCoreGlow() {
        if (getHealth() <= 0.0F){
            float fade = Mth.clamp((deathTime - 60.0F) / 19.0F, 0.0F, 1.0F);
            return Math.max(0.0F, 1.0F - fade);
        }
        if (stage == VicissitudeBossStage.TRANSITION){
            return 0.7F + 0.3F * Mth.sin(transitionTicks * 0.6F);
        }
        return isPhaseTwo() ? 0.85F : 0.55F;
    }

    private void tickCooldowns() {
        barrageCooldown = Math.max(0, barrageCooldown - 1);
        chestCooldown = Math.max(0, chestCooldown - 1);
        haloCooldown = Math.max(0, haloCooldown - 1);
        slashCooldown = Math.max(0, slashCooldown - 1);
        verticalSlashCooldown = Math.max(0, verticalSlashCooldown - 1);
        heavyCooldown = Math.max(0, heavyCooldown - 1);
        dashCooldown = Math.max(0, dashCooldown - 1);
        throwCooldown = Math.max(0, throwCooldown - 1);
        rangedCooldown = Math.max(0, rangedCooldown - 1);
        unstickCooldown = Math.max(0, unstickCooldown - 1);
    }

    private void tickStage() {
        switch (stage) {
            case DORMANT, PHASE_ONE -> {
                if (forcedPhaseTwo){
                    // Run phase one out instead of jumping to PHASE_TWO, so the normal transition does the handover.
                    phaseOneDurationTicks = 0;
                    phaseOneDurationLocked = true;
                    forcedPhaseTwo = false;
                }
                if (!isPhaseOneStarted()){
                    break;
                }
                if (tickEmptyEncounter()){
                    break;
                }
                phaseOneTicks++;
                if (phaseOneTicks >= phaseOneDurationTicks){
                    beginTransition();
                }
            }
            case TRANSITION -> {
                transitionTicks++;
                entityData.set(DATA_TRANSITION_TICKS, transitionTicks);
                tickTransitionEffects();
                if (transitionTicks >= TRANSITION_TICKS){
                    completeTransition();
                }
            }
            case PHASE_TWO -> {
                destroyBlockingEntities();
                if ((horizontalCollision || verticalCollision)
                    && tickCount % BLOCK_BREAK_INTERVAL == 0){
                    destroyBlockingBlocks();
                }
            }
            case DYING -> {
            }
        }
    }

    /** Counts down while no living opponent remains; at zero the encounter returns to DORMANT. */
    private boolean tickEmptyEncounter() {
        if (hasPossibleOpponent()){
            emptyEncounterTicks = 0;
            return false;
        }
        if (++emptyEncounterTicks < EMPTY_ENCOUNTER_RESET_TICKS){
            return false;
        }
        emptyEncounterTicks = 0;
        setStage(VicissitudeBossStage.DORMANT);
        phaseOneDurationLocked = false;
        phaseOneTicks = 0;
        phaseOneTargets.clear();
        targetPlayerDeaths.clear();
        phaseTwoParticipants.clear();
        phaseTwoPlayerDeaths.clear();
        damageAdaptation.clear();
        setTarget(null);
        clearGroundMarker();
        return true;
    }

    private boolean hasPossibleOpponent() {
        if (phaseOneTargets.isEmpty()){
            return false;
        }
        if (!(level() instanceof ServerLevel serverLevel)){
            return true;
        }
        for (UUID identity : phaseOneTargets) {
            Entity entity = serverLevel.getEntity(identity);
            if (entity instanceof LivingEntity living && living.isAlive()
                && living.level() == level() && !isIgnoredPlayer(living)){
                return true;
            }
        }
        return false;
    }

    private void tickTransitionEffects() {
        // Transition timeline: 0-14 hover, 15-29 wings fold, 30-44 feathers press, 45 equip, 45-59 blend.
        if (transitionTicks == 1){
            setDeltaMovement(Vec3.ZERO);
            getNavigation().stop();
            sendEvent(VicissitudeEffectPacket.EVENT_TRANSITION_FLASH, position());
        }
        if (transitionTicks == 30){
            SoundHelper.playSound(this, SoundRegistry.SOUL_SHATTER.get(), 1.2F,
                                  RandomHelper.randomBetween(level().getRandom(), 0.7F, 0.9F));
        }
        if (transitionTicks == 45){
            equipPhaseTwoWeapon();
        }
    }

    private void beginTransition() {
        setStage(VicissitudeBossStage.TRANSITION);
        transitionTicks = 0;
        entityData.set(DATA_TRANSITION_TICKS, 0);
        action = VicissitudeRig.Action.NONE;
        endAction();
        cancelFlyingScythe();
        setTarget(null);
        clearGroundMarker();
        clearPhaseOneProjectiles();
    }

    private void clearPhaseOneProjectiles() {
        if (!(level() instanceof ServerLevel serverLevel)){
            return;
        }
        AABB area = getBoundingBox().inflate(128.0D);
        for (VicissitudeSpiritBoltEntity bolt : serverLevel.getEntitiesOfClass(
                VicissitudeSpiritBoltEntity.class, area, candidate -> candidate.getOwner() == this)) {
            bolt.discard();
        }
        for (VicissitudeLightOrbEntity orb : serverLevel.getEntitiesOfClass(
                VicissitudeLightOrbEntity.class, area, candidate -> candidate.isOwnedBy(this))) {
            orb.discard();
        }
    }

    private void completeTransition() {
        setStage(VicissitudeBossStage.PHASE_TWO);
        phaseTwoReached = true;
        entityData.set(DATA_PHASE_TWO_REACHED, true);
        if (!(level() instanceof ServerLevel serverLevel)){
            return;
        }
        Set<ServerPlayer> recipients = new LinkedHashSet<>(bossEvent.getPlayers());
        phaseTwoParticipants.clear();
        for (UUID identity : phaseOneTargets) {
            LivingEntity member = resolveParticipant(serverLevel, identity);
            if (member == null || member.level() != level() || !member.isAlive()){
                continue;
            }
            if (member instanceof ServerPlayer player){
                recipients.add(player);
                if (isIgnoredPlayer(player)){
                    continue;
                }
                phaseTwoParticipants.add(identity);
                phaseTwoPlayerDeaths.put(identity, getDeathCount(player));
                if (bossDifficulty.confiscatesCurios()){
                    confiscateEquippedCurios(player);
                }
                continue;
            }
            phaseTwoParticipants.add(identity);
        }
        for (ServerPlayer player : recipients) {
            player.displayClientMessage(announcement(PHASE_TWO_MESSAGE_KEY), true);
        }
    }

    private void tickAction() {
        if (action == VicissitudeRig.Action.NONE){
            return;
        }
        int ticks = actionTicks();
        if (action != VicissitudeRig.Action.SCYTHE_RECOVER && (committedTarget == null
                                                               || !committedTarget.isAlive() || committedTarget.level() != level()
                                                               || isIgnoredPlayer(committedTarget))){
            endAction();
            return;
        }
        if (ticks >= action.duration()){
            if (action.isMelee() && !actionHitLanded){
                onMeleeMissed();
            }
            finishAction();
            return;
        }
        if (!actionReleased && ticks >= action.releaseTick()){
            actionReleased = true;
            releaseAction();
        }
        tickBladeHits();
        if (action == VicissitudeRig.Action.DASH){
            tickDash(ticks);
        }
    }

    private void finishAction() {
        VicissitudeRig.Action finished = action;
        endAction();
        if (finished == VicissitudeRig.Action.SCYTHE_RECOVER){
            pendingWeaponTier = -1;
        }
    }

    private boolean isPhaseOne() {
        return stage == VicissitudeBossStage.DORMANT || stage == VicissitudeBossStage.PHASE_ONE;
    }

    private boolean isPhaseOneStarted() {
        return stage == VicissitudeBossStage.PHASE_ONE;
    }

    private boolean isPhaseTwo() {
        return stage == VicissitudeBossStage.PHASE_TWO;
    }

    private void tickPhaseOneCombat() {
        if (action != VicissitudeRig.Action.NONE){
            return;
        }
        if (phaseOneTicks < PHASE_ONE_WARMUP_TICKS){
            return;
        }
        if ((phaseOneTicks - PHASE_ONE_WARMUP_TICKS) % BASE_ATTACK_INTERVAL_TICKS != 0){
            return;
        }
        LivingEntity target = currentPhaseOneTarget();
        if (target == null){
            return;
        }
        if (distanceTo(target) > NO_FIRE_RANGE){
            // Phase one bolts live for about 36 blocks, so firing from across the arena spawns duds.
            return;
        }
        currentBaseSlot = baseSlotIndex++;
        if (currentBaseSlot % 2 == 0 && tryRangedSpecialSkill(target)){
            return;
        }
        fanCount++;
        startAction(VicissitudeRig.Action.WING_RANGED, (currentBaseSlot / 2) % 2 == 0);
    }

    private boolean tryRangedSpecialSkill(LivingEntity target) {
        for (int attempt = 0; attempt < 3; attempt++) {
            int choice = specialRotator % 3;
            specialRotator++;
            switch (choice) {
                case 0 -> {
                    if (barrageCooldown <= 0){
                        barrageCooldown = WING_BARRAGE_COOLDOWN;
                        startAction(VicissitudeRig.Action.WING_BARRAGE, true);
                        return true;
                    }
                }
                case 1 -> {
                    if (chestCooldown <= 0 && findGroundPoint(target) != null){
                        chestCooldown = CHEST_CAST_COOLDOWN;
                        startAction(VicissitudeRig.Action.CAST_FROM_CHEST, false);
                        return true;
                    }
                }
                default -> {
                    if (haloCooldown <= 0 && findGroundPoint(target) != null){
                        haloCooldown = HALO_CAST_COOLDOWN;
                        startAction(VicissitudeRig.Action.CAST_FROM_HALO, false);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void tickPhaseTwoCombat() {
        if (action != VicissitudeRig.Action.NONE){
            return;
        }
        if (scytheRecoveryPending){
            scytheRecoveryPending = false;
            startAction(VicissitudeRig.Action.SCYTHE_RECOVER, false);
            return;
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || target.level() != level()
            || isIgnoredPlayer(target)){
            target = selectBalancedPhaseTwoTarget();
        }
        if (target == null){
            return;
        }
        double distance = distanceTo(target);
        if (distance > NO_FIRE_RANGE){
            return;
        }
        if (distance > THROW_MAX_RANGE){
            if (tryRangedSpecialSkill(target)){
                return;
            }
            if (rangedCooldown <= 0){
                rangedCooldown = RANGED_FALLBACK_COOLDOWN;
                startAction(VicissitudeRig.Action.RANGED_FALLBACK, baseSlotIndex++ % 2 == 0);
            }
            return;
        }
        if (distance >= THROW_MIN_RANGE){
            if (throwCooldown <= 0 && hasWeaponInHand() && scytheToken == null
                && hasLineOfSight(target)){
                throwCooldown = THROW_COOLDOWN;
                startAction(VicissitudeRig.Action.SCYTHE_THROW, false);
                return;
            }
            if (dashCooldown <= 0 && hasWeaponInHand() && scytheToken == null
                && distance >= DASH_MIN_RANGE && distance <= DASH_MAX_RANGE
                && hasLineOfSight(target)){
                dashCooldown = DASH_COOLDOWN;
                startAction(VicissitudeRig.Action.DASH, false);
                return;
            }
            if (tryRangedSpecialSkill(target)){
                return;
            }
            if (rangedCooldown <= 0){
                rangedCooldown = RANGED_FALLBACK_COOLDOWN;
                startAction(VicissitudeRig.Action.RANGED_FALLBACK, baseSlotIndex++ % 2 == 0);
            }
            return;
        }
        if (distance <= MELEE_COMMIT_RANGE){
            commandMelee(target);
        }
    }

    private void commandMelee(LivingEntity target) {
        if (scytheToken != null){
            return;
        }
        if (heavyCooldown <= 0){
            heavyCooldown = HEAVY_ATTACK_COOLDOWN;
            startAction(VicissitudeRig.Action.HEAVY_ATTACK, false);
            return;
        }
        if ((meleeAlternator + 1) % 3 == 0){
            if (verticalSlashCooldown > 0){
                return;
            }
            verticalSlashCooldown = VERTICAL_SLASH_COOLDOWN;
            meleeAlternator++;
            startAction(VicissitudeRig.Action.SLASH_VERTICAL, false);
            return;
        }
        if (slashCooldown > 0){
            return;
        }
        slashCooldown = SLASH_COOLDOWN;
        meleeAlternator++;
        startAction(VicissitudeRig.Action.SLASH_HORIZONTAL, baseSlotIndex++ % 2 == 0);
    }

    private double distanceTo(LivingEntity target) {
        return Math.sqrt(distanceToSqr(target.getX(), target.getY() + target.getBbHeight() * 0.5D,
                                       target.getZ()));
    }

    private void releaseAction() {
        LivingEntity target = committedTarget;
        switch (action) {
            case WING_RANGED -> fireFan(target);
            case WING_BARRAGE -> fireBarrageWave(target, 0);
            case CAST_FROM_CHEST -> resolveChestMark();
            case CAST_FROM_HALO -> resolveHaloVerdict();
            case SLASH_HORIZONTAL, SLASH_VERTICAL, HEAVY_ATTACK -> {
                // Melee deals no damage here; it is resolved while the blade is live, see tickBladeHits().
            }
            case SCYTHE_THROW -> throwScythe(target);
            case RANGED_FALLBACK -> fireFallbackVolley(target);
            case DASH -> {
            }
            default -> {
            }
        }
    }

    private void fireFan(@Nullable LivingEntity target) {
        if (target == null || !(level() instanceof ServerLevel serverLevel)){
            return;
        }
        boolean left = actionLeft;
        boolean homingSlot = fanCount % 2 == 1 && isPhaseOneStage();
        Vec3 aim = committedAim;
        for (int index = 0; index < 5; index++) {
            float offset = -24.0F + index * 12.0F;
            Vec3 origin = wingOrigin(left, index);
            Vec3 direction = aimedDirection(origin, aim, offset);
            if (index == 2 && homingSlot){
                spawnHomingOrb(serverLevel, target, origin);
                continue;
            }
            spawnBolt(serverLevel, origin, direction.scale(0.45D), true, 0.0F, 80);
        }
        playSwingSound(left);
        sendEvent(VicissitudeEffectPacket.EVENT_RELEASE, wingOrigin(left, 2));
    }

    private void fireBarrageWave(@Nullable LivingEntity target, int wave) {
        if (target == null || !target.isAlive() || target.level() != level()
            || isIgnoredPlayer(target) || !(level() instanceof ServerLevel serverLevel)){
            barrageWaveTick = 0L;
            lastBarrageWave = 0;
            return;
        }
        Vec3 aim = committedAim;
        double distance = position().distanceTo(committedAim);
        double spread = Math.min(30.0D, 6.0D + distance * 1.5D);
        for (int index = 0; index < 4; index++) {
            // The centre gap keeps a passable lane in every wave.
            float offset = (float) (spread * (0.35D + index * 0.35D));
            for (int side = 0; side < 2; side++) {
                Vec3 origin = wingOrigin(side == 0, index);
                spawnBolt(serverLevel, origin,
                          aimedDirection(origin, aim, side == 0 ? offset : -offset).scale(0.45D),
                          true, 0.0F, 80);
            }
        }
        playSwingSound(wave % 2 == 0);
        sendEvent(VicissitudeEffectPacket.EVENT_RELEASE, position().add(0.0D, 2.0D, 0.0D));
        if (wave < 2){
            lastBarrageWave = wave + 1;
            barrageWaveTick = level().getGameTime() + 8;
        }else {
            barrageWaveTick = 0L;
        }
    }

    private int lastBarrageWave;
    private long barrageWaveTick;

    private void tickBarrageWaves() {
        if (action != VicissitudeRig.Action.WING_BARRAGE || barrageWaveTick == 0L){
            return;
        }
        if (level().getGameTime() >= barrageWaveTick && lastBarrageWave <= 2){
            fireBarrageWave(committedTarget, lastBarrageWave);
        }
    }

    private void fireFallbackVolley(@Nullable LivingEntity target) {
        if (target == null || !(level() instanceof ServerLevel serverLevel)){
            return;
        }
        float damage = attackDamage() * 0.75F;
        Vec3 aim = committedAim;
        for (int index = -1; index <= 1; index++) {
            Vec3 origin = wingOrigin(index >= 0, Math.abs(index));
            spawnBolt(serverLevel, origin,
                      aimedDirection(origin, aim, index * 10.0F).scale(0.65D), false, damage, 120);
        }
        playSwingSound(true);
        sendEvent(VicissitudeEffectPacket.EVENT_RELEASE, position().add(0.0D, 2.0D, 0.0D));
    }

    /** {@code pressInPhaseOne} only presses in phase one; in phase two the volley deals damage. */
    private void spawnBolt(
            ServerLevel serverLevel, Vec3 origin, Vec3 motion, boolean pressInPhaseOne,
            float damage, int life) {
        boolean press = pressInPhaseOne && isPhaseOneStage();
        float actualDamage = press ? 0.0F : (damage > 0.0F ? damage : attackDamage() * 0.75F);
        if (countBolts(serverLevel, false) >= MAX_NON_HOMING_BOLTS){
            return;
        }
        VicissitudeSpiritBoltEntity bolt = new VicissitudeSpiritBoltEntity(serverLevel, this,
                                                                           origin, motion, press, actualDamage, life);
        serverLevel.addFreshEntity(bolt);
    }

    private void spawnHomingOrb(ServerLevel serverLevel, LivingEntity target, Vec3 origin) {
        if (countBolts(serverLevel, true) >= MAX_HOMING_ORBS){
            return;
        }
        VicissitudeLightOrbEntity orb = new VicissitudeLightOrbEntity(serverLevel, this, target, origin);
        serverLevel.addFreshEntity(orb);
    }

    private int countBolts(ServerLevel serverLevel, boolean homing) {
        AABB area = getBoundingBox().inflate(96.0D);
        if (homing){
            return serverLevel.getEntitiesOfClass(VicissitudeLightOrbEntity.class, area,
                                                  orb -> orb.isOwnedBy(this)).size();
        }
        return serverLevel.getEntitiesOfClass(VicissitudeSpiritBoltEntity.class, area,
                                              bolt -> bolt.getOwner() == this).size();
    }

    private void resolveChestMark() {
        Vec3 center = getGroundMarkerCenter();
        double radius = getGroundMarkerOuterRadius();
        float damage = attackDamage();
        for (LivingEntity victim : groundTargets(center, radius, 2.0D)) {
            // The candidate box is only a filter: judgement is the drawn circle.
            if (victim.distanceToSqr(center.x, victim.getY(), center.z) > radius * radius){
                continue;
            }
            hurtBySkill(victim, damage, true);
        }
        if (level() instanceof ServerLevel serverLevel){
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL,
                                      center.x, center.y + 0.2D, center.z, 12, radius * 0.5D, 0.1D, radius * 0.5D,
                                      0.02D);
        }
        sendEvent(VicissitudeEffectPacket.EVENT_RELEASE, center);
    }

    private void resolveHaloVerdict() {
        Vec3 center = getGroundMarkerCenter();
        double inner = getGroundMarkerInnerRadius();
        double outer = getGroundMarkerOuterRadius();
        float gapCenter = getGroundMarkerGapCenter();
        float gapWidth = getGroundMarkerGapWidth();
        float damage = attackDamage();
        for (LivingEntity victim : groundTargets(center, outer, 2.0D)) {
            double distance = Math.sqrt(victim.distanceToSqr(center.x, victim.getY(), center.z));
            if (distance < inner || distance > outer){
                continue;
            }
            float angle = (float) Math.toDegrees(Math.atan2(victim.getZ() - center.z,
                                                            victim.getX() - center.x));
            if (Math.abs(Mth.wrapDegrees(angle - gapCenter)) < gapWidth * 0.5F){
                continue;
            }
            hurtBySkill(victim, damage, true);
        }
        sendEvent(VicissitudeEffectPacket.EVENT_RELEASE, center);
    }

    private List<LivingEntity> groundTargets(Vec3 center, double radius, double height) {
        AABB area = new AABB(center.x - radius, center.y - 0.5D, center.z - radius,
                             center.x + radius, center.y + height, center.z + radius);
        List<LivingEntity> targets = new ArrayList<>();
        for (Entity entity : level().getEntities(this, area)) {
            if (entity instanceof LivingEntity living && isValidCombatParticipant(living)){
                targets.add(living);
            }
        }
        return targets;
    }

    /** Melee damage: the scythe's world-space segment is tested each tick of the hit window. */
    private void tickBladeHits() {
        int[] window = action.hitWindow();
        if (window == null){
            return;
        }
        int ticks = actionTicks();
        if (ticks < window[0] || ticks > window[1]){
            return;
        }
        refreshPose();
        VicissitudeRig.BladeSegment blade =
                VicissitudeRig.bladeSegment(serverPose, action, ticks);
        if (blade == null){
            return;
        }
        double reach = blade.length() + BLADE_HIT_RADIUS;
        float damage = attackDamage() * meleeMultiplier();
        boolean trace = MELEE_TRACE;
        if (trace){
            System.out.println("[melee] " + action + " tick " + ticks
                                       + " boss=" + fmt(getX()) + "," + fmt(getY()) + "," + fmt(getZ())
                                       + " yaw=" + fmt(getYRot())
                                       + " grip=" + fmt(blade.grip().x()) + "," + fmt(blade.grip().y())
                                       + "," + fmt(blade.grip().z())
                                       + " tip=" + fmt(blade.tip().x()) + "," + fmt(blade.tip().y())
                                       + "," + fmt(blade.tip().z()));
        }
        for (Entity entity : level().getEntities(this,
                                                 getBoundingBox().inflate(reach, reach, reach))) {
            if (!(entity instanceof LivingEntity living)){
                continue;
            }
            if (!isValidCombatParticipant(living)){
                if (trace){
                    System.out.println("[melee]   DROP " + living.getName().getString()
                                               + " not a participant (phase=" + stage
                                               + " roster=" + combatRosterSize() + ")");
                }
                continue;
            }
            double gap = VicissitudeRig.distanceToBladeBox(
                    VicissitudeRig.Box.of(living.getBoundingBox().minX,
                                          living.getBoundingBox().minY,
                                          living.getBoundingBox().minZ,
                                          living.getBoundingBox().maxX,
                                          living.getBoundingBox().maxY,
                                          living.getBoundingBox().maxZ),
                    blade, getX(), getY(), getZ(), getYRot());
            boolean sight = hasLineOfSight(living);
            if (trace){
                System.out.println("[melee]   " + living.getName().getString()
                                           + " gapBox=" + fmt(gap)
                                           + " limit=" + fmt(BLADE_HIT_RADIUS)
                                           + " sight=" + sight
                                           + (gap <= BLADE_HIT_RADIUS && sight ? "  -> HIT"
                                                                               : "  -> miss"));
            }
            if (gap > BLADE_HIT_RADIUS || !sight){
                continue;
            }
            hurtBySkill(living, damage, false);
            if (!actionHitLanded){
                actionHitLanded = true;
                onMeleeConnected();
            }
        }
    }

    /** Diagnostic melee trace, enabled with {@code -Dmaledict.meleeTrace=true}. */
    private static final boolean MELEE_TRACE =
            Boolean.getBoolean("maledict.meleeTrace");

    private static String fmt(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private int combatRosterSize() {
        return isPhaseTwo() ? phaseTwoParticipants.size() : phaseOneTargets.size();
    }

    private float meleeMultiplier() {
        return switch (action) {
            case SLASH_HORIZONTAL -> 1.0F;
            case SLASH_VERTICAL -> 1.25F;
            case HEAVY_ATTACK -> 1.75F;
            default -> 1.0F;
        };
    }

    private void onMeleeConnected() {
        boolean heavy = action == VicissitudeRig.Action.HEAVY_ATTACK;
        swing(InteractionHand.MAIN_HAND, true);
        spawnSlashEffect(heavy);
        if (heavy){
            Vec3 look = getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize();
            sendEvent(VicissitudeEffectPacket.EVENT_HEAVY_IMPACT,
                      position().add(look.scale(2.0D)).add(0.0D, getBbHeight() * 0.4D, 0.0D));
        }
    }

    private void onMeleeMissed() {
        spawnSlashEffect(action == VicissitudeRig.Action.HEAVY_ATTACK
                         || action == VicissitudeRig.Action.SLASH_VERTICAL);
    }

    private boolean hurtBySkill(LivingEntity victim, float damage, boolean area) {
        UUID identity = victim.getUUID();
        if (!roundDamagedTargets.add(identity)){
            // One damage instance per round and target.
            return false;
        }
        return hurtParticipant(victim,
                               DamageTypeHelper.create(level(), DamageTypeRegistry.SCYTHE_SWEEP, this), damage);
    }

    /** The encounter's only damage entry point for combat participants. */
    public boolean hurtParticipant(LivingEntity victim, DamageSource source, float damage) {
        if (damage <= 0.0F || victim.level().isClientSide){
            return false;
        }
        // Every skill hit lands: an attack is one authored instance, not a stream of ticks.
        victim.invulnerableTime = 0;
        if (!(victim instanceof Player)){
            return victim.hurt(source, damage);
        }
        DamagePress press = isPhaseOne() ? DamagePress.LIGHT : bossDifficulty.damagePress();
        return press == DamagePress.MEDIUM
               ? DamageProbe.mediumDamageMethod(victim, source, damage).success()
               : DamageProbe.lighterDamageMethod(victim, source, damage).success();
    }

    private void spawnSlashEffect(boolean vertical) {
        ParticleHelper.SlashParticleEffectBuilder particles = ParticleHelper
                .createSlashingEffect(ParticleEffectTypeRegistry.SCYTHE_SLASH)
                .setSpiritType(SpiritTypeRegistry.UMBRAL_SPIRIT);
        if (vertical){
            particles.setVerticalSlashAngle();
        }else {
            particles.setSlashAngle(getYRot());
        }
        particles.setMirrored(actionLeft);
        Vec3 direction = getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        particles.spawnSlashingParticle(level(), position().add(0.0D, getBbHeight() * 0.6D, 0.0D),
                                        direction);
    }

    private void tickDash(int ticks) {
        if (ticks < action.releaseTick()){
            return;
        }
        if (!dashDirectionLocked){
            dashDirectionLocked = true;
            dashHitTargets.clear();
            dashTicks = 0;
            Vec3 look = getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize();
            dashDirection = look.lengthSqr() < 1.0E-4D ? Vec3.ZERO : look;
        }
        if (dashTicks >= 12 || dashDirection == Vec3.ZERO){
            setDeltaMovement(getDeltaMovement().scale(0.4D));
            return;
        }
        dashTicks++;
        Vec3 step = dashDirection.scale(DASH_DISTANCE / 12.0D);
        if (level().clip(new net.minecraft.world.level.ClipContext(position(),
                                                                   position().add(step), net.minecraft.world.level.ClipContext.Block.COLLIDER,
                                                                   net.minecraft.world.level.ClipContext.Fluid.NONE, this))
                   .getType() != net.minecraft.world.phys.HitResult.Type.MISS){
            setDeltaMovement(Vec3.ZERO);
            dashTicks = 12;
            return;
        }
        if (!level().noCollision(this, getBoundingBox().expandTowards(step))){
            dashTicks = 12;
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        setDeltaMovement(Vec3.ZERO);
        setPos(getX() + step.x, getY() + step.y, getZ() + step.z);
        AABB sweep = getBoundingBox().inflate(0.6D);
        for (Entity entity : level().getEntities(this, sweep)) {
            if (!(entity instanceof LivingEntity living) || !isValidCombatParticipant(living)){
                continue;
            }
            if (dashHitTargets.add(living.getUUID())){
                hurtBySkill(living, attackDamage(), false);
            }
        }
    }

    private boolean hasWeaponInHand() {
        return !getMainHandItem().isEmpty();
    }

    private void throwScythe(@Nullable LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel) || scytheToken != null){
            return;
        }
        ItemStack weapon = getMainHandItem();
        if (weapon.isEmpty()){
            weapon = createWeaponForDifficulty(bossDifficulty);
        }
        Vec3 direction = committedAim.subtract(handAnchorWorldPosition()).normalize();
        VicissitudeScytheProjectileEntity scythe = new VicissitudeScytheProjectileEntity(
                serverLevel, this, handAnchorWorldPosition(), direction, weapon, attackDamage());
        if (!serverLevel.addFreshEntity(scythe)){
            // Only a successful spawn may empty the hand.
            return;
        }
        stashedWeapon = weapon.copy();
        setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        scytheToken = scythe.getUUID();
        setWeaponState(2);
        SoundHelper.playSound(this, SoundRegistry.SCYTHE_THROW.get(), 1.1F,
                              RandomHelper.randomBetween(level().getRandom(), 0.8F, 1.0F));
    }

    /** Called by the projectile when it is caught or gives up. */
    public void onScytheReturned(VicissitudeScytheProjectileEntity projectile, boolean caught) {
        if (scytheToken == null || !scytheToken.equals(projectile.getUUID())){
            return;
        }
        scytheToken = null;
        ItemStack stack = stashedWeapon.isEmpty() ? projectile.getItem() : stashedWeapon;
        stashedWeapon = ItemStack.EMPTY;
        if (!stack.isEmpty()){
            setItemSlot(EquipmentSlot.MAINHAND, stack.copy());
        }
        setWeaponState(1);
        // A catch must not erase a ground tell or truncate a volley already in progress.
        scytheRecoveryPending = true;
        if (caught){
            SoundHelper.playSound(this, SoundRegistry.SCYTHE_CATCH.get(), 1.0F,
                                  RandomHelper.randomBetween(level().getRandom(), 0.9F, 1.1F));
            sendEvent(VicissitudeEffectPacket.EVENT_SCYTHE_CATCH, handAnchorWorldPosition());
        }
        applyDeferredWeaponTier();
    }

    private void cancelFlyingScythe() {
        if (scytheToken == null || !(level() instanceof ServerLevel serverLevel)){
            return;
        }
        Entity entity = serverLevel.getEntity(scytheToken);
        if (entity != null){
            entity.discard();
        }
        scytheToken = null;
        // The stash is the authoritative copy; if it is gone the weapon is re-issued from code.
        ItemStack restored = stashedWeapon.isEmpty()
                             ? (getHealth() > 0.0F ? createWeaponForDifficulty(bossDifficulty)
                                                   : ItemStack.EMPTY)
                             : stashedWeapon.copy();
        stashedWeapon = ItemStack.EMPTY;
        setItemSlot(EquipmentSlot.MAINHAND, restored);
        setWeaponTier(bossDifficulty);
        setWeaponState(restored.isEmpty() ? 0 : 1);
    }

    /** Per-difficulty loot table; {@code Mob#getLootTable} is final, so this is the only override point. */
    @Override
    protected ResourceLocation getDefaultLootTable() {
        return bossDifficulty.lootTable();
    }

    /** Extra Age of Enlightenment drop, gated on {@code recentlyHit}: a player hit it in the last 100 ticks. */
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (!recentlyHit){
            return;
        }
        ItemEntity drop = spawnAtLocation(
                AgeOfEnlightenmentItem.create(bossDifficulty.enlightenmentLevel()));
        if (drop != null){
            drop.setExtendedLifetime();
        }
    }

    @Override
    protected void onFinalDeath(DamageSource source) {
        cancelFlyingScythe();
        // Everything still held goes to the world ledger; nothing is deleted or dropped here.
        if (level() instanceof ServerLevel serverLevel){
            for (Map.Entry<UUID, Deque<VicissitudeCurioLedger.Entry>> entry : confiscated.entrySet()) {
                VicissitudeCurioReturns.retain(serverLevel, entry.getKey(),
                                               new ArrayList<>(entry.getValue()));
                ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(entry.getKey());
                if (player != null){
                    VicissitudeCurioReturns.deliverAll(player);
                }
            }
            confiscated.clear();
        }
        setWeaponState(0);
    }

    @Override
    protected int getDeathDurationTicks() {
        return DEATH_TICKS;
    }

    @Override
    protected void onDeathTick(int ticks) {
        if (!level().isClientSide){
            setStage(VicissitudeBossStage.DYING);
            if (ticks == 36){
                // 36-59: wing roots fail, the one death shake.
                sendEvent(VicissitudeEffectPacket.EVENT_DEATH_CORE, position());
            }
            if (ticks == 60){
                // 60-79: the core goes out quietly.
                sendEvent(VicissitudeEffectPacket.EVENT_DEATH_EXTINGUISH,
                          anchorWorldPosition(VicissitudeRigData.Joint.HEAD_EFFECT_ANCHOR, 0.0F));
            }
        }
    }

    private void confiscateEquippedCurios(ServerPlayer player) {
        List<VicissitudeCurioLedger.Entry> taken = VicissitudeCurioReturns.confiscate(player);
        if (taken.isEmpty()){
            confiscated.remove(player.getUUID());
            return;
        }
        confiscated.put(player.getUUID(), new ArrayDeque<>(taken));
    }

    @Override
    protected void onDamageAccepted(DamageSource source, float amount) {
        hurtTicks = 6;
        if (isPhaseTwo() && bossDifficulty.confiscatesCurios()){
            returnOneCurio(resolveAttackingPlayer(source));
        }
    }

    private void returnOneCurio(@Nullable ServerPlayer preferredPlayer) {
        if (preferredPlayer != null && returnOneCurioTo(preferredPlayer)){
            return;
        }
        if (!(level() instanceof ServerLevel serverLevel) || phaseTwoParticipants.isEmpty()){
            return;
        }
        List<UUID> players = new ArrayList<>(phaseTwoParticipants);
        int start = Math.floorMod(curioReturnCursor, players.size());
        for (int offset = 0; offset < players.size(); offset++) {
            int index = (start + offset) % players.size();
            ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(players.get(index));
            if (player != null && returnOneCurioTo(player)){
                curioReturnCursor = index + 1;
                return;
            }
        }
    }

    private boolean returnOneCurioTo(ServerPlayer player) {
        Deque<VicissitudeCurioLedger.Entry> held = confiscated.get(player.getUUID());
        if (held == null || held.isEmpty()){
            return false;
        }
        // Hand the stack to the ledger first, then deliver: ownership moves exactly once.
        VicissitudeCurioLedger.Entry entry = held.removeFirst();
        VicissitudeCurioReturns.retain(player.serverLevel(), player.getUUID(), List.of(entry));
        if (!VicissitudeCurioReturns.deliverAll(player)){
            VicissitudeCurioReturns.retain(player.serverLevel(), player.getUUID(),
                                           new ArrayList<>(held));
            held.clear();
            confiscated.remove(player.getUUID());
            return true;
        }
        if (held.isEmpty()){
            confiscated.remove(player.getUUID());
        }
        return true;
    }

    /** Damage from anything that is not a player is halved. */
    private static final float NON_PLAYER_DAMAGE_MULTIPLIER = 0.5F;

    /** Applies the non-player cut, adaptation and distance falloff; hit location is only a cap. */
    @Override
    protected float modifyIncomingDamage(DamageSource source, float amount) {
        float scaled = amount;
        if (!isPlayerDamage(source)){
            scaled *= NON_PLAYER_DAMAGE_MULTIPLIER;
        }
        return scaled * damageAdaptation.adapt(source.getMsgId(), adaptationLevel())
               * distanceDamageScale(source);
    }

    /** Single-hit cap from the hit segment's {@code capWeight}. */
    @Override
    protected float incomingHitWeight(DamageSource source) {
        return nearestHitSegment(source).capWeight();
    }

    private VicissitudeRig.Segment nearestHitSegment(DamageSource source) {
        List<VicissitudeRig.SegmentVolume> volumes = segmentVolumes();
        Vec3 hit = source.getSourcePosition();
        if (hit != null){
            return VicissitudeRig.nearestSegment(volumes, hit.x, hit.y, hit.z);
        }
        LivingEntity attacker = resolveLivingAttacker(source);
        return attacker == null
               ? VicissitudeRig.Segment.BODY
               : VicissitudeRig.nearestSegment(volumes, attacker.getX(),
                                               attacker.getEyeY(), attacker.getZ());
    }

    private float distanceDamageScale(DamageSource source) {
        LivingEntity attacker = resolveLivingAttacker(source);
        if (attacker == null){
            return 1.0F;
        }
        double inner = damageRange();
        double outer = inner * 1.5D;
        double distance = Math.sqrt(distanceToSqr(attacker));
        if (distance <= inner){
            return 1.0F;
        }
        if (distance >= outer){
            return 0.0F;
        }
        return (float) ((outer - distance) / (outer - inner));
    }

    private static int adaptationLevel() {
        return Math.max(0, MaledictConfig.VICISSITUDE_ADAPTATION_LEVEL.get());
    }

    /** Player credit: the shooter counts, a player's pet does not. */
    private static boolean isPlayerDamage(DamageSource source) {
        return source.getEntity() instanceof Player || source.getDirectEntity() instanceof Player;
    }

    private List<VicissitudeRig.SegmentVolume> segmentVolumes() {
        return VicissitudeRig.segmentVolumes(serverPose, getX(), getY(), getZ(), getYRot());
    }

    private void refreshPose() {
        float death = getHealth() > 0.0F ? -1.0F : deathTime;
        VicissitudeRig.compute(serverPose, isPhaseTwoVisual(), getPhaseTwoBlend(), action,
                               actionTicks(), actionLeft, hurtTicks, level().getGameTime(), wingFold, death);
        applyHeadLook(serverPose, action, death, yHeadRot - yBodyRot, getXRot());
    }

    private static void applyHeadLook(
            VicissitudeRig.Pose pose, VicissitudeRig.Action action,
            float death, float yaw, float pitch) {
        if (action == VicissitudeRig.Action.NONE && death < 0.0F){
            pose.addRotation(VicissitudeRigData.Joint.HEAD_ROOT,
                             pitch * 0.6F, Mth.wrapDegrees(yaw) * 0.5F, 0.0F);
            VicissitudeRig.solve(pose);
        }
    }

    /** Shared scratch pose for the mesh and effects; callers must not mutate it. */
    public VicissitudeRig.Pose poseForRender(float partialTick) {
        float death = getHealth() > 0.0F ? -1.0F : deathTime + partialTick;
        float ticks = Math.max(0.0F, getActionTicks(partialTick));
        VicissitudeRig.compute(renderPose, isPhaseTwoVisual(),
                               getPhaseTwoBlend(), getRenderAction(), ticks, isActionLeft(), getHurtTicks(),
                               level().getGameTime() + partialTick, getWingFold(), death);
        applyHeadLook(renderPose, getRenderAction(), death,
                      Mth.rotLerp(partialTick, yHeadRotO, yHeadRot)
                      - Mth.rotLerp(partialTick, yBodyRotO, yBodyRot),
                      Mth.lerp(partialTick, xRotO, getXRot()));
        return renderPose;
    }

    public Vec3 anchorWorldPosition(VicissitudeRigData.Joint joint, float partialTick) {
        if (!level().isClientSide){
            refreshPose();
        }
        VicissitudeRig.Pose pose = level().isClientSide ? poseForRender(partialTick) : serverPose;
        double x = level().isClientSide ? Mth.lerp(partialTick, xOld, getX()) : getX();
        double y = level().isClientSide ? Mth.lerp(partialTick, yOld, getY()) : getY();
        double z = level().isClientSide ? Mth.lerp(partialTick, zOld, getZ()) : getZ();
        float yaw = level().isClientSide ? Mth.rotLerp(partialTick, yBodyRotO, yBodyRot) : getYRot();
        VicissitudeRig.V3 world = VicissitudeRig.worldPoint(pose, joint, 0.0F, 0.0F, 0.0F,
                                                            x, y, z, yaw);
        return new Vec3(world.x(), world.y(), world.z());
    }

    public Vec3 handAnchorWorldPosition() {
        return anchorWorldPosition(VicissitudeRigData.Joint.SCYTHE_HAND_ANCHOR, 0.0F);
    }

    private Vec3 wingOrigin(boolean left, int index) {
        // Releases run before the end-of-tick pose refresh.
        refreshPose();
        VicissitudeRigData.Joint joint = left
                                         ? VicissitudeRigData.Joint.WING_LEFT_ATTACK_ANCHOR
                                         : VicissitudeRigData.Joint.WING_RIGHT_ATTACK_ANCHOR;
        VicissitudeRig.V3 world = VicissitudeRig.worldPoint(serverPose, joint,
                                                            (left ? 1.0F : -1.0F) * index * 2.0F, 0.0F, 0.0F,
                                                            getX(), getY(), getZ(), getYRot());
        return new Vec3(world.x(), world.y(), world.z());
    }

    private static Vec3 aimPoint(LivingEntity target) {
        return target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
    }

    private static Vec3 aimedDirection(Vec3 origin, Vec3 aim, float offsetDegrees) {
        Vec3 direction = aim.subtract(origin);
        if (direction.lengthSqr() < 1.0E-4D){
            return new Vec3(0.0D, 0.0D, 1.0D);
        }
        return direction.normalize().yRot((float) Math.toRadians(offsetDegrees));
    }


    private float yawTo(LivingEntity target) {
        return yawTo(target.position());
    }

    private float yawTo(Vec3 point) {
        double dx = point.x - getX();
        double dz = point.z - getZ();
        return (float) (Math.toDegrees(Math.atan2(-dx, dz)));
    }

    /** The entity's own attribute, which already includes the weapon's baked modifiers. */
    private float attackDamage() {
        return Math.max(1.0F, (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
    }

    private void playSwingSound(boolean left) {
        SoundHelper.playSound(this, SoundRegistry.SCYTHE_SWEEP.get(), 0.9F,
                              RandomHelper.randomBetween(level().getRandom(), 0.9F, 1.2F));
    }

    private void tickWingCollision() {
        if (!(level() instanceof ServerLevel)){
            return;
        }
        List<VicissitudeRig.SegmentVolume> volumes = segmentVolumes();
        boolean blocked = false;
        for (VicissitudeRig.SegmentVolume volume : volumes) {
            if (!volume.segment().isWing()){
                continue;
            }
            AABB box = toAabb(volume.box()).expandTowards(getDeltaMovement());
            if (level().getBlockCollisions(this, box).iterator().hasNext()){
                blocked = true;
                break;
            }
        }
        wingsBlockedLastTick = blocked;
        if (blocked){
            wingFold = Mth.clamp(wingFold + 0.12F, 0.0F, 1.0F);
            setDeltaMovement(getDeltaMovement().scale(0.35D));
        }else {
            wingFold = Mth.clamp(wingFold - 0.06F, 0.0F, 1.0F);
        }
        AABB sweep = getBoundingBox().inflate(6.0D);
        for (Player player : level().getEntitiesOfClass(Player.class, sweep,
                                                        candidate -> candidate.isAlive() && !candidate.isSpectator())) {
            pushOutOfWings(player, volumes);
        }
    }

    private void pushOutOfWings(Player player, List<VicissitudeRig.SegmentVolume> volumes) {
        AABB playerBox = player.getBoundingBox();
        for (VicissitudeRig.SegmentVolume volume : volumes) {
            if (!volume.segment().isWing()){
                continue;
            }
            AABB wing = toAabb(volume.box());
            if (!wing.intersects(playerBox)){
                continue;
            }
            Vec3 away = player.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D);
            if (away.lengthSqr() < 1.0E-4D){
                away = new Vec3(1.0D, 0.0D, 0.0D);
            }
            away = away.normalize();
            if (!level().noCollision(player, player.getBoundingBox().move(away.scale(0.05D)))){
                // Wedged against a wall: yield with the wing instead of crushing.
                wingFold = Mth.clamp(wingFold + 0.1F, 0.0F, 1.0F);
                continue;
            }
            player.push(away.x * 0.06D, 0.0D, away.z * 0.06D);
        }
    }

    private void tickUnstick() {
        if (!(level() instanceof ServerLevel serverLevel)){
            return;
        }
        double travelled = Math.sqrt(sqr(getX() - stuckReferenceX) + sqr(getY() - stuckReferenceY)
                                     + sqr(getZ() - stuckReferenceZ));
        stuckTicks++;
        if (travelled >= STUCK_MIN_TRAVEL){
            stuckReferenceX = getX();
            stuckReferenceY = getY();
            stuckReferenceZ = getZ();
            stuckTicks = 0;
            return;
        }
        boolean targetPresent = getTarget() != null;
        boolean exempt = !targetPresent || action == VicissitudeRig.Action.SCYTHE_RECOVER
                         || stage == VicissitudeBossStage.TRANSITION || getHealth() <= 0.0F;
        if (exempt || stuckTicks < STUCK_WINDOW_TICKS || unstickCooldown > 0){
            return;
        }
        boolean obstructed = wingsBlockedLastTick
                             || !level().noCollision(this, getBoundingBox().inflate(0.1D));
        if (!obstructed){
            stuckReferenceX = getX();
            stuckReferenceY = getY();
            stuckReferenceZ = getZ();
            stuckTicks = 0;
            return;
        }
        if (tryUnstickTeleport(serverLevel)){
            stuckTicks = 0;
            unstickCooldown = UNSTICK_SUCCESS_COOLDOWN;
        }else {
            stuckTicks = 0;
        }
    }

    private boolean tryUnstickTeleport(ServerLevel serverLevel) {
        int checked = 0;
        for (int radius : UNSTICK_RADII) {
            for (int dx = -radius; dx <= radius && checked < UNSTICK_MAX_CANDIDATES; dx += 2) {
                for (int dz = -radius; dz <= radius && checked < UNSTICK_MAX_CANDIDATES; dz += 2) {
                    for (int dy = -8; dy <= 8 && checked < UNSTICK_MAX_CANDIDATES; dy += 2) {
                        checked++;
                        double x = getX() + dx;
                        double y = getY() + dy;
                        double z = getZ() + dz;
                        if (y < level().getMinBuildHeight() + 1
                            || y > level().getMaxBuildHeight() - getBbHeight() - 1){
                            continue;
                        }
                        BlockPos pos = BlockPos.containing(x, y, z);
                        if (!level().hasChunkAt(pos) || !level().isLoaded(pos)){
                            // Never force load chunks while searching.
                            continue;
                        }
                        if (isSpaceUsable(serverLevel, x, y, z)){
                            unstickTeleport(x, y, z);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private boolean isSpaceUsable(ServerLevel serverLevel, double x, double y, double z) {
        Vec3 old = position();
        setPos(x, y, z);
        boolean usable;
        try {
            if (!level().noCollision(this, getBoundingBox().deflate(0.05D))){
                usable = false;
            }else if (level().containsAnyLiquid(getBoundingBox())){
                usable = false;
            }else {
                usable = true;
                for (VicissitudeRig.SegmentVolume volume : segmentVolumes()) {
                    AABB box = toAabb(volume.box());
                    if (level().getBlockCollisions(this, box).iterator().hasNext()){
                        usable = false;
                        break;
                    }
                    if (!level().getEntitiesOfClass(Player.class, box).isEmpty()){
                        usable = false;
                        break;
                    }
                }
            }
        }
        finally {
            setPos(old.x, old.y, old.z);
        }
        return usable;
    }

    private void unstickTeleport(double x, double y, double z) {
        Vec3 before = position();
        setPos(x, y, z);
        setDeltaMovement(Vec3.ZERO);
        getNavigation().stop();
        if (action != VicissitudeRig.Action.NONE && action != VicissitudeRig.Action.SCYTHE_RECOVER){
            endAction();
            startAction(VicissitudeRig.Action.SCYTHE_RECOVER, false);
        }
        if (scytheToken != null && level() instanceof ServerLevel serverLevel){
            Entity entity = serverLevel.getEntity(scytheToken);
            if (entity instanceof VicissitudeScytheProjectileEntity scythe){
                scythe.recall();
            }
        }
        stuckReferenceX = x;
        stuckReferenceY = y;
        stuckReferenceZ = z;
        sendEvent(VicissitudeEffectPacket.EVENT_UNSTICK, position());
        if (level() instanceof ServerLevel serverLevel){
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                                      before.x, before.y + 1.0D, before.z, 12, 0.6D, 0.8D, 0.6D, 0.02D);
        }
    }

    private static AABB toAabb(VicissitudeRig.Box box) {
        return new AABB(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
    }

    private static double sqr(double value) {
        return value * value;
    }

    @Nullable
    private Vec3 findGroundPoint(LivingEntity target) {
        BlockPos origin = target.blockPosition();
        for (int drop = 0; drop <= 8; drop++) {
            BlockPos candidate = origin.below(drop);
            if (!level().getBlockState(candidate).getCollisionShape(level(), candidate).isEmpty()){
                return new Vec3(candidate.getX() + 0.5D, candidate.getY() + 1.0D,
                                candidate.getZ() + 0.5D);
            }
        }
        return null;
    }

    /** Called ten ticks into the windup; the marker centre never moves after this. */
    private void lockGroundMarker(double inner, double outer, float gapCenter, float gapWidth) {
        Vec3 center = groundMarkerTarget;
        if (center == null){
            return;
        }
        entityData.set(DATA_GROUND_X, (float) center.x);
        entityData.set(DATA_GROUND_Y, (float) center.y);
        entityData.set(DATA_GROUND_Z, (float) center.z);
        entityData.set(DATA_GROUND_INNER, (float) inner);
        entityData.set(DATA_GROUND_OUTER, (float) outer);
        entityData.set(DATA_GROUND_GAP_CENTER, gapCenter);
        entityData.set(DATA_GROUND_GAP_WIDTH, gapWidth);
        entityData.set(DATA_GROUND_START, level().getGameTime());
        entityData.set(DATA_GROUND_DURATION, action == null ? 0 : action.duration());
    }

    private Vec3 groundMarkerTarget = null;

    private void clearGroundMarker() {
        entityData.set(DATA_GROUND_DURATION, 0);
        entityData.set(DATA_GROUND_START, 0L);
        groundMarkerTarget = null;
    }

    public boolean isGroundMarkerActive() {
        return entityData.get(DATA_GROUND_DURATION) > 0
               && getGroundMarkerProgress(0.0F) < 1.0F;
    }

    public Vec3 getGroundMarkerCenter() {
        return new Vec3(entityData.get(DATA_GROUND_X), entityData.get(DATA_GROUND_Y),
                        entityData.get(DATA_GROUND_Z));
    }

    public double getGroundMarkerInnerRadius() {
        return entityData.get(DATA_GROUND_INNER);
    }

    public double getGroundMarkerOuterRadius() {
        return entityData.get(DATA_GROUND_OUTER);
    }

    public float getGroundMarkerGapCenter() {
        return entityData.get(DATA_GROUND_GAP_CENTER);
    }

    public float getGroundMarkerGapWidth() {
        return entityData.get(DATA_GROUND_GAP_WIDTH);
    }

    public float getGroundMarkerProgress(float partialTick) {
        int duration = entityData.get(DATA_GROUND_DURATION);
        if (duration <= 0){
            return 1.0F;
        }
        long elapsed = level().getGameTime() - entityData.get(DATA_GROUND_START);
        return Mth.clamp((elapsed + partialTick) / (duration + 8.0F), 0.0F, 1.0F);
    }

    private void tickGroundMarkerWindup() {
        if (action == VicissitudeRig.Action.CAST_FROM_CHEST){
            int ticks = actionTicks();
            if (ticks == 1){
                LivingEntity target = getTarget();
                groundMarkerTarget = target == null ? null : findGroundPoint(target);
                if (groundMarkerTarget == null){
                    return;
                }
            }
            if (ticks == 10){
                lockGroundMarker(0.0D, 2.0D, 0.0F, 0.0F);
            }
        }else if (action == VicissitudeRig.Action.CAST_FROM_HALO){
            int ticks = actionTicks();
            if (ticks == 1){
                LivingEntity target = getTarget();
                groundMarkerTarget = target == null ? null : findGroundPoint(target);
            }
            if (ticks == 10){
                float gap = level().getRandom().nextFloat() * 360.0F;
                lockGroundMarker(3.0D, 6.0D, gap, 60.0F);
            }
        }
    }

    public boolean isValidCombatParticipant(Entity entity) {
        if (!(entity instanceof LivingEntity living) || living == this || !living.isAlive()
            || living.level() != level()){
            return false;
        }
        if (isIgnoredPlayer(living)){
            return false;
        }
        if (isPhaseTwo()){
            return phaseTwoParticipants.contains(living.getUUID());
        }
        return phaseOneTargets.contains(living.getUUID());
    }

    /** Base cap ratio for a body hit; segment {@code capWeight} scales it (head 1.25x, outer wing 0.5x). */
    @Override
    protected float getVitalityDamageLimit() {
        float ratio = switch (bossDifficulty) {
            case SIMPLE -> 0.10F;
            case DIFFICULT -> 0.05F;
            case COMPLETE, EXTREME -> 0.01F;
        };
        return getVitalityMaximum() * ratio;
    }

    @Override
    protected boolean isDamageImmune(DamageSource source) {
        if (stage == VicissitudeBossStage.TRANSITION || getHealth() <= 0.0F){
            return true;
        }
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

    /** Vanilla-style aggro, no line of sight needed; hitting the DORMANT idol starts phase one. */
    @Override
    protected void onIncomingAttack(DamageSource source, float amount) {
        LivingEntity attacker = resolveLivingAttacker(source);
        if (isPhaseTwo()){
            LivingEntity owner = ownerOf(attacker);
            registerPhaseTwoParticipant(attacker);
            registerPhaseTwoParticipant(owner);
            return;
        }
        if (attacker == null || attacker == this || !attacker.isAlive()
            || isIgnoredPlayer(attacker)){
            return;
        }
        LivingEntity owner = ownerOf(attacker);
        registerPhaseOneParticipant(attacker);
        registerPhaseOneParticipant(owner);
        if (stage == VicissitudeBossStage.DORMANT){
            lockPhaseOneDuration();
            setStage(VicissitudeBossStage.PHASE_ONE);
            phaseOneTicks = 0;
            ServerPlayer starter = attacker instanceof ServerPlayer player ? player
                                                                           : owner instanceof ServerPlayer ownerPlayer ? ownerPlayer : null;
            if (starter != null){
                announceFirstAttack(starter);
            }
        }
        if (!isValidPhaseOneTarget(getTarget())){
            setTarget(preferredTarget(attacker, owner));
        }
    }

    /** {@code OwnableEntity#getOwnerUUID} is used rather than {@code getOwner()}: the owner may not be a player. */
    @Nullable
    private LivingEntity ownerOf(@Nullable LivingEntity attacker) {
        if (!(attacker instanceof OwnableEntity ownable)
            || !(level() instanceof ServerLevel serverLevel)){
            return null;
        }
        UUID ownerId = ownable.getOwnerUUID();
        if (ownerId == null){
            return null;
        }
        LivingEntity owner = resolveParticipant(serverLevel, ownerId);
        return owner == this ? null : owner;
    }

    private void registerPhaseOneParticipant(@Nullable LivingEntity participant) {
        if (participant == null || participant == this || !participant.isAlive()
            || participant.level() != level() || isIgnoredPlayer(participant)){
            return;
        }
        phaseOneTargets.add(participant.getUUID());
        if (participant instanceof ServerPlayer player){
            targetPlayerDeaths.put(player.getUUID(), getDeathCount(player));
        }
    }

    private LivingEntity preferredTarget(LivingEntity attacker, @Nullable LivingEntity owner) {
        return isValidPhaseOneTarget(owner) ? owner : attacker;
    }

    private void registerPhaseTwoParticipant(@Nullable LivingEntity participant) {
        if (participant == null || participant == this || !participant.isAlive()
            || participant.level() != level() || isIgnoredPlayer(participant)){
            return;
        }
        UUID identity = participant.getUUID();
        phaseTwoParticipants.add(identity);
        if (participant instanceof ServerPlayer player){
            phaseTwoPlayerDeaths.putIfAbsent(identity, getDeathCount(player));
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || target.level() != level()
            || isIgnoredPlayer(target)){
            setTarget(participant);
        }
    }

    private static Component announcement(String key) {
        return Component.translatable(key).withStyle(style -> style
                .withColor(TextColor.fromRgb(0xE6EDF5))
                .withBold(true));
    }

    /** Phase one line, shown once per player life. */
    private void announceFirstAttack(ServerPlayer player) {
        int deathCount = getDeathCount(player);
        if (announcedPlayerDeaths.getOrDefault(player.getUUID(), -1) != deathCount){
            player.displayClientMessage(announcement(PHASE_ONE_MESSAGE_KEY), true);
            announcedPlayerDeaths.put(player.getUUID(), deathCount);
        }
    }

    private void lockPhaseOneDuration() {
        if (!phaseOneDurationLocked){
            phaseOneDurationTicks = bossDifficulty.phaseOneDurationTicks();
            phaseOneDurationLocked = true;
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

    private void sendEvent(int eventId, Vec3 position) {
        if (!(level() instanceof ServerLevel serverLevel)){
            return;
        }
        VicissitudeEffectPacket packet = new VicissitudeEffectPacket(getId(), eventId,
                                                                     actionSequence, position);
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(position) < 96.0D * 96.0D){
                MaledictNetwork.sendEffect(player, packet);
            }
        }
    }

    @Nullable
    private LivingEntity currentPhaseOneTarget() {
        LivingEntity target = getTarget();
        return isValidPhaseOneTarget(target) ? target : selectNextPhaseOneTarget(false);
    }

    @Nullable
    private LivingEntity selectNextPhaseOneTarget(boolean advance) {
        if (!(level() instanceof ServerLevel serverLevel) || phaseOneTargets.isEmpty()){
            setTarget(null);
            return null;
        }
        List<UUID> targets = orderedPhaseOneTargets(serverLevel);
        int size = targets.size();
        int start = Math.floorMod(targetCursor, size);
        List<UUID> invalid = new ArrayList<>();
        for (int offset = 0; offset < size; offset++) {
            int index = (start + offset) % size;
            UUID identity = targets.get(index);
            Entity candidate = serverLevel.getEntity(identity);
            if (candidate instanceof LivingEntity living && isValidPhaseOneTarget(living)){
                targetCursor = advance ? index + 1 : index;
                setTarget(living);
                removeInvalidTargets(invalid);
                return living;
            }
            if (candidate != null && shouldRemovePhaseOneTarget(identity, candidate)){
                invalid.add(identity);
            }
        }
        removeInvalidTargets(invalid);
        setTarget(null);
        return null;
    }

    private List<UUID> orderedPhaseOneTargets(ServerLevel serverLevel) {
        List<UUID> players = new ArrayList<>();
        List<UUID> others = new ArrayList<>();
        var playerList = serverLevel.getServer().getPlayerList();
        for (UUID identity : phaseOneTargets) {
            if (playerList.getPlayer(identity) != null){
                players.add(identity);
            }else {
                others.add(identity);
            }
        }
        players.addAll(others);
        return players;
    }

    @Nullable
    private LivingEntity selectBalancedPhaseTwoTarget() {
        if (!(level() instanceof ServerLevel serverLevel)){
            return null;
        }
        LivingEntity selected = healthiestPhaseTwoTarget(serverLevel, true);
        if (selected == null){
            selected = healthiestPhaseTwoTarget(serverLevel, false);
        }
        setTarget(selected);
        return selected;
    }

    @Nullable
    private LivingEntity healthiestPhaseTwoTarget(ServerLevel serverLevel, boolean playersOnly) {
        return phaseTwoParticipants.stream()
                                   .map(identity -> resolveParticipant(serverLevel, identity))
                                   .filter(member -> member != null && member.level() == level()
                                                     && member.isAlive() && !isIgnoredPlayer(member)
                                                     && distanceToSqr(member) <= engagementRangeSqr())
                                   .filter(member -> !playersOnly || member instanceof ServerPlayer)
                                   .max(Comparator.comparingDouble(LivingEntity::getHealth))
                                   .orElse(null);
    }

    private boolean isValidPhaseOneTarget(@Nullable LivingEntity target) {
        if (target == null || target == this || !target.isAlive() || target.level() != level()){
            return false;
        }
        if (distanceToSqr(target) > engagementRangeSqr()){
            return false;
        }
        if (target instanceof ServerPlayer player){
            Integer registeredDeathCount = targetPlayerDeaths.get(player.getUUID());
            return !isIgnoredPlayer(player)
                   && (registeredDeathCount == null || registeredDeathCount == getDeathCount(player));
        }
        return !isIgnoredPlayer(target);
    }

    /** Creative and spectator players never join a roster and are never hit, even when they attack. */
    private static boolean isIgnoredPlayer(@Nullable LivingEntity target) {
        return target instanceof Player player && (player.isCreative() || player.isSpectator());
    }

    private boolean shouldRemovePhaseOneTarget(UUID identity, Entity candidate) {
        if (!(candidate instanceof ServerPlayer player)){
            return !(candidate instanceof LivingEntity living)
                   || !living.isAlive() || living.level() != level();
        }
        Integer registeredDeathCount = targetPlayerDeaths.get(identity);
        return !player.isAlive() || player.level() != level()
               || registeredDeathCount != null && registeredDeathCount != getDeathCount(player);
    }

    private void removeInvalidTargets(List<UUID> invalid) {
        phaseOneTargets.removeAll(invalid);
        for (UUID identity : invalid) {
            targetPlayerDeaths.remove(identity);
        }
    }

    private boolean isDivingMelee() {
        return action.isMelee() && committedTarget != null && committedTarget.isAlive();
    }

    /** Starts the descent before the swing commits, so the blade is at target height when the arm moves. */
    private boolean isApproachingMelee(@Nullable LivingEntity target) {
        if (action != VicissitudeRig.Action.NONE || target == null
            || !target.isAlive() || target.level() != level()){
            return false;
        }
        return distanceTo(target) <= MELEE_APPROACH_DISTANCE;
    }

    /** With no action given, the vertical cut's reach and height are used, so the approach is on target. */
    private Vec3 meleeStancePosition(LivingEntity target, double wantedY,
                                     @Nullable VicissitudeRig.Action forAction) {
        VicissitudeRig.Action swing = forAction != null && forAction.isMelee()
                                      ? forAction : VicissitudeRig.Action.SLASH_VERTICAL;
        double yaw = Math.toRadians(getYRot());
        double standOff = Math.max(0.0D, distanceTo(target) - swing.bladeForwardReach());
        return new Vec3(getX() - Math.sin(yaw) * standOff,
                        Mth.clamp(wantedY,
                                  level().getMinBuildHeight() + 1.0D,
                                  level().getMaxBuildHeight() - getBbHeight() - 1.0D),
                        getZ() + Math.cos(yaw) * standOff);
    }

    private double meleeDiveY(LivingEntity target, @Nullable VicissitudeRig.Action forAction) {
        VicissitudeRig.Action swing = forAction != null && forAction.isMelee()
                                      ? forAction : VicissitudeRig.Action.SLASH_VERTICAL;
        double targetCentre = target.getY() + target.getBbHeight() * 0.5D;
        return Mth.clamp(targetCentre - swing.bladeHeightAboveFeet(),
                         level().getMinBuildHeight() + 1.0D,
                         level().getMaxBuildHeight() - getBbHeight() - 1.0D);
    }

    private void updatePhaseOneMovement(@Nullable LivingEntity target) {
        if (target == null){
            double wantedY = Double.isNaN(idleHoverY) ? getY() + HOVER_HEIGHT : idleHoverY;
            steerToward(new Vec3(getX(), wantedY, getZ()), MAX_FLIGHT_SPEED);
            return;
        }
        if (action != VicissitudeRig.Action.NONE){
            if (isDivingMelee()){
                steerToward(meleeStancePosition(target, meleeDiveY(target, action), action), MAX_FLIGHT_SPEED);
                return;
            }
            setDeltaMovement(getDeltaMovement().scale(0.5D));
            hasImpulse = true;
            return;
        }
        if (isApproachingMelee(target)){
            steerToward(meleeStancePosition(target, meleeDiveY(target, null), null), MAX_FLIGHT_SPEED);
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
        steerToward(new Vec3(wanted.x, wantedY, wanted.z), MAX_FLIGHT_SPEED);
    }

    private void updatePhaseTwoMovement(@Nullable LivingEntity target) {
        if (target == null){
            setDeltaMovement(getDeltaMovement().scale(0.7D));
            hasImpulse = true;
            return;
        }
        if (action == VicissitudeRig.Action.DASH && dashDirectionLocked
            && actionTicks() >= action.releaseTick()){
            return;
        }
        if (action != VicissitudeRig.Action.NONE){
            if (isDivingMelee()){
                steerToward(meleeStancePosition(target, meleeDiveY(target, action), action),
                            PHASE_TWO_MAX_FLIGHT_SPEED);
                return;
            }
            setDeltaMovement(getDeltaMovement().scale(0.5D));
            hasImpulse = true;
            return;
        }
        if (isApproachingMelee(target)){
            steerToward(meleeStancePosition(target, meleeDiveY(target, null), null),
                        PHASE_TWO_MAX_FLIGHT_SPEED);
            return;
        }
        Vec3 away = position().subtract(target.position()).multiply(1.0D, 0.0D, 1.0D);
        if (away.lengthSqr() < 0.01D){
            double angle = tickCount * ORBIT_STEP;
            away = new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
        }else {
            away = away.normalize();
        }
        double wantedY = Mth.clamp(target.getY() + PHASE_TWO_HOVER_HEIGHT,
                                   level().getMinBuildHeight() + 1.0D,
                                   level().getMaxBuildHeight() - getBbHeight() - 1.0D);
        double holdingDistance = scytheToken == null ? PHASE_TWO_DISTANCE : 8.0D;
        double orbit = scytheToken == null ? 0.0D : (actionSequence % 2 == 0 ? 1.5D : -1.5D);
        Vec3 destination = new Vec3(
                target.getX() + away.x * holdingDistance - away.z * orbit,
                wantedY,
                target.getZ() + away.z * holdingDistance + away.x * orbit);
        steerToward(destination, PHASE_TWO_MAX_FLIGHT_SPEED);
    }

    private void steerToward(Vec3 destination, double maximumSpeed) {
        Vec3 offset = destination.subtract(position());
        if (offset.lengthSqr() < 0.01D){
            setDeltaMovement(getDeltaMovement().scale(0.7D));
            return;
        }
        double desiredSpeed = Math.min(maximumSpeed, offset.length() * 0.12D);
        Vec3 desiredMotion = offset.normalize().scale(desiredSpeed);
        Vec3 motion = getDeltaMovement().scale(1.0D - FLIGHT_STEERING)
                                        .add(desiredMotion.scale(FLIGHT_STEERING));
        if (motion.lengthSqr() > maximumSpeed * maximumSpeed){
            motion = motion.normalize().scale(maximumSpeed);
        }
        setDeltaMovement(motion);
        hasImpulse = true;
    }

    private void destroyBlockingEntities() {
        List<Entity> obstacles = level().getEntities(
                this,
                getBoundingBox().expandTowards(getDeltaMovement()).inflate(0.25D),
                entity -> entity.isAlive() && entity.isAttackable()
                          && !(entity instanceof Player)
                          && entity != getTarget());
        if (obstacles.isEmpty()){
            return;
        }
        float damage = Math.max(10.0F, attackDamage());
        boolean attacked = false;
        for (Entity obstacle : obstacles) {
            attacked |= obstacle.hurt(damageSources().mobAttack(this), damage);
        }
        if (attacked){
            swing(InteractionHand.MAIN_HAND, true);
        }
    }

    private void destroyBlockingBlocks() {
        if (!ForgeEventFactory.getMobGriefingEvent(level(), this)){
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
                        && ForgeEventFactory.onEntityDestroyBlock(this, position, state)){
                        destroyed = level().destroyBlock(position, true, this) || destroyed;
                    }
                }
            }
        }
        if (destroyed){
            level().levelEvent(null, 1022, center, 0);
        }
    }

    private void equipPhaseTwoWeapon() {
        setItemSlot(EquipmentSlot.MAINHAND, createWeaponForDifficulty(bossDifficulty));
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        applyWeaponAttributes();
        setWeaponTier(bossDifficulty);
        setWeaponState(1);
    }

    /** Keeps the hand matching the encounter while the weapon is meant to be held; a weapon in flight is left alone. */
    private void tickWeaponGuard(boolean fullCheck) {
        if (scytheToken != null || entityData.get(DATA_WEAPON_STATE) != 1){
            return;
        }
        ItemStack held = getMainHandItem();
        if (!held.isEmpty() && (!fullCheck || held.is(createWeaponForDifficulty(bossDifficulty)
                                                              .getItem()))){
            return;
        }
        equipPhaseTwoWeapon();
    }

    private ItemStack createWeaponForDifficulty(BossDifficulty difficulty) {
        if (difficulty == BossDifficulty.SIMPLE){
            return new ItemStack(ItemRegistry.SOUL_STAINED_STEEL_SCYTHE.get());
        }
        if (difficulty == BossDifficulty.DIFFICULT){
            return new ItemStack(ItemRegistry.EDGE_OF_DELIVERANCE.get());
        }
        ItemStack weapon = new ItemStack(MaledictItems.INCURSUS_BLADE.get());
        IncursusBladeItem.initializeStats(weapon);
        double level = difficulty == BossDifficulty.EXTREME ? 9.0D : 3.0D;
        for (String stat : INCURSUS_STATS) {
            IncursusBladeItem.setStat(weapon, stat, level);
        }
        return weapon;
    }

    public BossDifficulty getBossDifficulty() {
        return bossDifficulty;
    }

    public void setBossDifficulty(BossDifficulty difficulty) {
        bossDifficulty = difficulty == null ? BossDifficulty.SIMPLE : difficulty;
        setWeaponTier(bossDifficulty);
        if (!difficultyLocked && !level().isClientSide){
            applyDifficultyAttributes();
        }
        if (!phaseOneDurationLocked){
            phaseOneDurationTicks = bossDifficulty.phaseOneDurationTicks();
        }
        if (isPhaseTwo() && !level().isClientSide){
            if (scytheToken != null){
                pendingWeaponTier = bossDifficulty.ordinal();
                return;
            }
            equipPhaseTwoWeapon();
        }
    }

    private void applyDeferredWeaponTier() {
        if (pendingWeaponTier >= 0){
            pendingWeaponTier = -1;
            equipPhaseTwoWeapon();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("VicissitudeStage", (byte) stage.ordinal());
        tag.putBoolean("PhaseTwoReached", phaseTwoReached);
        tag.putInt("PhaseOneTicks", phaseOneTicks);
        tag.putInt("PhaseOneDuration", phaseOneDurationTicks);
        tag.putBoolean("PhaseOneDurationLocked", phaseOneDurationLocked);
        tag.putInt("PhaseOneTargetCursor", targetCursor);
        tag.putInt("CurioReturnCursor", curioReturnCursor);
        tag.putInt("BaseSlotIndex", baseSlotIndex);
        tag.putInt("SpecialRotator", specialRotator);
        tag.putInt("ActionSequence", actionSequence);
        tag.putString("VicissitudeDifficulty", bossDifficulty.serializedName());
        tag.putInt("BarrageCooldown", barrageCooldown);
        tag.putInt("ChestCooldown", chestCooldown);
        tag.putInt("HaloCooldown", haloCooldown);
        if (!Double.isNaN(idleHoverY)){
            tag.putDouble("PhaseOneHoverY", idleHoverY);
        }
        if (scytheToken != null){
            tag.putUUID("ScytheToken", scytheToken);
        }
        if (!stashedWeapon.isEmpty()){
            tag.put("StashedWeapon", stashedWeapon.save(new CompoundTag()));
        }
        tag.put("PhaseOneTargets", saveUuidSet(phaseOneTargets));
        tag.put("PhaseTwoPlayers", saveUuidSet(phaseTwoParticipants));
        tag.put("PhaseOneTargetDeaths", savePlayerDeathMap(targetPlayerDeaths));
        tag.put("PhaseOneAnnouncements", savePlayerDeathMap(announcedPlayerDeaths));
        tag.put("PhaseTwoPlayerDeaths", savePlayerDeathMap(phaseTwoPlayerDeaths));
        tag.put("ConfiscatedCurios", saveConfiscatedCurios());
        tag.put("DamageAdaptation", saveAdaptation());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        boolean hasStage = tag.contains("VicissitudeStage", Tag.TAG_BYTE);
        String savedDifficulty = tag.contains("VicissitudeDifficulty", Tag.TAG_STRING)
                                 ? tag.getString("VicissitudeDifficulty")
                                 : tag.getString("PhaseTwoMode");
        bossDifficulty = BossDifficulty.fromName(savedDifficulty);
        entityData.set(DATA_WEAPON_TIER, (byte) bossDifficulty.ordinal());
        if (hasStage){
            stage = VicissitudeBossStage.byId(tag.getByte("VicissitudeStage"));
        }else {
            // Legacy saves only know the boolean flags.
            stage = tag.getBoolean("PhaseTwoStarted") ? VicissitudeBossStage.PHASE_TWO
                                                      : tag.getBoolean("PhaseOneStarted") ? VicissitudeBossStage.PHASE_ONE
                                                                                          : VicissitudeBossStage.DORMANT;
        }
        phaseOneDurationLocked = tag.getBoolean("PhaseOneDurationLocked");
        phaseTwoReached = stage == VicissitudeBossStage.PHASE_TWO || tag.getBoolean("PhaseTwoReached");
        entityData.set(DATA_PHASE_TWO_REACHED, phaseTwoReached);
        int savedDuration = tag.getInt("PhaseOneDuration");
        int savedTicks = Mth.clamp(tag.getInt("PhaseOneTicks"), 0, 6000);
        if (savedDuration > 0){
            phaseOneDurationTicks = savedDuration;
            phaseOneTicks = Mth.clamp(savedTicks, 0, savedDuration);
        }else {
            phaseOneDurationTicks = bossDifficulty.phaseOneDurationTicks();
            // Legacy progress was measured against the old five minute timer; keep the ratio.
            float completed = Mth.clamp(savedTicks / 6000.0F, 0.0F, 1.0F);
            phaseOneTicks = Math.round(completed * phaseOneDurationTicks);
        }
        targetCursor = Math.max(0, tag.getInt("PhaseOneTargetCursor"));
        curioReturnCursor = Math.max(0, tag.getInt("CurioReturnCursor"));
        baseSlotIndex = Math.max(0, tag.getInt("BaseSlotIndex"));
        specialRotator = Math.max(0, tag.getInt("SpecialRotator"));
        actionSequence = tag.getInt("ActionSequence");
        barrageCooldown = Math.max(0, tag.getInt("BarrageCooldown"));
        chestCooldown = Math.max(0, tag.getInt("ChestCooldown"));
        haloCooldown = Math.max(0, tag.getInt("HaloCooldown"));
        if (tag.contains("PhaseOneHoverY", Tag.TAG_DOUBLE)){
            idleHoverY = tag.getDouble("PhaseOneHoverY");
        }
        if (tag.hasUUID("ScytheToken")){
            scytheToken = tag.getUUID("ScytheToken");
        }
        stashedWeapon = ItemStack.of(tag.getCompound("StashedWeapon"));
        loadUuidSet(tag.getList("PhaseOneTargets", Tag.TAG_INT_ARRAY), phaseOneTargets);
        loadUuidSet(tag.getList("PhaseTwoPlayers", Tag.TAG_INT_ARRAY), phaseTwoParticipants);
        loadPlayerDeathMap(tag.getList("PhaseOneTargetDeaths", Tag.TAG_COMPOUND), targetPlayerDeaths);
        loadPlayerDeathMap(tag.getList("PhaseOneAnnouncements", Tag.TAG_COMPOUND), announcedPlayerDeaths);
        loadPlayerDeathMap(tag.getList("PhaseTwoPlayerDeaths", Tag.TAG_COMPOUND), phaseTwoPlayerDeaths);
        loadConfiscatedCurios(tag.getList("ConfiscatedCurios", Tag.TAG_COMPOUND));
        loadAdaptation(tag.getList("DamageAdaptation", Tag.TAG_COMPOUND));
        entityData.set(DATA_STAGE, (byte) stage.ordinal());
        entityData.set(DATA_ACTION, (byte) VicissitudeRig.Action.NONE.ordinal());
        action = VicissitudeRig.Action.NONE;
        if (stage == VicissitudeBossStage.PHASE_TWO){
            setWeaponState(hasWeaponInHand() ? 1 : scytheToken != null ? 2 : 0);
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
            if (value.hasUUID("Player")){
                values.put(value.getUUID("Player"), value.getInt("Deaths"));
            }
        }
    }

    /** One record per damage message, list order is newest-first. */
    private ListTag saveAdaptation() {
        ListTag entries = new ListTag();
        for (String message : damageAdaptation.snapshot()) {
            CompoundTag value = new CompoundTag();
            value.putString("Message", message);
            entries.add(value);
        }
        return entries;
    }

    private void loadAdaptation(ListTag entries) {
        List<String> saved = new ArrayList<>();
        for (Tag entry : entries) {
            if (entry instanceof CompoundTag value){
                saved.add(value.getString("Message"));
            }
        }
        damageAdaptation.restore(saved);
    }

    private ListTag saveConfiscatedCurios() {
        ListTag players = new ListTag();
        for (Map.Entry<UUID, Deque<VicissitudeCurioLedger.Entry>> entry : confiscated.entrySet()) {
            CompoundTag player = new CompoundTag();
            player.putUUID("Player", entry.getKey());
            ListTag items = new ListTag();
            for (VicissitudeCurioLedger.Entry curio : entry.getValue()) {
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
        confiscated.clear();
        for (Tag playerTag : players) {
            CompoundTag player = (CompoundTag) playerTag;
            if (!player.hasUUID("Player")){
                continue;
            }
            Deque<VicissitudeCurioLedger.Entry> items = new ArrayDeque<>();
            for (Tag itemTag : player.getList("Items", Tag.TAG_COMPOUND)) {
                CompoundTag item = (CompoundTag) itemTag;
                ItemStack stack = ItemStack.of(item.getCompound("Stack"));
                if (!stack.isEmpty()){
                    items.addLast(new VicissitudeCurioLedger.Entry(
                            item.getString("Slot"), item.getInt("Index"), stack));
                }
            }
            if (!items.isEmpty()){
                confiscated.put(player.getUUID("Player"), items);
            }
        }
    }

    @Nullable
    private LivingEntity resolveLivingAttacker(DamageSource source) {
        Entity attacker = source.getEntity();
        if (!(attacker instanceof LivingEntity)){
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

    public enum BossDifficulty {
        /** Args: phase one ticks, max health, phase-two damage press, loot table path, enlightenment level. */
        SIMPLE(1800, 500.0D, DamagePress.LIGHT, "entities/first_vicissitude", 0),
        DIFFICULT(1400, 750.0D, DamagePress.LIGHT, "entities/first_vicissitude_difficult", 1),
        COMPLETE(1000, 1000.0D, DamagePress.MEDIUM, "entities/first_vicissitude_complete", 2),
        EXTREME(750, 1500.0D, DamagePress.MEDIUM, "entities/first_vicissitude_extreme", 3);

        private final int phaseOneDurationTicks;
        private final double maxHealth;
        private final DamagePress damagePress;
        private final ResourceLocation lootTable;
        private final int enlightenmentLevel;

        BossDifficulty(
                int phaseOneDurationTicks, double maxHealth, DamagePress damagePress,
                String lootTablePath, int enlightenmentLevel) {
            this.phaseOneDurationTicks = phaseOneDurationTicks;
            this.maxHealth = maxHealth;
            this.damagePress = damagePress;
            this.lootTable = ResourceLocation.fromNamespaceAndPath(Maledict.MODID, lootTablePath);
            this.enlightenmentLevel = enlightenmentLevel;
        }

        public int phaseOneDurationTicks() {
            return phaseOneDurationTicks;
        }

        public double maxHealth() {
            return maxHealth;
        }

        public DamagePress damagePress() {
            return damagePress;
        }

        public ResourceLocation lootTable() {
            return lootTable;
        }

        /** Enlightenment amplifier: {@code 0} is displayed as level I. */
        public int enlightenmentLevel() {
            return enlightenmentLevel;
        }

        private boolean confiscatesCurios() {
            return this == COMPLETE || this == EXTREME;
        }

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        private static BossDifficulty byId(int id) {
            BossDifficulty[] values = values();
            return id >= 0 && id < values.length ? values[id] : SIMPLE;
        }

        private static BossDifficulty fromName(String name) {
            try {
                return valueOf(name.toUpperCase(Locale.ROOT));
            }
            catch (IllegalArgumentException ignored) {
                return SIMPLE;
            }
        }
    }

    /** How hard the boss presses its damage through a player's defences. */
    public enum DamagePress {
        LIGHT,
        MEDIUM
    }

    /** Single combat goal; phase selection happens inside so goals never fight each other. */
    private static final class CombatGoal extends Goal {
        private final FirstVicissitudeBossEntity boss;
        private int targetRefreshCooldown;

        private CombatGoal(FirstVicissitudeBossEntity boss) {
            this.boss = boss;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return boss.isAlive();
        }

        @Override
        public boolean canContinueToUse() {
            return boss.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            boss.tickBarrageWaves();
            boss.tickGroundMarkerWindup();
            switch (boss.stage) {
                case DORMANT, PHASE_ONE -> {
                    LivingEntity target = boss.currentPhaseOneTarget();
                    boss.tickPhaseOneCombat();
                    boss.updatePhaseOneMovement(target);
                }
                case PHASE_TWO -> {
                    if (boss.action == VicissitudeRig.Action.NONE && targetRefreshCooldown-- <= 0){
                        boss.selectBalancedPhaseTwoTarget();
                        targetRefreshCooldown = 10;
                    }
                    LivingEntity target = boss.getTarget();
                    if (boss.action == VicissitudeRig.Action.NONE
                        && (target == null || !target.isAlive() || target.level() != boss.level())){
                        target = boss.selectBalancedPhaseTwoTarget();
                    }
                    boss.tickPhaseTwoCombat();
                    boss.updatePhaseTwoMovement(target);
                }
                case TRANSITION -> {
                    boss.setDeltaMovement(boss.getDeltaMovement().scale(0.6D));
                    boss.getNavigation().stop();
                }
                default -> {
                }
            }
        }
    }
}