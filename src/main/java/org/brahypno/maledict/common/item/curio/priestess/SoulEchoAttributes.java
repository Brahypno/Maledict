package org.brahypno.maledict.common.item.curio.priestess;

import com.google.common.collect.Multimap;
import net.minecraft.core.Holder;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 回声的属性表：活着查本体，但不让回声自己的 tracker 抽走本体的待同步变化。
 *
 * <p>1.21.1 的 {@link AttributeMap} 把「读」与「同步队列」放在同一层，所以这里把
 * {@code getAttributesToSync}/{@code getAttributesToUpdate}/{@code getSyncableAttributes}
 * 全部清空——只有本体自己的 tracker 会发属性包；客户端回声读的是同一维度的本体，
 * 属性值从本体的表里直接拿，不需要自己那份同步。
 *
 * <p>1.21.1 的签名与 1.20.1 不同：属性句柄是 {@code Holder<Attribute>}，修饰符 ID 是
 * {@code ResourceLocation}，{@code assignValues} 拆成 {@code assignAllValues} /
 * {@code assignBaseValues}。委托对象是 {@code body.get().getAttributes()}，套娃时先拆一层，
 * 免得 {@code assignAllValues} 自己递归。
 */
final class SoulEchoAttributes extends AttributeMap {
    private final Supplier<LivingEntity> body;

    SoulEchoAttributes(Supplier<LivingEntity> body) {
        super(AttributeSupplier.builder().build());
        this.body = body;
    }

    private AttributeMap attributes() {
        return body.get().getAttributes();
    }

    @Override
    public Set<AttributeInstance> getAttributesToSync() {
        return Collections.emptySet();
    }

    @Override
    public Set<AttributeInstance> getAttributesToUpdate() {
        return Collections.emptySet();
    }

    @Override
    public Collection<AttributeInstance> getSyncableAttributes() {
        return Collections.emptyList();
    }

    @Override
    @Nullable
    public AttributeInstance getInstance(Holder<Attribute> attribute) {
        return attributes().getInstance(attribute);
    }

    @Override
    public boolean hasAttribute(Holder<Attribute> attribute) {
        return attributes().hasAttribute(attribute);
    }

    @Override
    public boolean hasModifier(Holder<Attribute> attribute, ResourceLocation id) {
        return attributes().hasModifier(attribute, id);
    }

    @Override
    public double getValue(Holder<Attribute> attribute) {
        return attributes().getValue(attribute);
    }

    @Override
    public double getBaseValue(Holder<Attribute> attribute) {
        return attributes().getBaseValue(attribute);
    }

    @Override
    public double getModifierValue(Holder<Attribute> attribute, ResourceLocation id) {
        return attributes().getModifierValue(attribute, id);
    }

    @Override
    public void addTransientAttributeModifiers(Multimap<Holder<Attribute>, AttributeModifier> modifiers) {
        attributes().addTransientAttributeModifiers(modifiers);
    }

    @Override
    public void removeAttributeModifiers(Multimap<Holder<Attribute>, AttributeModifier> modifiers) {
        attributes().removeAttributeModifiers(modifiers);
    }

    @Override
    public void assignAllValues(AttributeMap source) {
        attributes().assignAllValues(unwrap(source));
    }

    @Override
    public void assignBaseValues(AttributeMap source) {
        attributes().assignBaseValues(unwrap(source));
    }

    @Override
    public ListTag save() {
        return attributes().save();
    }

    @Override
    public void load(ListTag tag) {
        attributes().load(tag);
    }

    private static AttributeMap unwrap(AttributeMap source) {
        return source instanceof SoulEchoAttributes echo ? echo.attributes() : source;
    }
}
