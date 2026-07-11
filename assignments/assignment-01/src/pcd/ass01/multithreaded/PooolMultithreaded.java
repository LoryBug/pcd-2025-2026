package pcd.ass01.multithreaded;

import pcd.ass01.common.Board;
import pcd.ass01.common.BoardConfig;
import pcd.ass01.common.CommandBuffer;
import pcd.ass01.view.GameView;
import pcd.ass01.view.ViewModel;

/** Entrypoint for the monitor-based multithreaded version of Poool. */
public final class PooolMultithreaded {

    private PooolMultithreaded() {
    }

    /**
     * Creates the shared model, command buffer, Swing view and active threads.
     *
     * @param args optional configuration name: {@code minimal}, {@code large} or {@code massive}
     */
    public static void main(String[] args) {
        BoardConfig config = parseConfig(args);
        Board board = new Board(config);
        CommandBuffer commands = new CommandBuffer(256);
        ViewModel viewModel = new ViewModel();
        viewModel.update(board.snapshot(0));
        GameView view = new GameView(viewModel, commands, 1200, 800);

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
