package org.brahypno.maledict.common.attribute;

import com.mojang.datafixers.util.Pair;
import com.sammy.malum.core.listeners.MalignantConversionReloadListener;
import com.sammy.malum.core.listeners.MalignantConversionReloadListener.MalignantConversionData;
import com.sammy.malum.registry.common.AttributeRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import org.brahypno.maledict.config.MaledictConfig;
import org.brahypno.maledict.registry.MaledictAttributes;
import team.lodestar.lodestone.registry.common.LodestoneAttributeRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class TranscendOmenTest {
    private static Attribute magic() { return LodestoneAttributeRegistry.MAGIC_PROFICIENCY.get(); }
    // A real defensive attribute allowing the specification's .25 -> .125 numeric example.
    // Lodestone's MAGIC_RESISTANCE has a minimum of 1 and cannot represent that example.
    private static Attribute resistance() { return Attributes.KNOCKBACK_RESISTANCE; }
    private static Attribute capacity() { return AttributeRegistry.SOUL_WARD_CAP.get(); }

    @GameTest(template = "test_empty", batch = "transcend_omen")
    public static void equipmentChangeDuringConversionCannotCacheOldStrength(GameTestHelper helper) {
        try (Fixture f = new Fixture(helper)) {
            f.rules(rule(magic()));
            f.base(magic(), 2);
            Consumer<LivingEvent.LivingTickEvent> equip = event -> {
                if (event.getEntity() == f.entity) {
                    f.base(MaledictAttributes.TRANSCEND_OMEN.get(), 0.75);
                }
            };
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingEvent.LivingTickEvent.class, equip);
            try {
                f.omen(0.5);
            } finally {
                MinecraftForge.EVENT_BUS.unregister(equip);
            }
            f.tick();
            f.expect(2.75, 5, 0.0625, "Equipment changes during the event must be reflected on the next tick");

            f.base(AttributeRegistry.MALIGNANT_CONVERSION.get(), 0.5);
            Consumer<LivingEvent.LivingTickEvent> effect = event -> {
                if (event.getEntity() == f.entity) {
                    f.base(AttributeRegistry.MALIGNANT_CONVERSION.get(), 0.7);
                }
            };
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingEvent.LivingTickEvent.class, effect);
            try {
                f.tick();
            } finally {
                MinecraftForge.EVENT_BUS.unregister(effect);
            }
            f.tick();
            f.expect(2.05, 19, 0.2375, "A fully cancelled malignant attribute must still detect an effect change");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "transcend_omen")
    public static void confirmedSignedSequenceAndFullInputConsumption(GameTestHelper helper) {
        try (Fixture f = new Fixture(helper)) {
            f.rules(rule(magic()));
            f.base(magic(), 1);
            f.entity.getAttribute(magic()).addTransientModifier(new AttributeModifier(UUID.randomUUID(),
                    "Equipment proficiency", 1, AttributeModifier.Operation.ADDITION));
            f.tick(); // Malum's ordinary zero-conversion initial state.
            f.base(AttributeRegistry.MALIGNANT_CONVERSION.get(), 0.5);
            f.tick();
            f.expect(1.5, 25, 0.375, "M=.5, R=0: ordinary Malum formulas");
            f.omen(0.25);
            f.expect(1.75, 22.5, 0.3125, "M=.5, R=.25: forward .25");
            f.omen(0.5);
            f.expect(2, 20, 0.25, "M=.5, R=.5: both directions off");
            f.omen(0.75);
            f.expect(2.25, 15, 0.1875, "M=.5, R=.75: reverse .25");
            f.base(AttributeRegistry.MALIGNANT_CONVERSION.get(), 0);
            f.omen(0.5);
            f.expect(2.5, 10, 0.125, "Confirmed option 2: excess armor is still consumed by 50%");
            f.omen(0);
            f.expect(2, 20, 0.25, "Removing Omen restores the complete original state");

            // 100% MULTIPLY_TOTAL suppression must be removed, never counteracted after the fact.
            f.rules(new MalignantConversionData(magic(), 1, true, rule(magic()).targetAttributes()));
            f.base(AttributeRegistry.MALIGNANT_CONVERSION.get(), 1);
            f.tick();
            f.equal(0, f.value(magic()), "Full forward suppression");
            f.omen(1);
            f.expect(2, 20, 0.25, "Full cancellation removes a -100% source modifier");
            f.base(AttributeRegistry.MALIGNANT_CONVERSION.get(), 0);
            f.tick();
            f.expect(3, 0, 0, "Full reverse consumption");
            f.base(AttributeRegistry.MALIGNANT_CONVERSION.get(), 1);
            f.omen(0);
            f.equal(0, f.value(magic()), "Reverse output must be gone before forward resumes");
            f.equal(30, f.value(Attributes.ARMOR), "Forward cannot convert its old reverse gain");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "transcend_omen")
    public static void defaultSourcesLeaveTheBudgetAndEquipmentReactivatesThem(GameTestHelper helper) {
        try (Fixture f = new Fixture(helper)) {
            f.rules(rule(magic()), rule(capacity()));
            f.base(magic(), 1);
            f.base(capacity(), 0);
            f.omen(0.5);
            f.expect(1, 20, 0.25, "Default-only sources create neither gains nor costs");
            UUID magicEquipment = UUID.randomUUID();
            f.entity.getAttribute(magic()).addTransientModifier(new AttributeModifier(magicEquipment,
                    "Proficiency equipment", 1, AttributeModifier.Operation.ADDITION));
            f.tick();
            f.expect(2.5, 10, 0.125, "A default capacity source must not dilute the equipped proficiency route");
            UUID capacityEquipment = UUID.randomUUID();
            f.entity.getAttribute(capacity()).addTransientModifier(new AttributeModifier(capacityEquipment,
                    "Capacity equipment", 1, AttributeModifier.Operation.ADDITION));
            f.tick();
            f.expect(2.25, 10, 0.125, "Two active routes split output and consume shared defense once");
            f.equal(1.25, f.value(capacity()), "Equipment activates a default-zero attribute");
            f.entity.getAttribute(magic()).removeModifier(magicEquipment);
            f.tick();
            f.expect(1, 10, 0.125, "Old reverse gains cannot keep an unequipped source active");
            f.equal(1.5, f.value(capacity()), "The remaining route receives the complete budget");
            f.entity.getAttribute(capacity()).removeModifier(capacityEquipment);
            f.tick();
            f.expect(1, 20, 0.25, "Removing the last equipped source restores all defense");
            f.equal(0, f.value(capacity()), "Default-zero source no longer participates");
            f.tick();
            f.expect(1, 20, 0.25, "An inactive route stays inactive on subsequent ticks");
            f.omen(0);
            f.expect(1, 20, 0.25, "Removing Omen preserves the original state");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "transcend_omen")
    public static void minimumShareCanExceedTheTotalBudgetAndRecalculates(GameTestHelper helper) {
        try (Fixture f = new Fixture(helper)) {
            f.rules(rule(magic()), rule(capacity()));
            f.base(magic(), 2);
            f.base(capacity(), 1);
            // Two routes with a 60% floor exercise the same over-budget branch as 101 routes at 1%.
            MaledictConfig.OMEN_MINIMUM_SHARE.set(0.6);
            f.omen(0.5);
            f.expect(2.3, 10, 0.125, "Each route receives its floor even when total shares reach 120%");
            f.equal(1.3, f.value(capacity()), "The floor applies equally to the second route");
            MaledictConfig.OMEN_MINIMUM_SHARE.set(0.01);
            f.tick();
            f.expect(2.25, 10, 0.125, "Changing the floor invalidates the cache and restores equal halves");
            f.equal(1.25, f.value(capacity()), "The floor is a minimum, not an extra additive share");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "transcend_omen")
    public static void fullConsumptionStillDetectsEquipmentAndDataReload(GameTestHelper helper) {
        try (Fixture f = new Fixture(helper)) {
            f.rules(rule(magic()));
            f.base(magic(), 2);
            f.base(capacity(), 1);
            f.base(resistance(), 1);
            f.omen(1);
            f.equal(4, f.value(magic()), "Armor limits the initial route to two");
            UUID equipment = UUID.randomUUID();
            f.entity.getAttribute(Attributes.ARMOR).addTransientModifier(new AttributeModifier(equipment,
                    "New equipment under full consumption", 5, AttributeModifier.Operation.ADDITION));
            f.tick();
            f.equal(4.5, f.value(magic()), "A defense displaying zero must still invalidate on new equipment");
            f.base(Attributes.ARMOR, 10);
            f.tick();
            f.equal(3.5, f.value(magic()), "Base-value changes must also invalidate under full consumption");

            // Malum's reload listener clears and repopulates the same map, rather than replacing its identity.
            f.rules(new MalignantConversionData(capacity(), 0.1, true, List.of(Pair.of(Attributes.ARMOR, 5D))));
            f.tick();
            f.equal(2, f.value(magic()), "Deleted route gain is removed");
            f.equal(4, f.value(capacity()), "New route reads all fifteen armor, ignoring consumptionRatio");
            f.equal(1, f.value(resistance()), "A target removed by reload regains its defense");
            f.rules();
            f.tick();
            f.equal(1, f.value(capacity()), "Empty datapack removes all old output");
            f.equal(15, f.value(Attributes.ARMOR), "Empty datapack removes all old costs");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "transcend_omen")
    public static void invalidRatiosAndMissingOrNonpositiveTargetsDoNotCreateRoutes(GameTestHelper helper) {
        try (Fixture f = new Fixture(helper)) {
            f.base(magic(), 2);
            f.base(capacity(), 1);
            f.base(Attributes.ARMOR_TOUGHNESS, 6);
            f.rules(new MalignantConversionData(magic(), 0.5, true, List.of(
                            Pair.of(Attributes.ARMOR, 10D), Pair.of(resistance(), Double.NaN),
                            Pair.of(Attributes.ARMOR_TOUGHNESS, 0D), Pair.of(Attributes.FLYING_SPEED, -1D),
                            Pair.of(Attributes.ATTACK_DAMAGE, Double.POSITIVE_INFINITY))),
                    new MalignantConversionData(capacity(), 1, false,
                            List.of(Pair.of(Attributes.FLYING_SPEED, 1D))));
            helper.assertTrue(f.entity.getAttribute(Attributes.FLYING_SPEED) == null, "Fixture needs a missing target");
            f.omen(0.5);
            f.equal(3, f.value(magic()), "Invalid ratios are skipped; the valid armor-only route remains");
            f.equal(0.25, f.value(resistance()), "Invalid target is not consumed");
            f.equal(6, f.value(Attributes.ARMOR_TOUGHNESS), "Zero ratio is not consumed");
            f.equal(1, f.value(capacity()), "A missing required target disables its route");
            f.rules(rule(magic()));
            f.base(resistance(), 0);
            f.tick();
            f.equal(2, f.value(magic()), "Nonpositive required candidate disables the whole route");
            f.equal(20, f.value(Attributes.ARMOR), "An inactive route consumes nothing");
            f.rules(new MalignantConversionData(magic(), 1, false, List.of(Pair.of(Attributes.ARMOR, 0D))));
            f.tick();
            f.equal(2, f.value(magic()), "No valid targets means no route");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "transcend_omen")
    public static void overlappingGraphUsesOneCleanSnapshot(GameTestHelper helper) {
        try (Fixture f = new Fixture(helper)) {
            f.base(magic(), 2);
            f.base(capacity(), 1);
            f.rules(new MalignantConversionData(magic(), 1, false, List.of(Pair.of(Attributes.ARMOR, 10D))),
                    new MalignantConversionData(capacity(), 1, false, List.of(Pair.of(magic(), 1D))));
            f.omen(0.5);
            f.equal(1.5, f.value(capacity()), "A route sees baseline magic, not another route's just-applied gain");
            f.equal(1.25, f.value(magic()), "Overlapping source is independently gained and consumed");
            f.base(Attributes.ARMOR, 10);
            f.tick();
            f.equal(1.5, f.value(capacity()), "Recalculation removes both old costs and gains before measuring");
            f.equal(1.125, f.value(magic()), "New armor produces a new gain with no recursive amplification");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "transcend_omen")
    public static void fullCurrentInputCostIncludesModifiersAndAttributeCaps(GameTestHelper helper) {
        try (Fixture f = new Fixture(helper)) {
            f.rules(new MalignantConversionData(magic(), 0.5, true, List.of(Pair.of(resistance(), 1D))));
            f.base(magic(), 2);
            f.base(resistance(), 0.5);
            var input = f.entity.getAttribute(resistance());
            input.addTransientModifier(new AttributeModifier(UUID.randomUUID(), "Equipment", 0.5,
                    AttributeModifier.Operation.ADDITION));
            input.addTransientModifier(new AttributeModifier(UUID.randomUUID(), "Base scaling", 1,
                    AttributeModifier.Operation.MULTIPLY_BASE));
            input.addTransientModifier(new AttributeModifier(UUID.randomUUID(), "Total scaling", 1,
                    AttributeModifier.Operation.MULTIPLY_TOTAL));
            f.equal(1, f.value(resistance()), "Raw four is capped to current one");
            f.omen(0.5);
            f.equal(2.5, f.value(magic()), "Gain uses the full current value, not the uncapped raw value");
            f.equal(0.5, f.value(resistance()), "Cost must halve current defense even when raw defense exceeded its cap");
            f.omen(0);
            f.equal(1, f.value(resistance()), "Clearing the cost restores the attribute's original capped value");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "transcend_omen")
    public static void utilityCancelsActualAttributesAndRestoresIndependentSources(GameTestHelper helper) {
        try (Fixture f = new Fixture(helper)) {
            f.rules(rule(magic()));
            f.base(magic(), 2);
            f.base(AttributeRegistry.MALIGNANT_CONVERSION.get(), 0.5);
            UUID first = UUID.randomUUID();
            UUID second = UUID.randomUUID();
            TranscendOmenUtil.setAmount(f.entity, first, 0.5);
            f.equal(0, f.value(AttributeRegistry.MALIGNANT_CONVERSION.get()), "Utility immediately cancels actual Malum scalar");
            f.equal(0.5, f.value(MaledictAttributes.TRANSCEND_OMEN.get()), "Omen retains its granted amount");
            f.tick();
            f.expect(2, 20, 0.25, "Complete cancellation survives the ordinary Malum event handler");
            f.equal(0.25, TranscendOmenUtil.setAmount(f.entity, second, 0.25), "Utility returns only the unspent reverse remainder");
            f.tick();
            f.expect(2.25, 15, 0.1875, "Independent Omen sources add before cancellation");
            f.equal(0.75, f.value(MaledictAttributes.TRANSCEND_OMEN.get()), "Omen's attribute itself is not reduced by cancellation");

            UUID fallen = UUID.randomUUID();
            var malignant = f.entity.getAttribute(AttributeRegistry.MALIGNANT_CONVERSION.get());
            malignant.addTransientModifier(new AttributeModifier(fallen, "Later Malum effect", 0.2,
                    AttributeModifier.Operation.ADDITION));
            f.tick();
            f.equal(2.05, f.value(magic()), "A later opposing effect leaves only .05 reverse gain");
            malignant.removeModifier(fallen);
            f.tick();
            f.equal(2.25, f.value(magic()), "Removing that effect restores the .25 remainder");

            TranscendOmenUtil.setAmount(f.entity, first, 0);
            f.tick();
            f.equal(0.25, f.value(AttributeRegistry.MALIGNANT_CONVERSION.get()), "Removing one Omen source restores forward conversion");
            f.expect(1.75, 20, 0.25, "Remaining malignant conversion skips reverse gain and defense consumption");
            TranscendOmenUtil.setAmount(f.entity, second, 0);
            f.tick();
            f.equal(0.5, f.value(AttributeRegistry.MALIGNANT_CONVERSION.get()), "Removing all Omen restores original Malum conversion");
            f.equal(0, f.entity.getAttribute(MaledictAttributes.TRANSCEND_OMEN.get()).getBaseValue(), "Utility never changes base values");
            f.equal(0.5, malignant.getBaseValue(), "Malum's base value is also preserved");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "transcend_omen")
    public static void loadedMalumRulesPreserveMagicResistanceFloor(GameTestHelper helper) {
        try (Fixture f = new Fixture(helper)) {
            Attribute recovery = AttributeRegistry.SOUL_WARD_RECOVERY_RATE.get();
            Attribute integrity = AttributeRegistry.SOUL_WARD_INTEGRITY.get();
            Attribute magicResistance = LodestoneAttributeRegistry.MAGIC_RESISTANCE.get();
            // Read the actual loaded recipes, including their target ratios; isolate the four core Malum routes.
            f.rules(f.original.get(ForgeRegistries.ATTRIBUTES.getKey(magic())),
                    f.original.get(ForgeRegistries.ATTRIBUTES.getKey(capacity())),
                    f.original.get(ForgeRegistries.ATTRIBUTES.getKey(recovery)),
                    f.original.get(ForgeRegistries.ATTRIBUTES.getKey(integrity)));
            f.base(magic(), 2);
            f.base(capacity(), 1);
            f.base(recovery, 2);
            f.base(integrity, 2);
            f.base(Attributes.ARMOR_TOUGHNESS, 8);
            f.base(magicResistance, 2);
            f.omen(1);
            f.equal(2.5, f.value(magic()), "Full reverse proficiency +.5");
            f.equal(6, f.value(capacity()), "Full reverse capacity +5");
            f.equal(2.5, f.value(recovery), "Full reverse recovery +.5");
            f.equal(2.2, f.value(integrity), "Full reverse integrity +.2");
            f.equal(0, f.value(Attributes.ARMOR), "Full reverse consumes all armor");
            f.equal(0, f.value(Attributes.ARMOR_TOUGHNESS), "Full reverse consumes all toughness");
            f.equal(1, f.value(magicResistance), "Even full consumption cannot break the dependency's lower bound");
        }
        helper.succeed();
    }

    private static MalignantConversionData rule(Attribute source) {
        return new MalignantConversionData(source, 0.5, true,
                List.of(Pair.of(Attributes.ARMOR, 10D), Pair.of(resistance(), 0.25D)));
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper helper;
        final LivingEntity entity;
        final Map<ResourceLocation, MalignantConversionData> original = MalignantConversionReloadListener.CONVERSION_DATA;
        final double originalMinimum = MaledictConfig.OMEN_MINIMUM_SHARE.get();

        Fixture(GameTestHelper helper) {
            this.helper = helper;
            entity = EntityType.ZOMBIE.create(helper.getLevel());
            // Do not add this entity to the world: each fixture changes the global datapack synchronously.
            MalignantConversionReloadListener.CONVERSION_DATA = new LinkedHashMap<>();
            base(Attributes.ARMOR, 20);
            base(resistance(), 0.25);
            base(capacity(), 0);
            // Numeric expectations use the default floor, independent of the local test-server config.
            MaledictConfig.OMEN_MINIMUM_SHARE.set(0.01);
        }

        void rules(MalignantConversionData... rules) {
            var data = MalignantConversionReloadListener.CONVERSION_DATA;
            data.clear();
            for (var rule : rules) data.put(ForgeRegistries.ATTRIBUTES.getKey(rule.sourceAttribute()), rule);
        }

        void base(Attribute attribute, double value) { entity.getAttribute(attribute).setBaseValue(value); }
        double value(Attribute attribute) { return entity.getAttributeValue(attribute); }
        void tick() { MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingTickEvent(entity)); }
        void omen(double value) { base(MaledictAttributes.TRANSCEND_OMEN.get(), value); tick(); }
        void expect(double magic, double armor, double resistance, String message) {
            equal(magic, value(magic()), message + " / proficiency");
            equal(armor, value(Attributes.ARMOR), message + " / armor");
            equal(resistance, value(resistance()), message + " / resistance");
        }
        void equal(double expected, double actual, String message) {
            helper.assertTrue(Math.abs(expected - actual) < 1e-8, message + ": expected " + expected + ", got " + actual);
        }
        @Override public void close() {
            MaledictConfig.OMEN_MINIMUM_SHARE.set(originalMinimum);
            MalignantConversionReloadListener.CONVERSION_DATA = original;
            entity.discard();
        }
    }
}
