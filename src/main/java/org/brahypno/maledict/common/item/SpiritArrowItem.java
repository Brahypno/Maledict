package org.brahypno.maledict.common.item;

import com.sammy.malum.common.item.spirit.ISpiritAffiliatedItem;
import com.sammy.malum.core.systems.spirit.type.SpiritArcanaType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.brahypno.maledict.common.entity.SpiritArrowEntity;

public class SpiritArrowItem extends ArrowItem implements ISpiritAffiliatedItem {
    private final SpiritArrowType arrowType;

    public SpiritArrowItem(SpiritArrowType arrowType, Properties properties) {
        super(properties);
        this.arrowType = arrowType;
    }

    @Override
    public AbstractArrow createArrow(Level level, ItemStack stack, LivingEntity shooter, ItemStack weapon) {
        SpiritArrowEntity arrow = new SpiritArrowEntity(level, shooter, arrowType);
        if (arrowType == SpiritArrowType.EARTHEN) {
            arrow.setBaseDamage(arrow.getBaseDamage() + 1.5D);
        }
        if (arrowType == SpiritArrowType.INFERNAL) {
            arrow.igniteForSeconds(100);
        }
        return arrow;
    }

    @Override
    public com.sammy.malum.core.systems.spirit.type.SpiritLike getDefiningSpiritType() {
        return arrowType.getSpiritType();
    }
}
