package org.brahypno.maledict.common.entity;

import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

public final class VicissitudeVitalityTest {
    @BeforeAll
    static void initializeMinecraftVersion() {
        SharedConstants.tryDetectVersion();
    }

    @Test
    void thirdDistinctKillBypasses() {
        VicissitudeVitality state = VicissitudeVitality.initial(1000.0F);
        state = state.killAttempt(10, 100);
        for (int probe = 0; probe < 20; probe++) {
            state = state.killAttempt(10, 100);
        }
        assertTrue(state.current() == 1000.0F && state.killAttempts() == 1,
                "One tick of probes must not immediately bypass");
        state = state.killAttempt(11, 100);
        assertTrue(state.current() == 1000.0F && state.killAttempts() == 2, "Second attempt must survive");
        state = state.killAttempt(12, 100);
        assertTrue(state.current() == 0.0F && state.killAttempts() == 3, "Third attempt must kill");
        assertTrue(state.killAttempt(13, 100).equals(state), "Repeated death calls must remain terminal");
    }

    @Test
    void expiredSequenceRestarts() {
        VicissitudeVitality state = VicissitudeVitality.initial(1000.0F)
                .killAttempt(0, 100).killAttempt(100, 100);
        assertTrue(state.killAttempts() == 2, "Window boundary must be inclusive");
        state = state.killAttempt(201, 100);
        assertTrue(state.killAttempts() == 1 && state.current() == 1000.0F, "Expired sequence must restart");
        state = state.killAttempt(20, 100);
        assertTrue(state.killAttempts() == 1, "Moving world time backwards must restart the sequence");
    }

    @Test
    void ordinaryWoundsDoNotResetTheSequence() {
        VicissitudeVitality state = VicissitudeVitality.initial(1000.0F)
                .killAttempt(10, 100).afterDamage(10.0F);
        assertTrue(state.current() == 990.0F, "Damage must come off the pool");
        state = state.killAttempt(20, 100).withCurrent(995.0F).killAttempt(30, 100);
        assertTrue(state.current() == 0.0F, "Damage/healing within the window must not erase bypass attempts");
    }

    @Test
    void persistenceKeepsWoundsAndDeadIdentities(@TempDir Path directory) throws IOException {
        UUID woundedId = UUID.randomUUID();
        UUID deadId = UUID.randomUUID();
        VicissitudeVitality wounded = VicissitudeVitality.initial(1400.0F)
                .afterDamage(14.0F).afterAcceptedHit(37, 20).killAttempt(11, 100).killAttempt(12, 100);
        VicissitudeVitality corpse = VicissitudeVitality.initial(1000.0F)
                .killAttempt(1, 100).killAttempt(2, 100).killAttempt(3, 100);
        VicissitudeVitalityLedger ledger = new VicissitudeVitalityLedger();
        ledger.commit(woundedId, wounded);
        ledger.commit(deadId, corpse);
        Path file = directory.resolve("ledger.dat");
        ledger.save(file.toFile());
        VicissitudeVitalityLedger restored =
                VicissitudeVitalityLedger.load(NbtIo.readCompressed(file.toFile()).getCompound("data"));
        assertTrue(wounded.equals(restored.find(woundedId)), "Saving must retain the open gate, wounds and timers");
        assertTrue(corpse.equals(restored.find(deadId)), "A dead UUID must stay dead on reload");
        assertTrue(restored.find(woundedId).killAttempt(13, 100).current() == 0.0F,
                "Reload must not reset two accumulated attempts");
        assertTrue(restored.find(UUID.randomUUID()) == null, "Different UUIDs must not share vitality");
    }

    @Test
    void entityNbtCannotRewriteTheLedgerOrBoundCapability() {
        UUID identity = UUID.randomUUID();
        VicissitudeVitality authority = VicissitudeVitality.initial(1000.0F)
                .afterDamage(10.0F);
        VicissitudeVitalityLedger ledger = new VicissitudeVitalityLedger();
        ledger.commit(identity, authority);
        VicissitudeVitalityCapability.Mirror capability = new VicissitudeVitalityCapability.Mirror();
        capability.synchronize(authority);
        CompoundTag forged = capability.serializeNBT();
        forged.putFloat("Current", 0.0F);
        forged.putInt("KillAttempts", 3);
        capability.deserializeNBT(forged);
        assertTrue(capability.snapshot().current() == 990.0F && capability.snapshot().killAttempts() == 0,
                "Live ForgeCaps mutation must not change vitality or spend bypass attempts");
        assertTrue(authority.equals(ledger.find(identity)), "Entity-side mutation must leave world authority intact");

        VicissitudeVitalityCapability.Mirror loaded = new VicissitudeVitalityCapability.Mirror();
        loaded.deserializeNBT(forged);
        loaded.synchronize(ledger.find(identity));
        assertTrue(loaded.snapshot().current() == 990.0F, "On join the world record must replace even valid forged entity NBT");
    }
}
