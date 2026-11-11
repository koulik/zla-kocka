import java.awt.AWTException;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.MouseInfo;
import java.awt.PointerInfo;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

public final class ZlaKocka {
    private static final String NAME = "Zlá Kočka";
    private static final String START = "Chytej myšku!";
    private static final String STOP = "Nech myšku na pokoji!";

    private static final class Limit {
        private final String label;
        private final Duration duration;

        Limit(String label, Duration duration) {
            this.label = label;
            this.duration = duration;
        }

        @Override public String toString() { return label; }
    }

    @FunctionalInterface
    interface PointerFactory {
        CursorMotion.Pointer create() throws AWTException;
    }

    private static final Limit[] LIMITS = {
            new Limit("Bez časového omezení", Duration.ZERO),
            new Limit("15 minut", Duration.ofMinutes(15)),
            new Limit("30 minut", Duration.ofMinutes(30)),
            new Limit("1 hodina", Duration.ofHours(1)),
            new Limit("2 hodiny", Duration.ofHours(2)),
            new Limit("3 hodiny", Duration.ofHours(3)),
            new Limit("4 hodiny", Duration.ofHours(4)),
            new Limit("8 hodin", Duration.ofHours(8))
    };

    private final CursorMotion motion;
    private final JFrame frame = new JFrame(NAME);
    private final JButton toggle = new CatFace.FaceButton();
    private final JComboBox<Limit> limit = new JComboBox<>(LIMITS);
    private final Timer timer = new Timer(18, event -> poll());
    private final MouseOverlay mouseOverlay = new MouseOverlay();
    private final CatFace face;
    private final List<Image> restingIcons = windowIcons(false);
    private final List<Image> huntingIcons = windowIcons(true);
    private TrayIcon trayIcon;
    private long shownSeconds = Long.MIN_VALUE;

    ZlaKocka(CursorMotion motion) {
        this.motion = motion;
        frame.setUndecorated(true);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowIconified(WindowEvent event) {
                if (trayIcon != null) {
                    frame.setVisible(false);
                }
                updatePolling();
            }

            @Override public void windowDeiconified(WindowEvent event) {
                updatePolling();
            }

            @Override public void windowClosing(WindowEvent event) {
                exit();
            }
        });
        toggle.addActionListener(event -> {
            if (motion.active()) {
                stop();
            } else {
                start(((Limit) limit.getSelectedItem()).duration);
            }
        });
        limit.getAccessibleContext().setAccessibleDescription("Doba od začátku chytání");
        face = new CatFace(frame, toggle, limit, this::minimize, this::exit);
        frame.getRootPane().setDefaultButton(toggle);
        frame.setContentPane(face);
        refresh();
        frame.pack();
        frame.setLocationRelativeTo(null);
        shapeWindow();
    }

    private void shapeWindow() {
        GraphicsDevice screen = frame.getGraphicsConfiguration().getDevice();
        frame.setBackground(new Color(0x0D0C12));
        if (screen.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)) {
            try {
                frame.setBackground(new Color(0, 0, 0, 0));
            } catch (UnsupportedOperationException ignored) {
                // Keep the opaque background if the display lacks a translucent configuration.
            }
        }
        if (screen.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT)) {
            try {
                frame.setShape(CatArt.silhouette());
            } catch (UnsupportedOperationException ignored) {
                // Retain the window background supported by this display.
            }
        }
    }

    private static List<Image> windowIcons(boolean active) {
        return Arrays.asList(CatIcon.draw(16, active), CatIcon.draw(32, active), CatIcon.draw(64, active));
    }

    private void setupTray() {
        if (!SystemTray.isSupported()) {
            return;
        }
        try {
            TrayIcon icon = new TrayIcon(restingIcons.get(1), NAME);
            icon.setImageAutoSize(true);
            icon.setPopupMenu(trayMenu(false));
            icon.addActionListener(event -> SwingUtilities.invokeLater(this::show));
            SystemTray tray = SystemTray.getSystemTray();
            tray.add(icon);
            trayIcon = icon;
            tray.addPropertyChangeListener("trayIcons", event -> SwingUtilities.invokeLater(() -> {
                if (trayIcon == icon && !Arrays.asList(tray.getTrayIcons()).contains(icon)) {
                    trayIcon = null;
                    if (!frame.isVisible()) {
                        show();
                    }
                }
            }));
        } catch (AWTException | SecurityException | UnsupportedOperationException error) {
            System.err.println(error.getMessage());
        }
    }

    private PopupMenu trayMenu(boolean active) {
        PopupMenu menu = new PopupMenu();
        menu.add(menuItem("Zobrazit okno", this::show));
        menu.addSeparator();
        if (active) {
            menu.add(menuItem("Nech myšku na pokoji", this::stop));
        } else {
            menu.add(menuItem("Chytej myšku", () -> start(Duration.ZERO)));
            menu.add(menuItem("Chytej myšku hodinu", () -> start(Duration.ofHours(1))));
            menu.add(menuItem("Chytej myšku 2 hodiny", () -> start(Duration.ofHours(2))));
            menu.add(menuItem("Chytej myšku 3 hodiny", () -> start(Duration.ofHours(3))));
        }
        menu.addSeparator();
        menu.add(menuItem("Ukončit", this::exit));
        return menu;
    }

    private static MenuItem menuItem(String label, Runnable action) {
        MenuItem item = new MenuItem(label);
        item.addActionListener(event -> SwingUtilities.invokeLater(action));
        return item;
    }

    private void minimize() {
        if (trayIcon != null) {
            frame.setVisible(false);
        } else {
            frame.setExtendedState(Frame.ICONIFIED);
        }
        updatePolling();
    }

    private void show() {
        frame.setExtendedState(Frame.NORMAL);
        frame.setVisible(true);
        frame.toFront();
        updatePolling();
    }

    private void start(Duration duration) {
        if (!motion.active()) {
            motion.start(System.nanoTime(), duration);
            refresh();
        }
    }

    private void stop() {
        motion.stop();
        refresh();
    }

    private void poll() {
        long now = System.nanoTime();
        boolean wasActive = motion.active();
        motion.tick(now);
        if (motion.active()) {
            updateCountdown(now);
        } else if (wasActive) {
            refresh();
        }

        boolean track = faceVisible();
        boolean spiral = motion.spiraling();
        PointerInfo pointer = track || spiral ? MouseInfo.getPointerInfo() : null;
        if (track) {
            face.lookAt(pointer == null ? null : pointer.getLocation());
        }
        if (spiral && pointer != null) {
            mouseOverlay.showAt(pointer.getLocation(), pointer.getDevice().getDefaultConfiguration());
        } else {
            mouseOverlay.hide();
        }
    }

    private void refresh() {
        boolean active = motion.active();
        toggle.setText(active ? STOP : START);
        toggle.setSelected(active);
        limit.setEnabled(!active);
        face.setHunting(active);
        List<Image> icons = active ? huntingIcons : restingIcons;
        frame.setIconImages(icons);
        if (trayIcon != null) {
            trayIcon.setToolTip(NAME + (active ? " · chytá" : " · odpočívá"));
            trayIcon.setImage(icons.get(1));
            trayIcon.setPopupMenu(trayMenu(active));
        }
        if (active) {
            shownSeconds = Long.MIN_VALUE;
            updateCountdown(System.nanoTime());
        } else {
            mouseOverlay.hide();
        }
        updatePolling();
    }

    private boolean faceVisible() {
        return frame.isShowing() && (frame.getExtendedState() & Frame.ICONIFIED) == 0;
    }

    private void updatePolling() {
        if (motion.active() || faceVisible()) {
            timer.setDelay(motion.active() ? 18 : 33);
            timer.start();
        } else {
            timer.stop();
        }
    }

    private void updateCountdown(long now) {
        long seconds = motion.secondsLeft(now);
        if (seconds == shownSeconds) {
            return;
        }
        shownSeconds = seconds;
        face.setCountdown(seconds < 0 ? "Bez časového omezení"
                : String.format("%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60));
    }

    private void exit() {
        motion.stop();
        timer.stop();
        mouseOverlay.dispose();
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
        frame.dispose();
        System.exit(0);
    }

    public static void main(String[] args) {
        if (System.getProperty("os.name", "").startsWith("Mac")
                && System.getProperty("apple.awt.UIElement") == null) {
            System.setProperty("apple.awt.UIElement", "true");
        }
        launch(AwtPointer::new);
    }

    static void launch(PointerFactory pointerFactory) {
        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("Zlá Kočka vyžaduje grafické prostředí.");
            System.exit(1);
        }
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                ZlaKocka app = new ZlaKocka(new CursorMotion(pointerFactory.create()));
                app.setupTray();
                app.show();
            } catch (Exception | LinkageError error) {
                JOptionPane.showMessageDialog(null, "Aplikaci se nepodařilo spustit.\n" + error.getMessage(),
                        NAME, JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}
