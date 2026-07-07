package pcd.ass01.common;

public final class Ball {

    private static final double FRICTION_FACTOR = 0.25;
    private static final double RESTITUTION_FACTOR = 1.0;

    private Vec2 pos;
    private Vec2 vel;
    private final double radius;
    private final double mass;
    private Player lastDirectTouch;

    public Ball(Vec2 pos, double radius, double mass, Vec2 vel, Player lastDirectTouch) {
        this.pos = pos;
        this.radius = radius;
        this.mass = mass;
        this.vel = vel;
        this.lastDirectTouch = lastDirectTouch;
    }

    public void updateState(long dt, Bounds bounds) {
        double speed = vel.abs();
        double dtScaled = dt * 0.001;
        if (speed > 0.001) {
            double dec = FRICTION_FACTOR * dtScaled;
            double factor = Math.max(0, speed - dec) / speed;
            vel = vel.mul(factor);
        } else {
            vel = new Vec2(0, 0);
        }
        pos = pos.sum(vel.mul(dtScaled));
        applyBoundaryConstraints(bounds);
    }

    public void addImpulse(Vec2 impulse) {
        vel = vel.sum(impulse);
    }

    public boolean isStopped() {
        return vel.abs() < 0.05;
    }

    public BallSnapshot snapshot() {
        return new BallSnapshot(pos, radius, lastDirectTouch);
    }

    public Vec2 pos() {
        return pos;
    }

    public double radius() {
        return radius;
    }

    public Player lastDirectTouch() {
        return lastDirectTouch;
    }

    public void setLastDirectTouch(Player lastDirectTouch) {
        this.lastDirectTouch = lastDirectTouch;
    }

    private void applyBoundaryConstraints(Bounds bounds) {
        if (pos.x() + radius > bounds.x1()) {
            pos = new Vec2(bounds.x1() - radius, pos.y());
            vel = vel.swapX();
        } else if (pos.x() - radius < bounds.x0()) {
            pos = new Vec2(bounds.x0() + radius, pos.y());
            vel = vel.swapX();
        } else if (pos.y() + radius > bounds.y1()) {
            pos = new Vec2(pos.x(), bounds.y1() - radius);
            vel = vel.swapY();
        } else if (pos.y() - radius < bounds.y0()) {
            pos = new Vec2(pos.x(), bounds.y0() + radius);
            vel = vel.swapY();
        }
    }

    public static boolean resolveCollision(Ball a, Ball b) {
        double dx = b.pos.x() - a.pos.x();
        double dy = b.pos.y() - a.pos.y();
        double dist = Math.hypot(dx, dy);
        double minD = a.radius + b.radius;

        if (dist >= minD || dist <= 1e-6) {
            return false;
        }

        double nx = dx / dist;
        double ny = dy / dist;
        double overlap = minD - dist;
        double totalM = a.mass + b.mass;

        double aFactor = overlap * (b.mass / totalM);
        a.pos = new Vec2(a.pos.x() - nx * aFactor, a.pos.y() - ny * aFactor);

        double bFactor = overlap * (a.mass / totalM);
        b.pos = new Vec2(b.pos.x() + nx * bFactor, b.pos.y() + ny * bFactor);

        double dvx = b.vel.x() - a.vel.x();
        double dvy = b.vel.y() - a.vel.y();
        double dvn = dvx * nx + dvy * ny;

        if (dvn <= 0) {
            double impulse = -(1 + RESTITUTION_FACTOR) * dvn / (1.0 / a.mass + 1.0 / b.mass);
            a.vel = new Vec2(a.vel.x() - (impulse / a.mass) * nx, a.vel.y() - (impulse / a.mass) * ny);
            b.vel = new Vec2(b.vel.x() + (impulse / b.mass) * nx, b.vel.y() + (impulse / b.mass) * ny);
        }

        return true;
    }
}
