package pcd.ass01.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Monitor-based implementation of the mutable game model.
 *
 * <p>All public methods are synchronized on this object. This makes the board
 * the single monitor that protects balls, scores and game status. The game loop
 * is the main writer; the bot only performs synchronized reads, and the view
 * receives immutable snapshots instead of reading this mutable state directly.
 */
public final class Board implements GameModel {

    private final List<Ball> smallBalls;
    private final Ball humanBall;
    private final Ball botBall;
    private final Bounds bounds;
    private final List<Hole> holes;
    private int humanScore;
    private int botScore;
    private GameStatus status;

    /** Creates a board from an immutable configuration. */
    public Board(BoardConfig config) {
        this.smallBalls = new ArrayList<>(config.smallBalls());
        this.humanBall = config.humanBall();
        this.botBall = config.botBall();
        this.bounds = config.bounds();
        this.holes = List.copyOf(config.holes());
        this.status = GameStatus.RUNNING;
    }

    /**
     * Runs one complete physical step while holding the board monitor.
     *
     * <p>The whole step is atomic with respect to other threads: no bot read,
     * command execution or snapshot can observe a partially updated board.
     */
    @Override
    public synchronized void updateState(long dt) {
        if (status != GameStatus.RUNNING) {
            return;
        }

        humanBall.updateState(dt, bounds);
        botBall.updateState(dt, bounds);
        for (Ball ball : smallBalls) {
            ball.updateState(dt, bounds);
        }

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

    /**
     * Synchronized read used by the bot thread to decide whether it can shoot.
     */
    @Override
    public synchronized boolean isBotStopped() {
        return botBall.isStopped();
    }

    /** Synchronized read of the game status. */
    @Override
    public synchronized GameStatus status() {
        return status;
    }

    /**
     * Builds an immutable snapshot while holding the board monitor.
     *
     * <p>After this method returns, the Swing EDT can render the snapshot
     * without touching the mutable board.
     */
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

    /** Removes small balls in holes and assigns points based on last direct player touch. */
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
