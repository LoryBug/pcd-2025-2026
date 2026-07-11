package pcd.ass01.multithreaded;

import java.util.Random;
import pcd.ass01.common.CommandBuffer;
import pcd.ass01.common.GameModel;
import pcd.ass01.common.GameStatus;
import pcd.ass01.common.KickBotCommand;
import pcd.ass01.common.Vec2;

/**
 * Autonomous producer of bot commands.
 *
 * <p>The bot never mutates the model directly. It reads synchronized model state
 * to decide whether it can shoot, then enqueues a {@code KickBotCommand} in the
 * shared command buffer.
 */
public final class BotThread extends Thread {

    private final GameModel model;
    private final CommandBuffer commands;
    private final Random random;
    private volatile boolean stopped;

    /**
     * @param model shared game model used for synchronized reads
     * @param commands command buffer used to publish bot decisions
     */
    public BotThread(GameModel model, CommandBuffer commands) {
        super("poool-bot");
        this.model = model;
        this.commands = commands;
        this.random = new Random(11);
    }

    /** Requests the bot loop to stop. */
    public void shutdown() {
        stopped = true;
        interrupt();
    }

    /** Periodically checks the bot ball and enqueues a command when it is stopped. */
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
