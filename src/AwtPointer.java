import java.awt.AWTException;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.geom.AffineTransform;

final class AwtPointer implements CursorMotion.Pointer {
    private GraphicsConfiguration configuration;
    private Robot robot;

    AwtPointer() throws AWTException {
        configuration = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();
        robot = new Robot(configuration.getDevice());
    }

    @Override
    public Point position() {
        PointerInfo info = MouseInfo.getPointerInfo();
        return info == null ? null : info.getLocation();
    }

    @Override
    public Rectangle boundsAt(Point position) {
        PointerInfo info = MouseInfo.getPointerInfo();
        if (info == null || !info.getLocation().equals(position)) {
            return null;
        }
        GraphicsConfiguration screen = info.getDevice().getDefaultConfiguration();
        if (!configuration.equals(screen)) {
            try {
                robot = new Robot(screen.getDevice());
                configuration = screen;
            } catch (AWTException | SecurityException error) {
                System.err.println(error.getMessage());
                return null;
            }
        }
        return screen.getBounds();
    }

    @Override
    public int radiusInCoordinates() {
        AffineTransform transform = configuration.getDefaultTransform();
        double scale = Math.max(1, Math.max(transform.getScaleX(), transform.getScaleY()));
        return (int) Math.floor(CursorMotion.RADIUS / scale);
    }

    @Override
    public boolean move(Point position) {
        robot.mouseMove(position.x, position.y);
        return true;
    }
}
