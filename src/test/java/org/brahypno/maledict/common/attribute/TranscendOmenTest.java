package org.brahypno.maledict.common.attribute;

import com.sammy.malum.core.listeners.MalignantConversionReloadListener;
import com.sammy.malum.core.listeners.MalignantConversionReloadListener.MalignantConversionData;
import com.sammy.malum.registry.common.MalumAttributes;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.brahypno.maledict.config.MaledictConfig;
import org.brahypno.maledict.registry.MaledictAttributes;
import team.lodestar.lodestone.registry.common.LodestoneAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@GameTestHolder("maledict")
@PrefixGameTestTemplate(false)
public final class TranscendOmenTest {
    private static net.minecraft.core.Holder<Attribute> magic() { return LodestoneAttributes.MAGIC_PROFICIENCY; }
    // A real defensive attribute allowing the specification's .25 -> .125 numeric example.
    // Lodestone's MAGIC_RESISTANCE has a minimum of 1 and cannot represent that example.
    private static net.minecraft.core.Holder<Attribute> resistance() { return Attributes.KNOCKBACK_RESISTANCE; }
    private static net.minecraft.core.Holder<Attribute> capacity() { return MalumAttributes.SOUL_WARD_CAPACITY; }

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
            f.expect(2.5, 15, 0.1875, "M=.5, R=.75: reverse .25");
            f.base(MalumAttributes.MALIGNANT_CONVERSION, 0);
            f.omen(0.5);
            f.expect(3, 10, 0.125, "Confirmed option 2: excess armor is still consumed by 50%");
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
            f.expect(4, 0, 0, "Full reverse consumption");
            f.base(MalumAttributes.MALIGNANT_CONVERSION, 1);
            f.omen(0);
            f.equal(0, f.value(magic()), "Reverse output must be gone before forward resumes");
            f.equal(30, f.value(Attributes.ARMOR), "Forward cannot convert its old reverse gain");
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
            f.equal(1.5, f.value(magic()), "Overlapping source is independently gained and consumed");
            f.base(Attributes.ARMOR, 10);
            f.tick();
            f.equal(1.5, f.value(capacity()), "Recalculation removes both old costs and gains before measuring");
            f.equal(1.25, f.value(magic()), "New armor produces a new gain with no recursive amplification");
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
            f.equal(3, f.value(magic()), "Gain uses the full current value, not the uncapped raw value");
            f.equal(0.5, f.value(resistance()), "Cost must halve current defense even when raw defense exceeded its cap");
            f.omen(0);
            f.equal(1, f.value(resistance()), "Clearing the cost restores the attribute's original capped value");
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
        final double originalMinimum = MaledictConfig.OMEN_MINIMUM_SHARE.get();

        Fixture(GameTestHelper helper) {
            this.helper = helper;
            entity = new net.neoforged.neoforge.common.util.FakePlayer(helper.getLevel(),
                    new com.mojang.authlib.GameProfile(UUID.randomUUID(), "OmenTest"));
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
            MaledictConfig.OMEN_MINIMUM_SHARE.set(originalMinimum);
            MalignantConversionReloadListener.CONVERSION_DATA = original;
            entity.discard();
        }
    }
}
