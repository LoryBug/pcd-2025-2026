package pcd.ass02.eventloop;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;
import pcd.ass02.common.FSReport;

public final class EventLoopFSStatLib implements AutoCloseable {

    private final Executor ioExecutor;

    public EventLoopFSStatLib() {
        this(Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors())));
    }

    public EventLoopFSStatLib(Executor ioExecutor) {
        this.ioExecutor = ioExecutor;
    }

    public CompletableFuture<FSReport> getFSReport(Path root, long maxFS, int nb) {
        FSReport.validate(maxFS, nb);
        return explore(root, maxFS, nb);
    }

    private CompletableFuture<FSReport> explore(Path path, long maxFS, int nb) {
        return CompletableFuture.supplyAsync(() -> list(path), ioExecutor)
            .thenCompose(entries -> {
                List<CompletableFuture<FSReport>> futures = new ArrayList<>();
                for (Path entry : entries) {
                    if (Files.isDirectory(entry)) {
                        futures.add(explore(entry, maxFS, nb));
                    } else if (Files.isRegularFile(entry)) {
                        futures.add(fileReport(entry, maxFS, nb));
                    }
                }
                return mergeAll(futures, nb);
            });
    }

    private CompletableFuture<FSReport> fileReport(Path file, long maxFS, int nb) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return FSReport.single(Files.size(file), maxFS, nb);
            } catch (IOException e) {
                return FSReport.empty(nb);
            }
        }, ioExecutor);
    }

    private CompletableFuture<FSReport> mergeAll(List<CompletableFuture<FSReport>> futures, int nb) {
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(done -> futures.stream()
                .map(CompletableFuture::join)
                .reduce(FSReport.empty(nb), FSReport::merge));
    }

    private List<Path> list(Path dir) {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    @Override
    public void close() {
        if (ioExecutor instanceof ExecutorService service) {
            service.shutdownNow();
        }
    }
}
