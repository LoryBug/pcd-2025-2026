package pcd.ass01.multithreaded;

import pcd.ass01.common.CommandBuffer;
import pcd.ass01.common.GameCommand;
import pcd.ass01.common.GameModel;
import pcd.ass01.common.GameStatus;
import pcd.ass01.view.GameView;
import pcd.ass01.view.ViewModel;

public final class GameLoopThread extends Thread {

    private final GameModel model;
    private final CommandBuffer commands;
    private final ViewModel viewModel;
    private final GameView view;
    private final int targetFps;
    private volatile boolean stopped;

    public GameLoopThread(GameModel model, CommandBuffer commands, ViewModel viewModel, GameView view, int targetFps) {
        super("poool-game-loop");
        this.model = model;
        this.commands = commands;
        this.viewModel = viewModel;
        this.view = view;
        this.targetFps = targetFps;
    }

    public void shutdown() {
        stopped = true;
        interrupt();
    }

    @Override
    public void run() {
        long lastUpdateTime = System.currentTimeMillis();
        long fpsStartTime = lastUpdateTime;
        int frameCount = 0;
        int fps = 0;
        long framePeriod = 1000 / targetFps;

        while (!stopped) {
            long frameStart = System.currentTimeMillis();

            for (GameCommand command : commands.drainToList()) {
                command.execute(model);
            }

            long now = System.currentTimeMillis();
            long elapsed = now - lastUpdateTime;
            lastUpdateTime = now;
            model.updateState(elapsed);

            frameCount++;
            long fpsElapsed = now - fpsStartTime;
            if (fpsElapsed >= 1000) {
                fps = (int) (frameCount * 1000 / fpsElapsed);
                frameCount = 0;
                fpsStartTime = now;
            }

            viewModel.update(model.snapshot(fps));
            try {
                view.render();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                stopped = true;
            }

            if (model.status() != GameStatus.RUNNING) {
                sleepQuietly(2500);
                stopped = true;
            }

            long frameTime = System.currentTimeMillis() - frameStart;
            long sleepTime = framePeriod - frameTime;
            if (sleepTime > 0) {
                sleepQuietly(sleepTime);
            }
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            stopped = true;
        }
    }
}
