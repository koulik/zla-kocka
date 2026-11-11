import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JWindow;

final class MouseOverlay {
    private static final int WIDTH = 34;
    private static final int HEIGHT = 23;
    private static final int GAP = 14;
    private static final Ellipse2D BODY = new Ellipse2D.Double(12, 8, 14, 13);
    private static final Ellipse2D EAR = new Ellipse2D.Double(19, 3, 8, 9);
    private static final java.awt.Shape HEAD = CatArt.polygon(18, 13, 23, 7, 28, 10, 32, 15, 27, 18, 21, 18);
    private static final BasicStroke TAIL_STROKE = new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final Path2D TAIL = tail();

    private JWindow window;
    private GraphicsConfiguration configuration;

    void showAt(Point pointer, GraphicsConfiguration screen) {
        Point location = placement(pointer, screen.getBounds());
        if (location == null) {
            hide();
            return;
        }
        if (window == null || !screen.equals(configuration)) {
            dispose();
            window = createWindow(screen);
            configuration = screen;
        }
        if (window.getX() != location.x || window.getY() != location.y) {
            window.setLocation(location);
        }
        if (!window.isVisible()) {
            window.setVisible(true);
        }
    }

    void hide() {
        if (window != null && window.isVisible()) {
            window.setVisible(false);
        }
    }

    void dispose() {
        if (window != null) {
            window.dispose();
            window = null;
        }
    }

    static Point placement(Point pointer, Rectangle bounds) {
        if (bounds.width < WIDTH || bounds.height < HEIGHT || !bounds.contains(pointer)) {
            return null;
        }
        int x = pointer.x + GAP;
        if (x + WIDTH > bounds.x + bounds.width) {
            x = pointer.x - GAP - WIDTH;
        }
        int y = Math.min(pointer.y + 10, bounds.y + bounds.height - HEIGHT);
        if (x < bounds.x) {
            x = bounds.x;
            y = pointer.y + GAP;
            if (y + HEIGHT > bounds.y + bounds.height) {
                y = pointer.y - GAP - HEIGHT;
            }
            if (y < bounds.y) {
                return null;
            }
        }
        return new Point(x, y);
    }

    private static JWindow createWindow(GraphicsConfiguration screen) {
        JWindow overlay = new JWindow((Window) null, screen);
        overlay.setType(Window.Type.POPUP);
        overlay.setFocusableWindowState(false);
        overlay.setAutoRequestFocus(false);
        overlay.setAlwaysOnTop(true);
        overlay.setContentPane(new JLabel(new ImageIcon(draw())));
        overlay.setBackground(new Color(0x211922));
        overlay.pack();

        GraphicsDevice device = screen.getDevice();
        if (device.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)) {
            try {
                overlay.setBackground(new Color(0, 0, 0, 0));
            } catch (UnsupportedOperationException ignored) {
                // Use the shape if a translucent background is unavailable.
            }
        }
        if (overlay.getBackground().getAlpha() != 0
                && device.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT)) {
            try {
                Area shape = new Area(BODY);
                shape.add(new Area(EAR));
                shape.add(new Area(HEAD));
                shape.add(new Area(TAIL_STROKE.createStrokedShape(TAIL)));
                overlay.setShape(shape);
            } catch (UnsupportedOperationException ignored) {
                // Retain a small opaque marker.
            }
        }
        return overlay;
    }

    private static BufferedImage draw() {
        BufferedImage icon = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = icon.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(TAIL_STROKE);
        g.setColor(new Color(0xDB8C9F));
        g.draw(TAIL);
        g.setColor(new Color(0xE7DCEB));
        g.fill(BODY);
        g.fill(HEAD);
        g.setColor(new Color(0xA77D9F));
        g.fill(EAR);
        g.setColor(new Color(0xECABB8));
        g.fill(new Ellipse2D.Double(20.5, 4.5, 5, 6));
        g.fill(new Ellipse2D.Double(29, 13, 3, 3));
        g.setColor(new Color(0x271B2A));
        g.fill(new Ellipse2D.Double(26, 11, 2.5, 2.5));
        g.dispose();
        return icon;
    }

    private static Path2D tail() {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(14, 15);
        path.curveTo(8, 20, 5, 19, 3, 16);
        path.curveTo(1, 13, 3, 10, 6, 11);
        return path;
    }
}
