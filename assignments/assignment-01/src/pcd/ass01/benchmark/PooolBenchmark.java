package pcd.ass01.benchmark;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import pcd.ass01.common.Board;
import pcd.ass01.common.BoardConfig;
import pcd.ass01.common.GameModel;
import pcd.ass01.common.Vec2;
import pcd.ass01.taskbased.TaskBasedBoard;

public final class PooolBenchmark {

    private static final long DT = 16;

    private PooolBenchmark() {
    }

    public static void main(String[] args) {
        run("minimal", 500);
        run("large", 200);
        run("massive", 20);
    }

    private static void run(String configName, int frames) {
        Result sequential = benchmarkSequential(configName, frames);
        Result taskBased = benchmarkTaskBased(configName, frames);
        double speedup = (double) sequential.totalMs() / taskBased.totalMs();
        System.out.printf(
            "%s,%d,%d,%d,%.2f,%.2f,%.2f%n",
            configName,
            frames,
            sequential.totalMs(),
            taskBased.totalMs(),
            sequential.avgFrameMs(),
            taskBased.avgFrameMs(),
            speedup
        );
    }

    private static Result benchmarkSequential(String configName, int frames) {
        Board board = new Board(config(configName));
        return benchmark(board, frames);
    }

    private static Result benchmarkTaskBased(String configName, int frames) {
        BoardConfig config = config(configName);
        int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
        int chunkSize = Math.max(32, config.smallBalls().size() / (cores * 4));
        ExecutorService executor = Executors.newFixedThreadPool(cores);
        try {
            return benchmark(new TaskBasedBoard(config, executor, chunkSize), frames);
        } finally {
            executor.shutdownNow();
        }
    }

    private static Result benchmark(GameModel model, int frames) {
        model.kickHuman(new Vec2(0.9, 1.1));
        model.kickBot(new Vec2(-0.8, 1.0));
        for (int i = 0; i < 20; i++) {
            model.updateState(DT);
        }

        long start = System.nanoTime();
        for (int i = 0; i < frames; i++) {
            model.updateState(DT);
        }
        long totalMs = Math.max(1, (System.nanoTime() - start) / 1_000_000);
        return new Result(totalMs, totalMs / (double) frames);
    }

    private static BoardConfig config(String name) {
        return switch (name) {
            case "minimal" -> BoardConfig.minimal();
            case "massive" -> BoardConfig.massive();
            default -> BoardConfig.large();
        };
    }

    private record Result(long totalMs, double avgFrameMs) {
    }
}
