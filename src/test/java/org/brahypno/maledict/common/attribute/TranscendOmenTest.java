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
            f.expect(25D / 12, 15, 0.1875, "M=.5, R=.75: reverse .25");
            f.base(AttributeRegistry.MALIGNANT_CONVERSION.get(), 0);
            f.omen(0.5);
            f.expect(13D / 6, 10, 0.125, "Excess armor is still consumed by 50%");
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
            f.expect(7D / 3, 0, 0, "Full reverse consumption");
            UUID equipment = UUID.randomUUID();
            var defense = f.entity.getAttribute(resistance());
            defense.addTransientModifier(new AttributeModifier(equipment, "Equipment", -0.125,
                    AttributeModifier.Operation.ADDITION));
            f.tick();
            f.expect(24D / 11, 0, 0, "Equipment still changes the gain under full defense consumption");
            defense.removeModifier(equipment);
            f.base(AttributeRegistry.MALIGNANT_CONVERSION.get(), 1);
            f.omen(0);
            f.equal(0, f.value(magic()), "Reverse output must be gone before forward resumes");
            f.equal(30, f.value(Attributes.ARMOR), "Forward cannot convert its old reverse gain");

            f.base(AttributeRegistry.MALIGNANT_CONVERSION.get(), 2);
            f.omen(0.5);
            f.equal(0.5, f.value(AttributeRegistry.MALIGNANT_CONVERSION.get()), "Malignant conversion caps at one before cancellation");
            f.base(AttributeRegistry.MALIGNANT_CONVERSION.get(), -1);
            f.tick();
            f.expect(13D / 6, 10, 0.125, "Negative raw malignant conversion is clamped to zero");

            Consumer<LivingEvent.LivingTickEvent> changeOmen = event -> {
                if (event.getEntity() == f.entity) {
                    f.base(MaledictAttributes.TRANSCEND_OMEN.get(), 0.75);
                }
            };
            MinecraftForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, LivingEvent.LivingTickEvent.class, changeOmen);
            try {
                f.omen(0.25);
                f.expect(2, 20, 0.25, "An omen change during recomputation discards the old gain and cost");
            } finally {
                MinecraftForge.EVENT_BUS.unregister(changeOmen);
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
            f.rules(new MalignantConversionData(magic(), 1, false, List.of(Pair.of(Attributes.ARMOR, 10D))),
                    new MalignantConversionData(capacity(), 1, false, List.of(Pair.of(magic(), 1D))));
            f.omen(0.5);
            f.equal(8D / 7, f.value(capacity()), "A route sees baseline magic, not another route's just-applied gain");
            f.equal(8D / 7, f.value(magic()), "Overlapping source is independently gained and consumed");
            f.base(Attributes.ARMOR, 10);
            f.tick();
            f.equal(8D / 7, f.value(capacity()), "Recalculation removes both old costs and gains before measuring");
            f.equal(13D / 12, f.value(magic()), "New armor produces a new gain with no recursive amplification");
            f.base(magic(), magic().getDefaultValue());
            f.base(capacity(), capacity().getDefaultValue());
            f.tick();
            f.equal(magic().getDefaultValue(), f.value(magic()), "Old modifiers cannot keep a default source active");
            f.equal(10, f.value(Attributes.ARMOR), "No eligible sources restore armor");
        }
        verifyLoadedRulesAndBudget(helper);
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
            f.equal(13D / 6, f.value(magic()), "Gain uses the full current value, not the uncapped raw value");
            f.equal(0.5, f.value(resistance()), "Cost must halve current defense even when raw defense exceeded its cap");
            f.omen(0);
            f.equal(1, f.value(resistance()), "Clearing the cost restores the attribute's original capped value");

            f.rules(new MalignantConversionData(resistance(), 1, false, List.of(Pair.of(resistance(), 1D))));
            f.omen(0.5);
            f.equal(0.5, f.value(resistance()), "A capped self-target is consumed after its gain");
            f.tick();
            f.equal(0.5, f.value(resistance()), "A self-target stays stable across ticks");
        }
        verifyDuplicateSourcesAndExtremeRatios(helper);
        helper.succeed();
    }

    private static void verifyLoadedRulesAndBudget(GameTestHelper helper) {
        try (Fixture f = new Fixture(helper)) {
            Attribute recovery = AttributeRegistry.SOUL_WARD_RECOVERY_RATE.get();
            Attribute integrity = AttributeRegistry.SOUL_WARD_INTEGRITY.get();
            Attribute magicResistance = LodestoneAttributeRegistry.MAGIC_RESISTANCE.get();
            Attribute mana = LodestoneAttributeRegistry.MAGIC_DAMAGE.get();
            f.rules(f.loadedRule(magic()), f.loadedRule(capacity()), f.loadedRule(recovery), f.loadedRule(integrity));
            f.base(magic(), 2);
            f.base(capacity(), 1);
            f.base(recovery, 2);
            f.base(integrity, 2);
            f.base(Attributes.ARMOR_TOUGHNESS, 8);
            f.base(magicResistance, 2);
            f.omen(0.5);
            f.equal(16D / 7, f.value(magic()), "Four loaded routes: proficiency +14.2857%");
            f.equal(1.4, f.value(capacity()), "Four loaded routes: capacity +40%");
            f.equal(16D / 7, f.value(recovery), "Four loaded routes: recovery +14.2857%");
            f.equal(62D / 29, f.value(integrity), "Four loaded routes: integrity +6.8966%");
            f.equal(10, f.value(Attributes.ARMOR), "Shared armor is halved once");
            f.equal(4, f.value(Attributes.ARMOR_TOUGHNESS), "Toughness is halved");
            f.equal(1, f.value(magicResistance), "Shared magic resistance is halved once");

            f.omen(1);
            f.equal(18D / 7, f.value(magic()), "Full reverse proficiency +28.5714%");
            f.equal(1.8, f.value(capacity()), "Full reverse capacity +80%");
            f.equal(18D / 7, f.value(recovery), "Full reverse recovery +28.5714%");
            f.equal(66D / 29, f.value(integrity), "Full reverse integrity +13.7931%");
            f.equal(0, f.value(Attributes.ARMOR), "Full reverse consumes all armor");
            f.equal(0, f.value(Attributes.ARMOR_TOUGHNESS), "Full reverse consumes all toughness");
            f.equal(1, f.value(magicResistance), "Magic resistance keeps its native floor");

            f.base(mana, 1_000);
            // Simulate Malum's Iron's Spellbooks recipe without requiring that mod on the test server.
            f.putRule(ForgeRegistries.ATTRIBUTES.getKey(mana), new MalignantConversionData(mana, 1, true,
                    List.of(Pair.of(Attributes.ARMOR, 0.04D), Pair.of(magicResistance, 0.04D))));
            f.omen(0.5);
            f.equal(1.32, f.value(capacity()), "A fifth eligible source takes the spend to one fifth each");
            f.equal(15_000D / 11, f.value(mana), "The compatible ratios give +36.3636% mana with five sources");

            f.rules(f.loadedRule(capacity()));
            f.base(Attributes.ARMOR, 40);
            f.tick();
            f.equal(10D / 7, f.value(capacity()), "One route keeps a quarter share, with capped armor 30");
            f.equal(15, f.value(Attributes.ARMOR), "Capped armor is halved after a rule reload");
            f.equal(2, f.value(magicResistance), "Targets dropped by reload regain their defense");
            f.equal(1_000, f.value(mana), "Sources dropped by reload lose their gain");
            f.equal(0.2, TranscendOmenConversion.gainFor(20, 0.5, 8), "Eight sources use one eighth each");
            f.equal(0.016, TranscendOmenConversion.gainFor(20, 0.5, 100), "One hundred sources use one percent each");
            f.equal(1.6 / 101, TranscendOmenConversion.gainFor(20, 0.5, 101), "The old minimum share no longer applies");
        }
    }

    private static void verifyDuplicateSourcesAndExtremeRatios(GameTestHelper helper) {
        try (Fixture f = new Fixture(helper)) {
            var armorRoute = new MalignantConversionData(capacity(), 1, false, List.of(Pair.of(Attributes.ARMOR, 10D)));
            var weakerRoute = new MalignantConversionData(capacity(), 1, false, List.of(Pair.of(resistance(), 0.25D)));
            var tiedRoute = new MalignantConversionData(capacity(), 1, false, List.of(Pair.of(resistance(), 0.125D)));
            f.putRule(ResourceLocation.fromNamespaceAndPath("a", "z"), armorRoute);
            f.putRule(ResourceLocation.fromNamespaceAndPath("b", "a"), tiedRoute);
            f.putRule(ResourceLocation.fromNamespaceAndPath("c", "a"), armorRoute);
            f.putRule(ResourceLocation.fromNamespaceAndPath("a", "a"), weakerRoute);
            f.base(capacity(), 1);
            f.omen(0.5);
            f.equal(8D / 7, f.value(capacity()), "One gain for the best rule across duplicate sources");
            f.equal(10, f.value(Attributes.ARMOR), "Equal coefficients choose the smallest full key");
            f.equal(0.25, f.value(resistance()), "Unselected targets do not pay the cost");

            Attribute mana = LodestoneAttributeRegistry.MAGIC_DAMAGE.get();
            f.base(mana, 1_000);
            f.rules(new MalignantConversionData(mana, 1, false, List.of(Pair.of(Attributes.ARMOR, Double.MIN_VALUE))));
            f.tick();
            f.equal(1_500, f.value(mana), "Finite inputs with an overflowing quotient still saturate");
            f.rules(new MalignantConversionData(mana, 1, false, List.of(
                    Pair.of(Attributes.ARMOR, Double.MIN_VALUE), Pair.of(resistance(), 0.125D),
                    Pair.of(Attributes.ARMOR_TOUGHNESS, Double.NaN), Pair.of(Attributes.ATTACK_DAMAGE, 0D),
                    Pair.of(Attributes.FLYING_SPEED, Double.POSITIVE_INFINITY))));
            f.tick();
            f.equal(8_000D / 7, f.value(mana), "A finite bottleneck wins over the overflowing quotient");
            f.equal(0.125, f.value(resistance()), "The finite target also pays the full cost");
            f.rules(new MalignantConversionData(mana, 1, false,
                    List.of(Pair.of(Attributes.ARMOR, 1D), Pair.of(Attributes.FLYING_SPEED, 1D))));
            f.tick();
            f.equal(1_000, f.value(mana), "A missing required target disables the route");
            f.equal(20, f.value(Attributes.ARMOR), "A disabled route consumes no armor");
        }
    }

    private static MalignantConversionData rule(Attribute source) {
        return new MalignantConversionData(source, 0.5, true,
                List.of(Pair.of(Attributes.ARMOR, 10D), Pair.of(resistance(), 0.25D)));
    }

    private static final class Fixture implements AutoCloseable {
        final GameTestHelper helper;
        final LivingEntity entity;
        final Map<ResourceLocation, MalignantConversionData> original = MalignantConversionReloadListener.CONVERSION_DATA;

        Fixture(GameTestHelper helper) {
            this.helper = helper;
            entity = EntityType.ZOMBIE.create(helper.getLevel());
            // Do not add this entity to the world: each fixture changes the global datapack synchronously.
            MalignantConversionReloadListener.CONVERSION_DATA = new LinkedHashMap<>();
            base(Attributes.ARMOR, 20);
            base(resistance(), 0.25);
            base(capacity(), 0);
        }

        void rules(MalignantConversionData... rules) {
            var data = MalignantConversionReloadListener.CONVERSION_DATA;
            data.clear();
            for (var rule : rules) putRule(ForgeRegistries.ATTRIBUTES.getKey(rule.sourceAttribute()), rule);
        }

        MalignantConversionData loadedRule(Attribute source) { return original.get(ForgeRegistries.ATTRIBUTES.getKey(source)); }
        void putRule(ResourceLocation id, MalignantConversionData rule) {
            MalignantConversionReloadListener.CONVERSION_DATA.put(id, rule);
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
            MalignantConversionReloadListener.CONVERSION_DATA = original;
            entity.discard();
        }
    }
}
