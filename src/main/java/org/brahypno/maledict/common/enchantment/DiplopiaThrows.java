package org.brahypno.maledict.common.enchantment;

import com.sammy.malum.common.entity.scythe.ScytheBoomerangEntity;
import com.sammy.malum.common.item.curiosities.weapons.scythe.MalumScytheItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.brahypno.maledict.common.weapon.incursus.IncursusBladeItem;
import org.brahypno.maledict.common.weapon.incursus.IncursusScytheBoomerangEntity;
import org.brahypno.maledict.registry.MaledictEnchantments;
import team.lodestar.lodestone.registry.common.LodestoneAttributeRegistry;

/**
 * Spawns the extra Rebound scythes granted by {@link DiplopiaEnchantment}.
 *
 * <p>Only the original throw owns the weapon. Every phantom here is an echo: it gets a copy of the
 * scythe stack for rendering and enchantment application, never calls
 * {@code TemporarilyDisabledItem#disable} or {@code ReboundEnchantment#pickupScythe}, and discards
 * itself on the return leg ({@link DiplopiaScytheBoomerangEntity},
 * {@link IncursusScytheBoomerangEntity}). The player therefore still restores exactly one scythe.
 */
public final class DiplopiaThrows {
    /**
     * Marker on the phantom's synced display copy. The client renderer reads it from the item
     * stack; the real weapon never receives it.
     */
    public static final String ECHO_TAG = "maledict:diplopia_echo";

    private static final int RETURN_TIMER = 8;

    /**
     * Malum's boomerang moves inside {@code super.tick()} before it evaluates its 3-block pickup
     * radius. A phantom must be discarded before that movement can cross the boundary, so the
     * guard uses the next tick's predicted position with a little margin.
     */
    private static final double ECHO_PICKUP_GUARD = 3.5;

    /**
     * Runs after Malum's own Rebound throw. The damage and launch position are recomputed with the
     * same formulas Malum uses; nothing can move or change attributes during that static call, so
     * the echoes leave from the same point and state as the original.
     */
    public static void afterReboundThrow(
            Level level, ServerPlayer player, InteractionHand hand, ItemStack scythe) {
        int extra = scythe.getEnchantmentLevel(MaledictEnchantments.DIPLOPIA.get());
        if (extra <= 0){
            return;
        }

        boolean enhanced = !MalumScytheItem.canSweep(player);
        float attackDamage = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float magicDamage = (float) player.getAttributeValue(LodestoneAttributeRegistry.MAGIC_DAMAGE.get());
        float velocity = enhanced ? 3.0f : 1.75f;
        Vec3 position = throwPosition(player, hand, enhanced);
        if (enhanced){
            attackDamage *= 1.5f;
            magicDamage *= 1.5f;
        }
        spawnEchoes(level, player, hand, scythe, attackDamage, magicDamage, 0.0f,
                    enhanced, position, velocity);
    }

    /**
     * Spawns one phantom per Diplopia level. Used by the Rebound injection above and directly by
     * the Incursus Blade's own throw, which already has the exact channel values.
     */
    public static void spawnEchoes(
            Level level,
            ServerPlayer player,
            InteractionHand hand,
            ItemStack scythe,
            float attackDamage,
            float magicDamage,
            float frozenDamage,
            boolean enhanced,
            Vec3 position,
            float velocity) {
        int extra = scythe.getEnchantmentLevel(MaledictEnchantments.DIPLOPIA.get());
        if (extra <= 0){
            return;
        }

        int slot = hand == InteractionHand.OFF_HAND
                   ? player.getInventory().getContainerSize() - 1
                   : player.getInventory().selected;
        float multiplier = DiplopiaEnchantment.ECHO_DAMAGE_MULTIPLIER;
        for (int i = 0; i < extra; i++){
            float yaw = player.getYRot() + Mth.nextFloat(
                    player.getRandom(),
                    -DiplopiaEnchantment.ECHO_YAW_SPREAD,
                    DiplopiaEnchantment.ECHO_YAW_SPREAD);
            float pitch = Mth.clamp(
                    player.getXRot() + Mth.nextFloat(
                            player.getRandom(),
                            -DiplopiaEnchantment.ECHO_PITCH_SPREAD,
                            DiplopiaEnchantment.ECHO_PITCH_SPREAD),
                    -90.0f, 90.0f);
            float speed = velocity * Mth.nextFloat(
                    player.getRandom(),
                    1.0f - DiplopiaEnchantment.ECHO_SPEED_JITTER,
                    1.0f + DiplopiaEnchantment.ECHO_SPEED_JITTER);
            int returnTimer = RETURN_TIMER
                              + player.getRandom().nextInt(DiplopiaEnchantment.ECHO_RETURN_TIMER_JITTER + 1);

            ItemStack echoStack = scythe.copy();
            echoStack.getOrCreateTag().putBoolean(ECHO_TAG, true);

            ScytheBoomerangEntity echo = scythe.getItem() instanceof IncursusBladeItem
                                         ? new IncursusScytheBoomerangEntity(
                                                 level, position.x, position.y, position.z,
                                                 frozenDamage * multiplier, true)
                                         : new DiplopiaScytheBoomerangEntity(
                                                 level, position.x, position.y, position.z);
            echo.setData(player, attackDamage * multiplier, magicDamage * multiplier, slot, returnTimer);
            echo.setItem(echoStack);
            echo.setEnhanced(enhanced);
            echo.shootFromRotation(player, pitch, yaw, 0.0f, speed, 0.0f);
            level.addFreshEntity(echo);
        }
    }

    /**
     * True when the next tick's travel would bring a returning phantom inside Malum's pickup
     * radius. The check runs before {@code super.tick()} moves the entity, so an echo never reaches
     * the base pickup branch (and never adds the Rebound cooldown or re-enables the weapon slot).
     */
    public static boolean reachedOwnerOnReturn(ScytheBoomerangEntity echo) {
        Entity owner = echo.getOwner();
        if (owner == null || echo.returnTimer > 0){
            return false;
        }
        Vec3 nextPosition = echo.position().add(echo.getDeltaMovement());
        return nextPosition.distanceTo(owner.position()) < ECHO_PICKUP_GUARD;
    }

    /** True when the synced item stack carries the phantom marker written by {@link #spawnEchoes}. */
    public static boolean isEcho(ScytheBoomerangEntity entity) {
        CompoundTag tag = entity.getItem().getTag();
        return tag != null && tag.getBoolean(ECHO_TAG);
    }

    /** Mirrors the launch position in {@code ReboundEnchantment#throwScythe}. */
    private static Vec3 throwPosition(ServerPlayer player, InteractionHand hand, boolean enhanced) {
        Vec3 position = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
        if (enhanced){
            int angle = hand == InteractionHand.MAIN_HAND ? 225 : 90;
            double radians = Math.toRadians(angle - player.yHeadRot);
            position = player.position()
                    .add(player.getLookAngle().scale(0.5))
                    .add(0.75 * Math.sin(radians), player.getBbHeight() * 0.9,
                         0.75 * Math.cos(radians));
        }
        return position;
    }

    private DiplopiaThrows() {
    }
}
