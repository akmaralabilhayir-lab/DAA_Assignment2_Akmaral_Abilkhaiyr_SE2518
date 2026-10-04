package com.akmaral.benchmark;

import com.akmaral.metrics.Metrics.Snapshot;
import com.akmaral.structures.DynamicArray;
import com.akmaral.structures.IntSequence;
import com.akmaral.structures.MinHeap;
import com.akmaral.structures.MyLinkedList;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.Random;

/** Deterministic workload inputs, ten discarded warm-ups, five measured repetitions. */
public final class Benchmark {
    private static final int[] SIZES = {100, 1000, 10000, 100000};
    private static final int WARMUPS = 10;
    private static final int RUNS = 5;
    private static volatile long blackhole;
    private static final String HEADER = "workload,variant,structure,n,time_ms,steps,moves,comparisons";

    public record Result(String workload, String variant, String structure, int n,
                         double timeMs, Snapshot counts) { }
    private record Sample(long nanos, Snapshot counts, long checksum) { }

    private Benchmark() { }

    public static void run(Path output) throws IOException {
        Files.createDirectories(output);
        Result[] results = new Result[36];
        Result[] bonus = new Result[16];
        int count = 0;
        int bonusCount = 0;
        try (PrintWriter csv = writer(output.resolve("results.csv"));
             PrintWriter raw = writer(output.resolve("raw-runs.csv"));
             PrintWriter extra = writer(output.resolve("buildheap.csv"))) {
            csv.println(HEADER);
            raw.println("workload,variant,structure,n,run,time_ms,steps,moves,comparisons,checksum");
            extra.println(HEADER);
            for (int n : SIZES) {
                int[] data = new int[n];
                Random random = new Random(42);
                for (int i = 0; i < n; i++) data[i] = random.nextInt(1000000);
                int[] indices = new int[10000];
                for (int i = 0; i < indices.length; i++) indices[i] = random.nextInt(n);
                int[] queries = new int[1000];
                for (int i = 0; i < queries.length; i++) {
                    queries[i] = i % 2 == 0 ? data[random.nextInt(n)] : -i - 1;
                }
                for (String workload : new String[]{"W1", "W2", "W3"}) {
                    String[] variants = workload.equals("W3") ? new String[]{"head", "middle"} : new String[]{"-"};
                    for (String variant : variants) {
                        for (String structure : new String[]{"DynamicArray", "MyLinkedList"}) {
                            Result result = measure(workload, variant, structure, data, indices, queries, raw);
                            results[count++] = result;
                            writeResult(csv, result);
                        }
                    }
                }
                Result heap = measure("W4", "-", "MinHeap", data, indices, queries, raw);
                results[count++] = heap;
                writeResult(csv, heap);
                int[] descending = new int[n];
                for (int i = 0; i < n; i++) descending[i] = n - i;
                for (String variant : new String[]{"random", "descending"}) {
                    for (String method : new String[]{"Floyd", "RepeatedInsert"}) {
                        Result result = measure("B", variant, method,
                                variant.equals("random") ? data : descending, indices, queries, raw);
                        bonus[bonusCount++] = result;
                        writeResult(extra, result);
                    }
                }
            }
            if (csv.checkError() || raw.checkError() || extra.checkError()) throw new IOException("Cannot write CSV");
        }
        Files.writeString(output.resolve("environment.txt"),
                "Recorded UTC: " + Instant.now() + "\nJava: " + System.getProperty("java.runtime.version")
                + "\nVM: " + System.getProperty("java.vm.name") + "\nOS: " + System.getProperty("os.name")
                + " " + System.getProperty("os.arch") + "\nAvailable processors: " + Runtime.getRuntime().availableProcessors()
                + "\nSeed: 42\nWarm-ups per case: " + WARMUPS + "\nMeasured runs per case: " + RUNS
                + "\nTiming includes instrumentation; setup is excluded except W4 and bonus construction."
                + "\nW4 sorted-output validation is outside the timed section.\n", StandardCharsets.UTF_8);
        Plots.write(results, bonus, output.resolve("plots"));
        System.out.println("Saved 36 workload cases, 16 bonus cases, raw runs and PNG plots to " + output);
    }

    private static Result measure(String workload, String variant, String structure, int[] data,
                                  int[] indices, int[] queries, PrintWriter raw) {
        Sample[] samples = new Sample[RUNS];
        Sample reference = null;
        for (int r = -WARMUPS; r < RUNS; r++) {
            Sample sample = execute(workload, variant, structure, data, indices, queries);
            blackhole = sample.checksum;
            if (reference == null) reference = sample;
            if (!sample.counts.equals(reference.counts) || sample.checksum != reference.checksum) {
                throw new IllegalStateException("Non-reproducible operations or output");
            }
            if (r >= 0) {
                samples[r] = sample;
                raw.printf(Locale.ROOT, "%s,%s,%s,%d,%d,%.6f,%d,%d,%d,%d%n",
                        workload, variant, structure, data.length, r + 1, sample.nanos / 1e6,
                        sample.counts.steps(), sample.counts.moves(), sample.counts.comparisons(), sample.checksum);
            }
        }
        // Five samples: insertion sort is sufficient and needs no collection.
        for (int i = 1; i < samples.length; i++) {
            Sample selected = samples[i];
            int j = i - 1;
            while (j >= 0 && samples[j].nanos > selected.nanos) {
                samples[j + 1] = samples[j--];
            }
            samples[j + 1] = selected;
        }
        Sample median = samples[RUNS / 2];
        Result result = new Result(workload, variant, structure, data.length, median.nanos / 1e6, median.counts);
        System.out.printf(Locale.ROOT, "%s %-10s %-14s n=%6d median=%10.4f ms%n",
                workload, variant, structure, data.length, result.timeMs);
        return result;
    }

    private static Sample execute(String workload, String variant, String structure,
                                  int[] data, int[] indices, int[] queries) {
        if (workload.equals("W4") || workload.equals("B")) {
            MinHeap heap = new MinHeap();
            int[] extracted = workload.equals("W4") ? new int[data.length] : null;
            long start = System.nanoTime();
            if (structure.equals("Floyd")) heap.buildHeap(data);
            else for (int value : data) heap.insert(value);
            if (extracted != null) {
                for (int i = 0; i < data.length; i++) extracted[i] = heap.extractMin();
            }
            long elapsed = System.nanoTime() - start;
            Snapshot counts = heap.metrics().snapshot();
            // Validation and checksum consumption do not enter the measured counters/time.
            if (extracted == null) {
                extracted = new int[data.length];
                for (int i = 0; i < data.length; i++) extracted[i] = heap.extractMin();
            }
            long checksum = 0;
            for (int i = 0; i < extracted.length; i++) {
                if (i > 0 && extracted[i - 1] > extracted[i]) throw new IllegalStateException("Heap output is not sorted");
                checksum = checksum * 31 + extracted[i];
            }
            return new Sample(elapsed, counts, checksum);
        }
        IntSequence sequence = structure.equals("DynamicArray") ? new DynamicArray() : new MyLinkedList();
        for (int value : data) sequence.add(value);
        sequence.metrics().reset();
        long checksum = 0;
        long start = System.nanoTime();
        switch (workload) {
            case "W1" -> { for (int index : indices) checksum += sequence.get(index); }
            case "W2" -> { for (int query : queries) if (sequence.contains(query)) checksum++; }
            case "W3" -> {
                int index = variant.equals("head") ? 0 : data.length / 2;
                for (int i = 0; i < 1000; i++) sequence.add(index, -i - 1);
                for (int i = 0; i < 1000; i++) checksum += sequence.remove(index);
            }
            default -> throw new IllegalArgumentException(workload);
        }
        long elapsed = System.nanoTime() - start;
        Snapshot counts = sequence.metrics().snapshot();
        if (workload.equals("W1")) {
            long expected = 0;
            for (int index : indices) expected += data[index];
            if (checksum != expected) throw new IllegalStateException("Incorrect random access");
        } else if (workload.equals("W2") && checksum != 500) {
            throw new IllegalStateException("Search must find exactly 500 values");
        } else if (workload.equals("W3")) {
            if (checksum != -500500 || sequence.size() != data.length) throw new IllegalStateException("Incorrect mutations");
            // Full sequence validation is covered by contract tests; spot checks avoid quadratic untimed traversal.
            for (int index : new int[]{0, data.length / 2, data.length - 1}) {
                if (sequence.get(index) != data[index]) throw new IllegalStateException("Original data changed");
            }
        }
        return new Sample(elapsed, counts, checksum);
    }

    private static PrintWriter writer(Path path) throws IOException {
        return new PrintWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8));
    }

    private static void writeResult(PrintWriter writer, Result r) {
        writer.printf(Locale.ROOT, "%s,%s,%s,%d,%.6f,%d,%d,%d%n", r.workload, r.variant, r.structure,
                r.n, r.timeMs, r.counts.steps(), r.counts.moves(), r.counts.comparisons());
    }
}
