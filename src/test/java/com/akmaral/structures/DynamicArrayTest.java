package com.akmaral.structures;

import com.akmaral.metrics.Metrics.Snapshot;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DynamicArrayTest extends SequenceContractTest {
    @Override IntSequence create() { return new DynamicArray(); }

    @Test void exactCountsForAccessSearchAndShifts() {
        DynamicArray a = new DynamicArray();
        a.add(10); a.add(20); a.add(30);
        a.metrics().reset();
        assertEquals(20, a.get(1));
        assertEquals(new Snapshot(1, 0, 0), a.metrics().snapshot());
        a.metrics().reset();
        assertFalse(a.contains(99));
        assertEquals(new Snapshot(3, 0, 3), a.metrics().snapshot());
        a.metrics().reset();
        a.add(0, 5);
        assertEquals(new Snapshot(3, 3, 0), a.metrics().snapshot());
        a.metrics().reset();
        assertEquals(5, a.remove(0));
        assertEquals(new Snapshot(4, 3, 0), a.metrics().snapshot());
    }

    @Test void resizeCountsEveryCopiedElement() {
        DynamicArray a = new DynamicArray();
        for (int i = 0; i < 8; i++) a.add(i);
        a.metrics().reset();
        a.add(8);
        assertEquals(new Snapshot(8, 8, 0), a.metrics().snapshot());
        for (int i = 0; i < 9; i++) assertEquals(i, a.get(i));
    }
}
