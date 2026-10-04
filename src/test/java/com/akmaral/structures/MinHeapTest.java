package com.akmaral.structures;

import com.akmaral.metrics.Metrics.Snapshot;
import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class MinHeapTest {
    private void checkProperty(MinHeap heap) {
        Snapshot before = heap.metrics().snapshot();
        int[] a = heap.toArray();
        for (int i = 1; i < a.length; i++) assertTrue(a[(i - 1) / 2] <= a[i]);
        assertEquals(before, heap.metrics().snapshot());
    }

    @Test void emptySingletonDuplicatesAndExtremes() {
        MinHeap heap = new MinHeap();
        assertThrows(IllegalStateException.class, heap::peekMin);
        assertThrows(IllegalStateException.class, heap::extractMin);
        heap.insert(1); checkProperty(heap);
        assertEquals(1, heap.peekMin());
        assertEquals(1, heap.extractMin()); checkProperty(heap);
        int[] values = {Integer.MAX_VALUE, 0, Integer.MIN_VALUE, 0, -1};
        for (int x : values) { heap.insert(x); checkProperty(heap); }
        Arrays.sort(values);
        for (int x : values) { assertEquals(x, heap.extractMin()); checkProperty(heap); }
        assertEquals(0, heap.size());
    }

    @Test void randomMixedOperationsMatchPriorityQueue() {
        MinHeap heap = new MinHeap();
        PriorityQueue<Integer> expected = new PriorityQueue<>();
        Random random = new Random(42);
        for (int i = 0; i < 5000; i++) {
            if (expected.isEmpty() || random.nextBoolean()) {
                int value = random.nextInt(200) - 100;
                heap.insert(value); expected.add(value);
            } else {
                assertEquals(expected.remove().intValue(), heap.extractMin());
            }
            assertEquals(expected.size(), heap.size());
            if (!expected.isEmpty()) assertEquals(expected.peek().intValue(), heap.peekMin());
            checkProperty(heap);
        }
        while (!expected.isEmpty()) {
            assertEquals(expected.remove().intValue(), heap.extractMin());
            checkProperty(heap);
        }
    }

    @Test void sortedOutputAndPropertyAfterEveryMutation() {
        MinHeap heap = new MinHeap();
        int[] expected = new Random(42).ints(2000).toArray();
        for (int x : expected) { heap.insert(x); checkProperty(heap); }
        Arrays.sort(expected);
        for (int x : expected) { assertEquals(x, heap.extractMin()); checkProperty(heap); }
    }

    @Test void floydBuildHandlesAllShapesAndDoesNotAliasInput() {
        Random random = new Random(42);
        for (int n : new int[]{0, 1, 2, 3, 4, 7, 8, 9, 100, 1000}) {
            int[] input = random.ints(n, -20, 21).toArray();
            int[] expected = input.clone();
            MinHeap heap = new MinHeap();
            heap.insert(999);
            heap.buildHeap(input); checkProperty(heap);
            assertArrayEquals(expected, input);
            if (n > 0) input[0] = 10000;
            Arrays.sort(expected);
            for (int x : expected) { assertEquals(x, heap.extractMin()); checkProperty(heap); }
        }
        assertThrows(NullPointerException.class, () -> new MinHeap().buildHeap(null));
    }

    @Test void countsComparisonsAndSwapsExactly() {
        MinHeap heap = new MinHeap();
        heap.insert(3); heap.insert(1);
        assertEquals(new Snapshot(1, 2, 1), heap.metrics().snapshot());
        heap.metrics().reset();
        assertEquals(1, heap.peekMin());
        assertEquals(new Snapshot(1, 0, 0), heap.metrics().snapshot());
        heap.metrics().reset();
        assertEquals(1, heap.extractMin());
        assertEquals(new Snapshot(3, 1, 0), heap.metrics().snapshot());
    }

    @Test void bottomUpComparisonsGrowLinearlyOnDescendingInput() {
        int n = 10000;
        int[] values = new int[n];
        for (int i = 0; i < n; i++) values[i] = n - i;
        MinHeap heap = new MinHeap();
        heap.buildHeap(values);
        checkProperty(heap);
        assertTrue(heap.metrics().comparisons() < 2L * n);
    }
}
