package pcd.ass01.common;

public record Vec2(double x, double y) {

    public Vec2 sum(Vec2 other) {
        return new Vec2(x + other.x, y + other.y);
    }

    public Vec2 sub(Vec2 other) {
        return new Vec2(x - other.x, y - other.y);
    }

    public Vec2 mul(double factor) {
        return new Vec2(x * factor, y * factor);
    }

    public double abs() {
        return Math.sqrt(x * x + y * y);
    }

    public Vec2 swapX() {
        return new Vec2(-x, y);
    }

    public Vec2 swapY() {
        return new Vec2(x, -y);
    }
}
