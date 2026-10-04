package com.akmaral.structures;

import com.akmaral.metrics.Metrics;

/** Singly linked list with a tail pointer for constant-time append. */
public final class MyLinkedList implements IntSequence {
    private static final class Node {
        final int value;
        Node next;
        Node(int value) { this.value = value; }
    }

    private Node head;
    private Node tail;
    private int size;
    private final Metrics metrics = new Metrics();

    @Override public void add(int value) { add(size, value); }

    @Override public void add(int index, int value) {
        checkPosition(index);
        Node node = new Node(value);
        if (size == 0) {
            head = node; metrics.move();
            tail = node; metrics.move();
        } else if (index == 0) {
            node.next = head; metrics.move();
            head = node; metrics.move();
        } else if (index == size) {
            tail.next = node; metrics.move();
            tail = node; metrics.move();
        } else {
            Node previous = nodeAt(index - 1);
            node.next = previous.next; metrics.move();
            previous.next = node; metrics.move();
        }
        size++;
    }

    @Override public int remove(int index) {
        checkElement(index);
        Node removed;
        if (index == 0) {
            removed = head;
            head = advance(head); metrics.move();
            if (size == 1) { tail = null; metrics.move(); }
        } else {
            Node previous = nodeAt(index - 1);
            removed = advance(previous);
            previous.next = removed.next; metrics.move();
            if (removed == tail) { tail = previous; metrics.move(); }
        }
        size--;
        return removed.value;
    }

    @Override public int get(int index) {
        checkElement(index);
        return nodeAt(index).value;
    }

    @Override public boolean contains(int value) {
        Node current = head;
        while (current != null) {
            metrics.compare();
            if (current.value == value) return true;
            current = advance(current);
        }
        return false;
    }

    private Node nodeAt(int index) {
        Node current = head;
        for (int i = 0; i < index; i++) current = advance(current);
        return current;
    }

    private Node advance(Node node) {
        metrics.step(); // Following next counts even when it reaches null.
        return node.next;
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
