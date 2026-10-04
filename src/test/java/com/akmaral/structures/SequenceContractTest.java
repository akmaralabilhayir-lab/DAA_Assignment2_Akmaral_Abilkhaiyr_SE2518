package com.akmaral.structures;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

/** Shared tests inherited by both implementations; collections are test oracles only. */
abstract class SequenceContractTest {
    abstract IntSequence create();

    @Test void emptyAndInvalidIndices() {
        IntSequence s = create();
        assertEquals(0, s.size());
        assertFalse(s.contains(3));
        assertThrows(IndexOutOfBoundsException.class, () -> s.get(0));
        assertThrows(IndexOutOfBoundsException.class, () -> s.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> s.remove(0));
        assertThrows(IndexOutOfBoundsException.class, () -> s.remove(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> s.add(-1, 8));
        assertThrows(IndexOutOfBoundsException.class, () -> s.add(1, 8));
        s.add(7);
        assertThrows(IndexOutOfBoundsException.class, () -> s.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> s.remove(1));
        assertThrows(IndexOutOfBoundsException.class, () -> s.add(2, 8));
        assertEquals(1, s.size());
        assertEquals(7, s.get(0));
    }

    @Test void singletonDuplicatesAndBoundaries() {
        IntSequence s = create();
        s.add(0, 7);
        assertTrue(s.contains(7));
        assertEquals(7, s.remove(0));
        s.add(Integer.MAX_VALUE);
        s.add(0, Integer.MIN_VALUE);
        s.add(s.size(), Integer.MAX_VALUE);
        assertEquals(Integer.MIN_VALUE, s.remove(0));
        assertEquals(Integer.MAX_VALUE, s.remove(s.size() - 1));
        assertEquals(Integer.MAX_VALUE, s.remove(0));
        assertEquals(0, s.size());
        s.add(9);
        assertEquals(9, s.get(0));
    }

    @Test void growthAndFullDrain() {
        IntSequence s = create();
        for (int i = 0; i < 5000; i++) s.add(i);
        for (int i = 0; i < 5000; i++) assertEquals(i, s.get(i));
        for (int i = 4999; i >= 0; i--) assertEquals(i, s.remove(s.size() - 1));
        assertEquals(0, s.size());
    }

    @Test void randomOperationsMatchReference() {
        IntSequence actual = create();
        ArrayList<Integer> expected = new ArrayList<>();
        Random random = new Random(42);
        for (int k = 0; k < 12000; k++) {
            int value = random.nextInt(101) - 50;
            int operation = random.nextInt(5);
            if (operation == 0 || expected.isEmpty()) {
                actual.add(value); expected.add(value);
            } else if (operation == 1) {
                int index = random.nextInt(expected.size() + 1);
                actual.add(index, value); expected.add(index, value);
            } else if (operation == 2) {
                int index = random.nextInt(expected.size());
                assertEquals(expected.remove(index).intValue(), actual.remove(index));
            } else if (operation == 3) {
                int index = random.nextInt(expected.size());
                assertEquals(expected.get(index).intValue(), actual.get(index));
            } else {
                assertEquals(expected.contains(value), actual.contains(value));
            }
            assertEquals(expected.size(), actual.size());
            if (k % 100 == 0) {
                for (int i = 0; i < expected.size(); i++) assertEquals(expected.get(i).intValue(), actual.get(i));
            }
        }
        for (int i = 0; i < expected.size(); i++) assertEquals(expected.get(i).intValue(), actual.get(i));
    }
}
