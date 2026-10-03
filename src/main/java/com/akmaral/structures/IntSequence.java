package com.akmaral.structures;

import com.akmaral.metrics.Metrics;

/** Shared primitive-int API for the two sequence implementations. */
public interface IntSequence {
    void add(int value);
    void add(int index, int value);
    int remove(int index);
    int get(int index);
    boolean contains(int value);
    int size();
    Metrics metrics();
}
