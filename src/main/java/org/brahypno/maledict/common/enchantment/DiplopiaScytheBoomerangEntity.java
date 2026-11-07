package org.brahypno.maledict.common.enchantment;

import com.sammy.malum.common.entity.scythe.ScytheBoomerangEntity;
import net.minecraft.world.level.Level;

/**
 * The phantom copy thrown by Diplopia for ordinary Malum scythes.
 *
 * <p>It deals the damage stored on it but never touches the thrower's inventory: once the return
 * leg reaches the owner it discards itself before Malum's pickup branch can restore and re-enable
 * the weapon slot, and it is never written to disk. The original throw is the only entity allowed
 * to call {@code ReboundEnchantment#pickupScythe}.
 */
public final class DiplopiaScytheBoomerangEntity extends ScytheBoomerangEntity {
    public DiplopiaScytheBoomerangEntity(Level level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Override
    public void tick() {
        if (!level().isClientSide() && DiplopiaThrows.reachedOwnerOnReturn(this)){
            discard();
            return;
        }
        super.tick();
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
