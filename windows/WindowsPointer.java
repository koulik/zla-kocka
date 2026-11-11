import java.awt.Point;
import java.awt.Rectangle;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

final class WindowsPointer implements CursorMotion.Pointer {
    private static final MemorySegment PER_MONITOR_AWARE_V2 = MemorySegment.ofAddress(-4);
    private final Arena arena = Arena.ofAuto();
    private final SymbolLookup user32 = SymbolLookup.libraryLookup("user32.dll", arena);
    private final MethodHandle getPosition = bind("GetPhysicalCursorPos", FunctionDescriptor.of(JAVA_INT, ADDRESS));
    private final MethodHandle setPosition = bind("SetPhysicalCursorPos", FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT));
    private final MethodHandle monitorFromRect = bind("MonitorFromRect", FunctionDescriptor.of(ADDRESS, ADDRESS, JAVA_INT));
    private final MethodHandle getMonitorInfo = bind("GetMonitorInfoW", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS));
    private final MethodHandle getClipCursor = bind("GetClipCursor", FunctionDescriptor.of(JAVA_INT, ADDRESS));
    private final MethodHandle setDpiContext = bind("SetThreadDpiAwarenessContext",
            FunctionDescriptor.of(ADDRESS, ADDRESS));
    private final MemorySegment point = arena.allocate(8, 4);
    private final MemorySegment rect = arena.allocate(16, 4);
    private final MemorySegment monitorInfo = arena.allocate(40, 4);

    private MethodHandle bind(String name, FunctionDescriptor descriptor) {
        return Linker.nativeLinker().downcallHandle(user32.findOrThrow(name), descriptor);
    }

    @Override
    public Point position() {
        try {
            if ((int) getPosition.invokeExact(point) == 0) {
                return null;
            }
            return new Point(point.get(JAVA_INT, 0), point.get(JAVA_INT, 4));
        } catch (Throwable error) {
            throw new IllegalStateException(error);
        }
    }

    @Override
    public Rectangle boundsAt(Point position) {
        try {
            MemorySegment previous = (MemorySegment) setDpiContext.invokeExact(PER_MONITOR_AWARE_V2);
            if (previous.equals(MemorySegment.NULL)) {
                return null;
            }
            try {
                return readBounds(position);
            } finally {
                MemorySegment ignored = (MemorySegment) setDpiContext.invokeExact(previous);
            }
        } catch (Throwable error) {
            throw new IllegalStateException(error);
        }
    }

    private Rectangle readBounds(Point position) throws Throwable {
        rect.set(JAVA_INT, 0, position.x);
        rect.set(JAVA_INT, 4, position.y);
        rect.set(JAVA_INT, 8, position.x + 1);
        rect.set(JAVA_INT, 12, position.y + 1);
        MemorySegment monitor = (MemorySegment) monitorFromRect.invokeExact(rect, 0);
        monitorInfo.set(JAVA_INT, 0, (int) monitorInfo.byteSize());
        if (monitor.equals(MemorySegment.NULL)
                || (int) getMonitorInfo.invokeExact(monitor, monitorInfo) == 0) {
            return null;
        }
        Rectangle bounds = readRect(monitorInfo, 4);
        if ((int) getClipCursor.invokeExact(rect) != 0) {
            bounds = bounds.intersection(readRect(rect, 0));
        }
        return bounds;
    }

    private static Rectangle readRect(MemorySegment segment, long offset) {
        int x = segment.get(JAVA_INT, offset);
        int y = segment.get(JAVA_INT, offset + 4);
        return new Rectangle(x, y, segment.get(JAVA_INT, offset + 8) - x,
                segment.get(JAVA_INT, offset + 12) - y);
    }

    @Override
    public boolean move(Point position) {
        try {
            return (int) setPosition.invokeExact(position.x, position.y) != 0;
        } catch (Throwable error) {
            throw new IllegalStateException(error);
        }
    }

}
