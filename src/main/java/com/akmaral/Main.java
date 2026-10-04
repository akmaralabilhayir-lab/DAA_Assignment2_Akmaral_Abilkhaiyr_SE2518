package com.akmaral;

import com.akmaral.benchmark.Benchmark;
import java.nio.file.Path;

public final class Main {
    private Main() { }
    public static void main(String[] args) throws Exception {
        if (args.length > 1) throw new IllegalArgumentException("Usage: java -jar target/daa-assignment2-1.0.jar [results-directory]");
        Benchmark.run(Path.of(args.length == 0 ? "results" : args[0]));
    }
}
