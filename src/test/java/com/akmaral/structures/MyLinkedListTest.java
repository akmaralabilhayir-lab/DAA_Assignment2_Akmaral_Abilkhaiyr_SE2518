package com.akmaral.structures;

import com.akmaral.metrics.Metrics.Snapshot;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MyLinkedListTest extends SequenceContractTest {
    @Override IntSequence create() { return new MyLinkedList(); }

    @Test void countsTraversalAndHeadLinkUpdates() {
        MyLinkedList list = new MyLinkedList();
        list.add(10); list.add(20); list.add(30);
        list.metrics().reset();
        assertEquals(30, list.get(2));
        assertEquals(new Snapshot(2, 0, 0), list.metrics().snapshot());
        list.metrics().reset();
        assertFalse(list.contains(99));
        assertEquals(new Snapshot(3, 0, 3), list.metrics().snapshot());
        list.metrics().reset();
        list.add(0, 5);
        assertEquals(new Snapshot(0, 2, 0), list.metrics().snapshot());
        list.metrics().reset();
        assertEquals(5, list.remove(0));
        assertEquals(new Snapshot(1, 1, 0), list.metrics().snapshot());
    }

    @Test void tailRemainsValidAfterLastRemovalAndReinsertion() {
        MyLinkedList list = new MyLinkedList();
        list.add(1); list.add(2); list.add(3);
        assertEquals(3, list.remove(2));
        list.add(4);
        assertEquals(4, list.get(2));
        while (list.size() > 0) list.remove(0);
        list.add(5); list.add(6);
        assertEquals(5, list.remove(0));
        assertEquals(6, list.remove(0));
    }
}
