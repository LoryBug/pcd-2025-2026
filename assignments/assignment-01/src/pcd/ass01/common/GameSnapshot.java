package pcd.ass01.common;

import java.util.List;

/**
 * Immutable view of the game state used by Swing rendering.
 *
 * <p>The board creates snapshots while holding its monitor. The EDT later reads
 * this object instead of reading the mutable board directly, avoiding races
 * between painting and physics updates.
 */
public record GameSnapshot(
        List<BallSnapshot> smallBalls,
        BallSnapshot humanBall,
        BallSnapshot botBall,
        List<Hole> holes,
        int humanScore,
        int botScore,
        int framePerSec,
        GameStatus status) {
}
