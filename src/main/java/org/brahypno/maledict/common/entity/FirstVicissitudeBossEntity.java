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
import net.minecraft.sounds.SoundEvent;
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
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
import org.brahypno.maledict.config.MaledictConfig;
import org.brahypno.maledict.common.curio.VicissitudeCurioReturns;
import org.brahypno.maledict.common.item.AgeOfEnlightenmentItem;
import org.brahypno.maledict.common.item.IncursusBladeItem;
import org.brahypno.maledict.network.MaledictNetwork;
import org.brahypno.maledict.network.VicissitudeEffectPacket;
import org.brahypno.maledict.registry.MaledictEntities;
import org.brahypno.maledict.registry.MaledictItems;
import org.brahypno.maledict.rig.VicissitudeRig;
import org.brahypno.maledict.rig.VicissitudeRigData;
import org.jetbrains.annotations.Nullable;
import team.lodestar.lodestone.helpers.DamageTypeHelper;
import team.lodestar.lodestone.helpers.RandomHelper;
import team.lodestar.lodestone.helpers.SoundHelper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The first Vicissitude encounter.
 *
 * <p>Server authority: stage, action, action sequence, weapon ownership, wing blocking, part
 * multipliers and the unstick search all live here. The client only reads synced state and the
 * one shot event packets, so a late joiner can rebuild the current pose from the action start
 * time without replaying anything.
 */
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
    /**
     * 一阶段的压血弹把玩家按到 1 血之后，下一次释放至少推迟这么久（一个基础攻击槽）。
     *
     * <p>追踪球最多能飞 200 tick，常常正好在胸/环的伤害释放前一刻落地；这段时间是给玩家
     * 从 1 血里喘口气的，不是给 Boss 的额外冷却。
     */
    public static final int PRESS_RECOVERY_TICKS = 30;
    /**
     * 一阶段名单空着多久就回到「未参战」。
     *
     * <p>没有对手的一阶段不该继续空转阶段计时、更不该自己走进二阶段：那只会留下一只
     * 谁也叫不动的雕像（见 19 的实测记录）。五个呼吸之后重新打它一次就能从头再来。
     */
    public static final int EMPTY_ENCOUNTER_RESET_TICKS = 100;
    public static final int MAX_NON_HOMING_BOLTS = 48;
    public static final int MAX_HOMING_ORBS = 2;
    public static final double MELEE_REACH = 5.0D;
    public static final double THROW_MIN_RANGE = 6.0D;
    public static final double THROW_MAX_RANGE = 24.0D;
    public static final double DASH_MIN_RANGE = 8.0D;
    public static final double DASH_MAX_RANGE = 20.0D;
    public static final double DASH_DISTANCE = 10.0D;
    public static final double NO_FIRE_RANGE = 64.0D;
    /** Quick reference for the chat announcement. */
    /** Phase one set piece line, shown once per player life when the fight starts. */
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
    /** Stable id for the difficulty health modifier, so it can be rewritten without stacking. */
    private static final UUID MAX_HEALTH_MODIFIER_ID =
            UUID.fromString("2c4b1d5a-9f34-4b1c-9a37-6d1f4c0a51e2");
    /** Base id for the baked weapon modifiers; each one gets the next least significant value. */
    private static final UUID WEAPON_ATTRIBUTE_ID =
            UUID.fromString("8a17c3d2-5e64-4d0b-9c31-7b2f5a0e6d44");
    /** How often the hand is checked against the weapon the encounter expects. */
    private static final int WEAPON_GUARD_INTERVAL = 20;

    private final ServerBossEvent bossEvent = new ServerBossEvent(
            getDisplayName(), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS);
    private final Set<UUID> phaseOneTargets = new LinkedHashSet<>();
    /**
     * 二阶段参战名单。
     *
     * <p>存档键仍是 {@code PhaseTwoPlayers}（旧档兼容），但名单里装的不再只有玩家：
     * 一阶段的参战者会整体带过来，宠物、召唤物和别的生物同样能打到二阶段。
     */
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
    @Nullable private LivingEntity committedTarget;
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
    /** 压血弹刚命中玩家后的喘息窗口，见 {@link #PRESS_RECOVERY_TICKS}。 */
    private int pressRecoveryTicks;
    /** 一阶段名单已经空了多久，见 {@link #EMPTY_ENCOUNTER_RESET_TICKS}。 */
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
    /** Client side render stack, rebuilt only when the synced tier changes. */
    private ItemStack displayWeapon = ItemStack.EMPTY;
    private int displayWeaponTier = -1;
    /** Weapon modifiers currently baked into the entity's attribute map. */
    private final List<AppliedAttribute> appliedWeaponAttributes = new ArrayList<>();

    public FirstVicissitudeBossEntity(EntityType<? extends FirstVicissitudeBossEntity> type,
                                      Level level) {
        super(type, level);
        moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
        xpReward = 100;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createBossAttributes()
                .add(Attributes.ATTACK_DAMAGE, 8.0D)
                // The encounter cannot be shoved around and ordinary hits mostly glance off; the
                // weapon in the hand contributes nothing to either value.
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.ARMOR, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.3D)
                .add(Attributes.FLYING_SPEED, 0.45D)
                // Kept in step with the engagement range in the config: this boss chases through
                // its own combat goal, but every vanilla and modded system that reads the
                // attribute should see the same reach the encounter actually uses.
                .add(Attributes.FOLLOW_RANGE, 96.0D);
    }

    /**
     * Writes the difficulty's health pool onto the attribute. The attribute stays the single
     * authority for the maximum; only the permanent modifier is rewritten, and only before the
     * vitality ledger captures the pool, so an in-progress fight can never be healed or
     * shortened by changing the mode.
     */
    private void applyDifficultyAttributes() {
        AttributeInstance attribute = getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) {
            return;
        }
        attribute.removeModifier(MAX_HEALTH_MODIFIER_ID);
        double delta = bossDifficulty.maxHealth() - VicissitudeBossEntity.BASE_MAX_HEALTH;
        if (delta != 0.0D) {
            attribute.addPermanentModifier(new AttributeModifier(MAX_HEALTH_MODIFIER_ID,
                    "Vicissitude difficulty health", delta, AttributeModifier.Operation.ADDITION));
        }
    }

    /**
     * Bakes the difficulty weapon's attribute loadout into the entity itself.
     *
     * <p>The values are read from the weapon the encounter holds, never typed out here: the item
     * stays the definition, the entity carries the result. Attack damage, attack speed and any
     * other attribute the weapon provides therefore survive a disarm, an inventory swap or a mod
     * that deletes the held item, and {@code attackDamage()} keeps reading the plain vanilla
     * attribute like every other mob.
     *
     * <p>The copies get their own ids, and {@link #suppressHeldItemAttributes()} removes the
     * weapon's own equipment modifiers every tick, so the same bonus is never counted twice.
     *
     * <p>Every id is cleared before it is written again: permanent modifiers are part of the
     * entity's saved attributes, so after a reload the attribute map already holds them while the
     * in-memory list below is empty. Adding a modifier whose id is present throws, which is
     * exactly what a boss reloading from disk used to do.
     */
    private void applyWeaponAttributes() {
        for (AppliedAttribute applied : appliedWeaponAttributes) {
            AttributeInstance instance = getAttribute(applied.attribute());
            if (instance != null) {
                instance.removeModifier(applied.id());
            }
        }
        appliedWeaponAttributes.clear();
        int slot = 0;
        for (var entry : createWeaponForDifficulty(bossDifficulty)
                .getAttributeModifiers(EquipmentSlot.MAINHAND).entries()) {
            UUID id = weaponModifierId(slot++);
            AttributeInstance instance = getAttribute(entry.getKey());
            if (instance == null) {
                // The entity simply does not carry that attribute (modded ones such as Lodestone's
                // magic damage are player only); there is nothing to bake in.
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

    /** Stable id per loadout slot, so a re-apply always clears the modifier it wrote before. */
    private static UUID weaponModifierId(int slot) {
        return new UUID(WEAPON_ATTRIBUTE_ID.getMostSignificantBits(),
                WEAPON_ATTRIBUTE_ID.getLeastSignificantBits() + slot);
    }

    /**
     * Strips whatever the hand currently provides. The encounter's own copies are permanent, so
     * nothing equipped may add to them; this also covers a replacement item dropped in by another
     * mod before the guard restores the real weapon.
     */
    private void suppressHeldItemAttributes() {
        ItemStack held = getMainHandItem();
        if (held.isEmpty()) {
            return;
        }
        getAttributes().removeAttributeModifiers(
                held.getAttributeModifiers(EquipmentSlot.MAINHAND));
    }

    /** One baked weapon modifier, remembered so it can be replaced instead of stacking. */
    private record AppliedAttribute(Attribute attribute, UUID id) {
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DATA_STAGE, (byte) VicissitudeBossStage.DORMANT.ordinal());
        entityData.define(DATA_ACTION, (byte) VicissitudeRig.Action.NONE.ordinal());
        entityData.define(DATA_ACTIVE_SIDE, (byte) 0);
        entityData.define(DATA_WEAPON_STATE, (byte) 0);
        // Field initializers have not run yet at this point, so the tier starts as SIMPLE (0).
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

    // ------------------------------------------------------------------ stage and sync

    public VicissitudeBossStage getStage() {
        return VicissitudeBossStage.byId(entityData.get(DATA_STAGE));
    }

    private void setStage(VicissitudeBossStage value) {
        stage = value;
        entityData.set(DATA_STAGE, (byte) value.ordinal());
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
        if (isPhaseTwoVisual()) {
            return 1.0F;
        }
        if (getStage() == VicissitudeBossStage.TRANSITION) {
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
        if (getHealth() > 0.0F || deathTime <= 0) {
            return -1.0F;
        }
        return deathTime;
    }

    public float getCoreGlow() {
        return entityData.get(DATA_CORE_GLOW);
    }

    /** Action clock in ticks; -1 when no action is running. */
    public float getActionTicks(float partialTick) {
        VicissitudeRig.Action current = getRenderAction();
        if (current == VicissitudeRig.Action.NONE) {
            return -1.0F;
        }
        long start = entityData.get(DATA_ACTION_START);
        return (float) (level().getGameTime() - start) + partialTick;
    }

    public int getActionSequence() {
        return entityData.get(DATA_ACTION_SEQUENCE);
    }

    /** True while the encounter is still the phase one pressure state. */
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

    /**
     * The stack the renderer draws, rebuilt from the synced tier rather than read from the hand.
     *
     * <p>Disarm effects, inventory swaps and mods that delete held items therefore cannot leave
     * the encounter visibly unarmed; the weapon on screen and the weapon in the hand are two
     * separate things by design.
     */
    public ItemStack getDisplayWeapon() {
        int tier = entityData.get(DATA_WEAPON_TIER);
        if (tier != displayWeaponTier || displayWeapon.isEmpty()) {
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

    /** Starts a new action; the release frame is executed exactly once by the action tick. */
    private void startAction(VicissitudeRig.Action next, boolean left) {
        action = next;
        actionLeft = left;
        actionReleased = false;
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

    // ------------------------------------------------------------------ tick

    @Override
    public void onAddedToWorld() {
        if (!level().isClientSide) {
            // The base class captures the health pool into the vitality ledger from here on, so
            // the difficulty modifier has to be in place before that call.
            applyDifficultyAttributes();
            difficultyLocked = true;
            if (entityData.get(DATA_WEAPON_STATE) == 1) {
                // A save loaded with the weapon already in hand never runs the equip step again,
                // so the weapon's attribute loadout is rebuilt here.
                applyWeaponAttributes();
            }
        }
        super.onAddedToWorld();
        if (!level().isClientSide && Double.isNaN(idleHoverY)) {
            idleHoverY = Math.min(level().getMaxBuildHeight() - getBbHeight() - 1.0D,
                    getY() + HOVER_HEIGHT);
            if (phaseOneDurationTicks <= 0) {
                phaseOneDurationTicks = bossDifficulty.phaseOneDurationTicks();
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
        if (level().isClientSide) {
            return;
        }
        syncDerivedState();
        bossEvent.setName(getDisplayName());
        bossEvent.setProgress(Mth.clamp(getHealth() / getMaxHealth(), 0.0F, 1.0F));
        if (getHealth() <= 0.0F) {
            return;
        }
        tickFacing();
        if (tickCount % 10 == 0) {
            forgetDeadParticipants();
        }
        if (tickCount % WEAPON_GUARD_INTERVAL == 0) {
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

    /** Aim point plus eye height the body should be oriented towards this tick. */
    private record FacingAim(Vec3 point, double eyeY) {
    }

    /**
     * Facing for several targets: a distance weighted blend of every engaged player inside the
     * engagement radius, so with two or three opponents the body settles on the group instead of
     * snapping between individuals every time the attack target rotates. During an attack the
     * committed target takes precedence, followed by a fixed direction through the recovery.
     */
    @Nullable
    private FacingAim blendedFacingAim() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        Set<UUID> roster = isPhaseTwo() ? phaseTwoParticipants : phaseOneTargets;
        if (roster.isEmpty()) {
            return null;
        }
        double range = engagementRange();
        Vec3 direction = Vec3.ZERO;
        double eyeSum = 0.0D;
        double weightSum = 0.0D;
        for (UUID identity : roster) {
            LivingEntity member = resolveParticipant(serverLevel, identity);
            if (member == null || !member.isAlive() || member.level() != level()
                || isIgnoredPlayer(member)) {
                continue;
            }
            Vec3 offset = member.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D);
            double distance = offset.length();
            if (distance < 0.05D || distance > range) {
                continue;
            }
            double weight = 1.0D / (1.0D + distance * 0.15D);
            direction = direction.add(offset.normalize().scale(weight));
            eyeSum += member.getEyeY() * weight;
            weightSum += weight;
        }
        if (weightSum <= 0.0D || direction.lengthSqr() < 1.0E-4D) {
            return null;
        }
        return new FacingAim(position().add(direction.normalize().scale(4.0D)), eyeSum / weightSum);
    }

    @Nullable
    private LivingEntity resolveParticipant(ServerLevel serverLevel, UUID identity) {
        ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(identity);
        if (player != null) {
            return player;
        }
        Entity entity = serverLevel.getEntity(identity);
        return entity instanceof LivingEntity living ? living : null;
    }

    /** Configurable aggro radius; the boss never starts or keeps a fight beyond it. */
    private static double engagementRange() {
        try {
            return MaledictConfig.VICISSITUDE_ENGAGEMENT_RANGE.get();
        } catch (IllegalStateException notLoaded) {
            return 12.0D;
        }
    }

    private double engagementRangeSqr() {
        double range = engagementRange();
        return range * range;
    }

    /**
     * 每 10 tick 清一次参战名单，规则只有一条：**不在场的人退出名单**。
     *
     * <p>不在场 = 解不开 UUID（离线、所在区块没加载、已经不在世界上）、不在这个维度、
     * 或者已经死了。玩家额外走原版的死亡计数规则（{@code forgiveDeadPlayers} 决定阵亡后
     * 是原谅还是继续记仇），并且在被移出时一并删掉死亡记录与公告记录，不留悬空 UUID。
     *
     * <p>离线玩家同样退出名单：饰品返还走的是账本 + 登录/重生/克隆钩子
     * （{@code VicissitudeCurioReturns}），不依赖这份名单，所以这里可以放心清干净。
     */
    private void forgetDeadParticipants() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean forgive = level().getGameRules().getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS);
        var playerList = serverLevel.getServer().getPlayerList();
        for (UUID identity : new ArrayList<>(phaseOneTargets)) {
            if (updateParticipant(playerList.getPlayer(identity), identity, forgive,
                    targetPlayerDeaths, serverLevel)) {
                phaseOneTargets.remove(identity);
                announcedPlayerDeaths.remove(identity);
            }
        }
        for (UUID identity : new ArrayList<>(phaseTwoParticipants)) {
            if (updateParticipant(playerList.getPlayer(identity), identity, forgive,
                    phaseTwoPlayerDeaths, serverLevel)) {
                phaseTwoParticipants.remove(identity);
            }
        }
    }

    private boolean updateParticipant(@Nullable ServerPlayer player, UUID identity, boolean forgive,
                                      Map<UUID, Integer> deathRecords, ServerLevel serverLevel) {
        if (player == null) {
            // 不是在线玩家：解不开、别的维度、死了都算不在场。
            return dropAbsentParticipant(identity, serverLevel, deathRecords);
        }
        if (player.level() != level()) {
            // Leaving the dimension always ends the engagement.
            deathRecords.remove(identity);
            clearTargetIf(identity);
            return true;
        }
        Integer recorded = deathRecords.get(identity);
        if (recorded != null && recorded != getDeathCount(player)) {
            if (forgive) {
                deathRecords.remove(identity);
                clearTargetIf(identity);
                return true;
            }
            // Angry mobs keep hunting the same player after respawn.
            deathRecords.put(identity, getDeathCount(player));
            return false;
        }
        if (!player.isAlive()) {
            // A dead entity is never an active target, with or without forgiveness.
            clearTargetIf(identity);
        }
        return false;
    }

    /**
     * 非玩家参战者与离线玩家的共同清理：拿不到活的实体就退出名单。
     *
     * <p>这里不区分"区块没加载"与"已经不存在"——两者都不在场，留着只会变成悬空 UUID，
     * 还会让 {@link #hasPossibleOpponent()} 误以为还有对手。
     */
    private boolean dropAbsentParticipant(UUID identity, ServerLevel serverLevel,
                                          Map<UUID, Integer> deathRecords) {
        Entity entity = serverLevel.getEntity(identity);
        if (entity instanceof LivingEntity living && living.isAlive() && living.level() == level()) {
            return false;
        }
        deathRecords.remove(identity);
        clearTargetIf(identity);
        return true;
    }

    private void clearTargetIf(UUID identity) {
        LivingEntity target = getTarget();
        if (target != null && identity.equals(target.getUUID())) {
            setTarget(null);
        }
    }

    private static final float YAW_RATE_TRACKING = 20.0F;
    /** Early windup catches the intended target before the direction locks. */
    private static final float YAW_RATE_WINDUP = 30.0F;
    /** A target far off axis gets a speed boost, so nobody can orbit faster than the body turns. */
    private static final float YAW_CATCH_UP_ARC = 90.0F;
    private static final float YAW_CATCH_UP_BOOST = 1.8F;

    /**
     * The boss owns its facing: it always turns towards the current target at a bounded rate.
     * Vanilla look control plus path following used to fight over the yaw, which snapped the
     * body around and left the model flying backwards; doing it here, after super.tick(), makes
     * the facing both smooth and deterministic.
     */
    private void tickFacing() {
        // Summoned but not yet provoked: an idol has no reason to follow anyone around.
        if (stage == VicissitudeBossStage.DORMANT || getHealth() <= 0.0F) {
            return;
        }
        if (action != VicissitudeRig.Action.NONE && actionTicks() >= action.aimLockTick()) {
            // Restore after vanilla movement/body controls, which also run during super.tick().
            setYRot(committedYaw);
            setYHeadRot(committedYaw);
            yBodyRot = committedYaw;
            setXRot(committedPitch);
            return;
        }
        FacingAim facing = action != VicissitudeRig.Action.NONE && committedTarget != null
                ? new FacingAim(aimPoint(committedTarget), committedTarget.getEyeY())
                : blendedFacingAim();
        if (facing == null) {
            return;
        }
        Vec3 aim = facing.point();
        if (action != VicissitudeRig.Action.NONE) {
            committedAim = aim;
        }
        double aimEyeY = facing.eyeY();
        float rate = YAW_RATE_TRACKING;
        if (action != VicissitudeRig.Action.NONE) {
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
        if (hurtTicks > 0) {
            hurtTicks--;
        }
        entityData.set(DATA_HURT_TICKS, hurtTicks);
        entityData.set(DATA_WING_FOLD, wingFold);
        entityData.set(DATA_CORE_GLOW, computeCoreGlow());
    }

    private float computeCoreGlow() {
        if (getHealth() <= 0.0F) {
            float fade = Mth.clamp((deathTime - 60.0F) / 19.0F, 0.0F, 1.0F);
            return Math.max(0.0F, 1.0F - fade);
        }
        if (stage == VicissitudeBossStage.TRANSITION) {
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
        pressRecoveryTicks = Math.max(0, pressRecoveryTicks - 1);
    }

    private void tickStage() {
        switch (stage) {
            case DORMANT, PHASE_ONE -> {
                if (!isPhaseOneStarted()) {
                    break;
                }
                if (tickEmptyEncounter()) {
                    break;
                }
                phaseOneTicks++;
                if (phaseOneTicks >= phaseOneDurationTicks) {
                    beginTransition();
                }
            }
            case TRANSITION -> {
                transitionTicks++;
                entityData.set(DATA_TRANSITION_TICKS, transitionTicks);
                tickTransitionEffects();
                if (transitionTicks >= TRANSITION_TICKS) {
                    completeTransition();
                }
            }
            case PHASE_TWO -> {
                destroyBlockingEntities();
                if ((horizontalCollision || verticalCollision)
                    && tickCount % BLOCK_BREAK_INTERVAL == 0) {
                    destroyBlockingBlocks();
                }
            }
            case DYING -> {
            }
        }
    }

    /**
     * 一阶段没有对手时倒数，数满就把遭遇战放回「未参战」，并把这份名单清干净。
     *
     * <p>判断的是「场上还有没有活的对手」，而不是名单里有没有 UUID：名单里的创造/旁观
     * 玩家（打到一半切了模式）不算对手；解不开 UUID 的会在
     * {@link #forgetDeadParticipants()} 里被清掉，这里只是不等它那 10 tick。
     *
     * <p>回到 DORMANT 只是解除阶段时长锁与计时，不动真生命、难度与武器。
     *
     * @return 这一 tick 是否已经放回未参战（调用方不再推进阶段计时）
     */
    private boolean tickEmptyEncounter() {
        if (hasPossibleOpponent()) {
            emptyEncounterTicks = 0;
            return false;
        }
        if (++emptyEncounterTicks < EMPTY_ENCOUNTER_RESET_TICKS) {
            return false;
        }
        emptyEncounterTicks = 0;
        setStage(VicissitudeBossStage.DORMANT);
        phaseOneDurationLocked = false;
        phaseOneTicks = 0;
        // 名单、死亡记录一起清：不放着任何指向"上一场"的 UUID。
        // 二阶段名单在一阶段本就应当是空的，顺手清掉以防旧档带来残留。
        phaseOneTargets.clear();
        targetPlayerDeaths.clear();
        phaseTwoParticipants.clear();
        phaseTwoPlayerDeaths.clear();
        setTarget(null);
        clearGroundMarker();
        return true;
    }

    /** 场上还有没有活着的、可以被当成对手的参战者，见 {@link #tickEmptyEncounter()}。 */
    private boolean hasPossibleOpponent() {
        if (phaseOneTargets.isEmpty()) {
            return false;
        }
        if (!(level() instanceof ServerLevel serverLevel)) {
            // 客户端不会走到这里（tickStage 只在服务端推进）；拿不到世界时按"还有对手"处理。
            return true;
        }
        for (UUID identity : phaseOneTargets) {
            Entity entity = serverLevel.getEntity(identity);
            if (entity instanceof LivingEntity living && living.isAlive()
                && living.level() == level() && !isIgnoredPlayer(living)) {
                return true;
            }
        }
        return false;
    }

    private void tickTransitionEffects() {
        // 0-14 hover and stop, 15-29 fold wings and offset the ring/shell, 30-44 press the
        // feathers down, 45 equip, 45-59 blend into the phase two pose.
        if (transitionTicks == 1) {
            setDeltaMovement(Vec3.ZERO);
            getNavigation().stop();
            sendEvent(VicissitudeEffectPacket.EVENT_TRANSITION_FLASH, position());
        }
        if (transitionTicks == 30) {
            SoundHelper.playSound(this, (SoundEvent) SoundRegistry.SOUL_SHATTER.get(), 1.2F,
                    RandomHelper.randomBetween(level().getRandom(), 0.7F, 0.9F));
        }
        if (transitionTicks == 45) {
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

    /**
     * The transition wipes leftover phase one attack entities and warnings, so the second phase
     * can never keep pressing health through old orbs.
     */
    private void clearPhaseOneProjectiles() {
        if (!(level() instanceof ServerLevel serverLevel)) {
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
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Set<ServerPlayer> recipients = new LinkedHashSet<>(bossEvent.getPlayers());
        phaseTwoParticipants.clear();
        for (UUID identity : phaseOneTargets) {
            LivingEntity member = resolveParticipant(serverLevel, identity);
            if (member == null || member.level() != level() || !member.isAlive()) {
                // 死掉或已经不在这个维度的不带走；离线的玩家留给账本处理返还。
                continue;
            }
            if (member instanceof ServerPlayer player) {
                recipients.add(player);
                // 名单里可能有中途切进创造/旁观的玩家：他们不带走，也不没收饰品。
                if (isIgnoredPlayer(player)) {
                    continue;
                }
                phaseTwoParticipants.add(identity);
                phaseTwoPlayerDeaths.put(identity, getDeathCount(player));
                if (bossDifficulty.confiscatesCurios()) {
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
        if (action == VicissitudeRig.Action.NONE) {
            return;
        }
        int ticks = actionTicks();
        if (action != VicissitudeRig.Action.SCYTHE_RECOVER && (committedTarget == null
                || !committedTarget.isAlive() || committedTarget.level() != level()
                || isIgnoredPlayer(committedTarget))) {
            endAction();
            return;
        }
        if (ticks >= action.duration()) {
            finishAction();
            return;
        }
        if (!actionReleased && ticks >= action.releaseTick()) {
            actionReleased = true;
            releaseAction();
        }
        if (action == VicissitudeRig.Action.DASH) {
            tickDash(ticks);
        }
    }

    private void finishAction() {
        VicissitudeRig.Action finished = action;
        endAction();
        if (finished == VicissitudeRig.Action.SCYTHE_RECOVER) {
            pendingWeaponTier = -1;
        }
    }

    // ------------------------------------------------------------------ combat scheduling

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
        if (action != VicissitudeRig.Action.NONE) {
            return;
        }
        if (pressRecoveryTicks > 0) {
            // 压血刚落地：这一槽不开始新动作，下一次释放自然被推后。
            return;
        }
        if (phaseOneTicks < PHASE_ONE_WARMUP_TICKS) {
            return;
        }
        if ((phaseOneTicks - PHASE_ONE_WARMUP_TICKS) % BASE_ATTACK_INTERVAL_TICKS != 0) {
            return;
        }
        LivingEntity target = currentPhaseOneTarget();
        if (target == null) {
            return;
        }
        if (distanceTo(target) > NO_FIRE_RANGE) {
            // Same rule as phase two: close the gap first. Phase one bolts only live long enough
            // for about 36 blocks, so firing from across the arena would only spawn duds.
            return;
        }
        currentBaseSlot = baseSlotIndex++;
        // Every other slot may be a special skill. Alternating instead of always preferring a
        // special keeps the wing fan the main source of pressure, as 07 requires.
        if (currentBaseSlot % 2 == 0 && tryRangedSpecialSkill(target)) {
            return;
        }
        fanCount++;
        startAction(VicissitudeRig.Action.WING_RANGED, (currentBaseSlot / 2) % 2 == 0);
    }

    /** Barrage, chest mark and halo verdict rotate; reusable in phase two as ranged options. */
    private boolean tryRangedSpecialSkill(LivingEntity target) {
        for (int attempt = 0; attempt < 3; attempt++) {
            int choice = specialRotator % 3;
            specialRotator++;
            switch (choice) {
                case 0 -> {
                    if (barrageCooldown <= 0) {
                        barrageCooldown = WING_BARRAGE_COOLDOWN;
                        startAction(VicissitudeRig.Action.WING_BARRAGE, true);
                        return true;
                    }
                }
                case 1 -> {
                    if (chestCooldown <= 0 && findGroundPoint(target) != null) {
                        chestCooldown = CHEST_CAST_COOLDOWN;
                        startAction(VicissitudeRig.Action.CAST_FROM_CHEST, false);
                        return true;
                    }
                }
                default -> {
                    if (haloCooldown <= 0 && findGroundPoint(target) != null) {
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
        if (action != VicissitudeRig.Action.NONE) {
            return;
        }
        if (scytheRecoveryPending) {
            scytheRecoveryPending = false;
            startAction(VicissitudeRig.Action.SCYTHE_RECOVER, false);
            return;
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || target.level() != level()
            || isIgnoredPlayer(target)) {
            target = selectBalancedPhaseTwoTarget();
        }
        if (target == null) {
            return;
        }
        double distance = distanceTo(target);
        if (distance > NO_FIRE_RANGE) {
            return;
        }
        if (distance > THROW_MAX_RANGE) {
            if (tryRangedSpecialSkill(target)) {
                return;
            }
            if (rangedCooldown <= 0) {
                rangedCooldown = RANGED_FALLBACK_COOLDOWN;
                startAction(VicissitudeRig.Action.RANGED_FALLBACK, baseSlotIndex++ % 2 == 0);
            }
            return;
        }
        if (distance >= THROW_MIN_RANGE) {
            if (throwCooldown <= 0 && hasWeaponInHand() && scytheToken == null
                && hasLineOfSight(target)) {
                throwCooldown = THROW_COOLDOWN;
                startAction(VicissitudeRig.Action.SCYTHE_THROW, false);
                return;
            }
            if (dashCooldown <= 0 && hasWeaponInHand() && scytheToken == null
                    && distance >= DASH_MIN_RANGE && distance <= DASH_MAX_RANGE
                    && hasLineOfSight(target)) {
                dashCooldown = DASH_COOLDOWN;
                startAction(VicissitudeRig.Action.DASH, false);
                return;
            }
            // Give a ready pursuit its turn before the ranged rotation consumes the opening.
            if (tryRangedSpecialSkill(target)) {
                return;
            }
            if (rangedCooldown <= 0) {
                rangedCooldown = RANGED_FALLBACK_COOLDOWN;
                startAction(VicissitudeRig.Action.RANGED_FALLBACK, baseSlotIndex++ % 2 == 0);
            }
            return;
        }
        if (distance <= MELEE_REACH + getBbWidth() * 0.5D) {
            commandMelee(target);
        }
    }

    private void commandMelee(LivingEntity target) {
        if (scytheToken != null) {
            return;
        }
        if (heavyCooldown <= 0) {
            heavyCooldown = HEAVY_ATTACK_COOLDOWN;
            startAction(VicissitudeRig.Action.HEAVY_ATTACK, false);
            return;
        }
        if ((meleeAlternator + 1) % 3 == 0) {
            if (verticalSlashCooldown > 0) {
                return;
            }
            verticalSlashCooldown = VERTICAL_SLASH_COOLDOWN;
            meleeAlternator++;
            startAction(VicissitudeRig.Action.SLASH_VERTICAL, false);
            return;
        }
        if (slashCooldown > 0) {
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

    // ------------------------------------------------------------------ release events

    private void releaseAction() {
        LivingEntity target = committedTarget;
        switch (action) {
            case WING_RANGED -> fireFan(target);
            case WING_BARRAGE -> fireBarrageWave(target, 0);
            case CAST_FROM_CHEST -> resolveChestMark();
            case CAST_FROM_HALO -> resolveHaloVerdict();
            case SLASH_HORIZONTAL -> resolveMelee(100.0F, MELEE_REACH, 1.0F, false);
            case SLASH_VERTICAL -> resolveVerticalSlash();
            case HEAVY_ATTACK -> resolveMelee(120.0F, MELEE_REACH, 1.75F, true);
            case SCYTHE_THROW -> throwScythe(target);
            case RANGED_FALLBACK -> fireFallbackVolley(target);
            case DASH -> {
            }
            default -> {
            }
        }
    }

    private void fireFan(@Nullable LivingEntity target) {
        if (target == null || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean left = actionLeft;
        boolean homingSlot = fanCount % 2 == 1 && isPhaseOneStage();
        Vec3 aim = committedAim;
        for (int index = 0; index < 5; index++) {
            float offset = -24.0F + index * 12.0F;
            Vec3 origin = wingOrigin(left, index);
            Vec3 direction = aimedDirection(origin, aim, offset);
            if (index == 2 && homingSlot) {
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
                || isIgnoredPlayer(target) || !(level() instanceof ServerLevel serverLevel)) {
            // No live target: the wave is dropped instead of being fired into empty air.
            barrageWaveTick = 0L;
            lastBarrageWave = 0;
            return;
        }
        Vec3 aim = committedAim;
        double distance = position().distanceTo(committedAim);
        double spread = Math.min(30.0D, 6.0D + distance * 1.5D);
        for (int index = 0; index < 4; index++) {
            // A deliberate center gap keeps at least one passable lane in every wave.
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
        if (wave < 2) {
            lastBarrageWave = wave + 1;
            barrageWaveTick = level().getGameTime() + 8;
        } else {
            barrageWaveTick = 0L;
        }
    }

    private int lastBarrageWave;
    private long barrageWaveTick;

    private void tickBarrageWaves() {
        if (action != VicissitudeRig.Action.WING_BARRAGE || barrageWaveTick == 0L) {
            return;
        }
        if (level().getGameTime() >= barrageWaveTick && lastBarrageWave <= 2) {
            fireBarrageWave(committedTarget, lastBarrageWave);
        }
    }

    private void fireFallbackVolley(@Nullable LivingEntity target) {
        if (target == null || !(level() instanceof ServerLevel serverLevel)) {
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

    /**
     * {@code pressInPhaseOne} is the authored phase one behaviour; in phase two the same volley
     * becomes ordinary damage, which is what lets the wing/chest/halo skills be reused there.
     */
    private void spawnBolt(ServerLevel serverLevel, Vec3 origin, Vec3 motion, boolean pressInPhaseOne,
                           float damage, int life) {
        boolean press = pressInPhaseOne && isPhaseOneStage();
        float actualDamage = press ? 0.0F : (damage > 0.0F ? damage : attackDamage() * 0.75F);
        if (countBolts(serverLevel, false) >= MAX_NON_HOMING_BOLTS) {
            // The cap is a hard skip: bolts are never queued for later.
            return;
        }
        VicissitudeSpiritBoltEntity bolt = new VicissitudeSpiritBoltEntity(serverLevel, this,
                origin, motion, press, actualDamage, life);
        serverLevel.addFreshEntity(bolt);
    }

    private void spawnHomingOrb(ServerLevel serverLevel, LivingEntity target, Vec3 origin) {
        if (countBolts(serverLevel, true) >= MAX_HOMING_ORBS) {
            return;
        }
        VicissitudeLightOrbEntity orb = new VicissitudeLightOrbEntity(serverLevel, this, target, origin);
        serverLevel.addFreshEntity(orb);
    }

    private int countBolts(ServerLevel serverLevel, boolean homing) {
        AABB area = getBoundingBox().inflate(96.0D);
        if (homing) {
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
            // Judgement is the drawn circle, not the square that was used to find candidates.
            if (victim.distanceToSqr(center.x, victim.getY(), center.z) > radius * radius) {
                continue;
            }
            hurtBySkill(victim, damage, true);
        }
        if (level() instanceof ServerLevel serverLevel) {
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
            if (distance < inner || distance > outer) {
                // Centre and everything outside the ring are safe, matching the drawn geometry.
                continue;
            }
            float angle = (float) Math.toDegrees(Math.atan2(victim.getZ() - center.z,
                    victim.getX() - center.x));
            if (Math.abs(Mth.wrapDegrees(angle - gapCenter)) < gapWidth * 0.5F) {
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
            if (entity instanceof LivingEntity living && isValidCombatParticipant(living)) {
                targets.add(living);
            }
        }
        return targets;
    }

    private void resolveMelee(float arcDegrees, double reach, float multiplier, boolean heavy) {
        Vec3 look = getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize();
        float damage = attackDamage() * multiplier;
        boolean hitAny = false;
        for (Entity entity : level().getEntities(this,
                getBoundingBox().inflate(reach, 2.0D, reach))) {
            if (!(entity instanceof LivingEntity living) || !isValidCombatParticipant(living)) {
                continue;
            }
            Vec3 offset = living.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D);
            if (offset.length() > reach + living.getBbWidth() * 0.5D || offset.lengthSqr() < 1.0E-4D) {
                continue;
            }
            double angle = Math.toDegrees(Math.acos(Mth.clamp(
                    offset.normalize().dot(look), -1.0D, 1.0D)));
            if (angle > arcDegrees * 0.5D || !hasLineOfSight(living)) {
                continue;
            }
            hitAny |= hurtBySkill(living, damage, false);
        }
        spawnSlashEffect(heavy);
        if (hitAny) {
            swing(InteractionHand.MAIN_HAND, true);
            if (heavy) {
                sendEvent(VicissitudeEffectPacket.EVENT_HEAVY_IMPACT,
                        position().add(look.scale(2.0D)).add(0.0D, getBbHeight() * 0.4D, 0.0D));
            }
        }
    }

    private void resolveVerticalSlash() {
        Vec3 look = getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize();
        Vec3 right = new Vec3(-look.z, 0.0D, look.x);
        float damage = attackDamage() * 1.25F;
        for (Entity entity : level().getEntities(this, getBoundingBox().inflate(MELEE_REACH, 2.0D,
                MELEE_REACH))) {
            if (!(entity instanceof LivingEntity living) || !isValidCombatParticipant(living)) {
                continue;
            }
            Vec3 offset = living.position().subtract(position());
            double forward = offset.x * look.x + offset.z * look.z;
            double lateral = Math.abs(offset.x * right.x + offset.z * right.z);
            if (forward < 0.0D || forward > MELEE_REACH || lateral > 1.5D
                    || !hasLineOfSight(living)) {
                continue;
            }
            hurtBySkill(living, damage, false);
        }
        spawnSlashEffect(true);
    }

    /**
     * 一阶段的压血弹（扇射球与追踪球）把玩家压到 1 血时，由弹体回调这里。
     *
     * <p>压血本身不掉血，真正会杀人的只有胸/环那两次普通伤害；而追踪球可以飞 200 tick，
     * 往往正好在释放帧前几 tick 才落地，于是「压到 1 血」和「释放」看起来是同一瞬间发生的。
     * 处理办法是把这次喘息记下来：{@link #PRESS_RECOVERY_TICKS} 之内不再开始新动作，
     * 并且把已经起手、还没释放的胸部/环技能直接作废（连预兆一起撤掉），下一轮重新起手。
     * 压血球与羽片齐射本身不造成伤害，不在这里打断。
     *
     * <p>只对玩家生效：宠物与召唤物被压血不需要这个窗口。
     */
    public void onPressLanded(LivingEntity victim) {
        if (!isPhaseOne() || !(victim instanceof Player)) {
            return;
        }
        pressRecoveryTicks = Math.max(pressRecoveryTicks, PRESS_RECOVERY_TICKS);
        if (action != VicissitudeRig.Action.NONE && !actionReleased && dealsPlayerDamage(action)) {
            endAction();
        }
    }

    /** 一阶段里唯一会对玩家造成伤害的两个技能，见 {@link #onPressLanded}。 */
    private static boolean dealsPlayerDamage(VicissitudeRig.Action action) {
        return action == VicissitudeRig.Action.CAST_FROM_CHEST
               || action == VicissitudeRig.Action.CAST_FROM_HALO;
    }

    private boolean hurtBySkill(LivingEntity victim, float damage, boolean area) {
        UUID identity = victim.getUUID();
        if (!roundDamagedTargets.add(identity)) {
            // One damage instance per round and target: a fan or wave never double dips.
            return false;
        }
        return hurtParticipant(victim,
                DamageTypeHelper.create(level(), DamageTypeRegistry.SCYTHE_SWEEP, this), damage);
    }

    /**
     * The encounter's only damage entry point for combat participants.
     *
     * <p>Players are hit through the ChangeLib probe ladder. Phase one always uses the light
     * ladder: its damage lands after the press projectiles have already driven the player down to
     * a single hit point, so armour, enchantments and caps keep deciding how much of it lands
     * (see {@link #onPressLanded}). Phase two keeps the per difficulty table of {@code 07}:
     * SIMPLE and DIFFICULT use the light ladder, COMPLETE and EXTREME the medium one. Everything
     * that is not a player is hit normally.
     */
    public boolean hurtParticipant(LivingEntity victim, DamageSource source, float damage) {
        if (damage <= 0.0F || victim.level().isClientSide) {
            return false;
        }
        // Every skill hit lands: an attack is one authored instance, not a stream of ticks.
        victim.invulnerableTime = 0;
        if (!(victim instanceof Player)) {
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
        if (vertical) {
            particles.setVerticalSlashAngle();
        } else {
            particles.setSlashAngle(getYRot());
        }
        particles.setMirrored(actionLeft);
        Vec3 direction = getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        particles.spawnSlashingParticle(level(), position().add(0.0D, getBbHeight() * 0.6D, 0.0D),
                direction);
    }

    // ------------------------------------------------------------------ dash

    private void tickDash(int ticks) {
        if (ticks < action.releaseTick()) {
            return;
        }
        if (!dashDirectionLocked) {
            dashDirectionLocked = true;
            dashHitTargets.clear();
            dashTicks = 0;
            Vec3 look = getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize();
            dashDirection = look.lengthSqr() < 1.0E-4D ? Vec3.ZERO : look;
        }
        if (dashTicks >= 12 || dashDirection == Vec3.ZERO) {
            setDeltaMovement(getDeltaMovement().scale(0.4D));
            return;
        }
        dashTicks++;
        Vec3 step = dashDirection.scale(DASH_DISTANCE / 12.0D);
        if (level().clip(new net.minecraft.world.level.ClipContext(position(),
                position().add(step), net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE, this))
                .getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
            // A solid obstacle stops the charge; the boss never teleports through walls.
            setDeltaMovement(Vec3.ZERO);
            dashTicks = 12;
            return;
        }
        if (!level().noCollision(this, getBoundingBox().expandTowards(step))) {
            dashTicks = 12;
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        setDeltaMovement(Vec3.ZERO);
        setPos(getX() + step.x, getY() + step.y, getZ() + step.z);
        AABB sweep = getBoundingBox().inflate(0.6D);
        for (Entity entity : level().getEntities(this, sweep)) {
            if (!(entity instanceof LivingEntity living) || !isValidCombatParticipant(living)) {
                continue;
            }
            if (dashHitTargets.add(living.getUUID())) {
                hurtBySkill(living, attackDamage(), false);
            }
        }
    }

    // ------------------------------------------------------------------ scythe throw

    private boolean hasWeaponInHand() {
        return !getMainHandItem().isEmpty();
    }

    private void throwScythe(@Nullable LivingEntity target) {
        if (!(level() instanceof ServerLevel serverLevel) || scytheToken != null) {
            return;
        }
        ItemStack weapon = getMainHandItem();
        if (weapon.isEmpty()) {
            // A disarm must not skip the throw either: fall back to the code owned copy.
            weapon = createWeaponForDifficulty(bossDifficulty);
        }
        Vec3 direction = committedAim.subtract(handAnchorWorldPosition()).normalize();
        VicissitudeScytheProjectileEntity scythe = new VicissitudeScytheProjectileEntity(
                serverLevel, this, handAnchorWorldPosition(), direction, weapon, attackDamage());
        if (!serverLevel.addFreshEntity(scythe)) {
            // Only a successful spawn may empty the hand.
            return;
        }
        stashedWeapon = weapon.copy();
        setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        scytheToken = scythe.getUUID();
        setWeaponState(2);
        SoundHelper.playSound(this, (SoundEvent) SoundRegistry.SCYTHE_THROW.get(), 1.1F,
                RandomHelper.randomBetween(level().getRandom(), 0.8F, 1.0F));
    }

    /** Called by the projectile when it is caught or gives up. */
    public void onScytheReturned(VicissitudeScytheProjectileEntity projectile, boolean caught) {
        if (scytheToken == null || !scytheToken.equals(projectile.getUUID())) {
            return;
        }
        scytheToken = null;
        ItemStack stack = stashedWeapon.isEmpty() ? projectile.getItem() : stashedWeapon;
        stashedWeapon = ItemStack.EMPTY;
        if (!stack.isEmpty()) {
            setItemSlot(EquipmentSlot.MAINHAND, stack.copy());
        }
        setWeaponState(1);
        // A catch must not erase a ground tell or truncate a volley already in progress.
        scytheRecoveryPending = true;
        if (caught) {
            SoundHelper.playSound(this, (SoundEvent) SoundRegistry.SCYTHE_CATCH.get(), 1.0F,
                    RandomHelper.randomBetween(level().getRandom(), 0.9F, 1.1F));
            sendEvent(VicissitudeEffectPacket.EVENT_SCYTHE_CATCH, handAnchorWorldPosition());
        }
        applyDeferredWeaponTier();
    }

    private void cancelFlyingScythe() {
        if (scytheToken == null || !(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        Entity entity = serverLevel.getEntity(scytheToken);
        if (entity != null) {
            entity.discard();
        }
        scytheToken = null;
        // The stash is the authoritative copy. A copy that was lost, disarmed or replaced while
        // the weapon flew is re-issued from code instead of leaving the encounter empty handed.
        ItemStack restored = stashedWeapon.isEmpty()
                             ? (getHealth() > 0.0F ? createWeaponForDifficulty(bossDifficulty)
                                                   : ItemStack.EMPTY)
                             : stashedWeapon.copy();
        stashedWeapon = ItemStack.EMPTY;
        setItemSlot(EquipmentSlot.MAINHAND, restored);
        setWeaponTier(bossDifficulty);
        setWeaponState(restored.isEmpty() ? 0 : 1);
    }

    /**
     * 常规掉落物表跟着难度走：四档各有一张表，内容见 {@code MaledictEntityLoot}
     * （珍金块、虚无板石、虚空盐）。
     *
     * <p>盖的是 {@code Mob#getDefaultLootTable}：Mob 把 {@code getLootTable()} 定成了 final，
     * 实体自己带一份显式表（NBT 上的 {@code DeathLootTable}）时才不经过这里——本实体不会带。
     *
     * <p>选表发生在死亡时，读的是实体自己那份权威难度（存档里缺失时已经退到 SIMPLE），
     * 所以旧存档不会因为找不到难度字段而掉不出东西。
     */
    @Override
    protected ResourceLocation getDefaultLootTable() {
        return bossDifficulty.lootTable();
    }

    /**
     * 启蒙之年的专属额外掉落，写法照抄下界之星：原版凋灵在 {@code dropCustomDeathLoot} 里
     * 把下界之星放进世界并让它不自然消失，这里同样——只不过额外交给 {@code recentlyHit} 把关。
     *
     * <p>{@code recentlyHit} 是原版 {@code lastHurtByPlayerTime > 0}，也就是最后 100 tick 内
     * 有玩家（含玩家射出的弹体、玩家驯服的宠物）对它造成过伤害：不是玩家杀的就不发。
     * 等级仍跟难度走（I–IV 级），见 {@link BossDifficulty#enlightenmentLevel()}。
     *
     * <p>调用时机由原版死亡流程保证：{@code VicissitudeBossEntity#dropAllDeathLoot} 只在真死
     * 那一次放行，所以这里不需要自己记账；{@code doMobLoot} 关闭时同一条路也不会走到。
     */
    @Override
    protected void dropCustomDeathLoot(DamageSource source, int looting, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, looting, recentlyHit);
        if (!recentlyHit) {
            return;
        }
        ItemEntity drop = spawnAtLocation(
                AgeOfEnlightenmentItem.create(bossDifficulty.enlightenmentLevel()));
        if (drop != null) {
            drop.setExtendedLifetime();
        }
    }

    @Override
    protected void onFinalDeath(DamageSource source) {
        cancelFlyingScythe();
        // Everything still held goes to the world ledger; nothing is deleted or dropped here.
        if (level() instanceof ServerLevel serverLevel) {
            for (Map.Entry<UUID, Deque<VicissitudeCurioLedger.Entry>> entry : confiscated.entrySet()) {
                VicissitudeCurioReturns.retain(serverLevel, entry.getKey(),
                        new ArrayList<>(entry.getValue()));
                ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(entry.getKey());
                if (player != null) {
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
        if (!level().isClientSide) {
            setStage(VicissitudeBossStage.DYING);
            if (ticks == 36) {
                // 36-59: wing roots fail, the one death shake.
                sendEvent(VicissitudeEffectPacket.EVENT_DEATH_CORE, position());
            }
            if (ticks == 60) {
                // 60-79: the core goes out quietly.
                sendEvent(VicissitudeEffectPacket.EVENT_DEATH_EXTINGUISH,
                        anchorWorldPosition(VicissitudeRigData.Joint.HEAD_EFFECT_ANCHOR, 0.0F));
            }
        }
    }

    // ------------------------------------------------------------------ curio handling

    private void confiscateEquippedCurios(ServerPlayer player) {
        List<VicissitudeCurioLedger.Entry> taken = VicissitudeCurioReturns.confiscate(player);
        if (taken.isEmpty()) {
            confiscated.remove(player.getUUID());
            return;
        }
        confiscated.put(player.getUUID(), new ArrayDeque<>(taken));
    }

    @Override
    protected void onDamageAccepted(DamageSource source, float amount) {
        hurtTicks = 6;
        if (isPhaseTwo() && bossDifficulty.confiscatesCurios()) {
            returnOneCurio(resolveAttackingPlayer(source));
        }
    }

    private void returnOneCurio(@Nullable ServerPlayer preferredPlayer) {
        if (preferredPlayer != null && returnOneCurioTo(preferredPlayer)) {
            return;
        }
        if (!(level() instanceof ServerLevel serverLevel) || phaseTwoParticipants.isEmpty()) {
            return;
        }
        List<UUID> players = new ArrayList<>(phaseTwoParticipants);
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

    /** Removes a stack only after delivery succeeded, otherwise it stays in the ledger. */
    private boolean returnOneCurioTo(ServerPlayer player) {
        Deque<VicissitudeCurioLedger.Entry> held = confiscated.get(player.getUUID());
        if (held == null || held.isEmpty()) {
            return false;
        }
        // Hand the stack to the ledger first, then try to deliver it: ownership moves exactly
        // once, so nothing can be returned twice or dropped between the two holders.
        VicissitudeCurioLedger.Entry entry = held.removeFirst();
        VicissitudeCurioReturns.retain(player.serverLevel(), player.getUUID(), List.of(entry));
        if (!VicissitudeCurioReturns.deliverAll(player)) {
            // Whatever could not be delivered stays in the ledger; the rest of the hold moves too.
            VicissitudeCurioReturns.retain(player.serverLevel(), player.getUUID(),
                    new ArrayList<>(held));
            held.clear();
            confiscated.remove(player.getUUID());
            return true;
        }
        if (held.isEmpty()) {
            confiscated.remove(player.getUUID());
        }
        return true;
    }

    // ------------------------------------------------------------------ part multipliers

    /**
     * 非玩家来源的伤害固定减半：这个 Boss 只认真打它的人，其余来源（别的生物、环境爆炸、
     * 别人的宠物）吃 50% 减免。部位倍率先算，再乘这一档，所以背刺之类仍然照常生效。
     */
    private static final float NON_PLAYER_DAMAGE_MULTIPLIER = 0.5F;

    @Override
    protected float modifyIncomingDamage(DamageSource source, float amount) {
        List<VicissitudeRig.SegmentVolume> volumes = segmentVolumes();
        Vec3 hit = source.getSourcePosition();
        VicissitudeRig.Segment segment;
        if (hit != null) {
            segment = VicissitudeRig.nearestSegment(volumes, hit.x, hit.y, hit.z);
        } else {
            LivingEntity attacker = resolveLivingAttacker(source);
            segment = attacker == null
                      ? VicissitudeRig.Segment.BODY
                      : VicissitudeRig.nearestSegment(volumes, attacker.getX(),
                              attacker.getEyeY(), attacker.getZ());
        }
        float scaled = amount * segment.multiplier();
        return isPlayerDamage(source) ? scaled : scaled * NON_PLAYER_DAMAGE_MULTIPLIER;
    }

    /**
     * 这一击算不算玩家的账。
     *
     * <p>看 {@code getEntity()}（箭矢这类弹体的出手者就是玩家）与 {@code getDirectEntity()}：
     * 玩家本人近战、玩家射出的弹体、玩家扣下的爆炸都算，玩家宠物与别的生物则不算。
     */
    private static boolean isPlayerDamage(DamageSource source) {
        return source.getEntity() instanceof Player || source.getDirectEntity() instanceof Player;
    }

    private List<VicissitudeRig.SegmentVolume> segmentVolumes() {
        return VicissitudeRig.segmentVolumes(serverPose, getX(), getY(), getZ(), getYRot());
    }

    // ------------------------------------------------------------------ geometry and pose

    private void refreshPose() {
        float death = getHealth() > 0.0F ? -1.0F : deathTime;
        VicissitudeRig.compute(serverPose, isPhaseTwoVisual(), getPhaseTwoBlend(), action,
                actionTicks(), actionLeft, hurtTicks, level().getGameTime(), wingFold, death);
        applyHeadLook(serverPose, action, death, yHeadRot - yBodyRot, getXRot());
    }

    private static void applyHeadLook(VicissitudeRig.Pose pose, VicissitudeRig.Action action,
                                      float death, float yaw, float pitch) {
        if (action == VicissitudeRig.Action.NONE && death < 0.0F) {
            pose.addRotation(VicissitudeRigData.Joint.HEAD_ROOT,
                    pitch * 0.6F, Mth.wrapDegrees(yaw) * 0.5F, 0.0F);
            VicissitudeRig.solve(pose);
        }
    }

    /** Shared render sample for the mesh and effects; callers must not mutate this scratch pose. */
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
        if (!level().isClientSide) {
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

    /** Where the fan converges: the target's torso, not the empty air above it. */
    private static Vec3 aimPoint(LivingEntity target) {
        return target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
    }

    /**
     * Direction from a wing tip to the aim point, spread horizontally by {@code offsetDegrees}.
     * Aiming in three dimensions is what makes part of a volley land on and around the target
     * instead of sailing over its head into the distance.
     */
    private static Vec3 aimedDirection(Vec3 origin, Vec3 aim, float offsetDegrees) {
        Vec3 direction = aim.subtract(origin);
        if (direction.lengthSqr() < 1.0E-4D) {
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

    /**
     * Damage comes from the entity's own attribute, exactly like any other mob. What changed is
     * where the weapon's contribution lives: {@link #applyWeaponAttributes()} bakes the difficulty
     * weapon's modifiers into the entity, so the number no longer depends on the item being in the
     * hand and is never written out by hand here.
     */
    private float attackDamage() {
        return Math.max(1.0F, (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
    }

    private void playSwingSound(boolean left) {
        SoundHelper.playSound(this, (SoundEvent) SoundRegistry.SCYTHE_SWEEP.get(), 0.9F,
                RandomHelper.randomBetween(level().getRandom(), 0.9F, 1.2F));
    }

    // ------------------------------------------------------------------ wing collision and unstick

    private void tickWingCollision() {
        if (!(level() instanceof ServerLevel)) {
            return;
        }
        List<VicissitudeRig.SegmentVolume> volumes = segmentVolumes();
        boolean blocked = false;
        for (VicissitudeRig.SegmentVolume volume : volumes) {
            if (!volume.segment().isWing()) {
                continue;
            }
            AABB box = toAabb(volume.box()).expandTowards(getDeltaMovement());
            if (level().getBlockCollisions(this, box).iterator().hasNext()) {
                blocked = true;
                break;
            }
        }
        wingsBlockedLastTick = blocked;
        if (blocked) {
            // Stop the blocked motion, fold the wings, then keep trying to path around.
            wingFold = Mth.clamp(wingFold + 0.12F, 0.0F, 1.0F);
            setDeltaMovement(getDeltaMovement().scale(0.35D));
        } else {
            wingFold = Mth.clamp(wingFold - 0.06F, 0.0F, 1.0F);
        }
        AABB sweep = getBoundingBox().inflate(6.0D);
        for (Player player : level().getEntitiesOfClass(Player.class, sweep,
                candidate -> candidate.isAlive() && !candidate.isSpectator())) {
            pushOutOfWings(player, volumes);
        }
    }

    /**
     * Only a real overlap with an actual wing segment moves a player, and then only with a short
     * velocity nudge: standing next to the boss must never feel like being bounced away.
     */
    private void pushOutOfWings(Player player, List<VicissitudeRig.SegmentVolume> volumes) {
        AABB playerBox = player.getBoundingBox();
        for (VicissitudeRig.SegmentVolume volume : volumes) {
            if (!volume.segment().isWing()) {
                continue;
            }
            AABB wing = toAabb(volume.box());
            if (!wing.intersects(playerBox)) {
                continue;
            }
            Vec3 away = player.position().subtract(position()).multiply(1.0D, 0.0D, 1.0D);
            if (away.lengthSqr() < 1.0E-4D) {
                away = new Vec3(1.0D, 0.0D, 0.0D);
            }
            away = away.normalize();
            // A short velocity nudge only: no teleport and no forced resync, so brushing a wing
            // never reads as being bounced away from the boss.
            if (!level().noCollision(player, player.getBoundingBox().move(away.scale(0.05D)))) {
                // The player is wedged against a wall: yield with the wing instead of crushing.
                wingFold = Mth.clamp(wingFold + 0.1F, 0.0F, 1.0F);
                continue;
            }
            player.push(away.x * 0.06D, 0.0D, away.z * 0.06D);
        }
    }

    private void tickUnstick() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        double travelled = Math.sqrt(sqr(getX() - stuckReferenceX) + sqr(getY() - stuckReferenceY)
                                     + sqr(getZ() - stuckReferenceZ));
        stuckTicks++;
        if (travelled >= STUCK_MIN_TRAVEL) {
            stuckReferenceX = getX();
            stuckReferenceY = getY();
            stuckReferenceZ = getZ();
            stuckTicks = 0;
            return;
        }
        boolean targetPresent = getTarget() != null;
        boolean exempt = !targetPresent || action == VicissitudeRig.Action.SCYTHE_RECOVER
                         || stage == VicissitudeBossStage.TRANSITION || getHealth() <= 0.0F;
        if (exempt || stuckTicks < STUCK_WINDOW_TICKS || unstickCooldown > 0) {
            return;
        }
        boolean obstructed = wingsBlockedLastTick
                             || !level().noCollision(this, getBoundingBox().inflate(0.1D));
        if (!obstructed) {
            stuckReferenceX = getX();
            stuckReferenceY = getY();
            stuckReferenceZ = getZ();
            stuckTicks = 0;
            return;
        }
        if (tryUnstickTeleport(serverLevel)) {
            stuckTicks = 0;
            unstickCooldown = UNSTICK_SUCCESS_COOLDOWN;
        } else {
            // No valid space yet: stay put and retry after another window.
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
                            || y > level().getMaxBuildHeight() - getBbHeight() - 1) {
                            continue;
                        }
                        BlockPos pos = BlockPos.containing(x, y, z);
                        if (!level().hasChunkAt(pos) || !level().isLoaded(pos)) {
                            // Never force load chunks while searching.
                            continue;
                        }
                        if (isSpaceUsable(serverLevel, x, y, z)) {
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
            if (!level().noCollision(this, getBoundingBox().deflate(0.05D))) {
                usable = false;
            } else if (level().containsAnyLiquid(getBoundingBox())) {
                usable = false;
            } else {
                usable = true;
                for (VicissitudeRig.SegmentVolume volume : segmentVolumes()) {
                    AABB box = toAabb(volume.box());
                    if (level().getBlockCollisions(this, box).iterator().hasNext()) {
                        usable = false;
                        break;
                    }
                    if (!level().getEntitiesOfClass(Player.class, box).isEmpty()) {
                        usable = false;
                        break;
                    }
                }
            }
        } finally {
            setPos(old.x, old.y, old.z);
        }
        return usable;
    }

    /** Emergency escape only: a finite search around the boss, never a chase teleport. */
    private void unstickTeleport(double x, double y, double z) {
        Vec3 before = position();
        setPos(x, y, z);
        setDeltaMovement(Vec3.ZERO);
        getNavigation().stop();
        // Interrupt the current attack into recovery: nothing already released is replayed.
        if (action != VicissitudeRig.Action.NONE && action != VicissitudeRig.Action.SCYTHE_RECOVER) {
            endAction();
            startAction(VicissitudeRig.Action.SCYTHE_RECOVER, false);
        }
        if (scytheToken != null && level() instanceof ServerLevel serverLevel) {
            Entity entity = serverLevel.getEntity(scytheToken);
            if (entity instanceof VicissitudeScytheProjectileEntity scythe) {
                scythe.recall();
            }
        }
        stuckReferenceX = x;
        stuckReferenceY = y;
        stuckReferenceZ = z;
        sendEvent(VicissitudeEffectPacket.EVENT_UNSTICK, position());
        if (level() instanceof ServerLevel serverLevel) {
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

    // ------------------------------------------------------------------ ground markers

    @Nullable
    private Vec3 findGroundPoint(LivingEntity target) {
        BlockPos origin = target.blockPosition();
        for (int drop = 0; drop <= 8; drop++) {
            BlockPos candidate = origin.below(drop);
            if (!level().getBlockState(candidate).getCollisionShape(level(), candidate).isEmpty()) {
                return new Vec3(candidate.getX() + 0.5D, candidate.getY() + 1.0D,
                        candidate.getZ() + 0.5D);
            }
        }
        return null;
    }

    /** The ground marker center is locked ten ticks into the windup and never slides after. */
    private void lockGroundMarker(double inner, double outer, float gapCenter, float gapWidth) {
        Vec3 center = groundMarkerTarget;
        if (center == null) {
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
        if (duration <= 0) {
            return 1.0F;
        }
        long elapsed = level().getGameTime() - entityData.get(DATA_GROUND_START);
        return Mth.clamp((elapsed + partialTick) / (duration + 8.0F), 0.0F, 1.0F);
    }

    private void tickGroundMarkerWindup() {
        if (action == VicissitudeRig.Action.CAST_FROM_CHEST) {
            int ticks = actionTicks();
            if (ticks == 1) {
                LivingEntity target = getTarget();
                groundMarkerTarget = target == null ? null : findGroundPoint(target);
                if (groundMarkerTarget == null) {
                    return;
                }
            }
            if (ticks == 10) {
                lockGroundMarker(0.0D, 2.0D, 0.0F, 0.0F);
            }
        } else if (action == VicissitudeRig.Action.CAST_FROM_HALO) {
            int ticks = actionTicks();
            if (ticks == 1) {
                LivingEntity target = getTarget();
                groundMarkerTarget = target == null ? null : findGroundPoint(target);
            }
            if (ticks == 10) {
                // The 60 degree safe gap is chosen once and stays visible.
                float gap = level().getRandom().nextFloat() * 360.0F;
                lockGroundMarker(3.0D, 6.0D, gap, 60.0F);
            }
        }
    }

    // ------------------------------------------------------------------ targets and damage rules

    public boolean isValidCombatParticipant(Entity entity) {
        if (!(entity instanceof LivingEntity living) || living == this || !living.isAlive()
            || living.level() != level()) {
            return false;
        }
        if (isIgnoredPlayer(living)) {
            return false;
        }
        if (isPhaseTwo()) {
            return phaseTwoParticipants.contains(living.getUUID());
        }
        return phaseOneTargets.contains(living.getUUID());
    }

    @Override
    protected float getVitalityDamageLimit() {
        float ratio = switch (bossDifficulty) {
            case SIMPLE -> 0.20F;
            case DIFFICULT -> 0.05F;
            case COMPLETE, EXTREME -> 0.01F;
        };
        return getVitalityMaximum() * ratio;
    }

    @Override
    protected boolean isDamageImmune(DamageSource source) {
        // Transition and the authored death sequence are damage free.
        if (stage == VicissitudeBossStage.TRANSITION || getHealth() <= 0.0F) {
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

    /**
     * 原版式的仇恨：谁打它，谁就进它的名单——不看视线，隔着墙也一样。
     *
     * <p>三处与原版一致：受击即结仇（对应 {@code HurtByTargetGoal}，原版被隔墙打中也会还手）、
     * 宠物与召唤物算在主人头上（对应原版的 {@code lastHurtByPlayer}，只是这里连非玩家的主人也算）、
     * 创造与旁观不结仇也不挨揍（原版的目标选择同样排除这两种身份）。
     *
     * <p>挨打就醒：{@link VicissitudeBossStage#DORMANT} 的像被任何人打中都会进入一阶段，
     * 只剩「没人在打它满 100 tick 就回去当像」这一条自有规则，见 {@link #tickEmptyEncounter()}。
     */
    @Override
    protected void onIncomingAttack(DamageSource source, float amount) {
        LivingEntity attacker = resolveLivingAttacker(source);
        if (isPhaseTwo()) {
            LivingEntity owner = ownerOf(attacker);
            registerPhaseTwoParticipant(attacker);
            registerPhaseTwoParticipant(owner);
            return;
        }
        if (attacker == null || attacker == this || !attacker.isAlive()
            || isIgnoredPlayer(attacker)) {
            return;
        }
        LivingEntity owner = ownerOf(attacker);
        registerPhaseOneParticipant(attacker);
        registerPhaseOneParticipant(owner);
        if (stage == VicissitudeBossStage.DORMANT) {
            lockPhaseOneDuration();
            setStage(VicissitudeBossStage.PHASE_ONE);
            phaseOneTicks = 0;
            ServerPlayer starter = attacker instanceof ServerPlayer player ? player
                                  : owner instanceof ServerPlayer ownerPlayer ? ownerPlayer : null;
            if (starter != null) {
                announceFirstAttack(starter);
            }
        }
        if (!isValidPhaseOneTarget(getTarget())) {
            setTarget(preferredTarget(attacker, owner));
        }
    }

    /**
     * 宠物与召唤物的主人，没有主人的（野生生物、玩家自己）返回 null。
     *
     * <p>拿 {@code OwnableEntity#getOwnerUUID} 而不是 {@code getOwner()}：后者只认玩家，
     * 而主人也可能是别的生物。解析方式与参战名单一致，主人离线时拿不到就当作没有。
     */
    @Nullable
    private LivingEntity ownerOf(@Nullable LivingEntity attacker) {
        if (!(attacker instanceof OwnableEntity ownable)
            || !(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        UUID ownerId = ownable.getOwnerUUID();
        if (ownerId == null) {
            return null;
        }
        LivingEntity owner = resolveParticipant(serverLevel, ownerId);
        return owner == this ? null : owner;
    }

    /**
     * 把一名攻击者写进一阶段名单。
     *
     * <p>创造与旁观在这里就被挡掉（{@link #isIgnoredPlayer}）：他们永远不参战、也不挨揍，
     * 出手不算数。围观者同理永远进不来，所以「不打旁观者」这条规则靠入口而不是靠过滤维持。
     */
    private void registerPhaseOneParticipant(@Nullable LivingEntity participant) {
        if (participant == null || participant == this || !participant.isAlive()
            || participant.level() != level() || isIgnoredPlayer(participant)) {
            return;
        }
        phaseOneTargets.add(participant.getUUID());
        if (participant instanceof ServerPlayer player) {
            targetPlayerDeaths.put(player.getUUID(), getDeathCount(player));
        }
    }

    /** 宠物、召唤物出手时账要算在主人头上：能选主人就选主人。 */
    private LivingEntity preferredTarget(LivingEntity attacker, @Nullable LivingEntity owner) {
        return owner != null && isValidPhaseOneTarget(owner) ? owner : attacker;
    }

    /**
     * 二阶段的参战者：与一阶段同一套归属规则。
     *
     * <p>名单在这里从「玩家」放宽到任意存活生物，宠物、召唤物和别的生物都能一路打到底；
     * 选目标时玩家优先，见 {@link #selectBalancedPhaseTwoTarget()}。
     */
    private void registerPhaseTwoParticipant(@Nullable LivingEntity participant) {
        if (participant == null || participant == this || !participant.isAlive()
            || participant.level() != level() || isIgnoredPlayer(participant)) {
            return;
        }
        UUID identity = participant.getUUID();
        phaseTwoParticipants.add(identity);
        if (participant instanceof ServerPlayer player) {
            phaseTwoPlayerDeaths.putIfAbsent(identity, getDeathCount(player));
        }
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || target.level() != level()
            || isIgnoredPlayer(target)) {
            setTarget(participant);
        }
    }

    /**
     * Set piece lines use the action bar, but with the mod's palette instead of vanilla white.
     * A Style can carry colour and weight; the action bar itself is a single fixed font size,
     * which is why this needs no custom overlay or timer.
     */
    private static Component announcement(String key) {
        return Component.translatable(key).withStyle(style -> style
                .withColor(TextColor.fromRgb(0xE6EDF5))
                .withBold(true));
    }

    /** The phase one set piece line, once per player life. */
    private void announceFirstAttack(ServerPlayer player) {
        int deathCount = getDeathCount(player);
        if (announcedPlayerDeaths.getOrDefault(player.getUUID(), -1) != deathCount) {
            player.displayClientMessage(announcement(PHASE_ONE_MESSAGE_KEY), true);
            announcedPlayerDeaths.put(player.getUUID(), deathCount);
        }
    }

    private void lockPhaseOneDuration() {
        if (!phaseOneDurationLocked) {
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
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        VicissitudeEffectPacket packet = new VicissitudeEffectPacket(getId(), eventId,
                actionSequence, position);
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(position) < 96.0D * 96.0D) {
                MaledictNetwork.sendEffect(player, packet);
            }
        }
    }

    // ------------------------------------------------------------------ movement and targets

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
        List<UUID> targets = orderedPhaseOneTargets(serverLevel);
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
            if (candidate != null && shouldRemovePhaseOneTarget(identity, candidate)) {
                invalid.add(identity);
            }
        }
        removeInvalidTargets(invalid);
        setTarget(null);
        return null;
    }

    /**
     * 一阶段轮换用的名单：玩家排前面，宠物、召唤物与别的生物排后面。
     *
     * <p>玩家拖着无常、周围还围着一群僵尸时，无常盯的仍然是人；
     * 名单里没有玩家时才会去招呼这群召唤物，见 19。
     */
    private List<UUID> orderedPhaseOneTargets(ServerLevel serverLevel) {
        List<UUID> players = new ArrayList<>();
        List<UUID> others = new ArrayList<>();
        var playerList = serverLevel.getServer().getPlayerList();
        for (UUID identity : phaseOneTargets) {
            if (playerList.getPlayer(identity) != null) {
                players.add(identity);
            } else {
                others.add(identity);
            }
        }
        players.addAll(others);
        return players;
    }

    @Nullable
    private LivingEntity selectBalancedPhaseTwoTarget() {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        // 玩家优先，其次才是宠物/召唤物/别的生物：中途加入的玩家不会被一只高血量的宠物挡住。
        LivingEntity selected = healthiestPhaseTwoTarget(serverLevel, true);
        if (selected == null) {
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
        if (target == null || target == this || !target.isAlive() || target.level() != level()) {
            return false;
        }
        if (distanceToSqr(target) > engagementRangeSqr()) {
            // Out of engagement range: the boss holds position instead of crossing the arena.
            return false;
        }
        if (target instanceof ServerPlayer player) {
            Integer registeredDeathCount = targetPlayerDeaths.get(player.getUUID());
            return !isIgnoredPlayer(player)
                   && (registeredDeathCount == null || registeredDeathCount == getDeathCount(player));
        }
        return !isIgnoredPlayer(target);
    }

    /**
     * 被无视的目标：创造与旁观的玩家。
     *
     * <p>他们永远不参战、也不挨揍，这一点不因为「受击还手」而改变：受击还手说的是
     * 打它的人（宠物、召唤物、别的生物）会进名单，而创造/旁观从入口就被排除，
     * 连自己出手都不算数——作者在创造模式下要试招，请让手下的生物去打它。
     */
    private static boolean isIgnoredPlayer(@Nullable LivingEntity target) {
        return target instanceof Player player && (player.isCreative() || player.isSpectator());
    }

    private boolean shouldRemovePhaseOneTarget(UUID identity, Entity candidate) {
        if (!(candidate instanceof ServerPlayer player)) {
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

    private void updatePhaseOneMovement(@Nullable LivingEntity target) {
        if (target == null) {
            double wantedY = Double.isNaN(idleHoverY) ? getY() + HOVER_HEIGHT : idleHoverY;
            steerToward(new Vec3(getX(), wantedY, getZ()), MAX_FLIGHT_SPEED);
            return;
        }
        if (action != VicissitudeRig.Action.NONE) {
            // Hold the station through the windup and the release: casting has to read as
            // aiming at the target, not as drifting past it.
            setDeltaMovement(getDeltaMovement().scale(0.5D));
            hasImpulse = true;
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
            setDeltaMovement(getDeltaMovement().scale(0.7D));
            hasImpulse = true;
            return;
        }
        if (action == VicissitudeRig.Action.DASH && dashDirectionLocked
            && actionTicks() >= action.releaseTick()) {
            return;
        }
        if (action != VicissitudeRig.Action.NONE) {
            // Plant the floating body through the tell and recovery; the rig supplies recoil.
            setDeltaMovement(getDeltaMovement().scale(0.5D));
            hasImpulse = true;
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
        float damage = Math.max(10.0F, attackDamage());
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

    // ------------------------------------------------------------------ weapon loadout

    private void equipPhaseTwoWeapon() {
        setItemSlot(EquipmentSlot.MAINHAND, createWeaponForDifficulty(bossDifficulty));
        setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        applyWeaponAttributes();
        setWeaponTier(bossDifficulty);
        setWeaponState(1);
    }

    /**
     * Keeps the hand matching the encounter while the weapon is supposed to be held.
     *
     * <p>The rendered weapon no longer depends on this stack, and neither does the damage, but the
     * throw loop and everything else that reads the hand still expect a scythe there. Only the
     * "weapon is held" state is policed, which leaves a weapon in flight alone.
     *
     * @param fullCheck also compares the item type, on the slow cadence; an empty hand is always
     *                  restored immediately
     */
    private void tickWeaponGuard(boolean fullCheck) {
        if (scytheToken != null || entityData.get(DATA_WEAPON_STATE) != 1) {
            return;
        }
        ItemStack held = getMainHandItem();
        if (!held.isEmpty() && (!fullCheck || held.is(createWeaponForDifficulty(bossDifficulty)
                .getItem()))) {
            return;
        }
        equipPhaseTwoWeapon();
    }

    private ItemStack createWeaponForDifficulty(BossDifficulty difficulty) {
        if (difficulty == BossDifficulty.SIMPLE) {
            return new ItemStack(ItemRegistry.SOUL_STAINED_STEEL_SCYTHE.get());
        }
        if (difficulty == BossDifficulty.DIFFICULT) {
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
        if (!difficultyLocked && !level().isClientSide) {
            // Before the fight starts the pool simply follows the mode; afterwards it is frozen.
            applyDifficultyAttributes();
        }
        if (!phaseOneDurationLocked) {
            phaseOneDurationTicks = bossDifficulty.phaseOneDurationTicks();
        }
        if (isPhaseTwo() && !level().isClientSide) {
            if (scytheToken != null) {
                // A weapon change while the scythe flies waits for this recovery.
                pendingWeaponTier = bossDifficulty.ordinal();
                return;
            }
            equipPhaseTwoWeapon();
        }
    }

    private void applyDeferredWeaponTier() {
        if (pendingWeaponTier >= 0) {
            pendingWeaponTier = -1;
            equipPhaseTwoWeapon();
        }
    }

    // ------------------------------------------------------------------ persistence

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
        if (!Double.isNaN(idleHoverY)) {
            tag.putDouble("PhaseOneHoverY", idleHoverY);
        }
        if (scytheToken != null) {
            tag.putUUID("ScytheToken", scytheToken);
        }
        if (!stashedWeapon.isEmpty()) {
            tag.put("StashedWeapon", stashedWeapon.save(new CompoundTag()));
        }
        tag.put("PhaseOneTargets", saveUuidSet(phaseOneTargets));
        tag.put("PhaseTwoPlayers", saveUuidSet(phaseTwoParticipants));
        tag.put("PhaseOneTargetDeaths", savePlayerDeathMap(targetPlayerDeaths));
        tag.put("PhaseOneAnnouncements", savePlayerDeathMap(announcedPlayerDeaths));
        tag.put("PhaseTwoPlayerDeaths", savePlayerDeathMap(phaseTwoPlayerDeaths));
        tag.put("ConfiscatedCurios", saveConfiscatedCurios());
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
        if (hasStage) {
            stage = VicissitudeBossStage.byId(tag.getByte("VicissitudeStage"));
        } else {
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
        if (savedDuration > 0) {
            phaseOneDurationTicks = savedDuration;
            phaseOneTicks = Mth.clamp(savedTicks, 0, savedDuration);
        } else {
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
        if (tag.contains("PhaseOneHoverY", Tag.TAG_DOUBLE)) {
            idleHoverY = tag.getDouble("PhaseOneHoverY");
        }
        if (tag.hasUUID("ScytheToken")) {
            scytheToken = tag.getUUID("ScytheToken");
        }
        stashedWeapon = ItemStack.of(tag.getCompound("StashedWeapon"));
        loadUuidSet(tag.getList("PhaseOneTargets", Tag.TAG_INT_ARRAY), phaseOneTargets);
        loadUuidSet(tag.getList("PhaseTwoPlayers", Tag.TAG_INT_ARRAY), phaseTwoParticipants);
        loadPlayerDeathMap(tag.getList("PhaseOneTargetDeaths", Tag.TAG_COMPOUND), targetPlayerDeaths);
        loadPlayerDeathMap(tag.getList("PhaseOneAnnouncements", Tag.TAG_COMPOUND), announcedPlayerDeaths);
        loadPlayerDeathMap(tag.getList("PhaseTwoPlayerDeaths", Tag.TAG_COMPOUND), phaseTwoPlayerDeaths);
        loadConfiscatedCurios(tag.getList("ConfiscatedCurios", Tag.TAG_COMPOUND));
        entityData.set(DATA_STAGE, (byte) stage.ordinal());
        entityData.set(DATA_ACTION, (byte) VicissitudeRig.Action.NONE.ordinal());
        action = VicissitudeRig.Action.NONE;
        if (stage == VicissitudeBossStage.PHASE_TWO) {
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
            if (value.hasUUID("Player")) {
                values.put(value.getUUID("Player"), value.getInt("Deaths"));
            }
        }
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
            if (!player.hasUUID("Player")) {
                continue;
            }
            Deque<VicissitudeCurioLedger.Entry> items = new ArrayDeque<>();
            for (Tag itemTag : player.getList("Items", Tag.TAG_COMPOUND)) {
                CompoundTag item = (CompoundTag) itemTag;
                ItemStack stack = ItemStack.of(item.getCompound("Stack"));
                if (!stack.isEmpty()) {
                    items.addLast(new VicissitudeCurioLedger.Entry(
                            item.getString("Slot"), item.getInt("Index"), stack));
                }
            }
            if (!items.isEmpty()) {
                confiscated.put(player.getUUID("Player"), items);
            }
        }
    }

    // ------------------------------------------------------------------ helpers

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

    /** Difficulty table: phase one gets shorter as the difficulty rises. */
    public enum BossDifficulty {
        /**
         * {@code maxHealth} is the pool the vitality ledger captures when the entity joins the
         * world. Attack damage is not listed here: it comes from the difficulty weapon's own
         * attribute modifiers, which {@link #applyWeaponAttributes()} bakes into the entity.
         *
         * <p>{@code damagePress} picks the ChangeLib damage ladder used when the boss damages a
         * player in <em>phase two</em>: {@code LIGHT} stops after the first authoritative hit, so
         * armour, enchantments and damage caps still decide how much lands, while {@code MEDIUM}
         * keeps pressing until the authored amount has actually been taken. The two easy modes use
         * the light ladder, the two hard modes the medium one; phase one always uses the light
         * ladder whatever the mode, see {@code hurtParticipant}.
         *
         * <p>The trailing path is this mode's loot table, see {@link #lootTable()}; the number
         * after it is the Age of Enlightenment level handed out by
         * {@link #enlightenmentLevel()} when a player lands the kill.
         */
        SIMPLE(1800, 500.0D, DamagePress.LIGHT, "entities/first_vicissitude", 0),
        DIFFICULT(1400, 750.0D, DamagePress.LIGHT, "entities/first_vicissitude_difficult", 1),
        COMPLETE(1000, 1000.0D, DamagePress.MEDIUM, "entities/first_vicissitude_complete", 2),
        EXTREME(750, 1500.0D, DamagePress.MEDIUM, "entities/first_vicissitude_extreme", 3);

        private final int phaseOneDurationTicks;
        private final double maxHealth;
        private final DamagePress damagePress;
        private final ResourceLocation lootTable;
        private final int enlightenmentLevel;

        BossDifficulty(int phaseOneDurationTicks, double maxHealth, DamagePress damagePress,
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

        /**
         * 该难度使用的常规掉落物表。
         *
         * <p>每一档都有自己的表，内容见 {@code MaledictEntityLoot}（四张表内容相同），
         * 这里只保存 ID。SIMPLE 用的就是实体默认路径
         * {@code maledict:entities/first_vicissitude}，所以照着默认 ID 找表的一方看到的
         * 是一份真奖励，而不是一张空表。
         */
        public ResourceLocation lootTable() {
            return lootTable;
        }

        /**
         * 该难度的启蒙之年等级，也就是物品 NBT 上的 amplifier：0 即游戏里显示的一级
         * （见 {@code EnlightenmentLevel}），四档由易到难正好是 I–IV 级。
         *
         * <p>这个数字曾经写在掉落物表里；启蒙之年改成玩家击杀才发的专属掉落后，
         * 它跟着发奖励的 {@code dropCustomDeathLoot} 一起搬到了实体侧，仍然只有这一处权威。
         */
        public int enlightenmentLevel() {
            return enlightenmentLevel;
        }

        private boolean confiscatesCurios() {
            return this == COMPLETE || this == EXTREME;
        }

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** Synced weapon tiers travel as a byte; anything unexpected falls back to SIMPLE. */
        private static BossDifficulty byId(int id) {
            BossDifficulty[] values = values();
            return id >= 0 && id < values.length ? values[id] : SIMPLE;
        }

        private static BossDifficulty fromName(String name) {
            try {
                return valueOf(name.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
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
                    if (boss.action == VicissitudeRig.Action.NONE && targetRefreshCooldown-- <= 0) {
                        boss.selectBalancedPhaseTwoTarget();
                        targetRefreshCooldown = 10;
                    }
                    LivingEntity target = boss.getTarget();
                    if (boss.action == VicissitudeRig.Action.NONE
                            && (target == null || !target.isAlive() || target.level() != boss.level())) {
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
