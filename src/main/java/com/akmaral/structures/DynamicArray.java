package com.akmaral.structures;

import com.akmaral.metrics.Metrics;

/** A resizable int array. Capacity doubles on growth and never shrinks. */
public final class DynamicArray implements IntSequence {
    private int[] data = new int[8];
    private int size;
    private final Metrics metrics = new Metrics();

    @Override public void add(int value) { add(size, value); }

    @Override public void add(int index, int value) {
        checkPosition(index);
        ensureCapacity();
        for (int i = size; i > index; i--) {
            data[i] = read(i - 1);
            metrics.move();
        }
        data[index] = value; // A new input value is not a shifted element.
        size++;
    }

    @Override public int remove(int index) {
        checkElement(index);
        int removed = read(index);
        for (int i = index; i < size - 1; i++) {
            data[i] = read(i + 1);
            metrics.move();
        }
        size--;
        return removed;
    }

    @Override public int get(int index) {
        checkElement(index);
        return read(index);
    }

    @Override public boolean contains(int value) {
        // Before iteration i, no entry in data[0..i) equals value.
        for (int i = 0; i < size; i++) {
            int current = read(i);
            metrics.compare();
            if (current == value) return true;
        }
        return false;
    }

    private int read(int index) {
        metrics.step();
        return data[index];
    }

    private void ensureCapacity() {
        if (size < data.length) return;
        int[] bigger = new int[Math.multiplyExact(data.length, 2)];
        for (int i = 0; i < size; i++) {
            bigger[i] = read(i);
            metrics.move();
        }
        data = bigger;
    }

    private void checkElement(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
    }

    private void checkPosition(int index) {
        if (index < 0 || index > size) throw new IndexOutOfBoundsException(index);
    }

    @Override public int size() { return size; }
    @Override public Metrics metrics() { return metrics; }
}
