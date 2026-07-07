package pcd.ass02.virtualthreads;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import pcd.ass02.common.FSReport;

public final class VirtualThreadFSStatLib {

    public CompletableFuture<FSReport> getFSReport(Path root, long maxFS, int nb) {
        FSReport.validate(maxFS, nb);
        CompletableFuture<FSReport> result = new CompletableFuture<>();
        Thread.startVirtualThread(() -> {
            try (ExecutorService executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
                result.complete(explore(root, maxFS, nb, executor));
            } catch (Throwable t) {
                result.completeExceptionally(t);
            }
        });
        return result;
    }

    private FSReport explore(Path dir, long maxFS, int nb, ExecutorService executor)
            throws ExecutionException, InterruptedException {
        FSReport local = FSReport.empty(nb);
        List<Future<FSReport>> children = new ArrayList<>();

        for (Path entry : list(dir)) {
            if (Files.isDirectory(entry)) {
                children.add(executor.submit(() -> explore(entry, maxFS, nb, executor)));
            } else if (Files.isRegularFile(entry)) {
                local = local.merge(fileReport(entry, maxFS, nb));
            }
        }

        for (Future<FSReport> child : children) {
            local = local.merge(child.get());
        }
        return local;
    }

    private FSReport fileReport(Path file, long maxFS, int nb) {
        try {
            return FSReport.single(Files.size(file), maxFS, nb);
        } catch (IOException e) {
            return FSReport.empty(nb);
        }
    }

    private List<Path> list(Path dir) {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.toList();
        } catch (IOException e) {
            return List.of();
        }
    }
}
