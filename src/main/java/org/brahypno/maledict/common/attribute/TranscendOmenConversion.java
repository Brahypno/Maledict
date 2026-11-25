package org.brahypno.maledict.common.attribute;

import net.minecraft.core.Holder;
import com.sammy.malum.registry.common.MalumAttachmentTypes;
import com.sammy.malum.core.handlers.MalignantConversionHandler;
import com.sammy.malum.core.listeners.MalignantConversionReloadListener;
import com.sammy.malum.core.listeners.MalignantConversionReloadListener.MalignantConversionData;
import com.sammy.malum.registry.common.MalumAttributes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.brahypno.maledict.Maledict;
import org.brahypno.maledict.registry.MaledictAttributes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** The reverse half of Malum's signed conversion axis. Only the server owns modifiers. */
@EventBusSubscriber(modid = Maledict.MODID)
public final class TranscendOmenConversion {
    // Shares are split as one over max(4, sources), so the four native Malum routes already spend the budget.
    private static final int BASELINE_SOURCE_COUNT = 4;
    // f(b) = 4b / (5 + b) approaches four without ever reaching it for a finite base.
    private static final double SATURATION_LIMIT = 4;
    private static final double HALF_SATURATION = 5;

    // Values contain attributes and numbers only, never a reference back to the entity.
    private static final Map<LivingEntity, State> STATES = new WeakHashMap<>();

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void prepareConversion(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) return;
        beforeForwardConversion(livingEntity);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void finishConversion(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity livingEntity)) return;
        afterForwardConversion(livingEntity);
    }

    public static double omen(LivingEntity entity) {
        AttributeInstance instance = entity.getAttribute(MaledictAttributes.TRANSCEND_OMEN);
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
            var handler = entity.getData(MalumAttachmentTypes.MALIGNANT_INFLUENCE);
            ((org.brahypno.maledict.mixin.MalignantInfluenceDataAccessor)(Object)handler).maledict$cachedAttributeValues().clear();
            ((org.brahypno.maledict.mixin.MalignantInfluenceDataAccessor)(Object)handler).maledict$cachedAttributeValues().put(MalumAttributes.MALIGNANT_CONVERSION, Double.NaN);
            state.data = Map.copyOf(data);
            state.watched.clear();
            state.watched.add(MalumAttributes.MALIGNANT_CONVERSION);
            state.watched.add(MaledictAttributes.TRANSCEND_OMEN);
            for (var rule : data.values()) {
                state.watched.add(rule.sourceAttribute());
                for (var target : rule.payoutData()) {
                    state.watched.add(target.attribute());
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
        AttributeInstance malignant = entity.getAttribute(MalumAttributes.MALIGNANT_CONVERSION);
        if (!Double.isFinite(strength) || strength <= 0 || (malignant != null && malignant.getValue() > 0)) {
            state.snapshot = snapshot(entity, state.watched);
            return;
        }

        // One route per source. Malum keys its table by source, but extension code can inject aliases.
        Map<Holder<Attribute>, Route> routes = new HashMap<>();
        for (var entry : state.data.entrySet()) {
            var rule = entry.getValue();
            Holder<Attribute> source = rule.sourceAttribute();
            AttributeInstance sourceInstance = entity.getAttribute(source);
            // Read the clean source before applying any reverse gain or consumption.
            if (sourceInstance == null) {
                continue;
            }
            double sourceValue = sourceInstance.getValue();
            if (!Double.isFinite(sourceValue) || sourceValue == source.value().getDefaultValue()) {
                continue;
            }
            double base = Double.POSITIVE_INFINITY;
            Set<Holder<Attribute>> targets = new HashSet<>();
            for (var target : rule.payoutData()) {
                double ratio = target.ratio();
                if (!Double.isFinite(ratio) || ratio <= 0) {
                    continue;
                }
                AttributeInstance instance = entity.getAttribute(target.attribute());
                // Native conversion creates Aegis. Requiring it at zero would block armor-only reversal.
                // A positive Aegis value still constrains this route and pays the ordinary consumption.
                if (isAegisAttribute(target.attribute()) && (instance == null || instance.getValue() == 0)) {
                    continue;
                }
                double value = instance == null ? 0 : instance.getValue();
                if (!Double.isFinite(value) || value <= 0) {
                    base = 0;
                    break;
                }
                // A finite, positive quotient can overflow. Keep it above any finite bottleneck.
                double candidate = Math.min(value / ratio, Double.MAX_VALUE);
                base = Math.min(base, candidate);
                targets.add(target.attribute());
            }
            if (Double.isFinite(base) && base > 0) {
                routes.merge(source, new Route(routeId(entry.getKey()), base, targets),
                        TranscendOmenConversion::preferredRoute);
            }
        }

        if (!routes.isEmpty()) {
            Set<Holder<Attribute>> consumed = new HashSet<>();
            for (var entry : routes.entrySet()) {
                Route route = entry.getValue();
                double gain = gainFor(route.base(), strength, routes.size());
                if (gain > 0) {
                    apply(entity, state, entry.getKey(), "gain", gain,
                            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
                }
                consumed.addAll(route.targets());
            }
            // Confirmed option 2: all participating defense loses the full reverse percentage ONCE,
            // regardless of the limiting target or how many routes share that defense.
            for (Holder<Attribute> target : consumed) {
                apply(entity, state, target, "consumption", TranscendOmenUtil.reductionModifier(entity.getAttribute(target), strength),
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
            }
        }
        state.snapshot = snapshot(entity, state.watched);
    }

    // The table key is the only identity an injected alias shares with the canonical rule.
    private static String routeId(Holder<Attribute> key) {
        return key.unwrapKey().map(ResourceKey::location).map(ResourceLocation::toString).orElse("");
    }

    // Highest coefficient wins; equal coefficients settle on the lexicographically smallest full key.
    private static Route preferredRoute(Route first, Route second) {
        if (first.base() != second.base()) {
            return first.base() > second.base() ? first : second;
        }
        return first.id().compareTo(second.id()) <= 0 ? first : second;
    }

    static double gainFor(double base, double strength, int sourceCount) {
        // 4 * (b / (5 + b)) never multiplies by the base first, so an absurd ratio cannot overflow.
        double saturated = SATURATION_LIMIT * (base / (HALF_SATURATION + base));
        return saturated * strength / Math.max(BASELINE_SOURCE_COUNT, sourceCount);
    }

    private static boolean isAegisAttribute(Holder<Attribute> attribute) {
        return attribute.equals(MalumAttributes.MALIGNANT_AEGIS_CAPACITY)
                || attribute.equals(MalumAttributes.MALIGNANT_AEGIS_RECOVERY_RATE)
                || attribute.equals(MalumAttributes.MALIGNANT_AEGIS_RECOVERY_GAIN);
    }

    private static void apply(LivingEntity entity, State state, Holder<Attribute> attribute, String kind,
                              double amount, AttributeModifier.Operation operation) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("maledict", "transcend_omen/" + kind + "/"
                + attribute.unwrapKey().orElseThrow().location().toString().replace(':', '/'));
        entity.getAttribute(attribute).addTransientModifier(new AttributeModifier(id, amount, operation));
        state.applied.computeIfAbsent(attribute, ignored -> new HashSet<>()).add(id);
    }

    private static void clearForwardModifiers(LivingEntity entity, Iterable<MalignantConversionData> rules) {
        for (var rule : rules) {
            AttributeInstance source = entity.getAttribute(rule.sourceAttribute());
            if (source != null) {
                source.removeModifier(MalignantConversionHandler.NEGATIVE_MODIFIER_ID);
            }
            ResourceLocation positiveId = MalignantConversionHandler.POSITIVE_MODIFIER_IDS.apply(rule.sourceAttribute());
            if (positiveId != null) {
                for (var target : rule.payoutData()) {
                    AttributeInstance instance = entity.getAttribute(target.attribute());
                    if (instance != null) {
                        instance.removeModifier(positiveId);
                    }
                }
            }
        }
    }

    private static Map<Holder<Attribute>, AttributeState> conversionInputs(LivingEntity entity) {
        return snapshot(entity, Set.of(MalumAttributes.MALIGNANT_CONVERSION,
                MaledictAttributes.TRANSCEND_OMEN));
    }

    private static Map<Holder<Attribute>, AttributeState> snapshot(LivingEntity entity, Set<Holder<Attribute>> watched) {
        Map<Holder<Attribute>, AttributeState> values = new HashMap<>();
        for (Holder<Attribute> attribute : watched) {
            AttributeInstance instance = entity.getAttribute(attribute);
            if (instance != null) {
                Set<ModifierState> modifiers = new HashSet<>();
                for (AttributeModifier modifier : instance.getModifiers()) {
                    modifiers.add(new ModifierState(modifier.id(), modifier.amount(), modifier.operation()));
                }
                // Checking only getValue() misses equipment changes under a -100% penalty.
                values.put(attribute, new AttributeState(instance.getBaseValue(), modifiers));
            }
        }
        return values;
    }

    private record Route(String id, double base, Set<Holder<Attribute>> targets) {}
    private record ModifierState(ResourceLocation id, double amount, AttributeModifier.Operation operation) {}
    private record AttributeState(double base, Set<ModifierState> modifiers) {}

    private static final class State {
        boolean initialized;
        boolean rebuilding;
        double reverseStrength;
        Map<Holder<Attribute>, MalignantConversionData> data = Map.of();
        final Set<Holder<Attribute>> watched = new HashSet<>();
        final Map<Holder<Attribute>, Set<ResourceLocation>> applied = new HashMap<>();
        Map<Holder<Attribute>, AttributeState> conversionInputs = Map.of();
        Map<Holder<Attribute>, AttributeState> snapshot = Map.of();
    }
}
