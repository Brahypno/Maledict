package org.brahypno.maledict.common.attribute;

import com.sammy.malum.common.capability.MalumLivingEntityDataCapability;
import com.sammy.malum.core.handlers.MalignantConversionHandler;
import com.sammy.malum.core.listeners.MalignantConversionReloadListener;
import com.sammy.malum.core.listeners.MalignantConversionReloadListener.MalignantConversionData;
import com.sammy.malum.registry.common.AttributeRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictAttributes;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/** The reverse half of Malum's signed conversion axis. Only the server owns modifiers. */
@Mod.EventBusSubscriber(modid = Maledict.MODID)
public final class TranscendOmenConversion {
    private static final int BASELINE_SOURCE_COUNT = 4;
    private static final double SATURATION_LIMIT = 4;
    private static final double HALF_SATURATION = 5;

    // Values contain attributes and numbers only, never a reference back to the entity.
    private static final Map<LivingEntity, State> STATES = new WeakHashMap<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void prepareConversion(LivingEvent.LivingTickEvent event) {
        beforeForwardConversion(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void finishConversion(LivingEvent.LivingTickEvent event) {
        afterForwardConversion(event.getEntity());
    }

    public static double omen(LivingEntity entity) {
        AttributeInstance instance = entity.getAttribute(MaledictAttributes.TRANSCEND_OMEN.get());
        return instance == null ? 0 : instance.getValue();
    }

    private static void beforeForwardConversion(LivingEntity entity) {
        if (entity.level().isClientSide) {
            return;
        }
        State state = STATES.get(entity);
        if (state == null && omen(entity) == 0) {
            return;
        }
        if (state == null) {
            state = new State();
            STATES.put(entity, state);
        }
        state.rebuilding = false;
        var data = MalignantConversionReloadListener.CONVERSION_DATA;
        boolean dataChanged = !state.initialized || !state.data.equals(data);
        if (!dataChanged && snapshot(entity, state.watched).equals(state.snapshot)) {
            return;
        }

        // Never measure a new reverse result through the old output or its defensive penalties.
        state.applied.forEach((attribute, ids) -> {
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance != null) {
                ids.forEach(instance::removeModifier);
            }
        });
        state.applied.clear();
        state.reverseStrength = TranscendOmenUtil.reduceMalignantConversion(entity);
        if (dataChanged) {
            // Reload can delete or retarget a rule. Malum only visits the new rules, so clean the old ones here.
            clearForwardModifiers(entity, state.data.values());
            clearForwardModifiers(entity, data.values());
            var handler = MalumLivingEntityDataCapability.getCapability(entity).malignantConversionHandler;
            handler.cachedAttributeValues.clear();
            handler.cachedAttributeValues.put(AttributeRegistry.MALIGNANT_CONVERSION.get(), Double.NaN);
            handler.skipConversionLogic = false;
            state.data = Map.copyOf(data);
            state.watched.clear();
            state.watched.add(AttributeRegistry.MALIGNANT_CONVERSION.get());
            state.watched.add(MaledictAttributes.TRANSCEND_OMEN.get());
            for (var rule : data.values()) {
                state.watched.add(rule.sourceAttribute());
                for (var target : rule.targetAttributes()) {
                    state.watched.add(target.getFirst());
                }
            }
            state.initialized = true;
        }
        state.conversionInputs = conversionInputs(entity);
        state.rebuilding = true;
    }

    /** Runs after Malum has removed any now-inactive forward bonuses and source penalties. */
    private static void afterForwardConversion(LivingEntity entity) {
        if (entity.level().isClientSide) {
            return;
        }
        State state = STATES.get(entity);
        if (state == null || !state.rebuilding) {
            return;
        }
        state.rebuilding = false;
        // Curios/effect handlers can change either scalar between HIGHEST and LOWEST.
        // Do not cache the new inputs against a result calculated with the old strength.
        // Include base/modifiers: a -100% cancellation hides changes to malignant.getValue().
        if (!conversionInputs(entity).equals(state.conversionInputs)) {
            state.snapshot = Map.of();
            return;
        }
        double strength = state.reverseStrength;
        AttributeInstance malignant = entity.getAttribute(AttributeRegistry.MALIGNANT_CONVERSION.get());
        if (!Double.isFinite(strength) || strength <= 0 || (malignant != null && malignant.getValue() > 0)) {
            state.snapshot = snapshot(entity, state.watched);
            return;
        }

        Map<Attribute, Route> routes = new HashMap<>();
        for (var entry : state.data.entrySet()) {
            var rule = entry.getValue();
            Attribute source = rule.sourceAttribute();
            AttributeInstance sourceInstance = entity.getAttribute(source);
            // Read the clean source before applying any reverse gain or consumption.
            if (sourceInstance == null) {
                continue;
            }
            double sourceValue = sourceInstance.getValue();
            if (!Double.isFinite(sourceValue) || sourceValue == source.getDefaultValue()) {
                continue;
            }
            double base = Double.POSITIVE_INFINITY;
            Set<Attribute> targets = new HashSet<>();
            for (var target : rule.targetAttributes()) {
                double ratio = target.getSecond();
                if (!Double.isFinite(ratio) || ratio <= 0) {
                    continue;
                }
                AttributeInstance instance = entity.getAttribute(target.getFirst());
                double value = instance == null ? 0 : instance.getValue();
                if (!Double.isFinite(value) || value <= 0) {
                    base = 0;
                    break;
                }
                // A finite, positive quotient can overflow. Keep it above any finite bottleneck.
                double candidate = Math.min(value / ratio, Double.MAX_VALUE);
                base = Math.min(base, candidate);
                targets.add(target.getFirst());
            }
            if (Double.isFinite(base) && base > 0) {
                routes.merge(source, new Route(entry.getKey(), base, targets), TranscendOmenConversion::preferredRoute);
            }
        }

        if (!routes.isEmpty()) {
            Set<Attribute> consumed = new HashSet<>();
            for (var entry : routes.entrySet()) {
                Route route = entry.getValue();
                double gain = gainFor(route.base(), strength, routes.size());
                if (gain > 0) {
                    apply(entity, state, entry.getKey(), "gain", gain, AttributeModifier.Operation.MULTIPLY_TOTAL);
                }
                consumed.addAll(route.targets());
            }
            // Confirmed option 2: all participating defense loses the full reverse percentage ONCE,
            // regardless of the limiting target or how many routes share that defense.
            for (Attribute target : consumed) {
                apply(entity, state, target, "consumption", TranscendOmenUtil.reductionModifier(entity.getAttribute(target), strength),
                        AttributeModifier.Operation.MULTIPLY_TOTAL);
            }
        }
        state.snapshot = snapshot(entity, state.watched);
    }

    // Malum keys the loaded table by source. Extension code can add aliases to that table.
    private static Route preferredRoute(Route first, Route second) {
        if (first.base() != second.base()) {
            return first.base() > second.base() ? first : second;
        }
        return first.id().toString().compareTo(second.id().toString()) <= 0 ? first : second;
    }

    static double gainFor(double base, double strength, int sourceCount) {
        double saturated = SATURATION_LIMIT * (base / (HALF_SATURATION + base));
        return saturated * strength / Math.max(BASELINE_SOURCE_COUNT, sourceCount);
    }

    private static void apply(LivingEntity entity, State state, Attribute attribute, String kind,
                              double amount, AttributeModifier.Operation operation) {
        UUID id = UUID.nameUUIDFromBytes(("maledict:transcend_omen/" + kind + "/"
                + ForgeRegistries.ATTRIBUTES.getKey(attribute)).getBytes(StandardCharsets.UTF_8));
        entity.getAttribute(attribute).addTransientModifier(new AttributeModifier(id, "Transcend Omen", amount, operation));
        state.applied.computeIfAbsent(attribute, ignored -> new HashSet<>()).add(id);
    }

    private static void clearForwardModifiers(LivingEntity entity, Iterable<MalignantConversionData> rules) {
        for (var rule : rules) {
            AttributeInstance source = entity.getAttribute(rule.sourceAttribute());
            if (source != null) {
                source.removeModifier(MalignantConversionHandler.NEGATIVE_MODIFIER_UUID);
            }
            UUID positiveId = MalignantConversionHandler.POSITIVE_MODIFIER_UUIDS.get(rule.sourceAttribute());
            if (positiveId != null) {
                for (var target : rule.targetAttributes()) {
                    AttributeInstance instance = entity.getAttribute(target.getFirst());
                    if (instance != null) {
                        instance.removeModifier(positiveId);
                    }
                }
            }
        }
    }

    private static Map<Attribute, AttributeState> conversionInputs(LivingEntity entity) {
        return snapshot(entity, Set.of(AttributeRegistry.MALIGNANT_CONVERSION.get(),
                MaledictAttributes.TRANSCEND_OMEN.get()));
    }

    private static Map<Attribute, AttributeState> snapshot(LivingEntity entity, Set<Attribute> watched) {
        Map<Attribute, AttributeState> values = new HashMap<>();
        for (Attribute attribute : watched) {
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance != null) {
                Set<ModifierState> modifiers = new HashSet<>();
                for (AttributeModifier modifier : instance.getModifiers()) {
                    modifiers.add(new ModifierState(modifier.getId(), modifier.getAmount(), modifier.getOperation()));
                }
                // Checking only getValue() misses equipment changes under a -100% penalty.
                values.put(attribute, new AttributeState(instance.getBaseValue(), modifiers));
            }
        }
        return values;
    }

    private record Route(ResourceLocation id, double base, Set<Attribute> targets) {}
    private record ModifierState(UUID id, double amount, AttributeModifier.Operation operation) {}
    private record AttributeState(double base, Set<ModifierState> modifiers) {}

    private static final class State {
        boolean initialized;
        boolean rebuilding;
        double reverseStrength;
        Map<ResourceLocation, MalignantConversionData> data = Map.of();
        final Set<Attribute> watched = new HashSet<>();
        final Map<Attribute, Set<UUID>> applied = new HashMap<>();
        Map<Attribute, AttributeState> conversionInputs = Map.of();
        Map<Attribute, AttributeState> snapshot = Map.of();
    }
}
