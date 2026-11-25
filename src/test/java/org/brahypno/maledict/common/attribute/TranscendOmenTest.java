package org.brahypno.maledict.common.attribute;

import com.sammy.malum.core.listeners.MalignantConversionReloadListener;
import com.sammy.malum.core.listeners.MalignantConversionReloadListener.MalignantConversionData;
import com.sammy.malum.registry.common.MalumAttributes;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.TestSupport;
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
    private static Holder<Attribute> magic() { return LodestoneAttributes.MAGIC_PROFICIENCY; }
    // A real defensive attribute allowing the specification's .25 -> .125 numeric example.
    // Lodestone's MAGIC_RESISTANCE has a minimum of 1 and cannot represent that example.
    private static Holder<Attribute> resistance() { return Attributes.KNOCKBACK_RESISTANCE; }
    private static Holder<Attribute> capacity() { return MalumAttributes.SOUL_WARD_CAPACITY; }
    private static Holder<Attribute> integrity() { return MalumAttributes.SOUL_WARD_INTEGRITY; }
    private static Holder<Attribute> recovery() { return MalumAttributes.SOUL_WARD_RECOVERY_RATE; }
    private static Holder<Attribute> mana() { return LodestoneAttributes.MAGIC_DAMAGE; }

    @GameTest(template = "test_empty", batch = "transcend_omen")
    public static void confirmedSignedSequenceAndFullInputConsumption(GameTestHelper helper) {
        try (Fixture f = new Fixture(helper)) {
            f.rules(rule(magic()));
            f.base(magic(), 1);
            f.entity.getAttribute(magic()).addTransientModifier(new AttributeModifier(TestSupport.modifierId(UUID.randomUUID()), 1, AttributeModifier.Operation.ADD_VALUE));
            f.tick(); // Malum's ordinary zero-conversion initial state.
            f.base(MalumAttributes.MALIGNANT_CONVERSION, 0.5);
            f.tick();
            f.expect(1.5, 25, 0.375, "M=.5, R=0: ordinary Malum formulas");
            f.omen(0.25);
            f.expect(1.75, 22.5, 0.3125, "M=.5, R=.25: forward .25");
            f.omen(0.5);
            f.expect(2, 20, 0.25, "M=.5, R=.5: both directions off");
            f.omen(0.75);
            f.expect(25D / 12, 15, 0.1875, "M=.5, R=.75: reverse .25 saturates its base of one");
            f.base(MalumAttributes.MALIGNANT_CONVERSION, 0);
            f.omen(0.5);
            f.expect(13D / 6, 10, 0.125, "Excess armor is still consumed by 50%");
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
            f.expect(7D / 3, 0, 0, "Full reverse consumption");
            ResourceLocation equipment = TestSupport.modifierId(UUID.randomUUID());
            var defense = f.entity.getAttribute(resistance());
            defense.addTransientModifier(new AttributeModifier(equipment, -0.125, AttributeModifier.Operation.ADD_VALUE));
            f.tick();
            f.expect(24D / 11, 0, 0, "Equipment still changes the gain under full defense consumption");
            defense.removeModifier(equipment);
            f.base(MalumAttributes.MALIGNANT_CONVERSION, 1);
            f.omen(0);
            f.equal(0, f.value(magic()), "Reverse output must be gone before forward resumes");
            f.equal(30, f.value(Attributes.ARMOR), "Forward cannot convert its old reverse gain");

            f.base(MalumAttributes.MALIGNANT_CONVERSION, 2);
            f.omen(0.5);
            f.equal(0.5, f.value(MalumAttributes.MALIGNANT_CONVERSION),
                    "Malignant conversion caps at one before cancellation");
            f.base(MalumAttributes.MALIGNANT_CONVERSION, -1);
            f.tick();
            f.expect(13D / 6, 10, 0.125, "Negative raw malignant conversion is clamped to zero");

            Consumer<EntityTickEvent.Pre> changeOmen = event -> {
                if (event.getEntity() == f.entity) {
                    f.base(MaledictAttributes.TRANSCEND_OMEN, 0.75);
                }
            };
            NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, changeOmen);
            try {
                f.omen(0.25);
                f.expect(2, 20, 0.25, "An omen change during recomputation discards the old gain and cost");
            } finally {
                NeoForge.EVENT_BUS.unregister(changeOmen);
            }
            f.tick();
            f.expect(2.25, 5, 0.0625, "The next tick uses the new strength");
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
            f.equal(8D / 7, f.value(capacity()), "A route sees baseline magic, not another route's just-applied gain");
            f.equal(8D / 7, f.value(magic()), "Overlapping source is independently gained and consumed");
            f.base(Attributes.ARMOR, 10);
            f.tick();
            f.equal(8D / 7, f.value(capacity()), "Recalculation removes both old costs and gains before measuring");
            f.equal(13D / 12, f.value(magic()), "New armor produces a new gain with no recursive amplification");
            f.base(magic(), magic().value().getDefaultValue());
            f.base(capacity(), capacity().value().getDefaultValue());
            f.tick();
            f.equal(magic().value().getDefaultValue(), f.value(magic()), "Old modifiers cannot keep a default source active");
            f.equal(10, f.value(Attributes.ARMOR), "No eligible sources restore armor");
        }
        verifyLoadedRulesAndBudget(helper);
        helper.succeed();
    }

    @GameTest(template = "test_empty", batch = "transcend_omen")
    public static void fullCurrentInputCostIncludesModifiersAndAttributeCaps(GameTestHelper helper) {
        try (Fixture f = new Fixture(helper)) {
            f.rules(new MalignantConversionData(magic(), 0.5, true, List.of(new MalignantConversionReloadListener.MalignantConversionAttributePayout(resistance(), 1D))));
            f.base(magic(), 2);
            f.base(resistance(), 0.5);
            var input = f.entity.getAttribute(resistance());
            input.addTransientModifier(new AttributeModifier(TestSupport.modifierId(UUID.randomUUID()), 0.5, AttributeModifier.Operation.ADD_VALUE));
            input.addTransientModifier(new AttributeModifier(TestSupport.modifierId(UUID.randomUUID()), 1, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            input.addTransientModifier(new AttributeModifier(TestSupport.modifierId(UUID.randomUUID()), 1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            f.equal(1, f.value(resistance()), "Raw four is capped to current one");
            f.omen(0.5);
            f.equal(13D / 6, f.value(magic()), "Gain uses the full current value, not the uncapped raw value");
            f.equal(0.5, f.value(resistance()), "Cost must halve current defense even when raw defense exceeded its cap");
            f.omen(0);
            f.equal(1, f.value(resistance()), "Clearing the cost restores the attribute's original capped value");

            f.rules(new MalignantConversionData(resistance(), 1, false, List.of(new MalignantConversionReloadListener.MalignantConversionAttributePayout(resistance(), 1D))));
            f.omen(0.5);
            f.equal(0.5, f.value(resistance()), "A capped self-target is consumed after its gain");
            f.tick();
            f.equal(0.5, f.value(resistance()), "A self-target stays stable across ticks");
        }
        verifyDuplicateSourcesAndExtremeRatios(helper);
        helper.succeed();
    }

    /** The four native Malum 1.8.2 routes, the Aegis exception, and the one-over-max(4, N) share. */
    private static void verifyLoadedRulesAndBudget(GameTestHelper helper) {
        try (Fixture f = new Fixture(helper)) {
            f.rules(f.loadedRule(magic()), f.loadedRule(capacity()), f.loadedRule(integrity()), f.loadedRule(recovery()));
            f.base(magic(), 2);
            f.base(capacity(), 1);
            f.base(integrity(), 2);
            f.base(recovery(), 2);
            f.omen(0.5);
            f.equal(16D / 7, f.value(magic()), "Four loaded routes: proficiency uses armor / 10");
            f.equal(13D / 9, f.value(capacity()), "Four loaded routes: capacity uses armor / .5");
            f.equal(16D / 7, f.value(integrity()), "Four loaded routes: integrity uses armor / 10");
            f.equal(13D / 6, f.value(recovery()), "Four loaded routes: recovery rate is capped by the Aegis rate of one");
            f.equal(10, f.value(Attributes.ARMOR), "Shared armor is halved once");
            f.equal(0, f.value(MalumAttributes.MALIGNANT_AEGIS_CAPACITY),
                    "A zero Aegis target neither limits the route nor pays the cost");
            f.equal(0.5, f.value(MalumAttributes.MALIGNANT_AEGIS_RECOVERY_RATE),
                    "A default Aegis recovery rate of one limits the route and pays the cost");

            f.omen(1);
            f.equal(18D / 7, f.value(magic()), "Full reverse proficiency +28.5714%");
            f.equal(17D / 9, f.value(capacity()), "Full reverse capacity +88.8889%");
            f.equal(18D / 7, f.value(integrity()), "Full reverse integrity +28.5714%");
            f.equal(7D / 3, f.value(recovery()), "Full reverse recovery rate +16.6667%");
            f.equal(0, f.value(Attributes.ARMOR), "Full reverse consumes all armor");
            f.equal(0, f.value(MalumAttributes.MALIGNANT_AEGIS_RECOVERY_RATE), "Full reverse consumes the Aegis rate");

            // A positive Aegis value is a real constraint and a real cost on this line.
            f.base(MalumAttributes.MALIGNANT_AEGIS_CAPACITY, 2);
            f.omen(0.5);
            f.equal(56D / 27, f.value(magic()), "A positive Aegis still limits the armor route");
            f.equal(17D / 13, f.value(capacity()), "A positive Aegis constrains capacity through its own ratio");
            f.equal(56D / 27, f.value(integrity()), "Integrity shares the proficiency bottleneck");
            f.equal(62D / 29, f.value(recovery()), "Recovery rate keeps the Aegis capacity bottleneck");
            f.equal(10, f.value(Attributes.ARMOR), "Armor is still halved once");
            f.equal(1, f.value(MalumAttributes.MALIGNANT_AEGIS_CAPACITY), "A positive Aegis is halved");
            f.equal(0.5, f.value(MalumAttributes.MALIGNANT_AEGIS_RECOVERY_RATE), "The Aegis rate is halved too");

            // Five sources: the share drops to one fifth and Malum's Iron's Spellbooks ratio still saturates.
            f.base(MalumAttributes.MALIGNANT_AEGIS_CAPACITY, 0);
            f.base(mana(), 1_000);
            f.putRule(mana(), new MalignantConversionData(mana(), 1, true,
                    List.of(new MalignantConversionReloadListener.MalignantConversionAttributePayout(Attributes.ARMOR, 0.04D),
                            new MalignantConversionReloadListener.MalignantConversionAttributePayout(MalumAttributes.MALIGNANT_AEGIS_CAPACITY, 0.02D))));
            f.omen(0.5);
            f.equal(61D / 45, f.value(capacity()), "A fifth eligible source takes the spend to one fifth each");
            f.equal(141_000D / 101, f.value(mana()), "Armor / .04 saturates at +39.604% with five sources");

            f.rules(f.loadedRule(capacity()));
            f.base(Attributes.ARMOR, 40);
            f.tick();
            f.equal(19D / 13, f.value(capacity()), "One route keeps a quarter share, with capped armor 30");
            f.equal(15, f.value(Attributes.ARMOR), "Capped armor is halved after a rule reload");
            f.equal(2, f.value(magic()), "Sources dropped by reload lose their gain");
            f.equal(1_000, f.value(mana()), "An injected source dropped by reload loses its gain");
            f.equal(0.2, TranscendOmenConversion.gainFor(20, 0.5, 8), "Eight sources use one eighth each");
            f.equal(0.016, TranscendOmenConversion.gainFor(20, 0.5, 100), "One hundred sources use one percent each");
            f.equal(1.6 / 101, TranscendOmenConversion.gainFor(20, 0.5, 101), "The old minimum share no longer applies");
        }
    }

    /** Malum keys its table by source, so only an injected alias can put one source under two keys. */
    private static void verifyDuplicateSourcesAndExtremeRatios(GameTestHelper helper) {
        try (Fixture f = new Fixture(helper)) {
            var armorRoute = new MalignantConversionData(capacity(), 1, false,
                    List.of(new MalignantConversionReloadListener.MalignantConversionAttributePayout(Attributes.ARMOR, 10D)));
            var tiedRoute = new MalignantConversionData(capacity(), 1, false,
                    List.of(new MalignantConversionReloadListener.MalignantConversionAttributePayout(resistance(), 0.125D)));
            var weakerRoute = new MalignantConversionData(capacity(), 1, false,
                    List.of(new MalignantConversionReloadListener.MalignantConversionAttributePayout(resistance(), 0.25D)));
            f.rules();
            f.putRule(magic(), armorRoute);
            f.putRule(integrity(), armorRoute);
            f.putRule(recovery(), tiedRoute);
            f.putRule(MalumAttributes.MALIGNANT_AEGIS_RECOVERY_RATE, weakerRoute);
            f.base(capacity(), 1);
            f.omen(0.5);
            f.equal(8D / 7, f.value(capacity()), "One gain for the best rule across duplicate sources");
            f.equal(10, f.value(Attributes.ARMOR), "Equal coefficients choose the smallest full key");
            f.equal(0.25, f.value(resistance()), "Unselected targets do not pay the cost");

            helper.assertTrue(f.entity.getAttribute(Attributes.SPAWN_REINFORCEMENTS_CHANCE) == null,
                    "The fixture must not carry a zombie-only attribute");
            f.base(mana(), 1_000);
            f.rules(new MalignantConversionData(mana(), 1, false,
                    List.of(new MalignantConversionReloadListener.MalignantConversionAttributePayout(Attributes.ARMOR, Double.MIN_VALUE))));
            f.tick();
            f.equal(1_500, f.value(mana()), "Finite inputs with an overflowing quotient still saturate");
            f.rules(new MalignantConversionData(mana(), 1, false, List.of(
                    new MalignantConversionReloadListener.MalignantConversionAttributePayout(Attributes.ARMOR, Double.MIN_VALUE),
                    new MalignantConversionReloadListener.MalignantConversionAttributePayout(resistance(), 0.125D),
                    new MalignantConversionReloadListener.MalignantConversionAttributePayout(Attributes.SPAWN_REINFORCEMENTS_CHANCE, Double.NaN),
                    new MalignantConversionReloadListener.MalignantConversionAttributePayout(Attributes.ATTACK_SPEED, 0D),
                    new MalignantConversionReloadListener.MalignantConversionAttributePayout(Attributes.FLYING_SPEED, Double.POSITIVE_INFINITY))));
            f.tick();
            f.equal(8_000D / 7, f.value(mana()), "A finite bottleneck wins over the overflowing quotient");
            f.equal(0.125, f.value(resistance()), "The finite target also pays the full cost");
            f.rules(new MalignantConversionData(mana(), 1, false,
                    List.of(new MalignantConversionReloadListener.MalignantConversionAttributePayout(Attributes.ARMOR, 1D),
                            new MalignantConversionReloadListener.MalignantConversionAttributePayout(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 1D))));
            f.tick();
            f.equal(1_000, f.value(mana()), "A missing required target disables the route");
            f.equal(20, f.value(Attributes.ARMOR), "A disabled route consumes no armor");
        }
    }

    private static MalignantConversionData rule(Holder<Attribute> source) {
        return new MalignantConversionData(source, 0.5, true,
                List.of(new MalignantConversionReloadListener.MalignantConversionAttributePayout(Attributes.ARMOR, 10D),
                        new MalignantConversionReloadListener.MalignantConversionAttributePayout(resistance(), 0.25D)));
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper helper;
        final LivingEntity entity;
        final Map<Holder<Attribute>, MalignantConversionData> original = MalignantConversionReloadListener.CONVERSION_DATA;

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

        MalignantConversionData loadedRule(Holder<Attribute> source) { return original.get(source); }

        void rules(MalignantConversionData... rules) {
            var data = MalignantConversionReloadListener.CONVERSION_DATA;
            data.clear();
            for (var rule : rules) data.put(rule.sourceAttribute(), rule);
        }

        void putRule(Holder<Attribute> key, MalignantConversionData rule) {
            MalignantConversionReloadListener.CONVERSION_DATA.put(key, rule);
        }

        void base(Holder<Attribute> attribute, double value) { entity.getAttribute(attribute).setBaseValue(value); }
        double value(Holder<Attribute> attribute) { return entity.getAttributeValue(attribute); }
        void tick() { NeoForge.EVENT_BUS.post(new EntityTickEvent.Pre(entity)); }
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
