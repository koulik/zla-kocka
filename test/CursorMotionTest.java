import java.awt.Point;
import java.awt.Rectangle;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class CursorMotionTest {
    private static final long SECOND = Duration.ofSeconds(1).toNanos();
    private static final long STEP = Duration.ofMillis(18).toNanos();
    private static final Rectangle SCREEN = new Rectangle(-1600, -80, 1600, 900);

    private static final class FakePointer implements CursorMotion.Pointer {
        Point location;
        final int radius;
        final List<Point> moves = new ArrayList<>();

        FakePointer(Point location) {
            this(location, CursorMotion.RADIUS);
        }

        FakePointer(Point location, int radius) {
            this.location = location;
            this.radius = radius;
        }

        @Override public Point position() { return location; }
        @Override public Rectangle boundsAt(Point point) { return SCREEN; }
        @Override public int radiusInCoordinates() { return radius; }

        @Override public boolean move(Point point) {
            require(SCREEN.contains(point), "Spirála opustila monitor.");
            location = point;
            moves.add(point);
            return true;
        }
    }

    public static void main(String[] args) {
        spiralAfterIdle(new Point(-800, 300), CursorMotion.RADIUS);
        spiralAfterIdle(new Point(-1600, -80), CursorMotion.RADIUS);
        spiralAfterIdle(new Point(-1, 819), CursorMotion.RADIUS);
        spiralAfterIdle(new Point(-1600, -80), 50);
        userInterruptsSpiral();
        timerStopsMidSpiral();
        timerPreservesUserPosition();
        unavailablePointer();
        jitterKeepsProgressAndReturnPosition();
        smallStepsAccumulate();
        System.out.println("10 scénářů prošlo.");
    }

    private static void spiralAfterIdle(Point anchor, int radius) {
        FakePointer pointer = new FakePointer(new Point(-20, 0), radius);
        CursorMotion motion = new CursorMotion(pointer);
        motion.start(0, Duration.ZERO);
        require(motion.secondsLeft(0) == -1, "Neomezený režim dostal odpočet.");
        long lastUserMovement = 58 * SECOND;
        pointer.location = anchor;
        motion.tick(lastUserMovement);
        long first = lastUserMovement + CursorMotion.IDLE;
        motion.tick(first - 1);
        require(pointer.moves.isEmpty(), "Pohyb začal před uplynutím 59 sekund.");
        require(!motion.spiraling(), "Spirála je hlášena během čekání.");
        motion.tick(first);
        require(motion.spiraling(), "Začátek spirály nebyl ohlášen.");
        long end = first + CursorMotion.SPIRAL;
        for (long now = first + STEP; now < end; now += STEP) {
            motion.tick(now);
        }
        motion.tick(end);
        require(!motion.spiraling(), "Spirála je stále hlášena po návratu kurzoru.");
        require(pointer.moves.size() > 40, "Spirála nemá dostatek kroků.");
        for (Point point : pointer.moves) {
            require(point.distance(anchor) <= radius, "Spirála překročila poloměr.");
        }
        require(pointer.location.equals(anchor), "Kurzor se nevrátil na původní souřadnice.");
        int count = pointer.moves.size();
        motion.tick(end + CursorMotion.IDLE - 1);
        require(pointer.moves.size() == count, "Spirála se opakovala příliš brzy.");
        motion.tick(end + CursorMotion.IDLE);
        motion.tick(end + CursorMotion.IDLE + STEP);
        require(pointer.moves.size() > count, "V neomezeném režimu se spirála neopakovala.");
        motion.stop();
        require(pointer.location.equals(anchor), "Ruční vypnutí nedokončilo návrat.");
    }

    private static void userInterruptsSpiral() {
        FakePointer pointer = new FakePointer(new Point(-800, 300));
        CursorMotion motion = new CursorMotion(pointer);
        motion.start(0, Duration.ZERO);
        motion.tick(CursorMotion.IDLE);
        motion.tick(CursorMotion.IDLE + STEP);
        require(!pointer.moves.isEmpty(), "Spirála se nespustila.");
        Point userPosition = new Point(pointer.location.x + 1, pointer.location.y);
        pointer.location = userPosition;
        long interrupted = CursorMotion.IDLE + 2 * STEP;
        motion.tick(interrupted);
        require(!motion.spiraling(), "Spirála je stále hlášena po zásahu uživatele.");
        int count = pointer.moves.size();
        motion.tick(interrupted + CursorMotion.IDLE - 1);
        require(pointer.moves.size() == count && pointer.location.equals(userPosition),
                "Aplikace po zásahu uživatele pokračovala v pohybu.");
    }

    private static void timerStopsMidSpiral() {
        Point anchor = new Point(-800, 300);
        FakePointer pointer = new FakePointer(anchor);
        CursorMotion motion = new CursorMotion(pointer);
        motion.start(0, Duration.ofSeconds(60));
        require(motion.secondsLeft(0) == 60, "Odpočet má chybnou počáteční hodnotu.");
        motion.tick(CursorMotion.IDLE);
        motion.tick(CursorMotion.IDLE + STEP);
        require(!pointer.location.equals(anchor), "Spirála se nespustila.");
        motion.tick(60 * SECOND);
        require(!motion.spiraling(), "Spirála je stále hlášena po vypršení odpočtu.");
        require(!motion.active() && pointer.location.equals(anchor), "Odpočet nezastavil pohyb s návratem.");
        int count = pointer.moves.size();
        motion.tick(200 * SECOND);
        require(pointer.moves.size() == count, "Pohyb pokračoval po automatickém vypnutí.");
    }

    private static void timerPreservesUserPosition() {
        FakePointer pointer = new FakePointer(new Point(-800, 300));
        CursorMotion motion = new CursorMotion(pointer);
        motion.start(0, Duration.ofSeconds(60));
        motion.tick(CursorMotion.IDLE);
        motion.tick(CursorMotion.IDLE + STEP);
        Point userPosition = new Point(-210, 20);
        pointer.location = userPosition;
        motion.tick(60 * SECOND);
        require(!motion.active() && pointer.location.equals(userPosition), "Vypnutí přemístilo kurzor uživatele.");
    }

    private static void unavailablePointer() {
        FakePointer pointer = new FakePointer(null);
        CursorMotion motion = new CursorMotion(pointer);
        motion.start(0, Duration.ZERO);
        motion.tick(70 * SECOND);
        pointer.location = new Point(-800, 300);
        motion.tick(71 * SECOND);
        motion.tick(100 * SECOND);
        require(pointer.moves.isEmpty(), "Pohyb začal hned po obnovení přístupu ke kurzoru.");
    }

    private static void jitterKeepsProgressAndReturnPosition() {
        Point start = new Point(-800, 300);
        FakePointer pointer = new FakePointer(start);
        CursorMotion motion = new CursorMotion(pointer);
        motion.start(0, Duration.ZERO);
        long halfway = CursorMotion.IDLE / 2;
        pointer.location = new Point(start.x + 4, start.y - 4);
        motion.tick(halfway);
        require(motion.waitingProgress(halfway) == 0.5, "Pohyb v toleranci vynuloval průběh čekání.");
        Point beforeSpiral = new Point(start.x - 4, start.y + 4);
        pointer.location = beforeSpiral;
        motion.tick(CursorMotion.IDLE);
        require(motion.spiraling(), "Pohyb v toleranci odložil spirálu.");
        motion.tick(CursorMotion.IDLE + STEP);
        require(!pointer.moves.isEmpty(), "Spirála se po drobném pohybu nevykonala.");
        long end = CursorMotion.IDLE + CursorMotion.SPIRAL;
        motion.tick(end);
        require(pointer.location.equals(beforeSpiral), "Kurzor se nevrátil na místo před spirálou.");
        require(motion.waitingProgress(end) == 0.0, "Čekání se po spirále neobnovilo.");
    }

    private static void smallStepsAccumulate() {
        Point start = new Point(-800, 300);
        FakePointer pointer = new FakePointer(start);
        CursorMotion motion = new CursorMotion(pointer);
        motion.start(0, Duration.ZERO);
        for (int pixels = 1; pixels <= 5; pixels++) {
            pointer.location = new Point(start.x, start.y + pixels);
            motion.tick(pixels * 10 * SECOND);
        }
        long lastMovement = 50 * SECOND;
        require(motion.waitingProgress(lastMovement) == 0.0, "Pomalý pohyb o 5 px nevynuloval čekání.");
        motion.tick(CursorMotion.IDLE);
        require(!motion.spiraling(), "Spirála začala v původním termínu.");
        motion.tick(lastMovement + CursorMotion.IDLE);
        require(motion.spiraling(), "Spirála nezačala po resetovaném čekání.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
