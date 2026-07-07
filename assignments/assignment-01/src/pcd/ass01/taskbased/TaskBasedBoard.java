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

    @Override
    public synchronized void kickHuman(Vec2 impulse) {
        if (status == GameStatus.RUNNING) {
            humanBall.addImpulse(impulse);
        }
    }

    @Override
    public synchronized void kickBot(Vec2 impulse) {
        if (status == GameStatus.RUNNING) {
            botBall.addImpulse(impulse);
        }
    }

    @Override
    public synchronized boolean isBotStopped() {
        return botBall.isStopped();
    }

    @Override
    public synchronized GameStatus status() {
        return status;
    }

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
