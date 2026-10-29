package org.brahypno.maledict.common.vicissitude;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.Test;
import org.brahypno.maledict.common.combat.DamageAdaptation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DamageAdaptationStorageTest {
    @Test
    void encodedNbtRoundTripKeepsTheNextReductionAndEviction() throws IOException {
        DamageAdaptation original = new DamageAdaptation();
        for (int hit = 0; hit < 5; hit++) original.adapt("A", 2);
        for (int hit = 0; hit < 2; hit++) original.adapt("B", 2);

        CompoundTag saved = new CompoundTag();
        saved.put("DamageAdaptation", DamageAdaptationStorage.save(original));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        NbtIo.write(saved, new DataOutputStream(bytes));
        CompoundTag decoded = NbtIo.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        DamageAdaptation loaded = new DamageAdaptation();
        DamageAdaptationStorage.load(loaded, decoded.getList("DamageAdaptation", Tag.TAG_COMPOUND));

        assertEquals(List.of("B", "A"), loaded.snapshot());
        assertEquals(Map.of("A", 5, "B", 2), loaded.hitCounts());
        assertEquals(Math.exp(-5), loaded.adapt("A", 2), 1.0E-6);
        assertEquals(1.0F, loaded.adapt("C", 2));
        assertEquals(1.0F, loaded.adapt("B", 2), "An evicted message must still return at full damage");
    }

}
