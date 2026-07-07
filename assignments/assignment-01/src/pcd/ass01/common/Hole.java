package pcd.ass01.common;

public record Hole(Vec2 center, double radius) {

    public boolean contains(Vec2 point) {
        return point.sub(center).abs() <= radius;
    }
}
