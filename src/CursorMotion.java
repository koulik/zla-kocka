import java.awt.Point;
import java.awt.Rectangle;
import java.time.Duration;

final class CursorMotion {
    interface Pointer {
        Point position();
        Rectangle boundsAt(Point point);
        boolean move(Point point);
    }

    static final long IDLE = Duration.ofSeconds(59).toNanos();
    static final long SPIRAL = Duration.ofMillis(1_350).toNanos();
    static final int RADIUS = 100;

    private final Pointer pointer;
    private Point expected;
    private long lastMovement;
    private long started;
    private long duration;
    private long spiralStarted;
    private Point origin;
    private Point center;
    private double radius;
    private boolean active;

    CursorMotion(Pointer pointer) {
        this.pointer = pointer;
    }

    void start(long now, Duration limit) {
        expected = pointer.position();
        started = lastMovement = now;
        duration = limit.toNanos();
        active = true;
    }

    void stop() {
        if (origin != null && expected.equals(pointer.position())) {
            pointer.move(origin);
        }
        origin = null;
        active = false;
    }

    boolean active() {
        return active;
    }

    long secondsLeft(long now) {
        if (duration == 0) {
            return -1;
        }
        long remaining = Math.max(0, duration - (now - started));
        return (remaining + 999_999_999) / 1_000_000_000;
    }

    void tick(long now) {
        if (!active) {
            return;
        }
        if (duration != 0 && now - started >= duration) {
            stop();
            return;
        }

        Point current = pointer.position();
        if (current == null || !current.equals(expected)) {
            // A person moved the mouse, or Windows is temporarily on another desktop.
            origin = null;
            expected = current;
            lastMovement = now;
            return;
        }

        if (origin == null && now - lastMovement >= IDLE) {
            lastMovement = now;
            Rectangle bounds = pointer.boundsAt(current);
            if (bounds != null && bounds.contains(current)) {
                beginSpiral(now, current, bounds);
            }
        }
        if (origin != null) {
            advanceSpiral(now);
        }
    }

    private void beginSpiral(long now, Point current, Rectangle bounds) {
        // Shift the spiral inwards near a screen edge. Each frame stays within
        // the monitor and within the requested distance of the original point.
        int inset = Math.min(RADIUS, Math.min(bounds.width - 1, bounds.height - 1) / 2);
        if (inset == 0) {
            return;
        }
        int x = Math.clamp(current.x, bounds.x + inset, bounds.x + bounds.width - 1 - inset);
        int y = Math.clamp(current.y, bounds.y + inset, bounds.y + bounds.height - 1 - inset);
        double scale = Math.min(1.0, (RADIUS - 1) / (current.distance(x, y) + inset));
        center = new Point(current.x + (int) Math.round((x - current.x) * scale),
                current.y + (int) Math.round((y - current.y) * scale));
        radius = Math.floor(inset * scale);
        origin = current;
        spiralStarted = now;
    }

    private void advanceSpiral(long now) {
        double progress = Math.min(1.0, (now - spiralStarted) / (double) SPIRAL);
        double envelope = Math.sin(Math.PI * progress);
        double angle = 5 * Math.PI * progress;
        Point target = progress == 1.0 ? origin : new Point(
                origin.x + (int) Math.round(envelope * (center.x - origin.x + radius * Math.cos(angle))),
                origin.y + (int) Math.round(envelope * (center.y - origin.y + radius * Math.sin(angle))));
        if (!target.equals(expected)) {
            Point current = pointer.position();
            if (!expected.equals(current)) {
                expected = current;
                origin = null;
                lastMovement = now;
                return;
            }
            if (!pointer.move(target)) {
                if (current.equals(pointer.position())) {
                    pointer.move(origin);
                }
                expected = pointer.position();
                origin = null;
                lastMovement = now;
                return;
            }
            expected = target;
        }
        if (progress == 1.0) {
            origin = null;
            lastMovement = now;
        }
    }
}
