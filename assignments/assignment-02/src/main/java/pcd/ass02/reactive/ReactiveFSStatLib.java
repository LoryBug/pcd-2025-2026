package pcd.ass02.reactive;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import pcd.ass02.common.FSReport;

public final class ReactiveFSStatLib implements AutoCloseable {

    private final ScheduledExecutorService ioExecutor;
    private final Scheduler ioScheduler;

    public ReactiveFSStatLib() {
        this(Executors.newScheduledThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors())));
    }

    public ReactiveFSStatLib(ScheduledExecutorService ioExecutor) {
        this.ioExecutor = ioExecutor;
        this.ioScheduler = new ExecutorServiceScheduler(ioExecutor);
    }

    public Single<FSReport> getFSReport(Path root, long maxFS, int nb) {
        FSReport.validate(maxFS, nb);
        return walk(root)
            .filter(Files::isRegularFile)
            .flatMapSingle(file -> Single.fromCallable(() -> FSReport.single(Files.size(file), maxFS, nb))
                .onErrorReturnItem(FSReport.empty(nb))
                .subscribeOn(ioScheduler))
            .reduce(FSReport.empty(nb), FSReport::merge);
    }

    private Flowable<Path> walk(Path dir) {
        return Flowable.defer(() -> Flowable.fromIterable(list(dir)))
            .subscribeOn(ioScheduler)
            .flatMap(path -> {
                if (Files.isDirectory(path)) {
                    return walk(path);
                }
                return Flowable.just(path);
            });
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
        ioExecutor.shutdownNow();
    }

    private static final class ExecutorServiceScheduler extends Scheduler {

        private final ScheduledExecutorService executor;

        private ExecutorServiceScheduler(ScheduledExecutorService executor) {
            this.executor = executor;
        }

        @Override
        public Worker createWorker() {
            return new ExecutorWorker(executor);
        }
    }

    private static final class ExecutorWorker extends Scheduler.Worker {

        private final ScheduledExecutorService executor;
        private final AtomicBoolean disposed = new AtomicBoolean();

        private ExecutorWorker(ScheduledExecutorService executor) {
            this.executor = executor;
        }

        @Override
        public Disposable schedule(Runnable run, long delay, TimeUnit unit) {
            if (disposed.get()) {
                return Disposed.INSTANCE;
            }
            Future<?> future = executor.schedule(() -> {
                if (!disposed.get()) {
                    run.run();
                }
            }, delay, unit);
            return new FutureDisposable(future);
        }

        @Override
        public void dispose() {
            disposed.set(true);
        }

        @Override
        public boolean isDisposed() {
            return disposed.get();
        }
    }

    private static final class FutureDisposable implements Disposable {

        private final Future<?> future;

        private FutureDisposable(Future<?> future) {
            this.future = future;
        }

        @Override
        public void dispose() {
            future.cancel(true);
        }

        @Override
        public boolean isDisposed() {
            return future.isCancelled() || future.isDone();
        }
    }

    private enum Disposed implements Disposable {
        INSTANCE;

        @Override
        public void dispose() {
        }

        @Override
        public boolean isDisposed() {
            return true;
        }
    }
}
