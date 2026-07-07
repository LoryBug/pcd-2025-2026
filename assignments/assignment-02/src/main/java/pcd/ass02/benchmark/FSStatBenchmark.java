package pcd.ass02.benchmark;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Stream;
import pcd.ass02.common.FSReport;
import pcd.ass02.eventloop.EventLoopFSStatLib;
import pcd.ass02.reactive.ReactiveFSStatLib;
import pcd.ass02.virtualthreads.VirtualThreadFSStatLib;

public final class FSStatBenchmark {

    private static final int DEFAULT_RUNS = 5;
    private static final long DEFAULT_MAX_FS = 32 * 1024;
    private static final int DEFAULT_NB = 8;
    private static final int DIRS = 30;
    private static final int FILES_PER_DIR = 40;

    private FSStatBenchmark() {
    }

    public static void main(String[] args) throws Exception {
        boolean generated = args.length == 0;
        Path root = generated ? createDataset() : Path.of(args[0]).toAbsolutePath().normalize();
        long maxFS = args.length > 1 ? Long.parseLong(args[1]) : DEFAULT_MAX_FS;
        int nb = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_NB;
        int runs = args.length > 3 ? Integer.parseInt(args[3]) : DEFAULT_RUNS;

        try {
            System.out.println("root=" + root);
            System.out.println("maxFS=" + maxFS + ", nb=" + nb + ", runs=" + runs);
            System.out.println("version,totalFiles,overMax,avgMs,minMs,bands");

            FSReport eventLoop = bench("eventloop", runs, () -> {
                try (EventLoopFSStatLib lib = new EventLoopFSStatLib()) {
                    return lib.getFSReport(root, maxFS, nb).join();
                }
            });
            FSReport reactive = bench("reactive", runs, () -> {
                try (ReactiveFSStatLib lib = new ReactiveFSStatLib()) {
                    return lib.getFSReport(root, maxFS, nb).blockingGet();
                }
            });
            FSReport virtualThreads = bench("vt", runs,
                () -> new VirtualThreadFSStatLib().getFSReport(root, maxFS, nb).join());

            if (!eventLoop.equals(reactive) || !eventLoop.equals(virtualThreads)) {
                throw new IllegalStateException("Versions produced different reports");
            }
        } finally {
            if (generated) {
                deleteRecursively(root);
            }
        }
    }

    private static FSReport bench(String name, int runs, ReportSupplier supplier) throws Exception {
        supplier.get();
        long totalNanos = 0;
        long minNanos = Long.MAX_VALUE;
        FSReport report = null;

        for (int i = 0; i < runs; i++) {
            long start = System.nanoTime();
            report = supplier.get();
            long elapsed = System.nanoTime() - start;
            totalNanos += elapsed;
            minNanos = Math.min(minNanos, elapsed);
        }

        double avgMs = totalNanos / 1_000_000.0 / runs;
        double minMs = minNanos / 1_000_000.0;
        System.out.printf(Locale.ROOT, "%s,%d,%d,%.2f,%.2f,%s%n",
            name, report.totalFiles(), report.overMaxCount(), avgMs, minMs, report.bandCounts());
        return report;
    }

    private static Path createDataset() throws IOException {
        Path root = Files.createTempDirectory("pcd-ass02-bench-");
        for (int dirIndex = 0; dirIndex < DIRS; dirIndex++) {
            Path dir = root.resolve("group-" + (dirIndex / 10)).resolve("dir-" + dirIndex);
            Files.createDirectories(dir);
            for (int fileIndex = 0; fileIndex < FILES_PER_DIR; fileIndex++) {
                int size = (dirIndex * 131 + fileIndex * 8191) % (64 * 1024);
                Files.write(dir.resolve("file-" + fileIndex + ".bin"), new byte[size]);
            }
        }
        return root;
    }

    private static void deleteRecursively(Path root) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    @FunctionalInterface
    private interface ReportSupplier {
        FSReport get() throws Exception;
    }
}
