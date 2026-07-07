package pcd.ass01.multithreaded;

import java.util.Random;
import pcd.ass01.common.CommandBuffer;
import pcd.ass01.common.GameModel;
import pcd.ass01.common.GameStatus;
import pcd.ass01.common.KickBotCommand;
import pcd.ass01.common.Vec2;

public final class BotThread extends Thread {

    private final GameModel model;
    private final CommandBuffer commands;
    private final Random random;
    private volatile boolean stopped;

    public BotThread(GameModel model, CommandBuffer commands) {
        super("poool-bot");
        this.model = model;
        this.commands = commands;
        this.random = new Random(11);
    }

    public void shutdown() {
        stopped = true;
        interrupt();
    }

    @Override
    public void run() {
        while (!stopped && model.status() == GameStatus.RUNNING) {
            if (model.isBotStopped()) {
                double angle = Math.PI * 0.15 + random.nextDouble() * Math.PI * 0.7;
                double speed = 0.7 + random.nextDouble() * 0.4;
                commands.offer(new KickBotCommand(new Vec2(Math.cos(angle) * speed, Math.sin(angle) * speed)));
            }
            try {
                Thread.sleep(1800);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                stopped = true;
            }
        }
    }
}
