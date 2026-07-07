package pcd.ass01.common;

import java.util.List;

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
