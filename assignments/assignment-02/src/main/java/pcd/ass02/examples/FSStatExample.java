package pcd.ass02.examples;

import java.nio.file.Path;
import pcd.ass02.common.FSReport;
import pcd.ass02.eventloop.EventLoopFSStatLib;
import pcd.ass02.reactive.ReactiveFSStatLib;
import pcd.ass02.virtualthreads.VirtualThreadFSStatLib;

public final class FSStatExample {

    private FSStatExample() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: <eventloop|reactive|vt> <directory> <max-file-size> <num-bands>");
            System.out.println("Example: eventloop . 1048576 8");
            return;
        }

        String version = args[0].toLowerCase();
        Path root = Path.of(args[1]);
        long maxFS = Long.parseLong(args[2]);
        int nb = Integer.parseInt(args[3]);

        long start = System.currentTimeMillis();
        FSReport report = switch (version) {
            case "eventloop", "async" -> runEventLoop(root, maxFS, nb);
            case "reactive", "rx" -> runReactive(root, maxFS, nb);
            case "vt", "virtual", "virtualthreads" -> new VirtualThreadFSStatLib().getFSReport(root, maxFS, nb).join();
            default -> throw new IllegalArgumentException("Unknown version: " + version);
        };
        long elapsed = System.currentTimeMillis() - start;

        System.out.println("Version: " + version);
        System.out.println("Root: " + root.toAbsolutePath());
        System.out.println("Total files: " + report.totalFiles());
        System.out.println("Bands (last is > MaxFS): " + report.bandCounts());
        System.out.println("Elapsed ms: " + elapsed);
    }

    private static FSReport runEventLoop(Path root, long maxFS, int nb) {
        try (EventLoopFSStatLib lib = new EventLoopFSStatLib()) {
            return lib.getFSReport(root, maxFS, nb).join();
        }
    }

    private static FSReport runReactive(Path root, long maxFS, int nb) {
        try (ReactiveFSStatLib lib = new ReactiveFSStatLib()) {
            return lib.getFSReport(root, maxFS, nb).blockingGet();
        }
    }
}
