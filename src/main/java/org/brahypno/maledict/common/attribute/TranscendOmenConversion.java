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
import org.brahypno.maledict.config.MaledictConfig;
import org.brahypno.maledict.registry.MaledictAttributes;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/** The reverse half of Malum's signed conversion axis. Only the server owns modifiers. */
@Mod.EventBusSubscriber(modid = Maledict.MODID)
public final class TranscendOmenConversion {
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
        double minimumShare = MaledictConfig.OMEN_MINIMUM_SHARE.get();
        if (!dataChanged
                && minimumShare == state.minimumShare
                && snapshot(entity, state.watched).equals(state.snapshot)) {
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
        state.minimumShare = minimumShare;
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
        if (strength <= 0 || (malignant != null && malignant.getValue() > 0)) {
            state.snapshot = snapshot(entity, state.watched);
            return;
        }

        List<Route> routes = new ArrayList<>();
        for (var rule : state.data.values()) {
            AttributeInstance source = entity.getAttribute(rule.sourceAttribute());
            // Read the clean source before applying any reverse gain or consumption.
            if (source == null || source.getValue() == rule.sourceAttribute().getDefaultValue()) {
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
                double candidate = instance == null ? 0 : instance.getValue() / ratio;
                if (!Double.isFinite(candidate) || candidate <= 0) {
                    base = 0;
                    break;
                }
                base = Math.min(base, candidate);
                targets.add(target.getFirst());
            }
            if (Double.isFinite(base) && base > 0) {
                routes.add(new Route(rule.sourceAttribute(), base, targets));
            }
        }

        if (!routes.isEmpty()) {
            // A hard per-route floor may deliberately take the total budget above 100%.
            double share = Math.max(1D / routes.size(), state.minimumShare);
            Set<Attribute> consumed = new HashSet<>();
            for (Route route : routes) {
                apply(entity, state, route.source(), "gain", route.base() * strength * share,
                        AttributeModifier.Operation.ADDITION);
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

    private record Route(Attribute source, double base, Set<Attribute> targets) {}
    private record ModifierState(UUID id, double amount, AttributeModifier.Operation operation) {}
    private record AttributeState(double base, Set<ModifierState> modifiers) {}

    private static final class State {
        boolean initialized;
        boolean rebuilding;
        double reverseStrength;
        double minimumShare;
        Map<ResourceLocation, MalignantConversionData> data = Map.of();
        final Set<Attribute> watched = new HashSet<>();
        final Map<Attribute, Set<UUID>> applied = new HashMap<>();
        Map<Attribute, AttributeState> conversionInputs = Map.of();
        Map<Attribute, AttributeState> snapshot = Map.of();
    }
}
