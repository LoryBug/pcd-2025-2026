package pcd.ass01.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public record BoardConfig(Bounds bounds, List<Ball> smallBalls, Ball humanBall, Ball botBall, List<Hole> holes) {

    public static BoardConfig large() {
        return create(400);
    }

    public static BoardConfig minimal() {
        return create(20);
    }

    public static BoardConfig massive() {
        return create(4500);
    }

    private static BoardConfig create(int nSmallBalls) {
        Bounds bounds = new Bounds(-1, -1, 1, 1);
        double holeRadius = 0.08;
        List<Hole> holes = List.of(
            new Hole(new Vec2(bounds.x0() + holeRadius, bounds.y1() - holeRadius), holeRadius),
            new Hole(new Vec2(bounds.x1() - holeRadius, bounds.y1() - holeRadius), holeRadius)
        );

        List<Ball> smallBalls = new ArrayList<>();
        Random rand = new Random(7);
        int columns = (int) Math.ceil(Math.sqrt(nSmallBalls));
        double spacing = 1.65 / columns;
        double radius = Math.min(0.012, spacing * 0.35);
        for (int i = 0; i < nSmallBalls; i++) {
            int row = i / columns;
            int col = i % columns;
            double x = -0.82 + col * spacing + rand.nextDouble() * spacing * 0.2;
            double y = -0.35 + row * spacing + rand.nextDouble() * spacing * 0.2;
            if (y > 0.75) {
                y = -0.35 + rand.nextDouble() * 1.0;
            }
            smallBalls.add(new Ball(new Vec2(x, y), radius, 1.0, new Vec2(0, 0), Player.NONE));
        }

        Ball humanBall = new Ball(new Vec2(-0.35, -0.75), 0.035, 8.0, new Vec2(0, 0), Player.HUMAN);
        Ball botBall = new Ball(new Vec2(0.35, -0.75), 0.035, 8.0, new Vec2(0, 0), Player.BOT);
        return new BoardConfig(bounds, smallBalls, humanBall, botBall, holes);
    }
}
