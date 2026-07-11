package pcd.ass01.taskbased;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import pcd.ass01.common.Ball;
import pcd.ass01.common.BallSnapshot;
import pcd.ass01.common.BoardConfig;
import pcd.ass01.common.Bounds;
import pcd.ass01.common.GameModel;
import pcd.ass01.common.GameSnapshot;
import pcd.ass01.common.GameStatus;
import pcd.ass01.common.Hole;
import pcd.ass01.common.Player;
import pcd.ass01.common.Vec2;

/**
 * Task-based implementation of the game model.
 *
 * <p>The public API has the same monitor discipline as {@code Board}: all
 * public methods are synchronized on this object. The difference is inside
 * {@link #updateState(long)}: the independent movement of small balls is split
 * into chunks and submitted to an {@link ExecutorService}. Collision resolution
 * remains serial because {@code Ball.resolveCollision(a, b)} mutates both balls.
 */
public final class TaskBasedBoard implements GameModel {

    private final List<Ball> smallBalls;
    private final Ball humanBall;
    private final Ball botBall;
    private final Bounds bounds;
    private final List<Hole> holes;
    private final ExecutorService executor;
    private final int chunkSize;
    private int humanScore;
    private int botScore;
    private GameStatus status;

    /**
     * @param config initial balls, bounds and holes
     * @param executor worker pool used for chunked small-ball updates
     * @param chunkSize maximum number of small balls handled by one task
     */
    public TaskBasedBoard(BoardConfig config, ExecutorService executor, int chunkSize) {
        this.smallBalls = new ArrayList<>(config.smallBalls());
        this.humanBall = config.humanBall();
        this.botBall = config.botBall();
        this.bounds = config.bounds();
        this.holes = List.copyOf(config.holes());
        this.executor = executor;
        this.chunkSize = Math.max(1, chunkSize);
        this.status = GameStatus.RUNNING;
    }

    /**
     * Runs one frame update while holding the board monitor.
     *
     * <p>Only the position update of small balls is data-parallel. The game loop
     * waits at a {@code Future.get()} barrier before continuing with collisions,
     * holes and scoring.
     */
    @Override
    public synchronized void updateState(long dt) {
        if (status != GameStatus.RUNNING) {
            return;
        }

        humanBall.updateState(dt, bounds);
        botBall.updateState(dt, bounds);
        updateSmallBallsInParallel(dt);
        resolveSmallBallCollisions();
        resolvePlayerCollisions(humanBall, Player.HUMAN);
        resolvePlayerCollisions(botBall, Player.BOT);
        collectBallsInHoles();
        checkPlayerBallsInHoles();
        checkEndByNoSmallBalls();
    }

    /** Adds an impulse to the human ball while holding the board monitor. */
    @Override
    public synchronized void kickHuman(Vec2 impulse) {
        if (status == GameStatus.RUNNING) {
            humanBall.addImpulse(impulse);
        }
    }

    /** Adds an impulse to the bot ball while holding the board monitor. */
    @Override
    public synchronized void kickBot(Vec2 impulse) {
        if (status == GameStatus.RUNNING) {
            botBall.addImpulse(impulse);
        }
    }

    /** Synchronized read used by the bot thread before creating a kick command. */
    @Override
    public synchronized boolean isBotStopped() {
        return botBall.isStopped();
    }

    /** Synchronized read of the current game status. */
    @Override
    public synchronized GameStatus status() {
        return status;
    }

    /** Creates an immutable snapshot for rendering. */
    @Override
    public synchronized GameSnapshot snapshot(int fps) {
        List<BallSnapshot> balls = new ArrayList<>(smallBalls.size());
        for (Ball ball : smallBalls) {
            balls.add(ball.snapshot());
        }
        return new GameSnapshot(
            List.copyOf(balls),
            humanBall.snapshot(),
            botBall.snapshot(),
            holes,
            humanScore,
            botScore,
            fps,
            status
        );
    }

    /**
     * Updates small balls in parallel by assigning disjoint index ranges to executor tasks.
     *
     * <p>The method waits on all futures before returning, so collision resolution starts only
     * after every movement task has completed.
     */
    private void updateSmallBallsInParallel(long dt) {
        List<Future<?>> futures = new ArrayList<>();
        for (int start = 0; start < smallBalls.size(); start += chunkSize) {
            int from = start;
            int to = Math.min(start + chunkSize, smallBalls.size());
            futures.add(executor.submit(() -> {
                for (int i = from; i < to; i++) {
                    smallBalls.get(i).updateState(dt, bounds);
                }
            }));
        }
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                status = GameStatus.DRAW;
                return;
            } catch (ExecutionException e) {
                throw new IllegalStateException("Task-based physics update failed", e);
            }
        }
    }

    /** Resolves all small-small collisions serially because each collision mutates both balls. */
    private void resolveSmallBallCollisions() {
        for (int i = 0; i < smallBalls.size() - 1; i++) {
            for (int j = i + 1; j < smallBalls.size(); j++) {
                Ball a = smallBalls.get(i);
                Ball b = smallBalls.get(j);
                if (Ball.resolveCollision(a, b)) {
                    a.setLastDirectTouch(Player.NONE);
                    b.setLastDirectTouch(Player.NONE);
                }
            }
        }
    }

    /** Resolves collisions between a player ball and all small balls, updating scoring ownership. */
    private void resolvePlayerCollisions(Ball playerBall, Player player) {
        for (Ball smallBall : smallBalls) {
            if (Ball.resolveCollision(playerBall, smallBall)) {
                smallBall.setLastDirectTouch(player);
            }
        }
    }

    private void collectBallsInHoles() {
        Iterator<Ball> it = smallBalls.iterator();
        while (it.hasNext()) {
            Ball ball = it.next();
            if (isInAnyHole(ball)) {
                if (ball.lastDirectTouch() == Player.HUMAN) {
                    humanScore++;
                } else if (ball.lastDirectTouch() == Player.BOT) {
                    botScore++;
                }
                it.remove();
            }
        }
    }

    private void checkPlayerBallsInHoles() {
        if (isInAnyHole(humanBall)) {
            status = GameStatus.BOT_WON;
        } else if (isInAnyHole(botBall)) {
            status = GameStatus.HUMAN_WON;
        }
    }

    private void checkEndByNoSmallBalls() {
        if (status == GameStatus.RUNNING && smallBalls.isEmpty()) {
            if (humanScore > botScore) {
                status = GameStatus.HUMAN_WON;
            } else if (botScore > humanScore) {
                status = GameStatus.BOT_WON;
            } else {
                status = GameStatus.DRAW;
            }
        }
    }

    private boolean isInAnyHole(Ball ball) {
        for (Hole hole : holes) {
            if (hole.contains(ball.pos())) {
                return true;
            }
        }
        return false;
    }
}
