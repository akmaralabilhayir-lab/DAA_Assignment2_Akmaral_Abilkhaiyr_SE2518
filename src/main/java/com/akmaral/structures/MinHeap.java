package com.akmaral.structures;

import com.akmaral.metrics.Metrics;

/** Array-based min heap, with doubling growth and Floyd bottom-up construction. */
public final class MinHeap {
    private int[] data = new int[8];
    private int size;
    private final Metrics metrics = new Metrics();

    public void insert(int value) {
        ensureCapacity();
        int index = size++;
        data[index] = value;
        while (index > 0) {
            int parent = (index - 1) / 2;
            int parentValue = read(parent);
            metrics.compare();
            if (parentValue <= value) break;
            data[index] = parentValue; metrics.move();
            data[parent] = value; metrics.move();
            index = parent;
        }
    }

    public int peekMin() {
        requireNotEmpty();
        return read(0);
    }

    public int extractMin() {
        requireNotEmpty();
        int result = read(0);
        size--;
        if (size > 0) {
            data[0] = read(size); metrics.move();
            bubbleDown(0);
        }
        return result;
    }

    /** Replace the contents with a heap built from a defensive copy in O(n). */
    public void buildHeap(int[] input) {
        if (input == null) throw new NullPointerException("input");
        data = new int[Math.max(8, input.length)];
        size = input.length;
        for (int i = 0; i < size; i++) {
            metrics.step(); // Read of the caller's input array.
            data[i] = input[i]; metrics.move();
        }
        for (int parent = size / 2 - 1; parent >= 0; parent--) bubbleDown(parent);
    }

    private void bubbleDown(int index) {
        int value = read(index);
        // Only edges from index to its children can violate the heap order.
        while (index < size / 2) {
            int child = 2 * index + 1;
            int childValue = read(child);
            int right = child + 1;
            if (right < size) {
                int rightValue = read(right);
                metrics.compare();
                if (rightValue < childValue) {
                    child = right;
                    childValue = rightValue;
                }
            }
            metrics.compare();
            if (value <= childValue) break;
            data[index] = childValue; metrics.move();
            data[child] = value; metrics.move();
            index = child;
        }
    }

    private int read(int index) {
        metrics.step();
        return data[index];
    }

    private void ensureCapacity() {
        if (size < data.length) return;
        int[] bigger = new int[Math.multiplyExact(data.length, 2)];
        for (int i = 0; i < size; i++) {
            bigger[i] = read(i); metrics.move();
        }
        data = bigger;
    }

    private void requireNotEmpty() {
        if (size == 0) throw new IllegalStateException("Heap is empty");
    }

    /** Diagnostic copy for tests; excluded from the operation metrics. */
    public int[] toArray() { return dataCopy(); }

    private int[] dataCopy() {
        int[] result = new int[size];
        System.arraycopy(data, 0, result, 0, size);
        return result;
    }

    public int size() { return size; }
    public Metrics metrics() { return metrics; }
}
