package org.brahypno.maledict.mixin;

import com.sammy.malum.common.data.attachment.MalignantInfluenceData;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.HashMap;

/** Malum exposes cache reads and writes, but no invalidation API for changed conversion rules. */
@Mixin(value = MalignantInfluenceData.class, remap = false)
public interface MalignantInfluenceDataAccessor {
    @Accessor("cachedAttributeValues")
    HashMap<Holder<Attribute>, Double> maledict$cachedAttributeValues();
}
