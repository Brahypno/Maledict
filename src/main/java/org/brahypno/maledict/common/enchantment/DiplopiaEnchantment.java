package org.brahypno.maledict.common.enchantment;

import com.sammy.malum.registry.common.item.EnchantmentRegistry;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * Makes Malum's Rebound throw one extra phantom scythe per level. The phantoms are spawned by
 * {@link DiplopiaThrows}; this class only carries the enchantment data and its tuning values.
 */
public final class DiplopiaEnchantment extends Enchantment {
    public static final int MAX_LEVEL = 3;

    /** Share of the original throw's damage every phantom deals. */
    public static final float ECHO_DAMAGE_MULTIPLIER = 0.5f;

    /** Largest random yaw deviation from the original throw's direction, in degrees. */
    public static final float ECHO_YAW_SPREAD = 12.0f;

    /** Largest random pitch deviation from the original throw's direction, in degrees. */
    public static final float ECHO_PITCH_SPREAD = 7.0f;

    /** Random multiplier applied to each phantom's launch speed, plus or minus this fraction. */
    public static final float ECHO_SPEED_JITTER = 0.15f;

    /** Extra outbound ticks each phantom may roll, so the volley does not turn back in lockstep. */
    public static final int ECHO_RETURN_TIMER_JITTER = 4;

    public DiplopiaEnchantment() {
        super(Rarity.RARE, EnchantmentRegistry.SCYTHE_ONLY,
              new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND});
    }

    @Override
    public int getMinCost(int level) {
        return 10 + (level - 1) * 10;
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 15;
    }

    @Override
    public int getMaxLevel() {
        return MAX_LEVEL;
    }
}
