package org.brahypno.maledict.common.curio.priestess;

import com.google.common.collect.Multimap;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/** Live attribute access without letting the echo's tracker consume the body's pending updates. */
final class SoulEchoAttributes extends AttributeMap {
    private final Supplier<LivingEntity> body;

    SoulEchoAttributes(Supplier<LivingEntity> body) {
        super(AttributeSupplier.builder().build());
        this.body = body;
    }

    private AttributeMap attributes() { return body.get().getAttributes(); }

    // ServerEntity clears this set after sending. Only the body's tracker may drain its changes;
    // the client echo reads the client's body, so it needs no attribute packets of its own.
    @Override public Set<AttributeInstance> getDirtyAttributes() { return Collections.emptySet(); }
    @Override public Collection<AttributeInstance> getSyncableAttributes() { return Collections.emptyList(); }
    @Override public AttributeInstance getInstance(Attribute attribute) { return attributes().getInstance(attribute); }
    @Override public boolean hasAttribute(Attribute attribute) { return attributes().hasAttribute(attribute); }
    @Override public boolean hasModifier(Attribute attribute, UUID id) { return attributes().hasModifier(attribute, id); }
    @Override public double getValue(Attribute attribute) { return attributes().getValue(attribute); }
    @Override public double getBaseValue(Attribute attribute) { return attributes().getBaseValue(attribute); }
    @Override public double getModifierValue(Attribute attribute, UUID id) {
        return attributes().getModifierValue(attribute, id);
    }
    @Override public void removeAttributeModifiers(Multimap<Attribute, AttributeModifier> modifiers) {
        attributes().removeAttributeModifiers(modifiers);
    }
    @Override public void addTransientAttributeModifiers(Multimap<Attribute, AttributeModifier> modifiers) {
        attributes().addTransientAttributeModifiers(modifiers);
    }
    @Override public void assignValues(AttributeMap source) {
        attributes().assignValues(source instanceof SoulEchoAttributes echo ? echo.attributes() : source);
    }
    @Override public ListTag save() { return attributes().save(); }
    @Override public void load(ListTag tag) { attributes().load(tag); }
}
