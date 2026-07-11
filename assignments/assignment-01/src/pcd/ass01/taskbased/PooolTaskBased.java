package pcd.ass01.taskbased;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import pcd.ass01.common.BoardConfig;
import pcd.ass01.common.CommandBuffer;
import pcd.ass01.multithreaded.BotThread;
import pcd.ass01.multithreaded.GameLoopThread;
import pcd.ass01.view.GameView;
import pcd.ass01.view.ViewModel;

/** Entrypoint for the task-based version of Poool. */
public final class PooolTaskBased {

    private PooolTaskBased() {
    }

    /**
     * Creates the task-based board, the worker pool and the same runtime threads
     * used by the monitor-based version.
     *
     * @param args optional configuration name: {@code minimal}, {@code large} or {@code massive}
     */
    public static void main(String[] args) {
        BoardConfig config = parseConfig(args);
        int cores = Math.max(1, Runtime.getRuntime().availableProcessors());
        int chunkSize = Math.max(32, config.smallBalls().size() / (cores * 4));
        ExecutorService executor = Executors.newFixedThreadPool(cores);
        Runtime.getRuntime().addShutdownHook(new Thread(executor::shutdownNow, "poool-task-executor-shutdown"));

        TaskBasedBoard board = new TaskBasedBoard(config, executor, chunkSize);
        CommandBuffer commands = new CommandBuffer(256);
        ViewModel viewModel = new ViewModel();
        viewModel.update(board.snapshot(0));
        GameView view = new GameView(viewModel, commands, 1200, 800, "Poool - Task-based");

        GameLoopThread gameLoop = new GameLoopThread(board, commands, viewModel, view, 60);
        BotThread bot = new BotThread(board, commands);
        gameLoop.start();
        bot.start();
    }

    private static BoardConfig parseConfig(String[] args) {
        if (args.length == 0) {
            return BoardConfig.large();
        }
        return switch (args[0].toLowerCase()) {
            case "minimal" -> BoardConfig.minimal();
            case "massive" -> BoardConfig.massive();
            default -> BoardConfig.large();
        };
    }
}
