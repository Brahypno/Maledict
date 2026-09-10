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

/** Run with gradlew test; entity integration still needs a Forge game. */
public final class VicissitudeVitalityTest {
    @BeforeAll
    static void initializeMinecraftVersion() {
        SharedConstants.tryDetectVersion();
    }

    // 示例 1：同 tick 去重；三个不同 tick 的 kill 才能击穿。
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

    // 示例 2：验证连续尝试窗口的边界和超时重置。
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

    // 示例 3：普通扣血、治疗与 kill 计数共存。
    @Test
    void ordinaryWoundsDoNotResetTheSequence() {
        VicissitudeVitality state = VicissitudeVitality.initial(1000.0F)
                .killAttempt(10, 100).afterDamage(10.0F, 11, 20);
        assertTrue(state.current() == 990.0F && state.nextHit() == 31, "Damage must preserve its cooldown");
        state = state.killAttempt(20, 100).withCurrent(995.0F).killAttempt(30, 100);
        assertTrue(state.current() == 0.0F, "Damage/healing within the window must not erase bypass attempts");
    }

    // 示例 4：实际写入文件并重载，验证 UUID 隔离与状态保留。
    @Test
    void persistenceKeepsWoundsAndDeadIdentities(@TempDir Path directory) throws IOException {
        UUID woundedId = UUID.randomUUID();
        UUID deadId = UUID.randomUUID();
        VicissitudeVitality wounded = VicissitudeVitality.initial(1400.0F)
                .afterDamage(14.0F, 10, 20).killAttempt(11, 100).killAttempt(12, 100);
        VicissitudeVitality corpse = VicissitudeVitality.initial(1000.0F)
                .killAttempt(1, 100).killAttempt(2, 100).killAttempt(3, 100);
        VicissitudeVitalityLedger ledger = new VicissitudeVitalityLedger();
        ledger.commit(woundedId, wounded);
        ledger.commit(deadId, corpse);
        Path file = directory.resolve("ledger.dat");
        ledger.save(file.toFile());
        VicissitudeVitalityLedger restored =
                VicissitudeVitalityLedger.load(NbtIo.readCompressed(file.toFile()).getCompound("data"));
        assertTrue(wounded.equals(restored.find(woundedId)), "Saving must retain captured bonus, wounds and timers");
        assertTrue(corpse.equals(restored.find(deadId)), "A dead UUID must stay dead on reload");
        assertTrue(restored.find(woundedId).killAttempt(13, 100).current() == 0.0F,
                "Reload must not reset two accumulated attempts");
        assertTrue(restored.find(UUID.randomUUID()) == null, "Different UUIDs must not share vitality");
    }

    // 示例 5：伪造 Capability 的 NBT，验证绑定后的防反写行为。
    @Test
    void entityNbtCannotRewriteTheLedgerOrBoundCapability() {
        UUID identity = UUID.randomUUID();
        VicissitudeVitality authority = VicissitudeVitality.initial(1000.0F).afterDamage(10.0F, 1, 20);
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
