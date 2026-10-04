# DAA Assignment 2 - In-Memory Workload Engine

**Akmaral Abilkhaiyr | SE-2518**

Primitive `int` implementations of a dynamic array, singly linked list and binary min heap, with physical-operation counters, JUnit 5 tests and reproducible benchmarks.

- Repository: https://github.com/akmaralabilhayir-lab/DAA_Assignment2_Akmaral_Abilkhaiyr_SE2518
- Submission branch: `main`; release tag: `v1.0`.
- Analysis: [REPORT.md](REPORT.md) and the five-page [REPORT.pdf](REPORT.pdf).
- Measurements: [results/results.csv](results/results.csv); individual measured runs: [results/raw-runs.csv](results/raw-runs.csv).
- Bonus B: Floyd's linear-time `buildHeap`, with [measurements](results/buildheap.csv) and a [plot](results/plots/buildheap.png).

## Requirements

JDK **17 or newer** and Maven **3.8+**. The submitted measurements were collected using OpenJDK 25.0.1 on Windows 11. The project compiles with Java 17 language/API compatibility. Maven downloads its plugins and JUnit on the first build; benchmark and chart generation have no third-party runtime dependencies.

## Build and test

```sh
mvn clean verify
```

This runs all **18 JUnit 5 tests** and builds `target/daa-assignment2-1.0.jar`. Tests compare random operations with `ArrayList`/`PriorityQueue`, cover empty/singleton/duplicate/extreme values and invalid indices, verify selected exact counter values, and check the heap property after every mutation in the heap tests.

## Run the benchmark and generate all plots

After building, one command generates the CSV files, environment record and five PNG charts:

```sh
java -jar target/daa-assignment2-1.0.jar
```

To preserve the submitted measurements, choose another output folder:

```sh
java -jar target/daa-assignment2-1.0.jar results-local
```

The full run takes roughly a minute on the recorded machine, depending on other applications. The sequence is **10 discarded warm-ups + 5 measured repetitions per case**, using a new structure each time. Data and query arrays are generated once for each size with `new Random(42)` and shared by both sequence implementations. Runtime varies between runs; operation counts are deterministic. The report describes the committed measurements and does not automatically change when the benchmark is rerun.

## Open in IntelliJ IDEA

1. Choose **File -> Open** and select this folder or its `pom.xml` (extract the ZIP first).
2. Import as a Maven project and select a JDK 17+ under **Project Structure -> Project SDK**.
3. Reload Maven. Run the Maven `verify` goal, or run the test classes in `src/test/java`.
4. Run `com.akmaral.Main` with the project root as the working directory to generate `results/`.

If `mvn` is unavailable in the terminal, use IntelliJ's Maven tool window; the bundled Maven distribution works. If compilation reports an unsupported release, check the JDK used by Maven as well as the Project SDK.

## Layout

```text
src/main/java/com/akmaral/
  Main.java                    benchmark entry point
  structures/                  IntSequence, DynamicArray, MyLinkedList, MinHeap
  metrics/Metrics.java         long counters and immutable snapshots
  benchmark/                   workload runner and Java2D PNG plotting
src/test/java/com/akmaral/structures/
results/
  results.csv                  36 required workload cases
  raw-runs.csv                 260 individual measurements (required + bonus)
  buildheap.csv                16 bonus construction cases
  environment.txt              runtime and measurement settings
  plots/                       W1.png, W2.png, W3.png, W4.png, buildheap.png
README.md
REPORT.md
REPORT.pdf
```

## Workload and counter conventions

| Case | Measured work |
|---|---|
| W1 | 10,000 `get(index)` calls after filling the structure |
| W2 | 1,000 `contains` queries: 500 sampled present values and 500 negative absent values |
| W3 head | 1,000 insertions at zero, followed by 1,000 removals at zero |
| W3 middle | Same operations at the fixed original index `n / 2`, even while size changes |
| W4 | `n` inserts followed by `n` extractions; sorted output is checked outside timing |

Sizes are 100, 1,000, 10,000 and 100,000. W1-W3 filling, input generation and counter resets are outside timing. W3 includes array growth if it occurs. W4 includes heap growth. Time includes counter updates and benchmark loop/checksum overhead; these are instrumented measurements, not isolated hardware-latency measurements.

- **steps:** each read of an array cell inside a structure method, or each traversal of a list `next` link (including reaching `null`). A local cached integer is not another read. `get(0)` on the list has zero next-link steps but still takes constant time.
- **moves:** relocation of an existing array element (including growth copies and two writes per heap swap), or an explicit assignment to `head`, `tail` or `Node.next`. Storing a new input integer in an array is not a relocation. Floyd's input copy counts reads and moves.
- **comparisons:** comparisons between stored/input element values. Bounds checks, loop conditions and comparisons of references are excluded.

Node value reads and allocations are not separately counted by the assignment's counter model. Test-only diagnostic `MinHeap.toArray()` does not affect counters. `Random` and `Locale` are utility classes; no `java.util` collection is used in production code. Standard collections and boxed oracle values appear only in tests.

## Git history and submission

Development is recorded on `feature/array`, `feature/list`, `feature/heap` and `feature/metrics`, with tested merges into `main`. The history records the actual implementation session; dates are not backdated. Inspect it with:

```sh
git log --all --graph --oneline --decorate
git show v1.0 --stat
```

The release ZIP excludes build output, IDE settings and `.git`; the full history is on GitHub. Upload `DAA_Assignment2_Akmaral_Abilkhaiyr_SE2518.zip` to Moodle and include the repository link. Moodle upload is a separate step.

## Walkthrough before the defense

Follow an insertion through array growth, track the list's head/tail after removing the last node, and trace heap bubble-down on `[9, 2, 3, 7, 8]`. Explain why list head insertion is constant-time but insertion by a middle index requires traversal. Reproduce the five parts of each invariant proof in the report. Distinguish single-operation worst-case costs from amortized costs and explain why a counter is not a CPU instruction count.

The project was prepared with AI assistance. The assignment document restricts AI use to debugging and concept explanations; review the course rules and ensure you can explain the implementation before submitting.
