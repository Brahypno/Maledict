package org.brahypno.maledict.common.attribute;

import com.mojang.datafixers.util.Pair;
import com.sammy.malum.core.listeners.MalignantConversionReloadListener;
import com.sammy.malum.core.listeners.MalignantConversionReloadListener.MalignantConversionData;
import com.sammy.malum.registry.common.MalumAttributes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.core.registries.BuiltInRegistries;
import org.brahypno.maledict.config.MaledictConfig;
import org.brahypno.maledict.registry.MaledictAttributes;
import team.lodestar.lodestone.registry.common.LodestoneAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class TranscendOmenTest {
    private static net.minecraft.core.Holder<Attribute> magic() { return LodestoneAttributes.MAGIC_PROFICIENCY; }
    // A real defensive attribute allowing the specification's .25 -> .125 numeric example.
    // Lodestone's MAGIC_RESISTANCE has a minimum of 1 and cannot represent that example.
    private static net.minecraft.core.Holder<Attribute> resistance() { return Attributes.KNOCKBACK_RESISTANCE; }
    private static net.minecraft.core.Holder<Attribute> capacity() { return MalumAttributes.SOUL_WARD_CAPACITY; }

    @GameTest(template = "test_empty", batch = "transcend_omen")
    public static void equipmentChangeDuringConversionCannotCacheOldStrength(GameTestHelper helper) {
        try (Fixture f = new Fixture(helper)) {
            f.rules(rule(magic()));
            f.base(magic(), 2);
            Consumer<net.neoforged.neoforge.event.tick.EntityTickEvent.Pre> equip = event -> {
                if (event.getEntity() == f.entity) {
                    f.base(MaledictAttributes.TRANSCEND_OMEN, 0.75);
                }
            };
            NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, net.neoforged.neoforge.event.tick.EntityTickEvent.Pre.class, equip);
            try {
                f.omen(0.5);
            } finally {
                NeoForge.EVENT_BUS.unregister(equip);
            }
            f.tick();
            f.expect(2.75, 5, 0.0625, "Equipment changes during the event must be reflected on the next tick");

            f.base(MalumAttributes.MALIGNANT_CONVERSION, 0.5);
            Consumer<net.neoforged.neoforge.event.tick.EntityTickEvent.Pre> effect = event -> {
                if (event.getEntity() == f.entity) {
                    f.base(MalumAttributes.MALIGNANT_CONVERSION, 0.7);
                }
            };
            NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, net.neoforged.neoforge.event.tick.EntityTickEvent.Pre.class, effect);
            try {
                f.tick();
            } finally {
                NeoForge.EVENT_BUS.unregister(effect);
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
            f.entity.getAttribute(magic()).addTransientModifier(new AttributeModifier(org.brahypno.maledict.TestSupport.modifierId(UUID.randomUUID()), 1, AttributeModifier.Operation.ADD_VALUE));
            f.tick(); // Malum's ordinary zero-conversion initial state.
            f.base(MalumAttributes.MALIGNANT_CONVERSION, 0.5);
            f.tick();
            f.expect(1.5, 25, 0.375, "M=.5, R=0: ordinary Malum formulas");
            f.omen(0.25);
            f.expect(1.75, 22.5, 0.3125, "M=.5, R=.25: forward .25");
            f.omen(0.5);
            f.expect(2, 20, 0.25, "M=.5, R=.5: both directions off");
            f.omen(0.75);
            f.expect(2.25, 15, 0.1875, "M=.5, R=.75: reverse .25");
            f.base(MalumAttributes.MALIGNANT_CONVERSION, 0);
            f.omen(0.5);
            f.expect(2.5, 10, 0.125, "Confirmed option 2: excess armor is still consumed by 50%");
            f.omen(0);
            f.expect(2, 20, 0.25, "Removing Omen restores the complete original state");

            // 100% MULTIPLY_TOTAL suppression must be removed, never counteracted after the fact.
            f.rules(new MalignantConversionData(magic(), 1, true, rule(magic()).payoutData()));
            f.base(MalumAttributes.MALIGNANT_CONVERSION, 1);
            f.tick();
            f.equal(0, f.value(magic()), "Full forward suppression");
            f.omen(1);
            f.expect(2, 20, 0.25, "Full cancellation removes a -100% source modifier");
            f.base(MalumAttributes.MALIGNANT_CONVERSION, 0);
            f.tick();
            f.expect(3, 0, 0, "Full reverse consumption");
            f.base(MalumAttributes.MALIGNANT_CONVERSION, 1);
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
            f.entity.getAttribute(magic()).addTransientModifier(new AttributeModifier(org.brahypno.maledict.TestSupport.modifierId(magicEquipment), 1, AttributeModifier.Operation.ADD_VALUE));
            f.tick();
            f.expect(2.5, 10, 0.125, "A default capacity source must not dilute the equipped proficiency route");
            UUID capacityEquipment = UUID.randomUUID();
            f.entity.getAttribute(capacity()).addTransientModifier(new AttributeModifier(org.brahypno.maledict.TestSupport.modifierId(capacityEquipment), 1, AttributeModifier.Operation.ADD_VALUE));
            f.tick();
            f.expect(2.25, 10, 0.125, "Two active routes split output and consume shared defense once");
            f.equal(1.25, f.value(capacity()), "Equipment activates a default-zero attribute");
            f.entity.getAttribute(magic()).removeModifier(org.brahypno.maledict.TestSupport.modifierId(magicEquipment));
            f.tick();
            f.expect(1, 10, 0.125, "Old reverse gains cannot keep an unequipped source active");
            f.equal(1.5, f.value(capacity()), "The remaining route receives the complete budget");
            f.entity.getAttribute(capacity()).removeModifier(org.brahypno.maledict.TestSupport.modifierId(capacityEquipment));
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
        double originalMinimum = MaledictConfig.OMEN_MINIMUM_SHARE.get();
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
        } finally {
            MaledictConfig.OMEN_MINIMUM_SHARE.set(originalMinimum);
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
            f.entity.getAttribute(Attributes.ARMOR).addTransientModifier(new AttributeModifier(org.brahypno.maledict.TestSupport.modifierId(equipment), 5, AttributeModifier.Operation.ADD_VALUE));
            f.tick();
            f.equal(4.5, f.value(magic()), "A defense displaying zero must still invalidate on new equipment");
            f.base(Attributes.ARMOR, 10);
            f.tick();
            f.equal(3.5, f.value(magic()), "Base-value changes must also invalidate under full consumption");

            // Malum's reload listener clears and repopulates the same map, rather than replacing its identity.
            f.rules(new MalignantConversionData(capacity(), 0.1, true, List.of(new MalignantConversionReloadListener.MalignantConversionAttributePayout(Attributes.ARMOR, 5D))));
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
                            new MalignantConversionReloadListener.MalignantConversionAttributePayout(Attributes.ARMOR, 10D), new MalignantConversionReloadListener.MalignantConversionAttributePayout(resistance(), Double.NaN),
                            new MalignantConversionReloadListener.MalignantConversionAttributePayout(Attributes.ARMOR_TOUGHNESS, 0D), new MalignantConversionReloadListener.MalignantConversionAttributePayout(Attributes.FLYING_SPEED, -1D),
                            new MalignantConversionReloadListener.MalignantConversionAttributePayout(Attributes.ATTACK_DAMAGE, Double.POSITIVE_INFINITY))),
                    new MalignantConversionData(capacity(), 1, false,
                            List.of(new MalignantConversionReloadListener.MalignantConversionAttributePayout(Attributes.FLYING_SPEED, 1D))));
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
            f.rules(new MalignantConversionData(magic(), 1, false, List.of(new MalignantConversionReloadListener.MalignantConversionAttributePayout(Attributes.ARMOR, 0D))));
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
            f.rules(new MalignantConversionData(magic(), 1, false, List.of(new MalignantConversionReloadListener.MalignantConversionAttributePayout(Attributes.ARMOR, 10D))),
                    new MalignantConversionData(capacity(), 1, false, List.of(new MalignantConversionReloadListener.MalignantConversionAttributePayout(magic(), 1D))));
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
            f.rules(new MalignantConversionData(magic(), 0.5, true, List.of(new MalignantConversionReloadListener.MalignantConversionAttributePayout(resistance(), 1D))));
            f.base(magic(), 2);
            f.base(resistance(), 0.5);
            var input = f.entity.getAttribute(resistance());
            input.addTransientModifier(new AttributeModifier(org.brahypno.maledict.TestSupport.modifierId(UUID.randomUUID()), 0.5, AttributeModifier.Operation.ADD_VALUE));
            input.addTransientModifier(new AttributeModifier(org.brahypno.maledict.TestSupport.modifierId(UUID.randomUUID()), 1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            input.addTransientModifier(new AttributeModifier(org.brahypno.maledict.TestSupport.modifierId(UUID.randomUUID()), 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
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
            f.base(MalumAttributes.MALIGNANT_CONVERSION, 0.5);
            UUID first = UUID.randomUUID();
            UUID second = UUID.randomUUID();
            TranscendOmenUtil.setAmount(f.entity, org.brahypno.maledict.TestSupport.modifierId(first), 0.5);
            f.equal(0, f.value(MalumAttributes.MALIGNANT_CONVERSION), "Utility immediately cancels actual Malum scalar");
            f.equal(0.5, f.value(MaledictAttributes.TRANSCEND_OMEN), "Omen retains its granted amount");
            f.tick();
            f.expect(2, 20, 0.25, "Complete cancellation survives the ordinary Malum event handler");
            f.equal(0.25, TranscendOmenUtil.setAmount(f.entity, org.brahypno.maledict.TestSupport.modifierId(second), 0.25), "Utility returns only the unspent reverse remainder");
            f.tick();
            f.expect(2.25, 15, 0.1875, "Independent Omen sources add before cancellation");
            f.equal(0.75, f.value(MaledictAttributes.TRANSCEND_OMEN), "Omen's attribute itself is not reduced by cancellation");

            UUID fallen = UUID.randomUUID();
            var malignant = f.entity.getAttribute(MalumAttributes.MALIGNANT_CONVERSION);
            malignant.addTransientModifier(new AttributeModifier(org.brahypno.maledict.TestSupport.modifierId(fallen), 0.2, AttributeModifier.Operation.ADD_VALUE));
            f.tick();
            f.equal(2.05, f.value(magic()), "A later opposing effect leaves only .05 reverse gain");
            malignant.removeModifier(org.brahypno.maledict.TestSupport.modifierId(fallen));
            f.tick();
            f.equal(2.25, f.value(magic()), "Removing that effect restores the .25 remainder");

            TranscendOmenUtil.setAmount(f.entity, org.brahypno.maledict.TestSupport.modifierId(first), 0);
            f.tick();
            f.equal(0.25, f.value(MalumAttributes.MALIGNANT_CONVERSION), "Removing one Omen source restores forward conversion");
            f.expect(1.75, 20, 0.25, "Remaining malignant conversion skips reverse gain and defense consumption");
            TranscendOmenUtil.setAmount(f.entity, org.brahypno.maledict.TestSupport.modifierId(second), 0);
            f.tick();
            f.equal(0.5, f.value(MalumAttributes.MALIGNANT_CONVERSION), "Removing all Omen restores original Malum conversion");
            f.equal(0, f.entity.getAttribute(MaledictAttributes.TRANSCEND_OMEN).getBaseValue(), "Utility never changes base values");
            f.equal(0.5, malignant.getBaseValue(), "Malum's base value is also preserved");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "transcend_omen")
    public static void loadedMalumRulesIgnoreZeroAegisAndConsumePositiveAegis(GameTestHelper helper) {
        try (Fixture f = new Fixture(helper)) {
            var recovery = MalumAttributes.SOUL_WARD_RECOVERY_RATE;
            var integrity = MalumAttributes.SOUL_WARD_INTEGRITY;
            var aegis = MalumAttributes.MALIGNANT_AEGIS_CAPACITY;
            var aegisRate = MalumAttributes.MALIGNANT_AEGIS_RECOVERY_RATE;
            var magicResistance = LodestoneAttributes.MAGIC_RESISTANCE;
            f.rules(f.original.get(magic()), f.original.get(capacity()),
                    f.original.get(recovery), f.original.get(integrity));
            f.base(magic(), 2);
            f.base(capacity(), 1);
            f.base(recovery, 2);
            f.base(integrity, 2);
            f.base(Attributes.ARMOR_TOUGHNESS, 8);
            f.base(magicResistance, 2);
            f.base(aegis, 0);
            f.base(aegisRate, 1);
            f.omen(1);
            f.equal(2.5, f.value(magic()), "Armor alone supports the native proficiency route");
            f.equal(11, f.value(capacity()), "The native capacity ratio is .5 armor per unit");
            f.equal(2.25, f.value(recovery), "Positive Aegis recovery rate still limits its native route");
            f.equal(2.5, f.value(integrity), "Native integrity now reverses armor rather than toughness");
            f.equal(0, f.value(Attributes.ARMOR), "Shared armor is consumed once");
            f.equal(0, f.value(aegis), "Absent capacity is neither required nor created");
            f.equal(0, f.value(aegisRate), "Positive recovery rate participates in consumption");
            f.equal(8, f.value(Attributes.ARMOR_TOUGHNESS), "A defense absent from native payouts stays unchanged");
            f.equal(2, f.value(magicResistance), "Native rules no longer consume magic resistance");

            f.base(aegis, 2);
            f.tick();
            f.equal(2.1, f.value(magic()), "Positive capacity limits proficiency through its ratio of five");
            f.equal(3, f.value(capacity()), "Positive capacity also limits the ward capacity route");
            f.equal(2.2, f.value(recovery), "Positive capacity limits recovery before route sharing");
            f.equal(2.1, f.value(integrity), "Positive capacity limits integrity");
            f.equal(0, f.value(aegis), "Positive shared capacity is consumed once");
            f.omen(0);
            f.equal(20, f.value(Attributes.ARMOR), "Removing Omen restores armor");
            f.equal(2, f.value(aegis), "Removing Omen restores capacity");
        }
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "transcend_omen")
    public static void allThreeAegisInputsIgnoreOnlyZeroValues(GameTestHelper helper) {
        try (Fixture f = new Fixture(helper)) {
            var capacity = MalumAttributes.MALIGNANT_AEGIS_CAPACITY;
            var rate = MalumAttributes.MALIGNANT_AEGIS_RECOVERY_RATE;
            var gain = MalumAttributes.MALIGNANT_AEGIS_RECOVERY_GAIN;
            f.rules(new MalignantConversionData(magic(), 1, true, java.util.List.of(
                    new MalignantConversionReloadListener.MalignantConversionAttributePayout(Attributes.ARMOR, 10),
                    new MalignantConversionReloadListener.MalignantConversionAttributePayout(capacity, 5),
                    new MalignantConversionReloadListener.MalignantConversionAttributePayout(rate, 1),
                    new MalignantConversionReloadListener.MalignantConversionAttributePayout(gain, 1))));
            f.base(magic(), 2);
            f.base(capacity, 0); f.base(rate, 0); f.base(gain, 0);
            f.omen(.5);
            f.equal(3, f.value(magic()), "Three zero Aegis targets cannot block armor conversion");
            f.equal(10, f.value(Attributes.ARMOR), "The armor cost still applies");
            f.base(capacity, 5); f.base(rate, .25); f.base(gain, .5);
            f.tick();
            f.equal(2.125, f.value(magic()), "The smallest positive Aegis input limits the route");
            f.equal(2.5, f.value(capacity), "Capacity pays the full reverse percentage");
            f.equal(.125, f.value(rate), "Recovery rate pays the full reverse percentage");
            f.equal(.25, f.value(gain), "Recovery gain pays the full reverse percentage");
        }
        helper.succeed();
    }

    private static MalignantConversionData rule(net.minecraft.core.Holder<Attribute> source) {
        return new MalignantConversionData(source, 0.5, true,
                List.of(new MalignantConversionReloadListener.MalignantConversionAttributePayout(Attributes.ARMOR, 10D), new MalignantConversionReloadListener.MalignantConversionAttributePayout(resistance(), 0.25D)));
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper helper;
        final LivingEntity entity;
        final Map<net.minecraft.core.Holder<Attribute>, MalignantConversionData> original = MalignantConversionReloadListener.CONVERSION_DATA;

        Fixture(GameTestHelper helper) {
            this.helper = helper;
            entity = new net.neoforged.neoforge.common.util.FakePlayer(helper.getLevel(),
                    new com.mojang.authlib.GameProfile(UUID.randomUUID(), "OmenTest"));
            // Do not add this entity to the world: each fixture changes the global datapack synchronously.
            MalignantConversionReloadListener.CONVERSION_DATA = new LinkedHashMap<>();
            base(Attributes.ARMOR, 20);
            base(resistance(), 0.25);
            base(capacity(), 0);
        }

        void rules(MalignantConversionData... rules) {
            var data = MalignantConversionReloadListener.CONVERSION_DATA;
            data.clear();
            for (var rule : rules) data.put(rule.sourceAttribute(), rule);
        }

        void base(net.minecraft.core.Holder<Attribute> attribute, double value) { entity.getAttribute(attribute).setBaseValue(value); }
        double value(net.minecraft.core.Holder<Attribute> attribute) { return entity.getAttributeValue(attribute); }
        void tick() { NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.EntityTickEvent.Pre(entity)); }
        void omen(double value) { base(MaledictAttributes.TRANSCEND_OMEN, value); tick(); }
        void expect(double magic, double armor, double resistance, String message) {
            equal(magic, value(magic()), message + " / proficiency");
            equal(armor, value(Attributes.ARMOR), message + " / armor");
            equal(resistance, value(resistance()), message + " / resistance");
        }
        void equal(double expected, double actual, String message) {
            helper.assertTrue(Math.abs(expected - actual) < 1e-8, message + ": expected " + expected + ", got " + actual);
        }
        @Override public void close() {
            MalignantConversionReloadListener.CONVERSION_DATA = original;
            entity.discard();
        }
    }
}
