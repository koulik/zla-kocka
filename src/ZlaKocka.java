import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Duration;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

public final class ZlaKocka {
    private static final String NAME = "Zlá Kočka";
    private static final Color PAPER = new Color(0xFFF9F3);
    private static final Color INK = new Color(0x35201F);
    private static final Color MUTED = new Color(0x6F5753);
    private static final Color ACCENT = new Color(0x92391F);

    private record Limit(String label, Duration duration) {
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
            new Limit("4 hodiny", Duration.ofHours(4)),
            new Limit("8 hodin", Duration.ofHours(8))
    };

    private final CursorMotion motion;
    private final JFrame frame = new JFrame(NAME);
    private final JButton toggle = new ToggleButton();
    private final JLabel status = new JLabel();
    private final JLabel limitLabel = new JLabel("Automatické vypnutí");
    private final JComboBox<Limit> limit = new JComboBox<>(LIMITS);
    private final Timer timer = new Timer(18, event -> poll());
    private TrayIcon trayIcon;

    ZlaKocka(CursorMotion motion) {
        this.motion = motion;
        frame.setIconImages(List.of(CatIcon.draw(16), CatIcon.draw(32), CatIcon.draw(64)));
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowIconified(WindowEvent event) {
                if (trayIcon != null) {
                    frame.setVisible(false);
                }
            }

            @Override public void windowClosing(WindowEvent event) {
                exit();
            }
        });

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(PAPER);
        content.setBorder(BorderFactory.createEmptyBorder(26, 28, 26, 28));
        content.add(fullWidth(header(), 60));
        content.add(Box.createVerticalStrut(24));
        content.add(fullWidth(toggle, 45));
        content.add(Box.createVerticalStrut(10));
        status.setForeground(MUTED);
        status.setFont(status.getFont().deriveFont(12f));
        content.add(fullWidth(status, 22));
        content.add(Box.createVerticalStrut(18));

        limitLabel.setLabelFor(limit);
        limitLabel.setForeground(INK);
        limitLabel.setFont(limitLabel.getFont().deriveFont(Font.BOLD, 12f));
        limit.getAccessibleContext().setAccessibleDescription("Doba od zapnutí pohybu kurzoru");
        content.add(fullWidth(limitLabel, 19));
        content.add(Box.createVerticalStrut(5));
        content.add(fullWidth(limit, 32));

        toggle.addActionListener(event -> {
            if (motion.active()) {
                motion.stop();
            } else {
                Limit selected = LIMITS[limit.getSelectedIndex()];
                motion.start(System.nanoTime(), selected.duration());
            }
            refresh();
        });
        frame.getRootPane().setDefaultButton(toggle);
        frame.setContentPane(content);
        frame.setResizable(false);
        refresh();
        frame.pack();
        frame.setLocationRelativeTo(null);
    }

    private static JPanel header() {
        JPanel header = new JPanel(new BorderLayout(13, 0));
        header.setOpaque(false);
        header.add(new JLabel(new ImageIcon(CatIcon.draw(56))), BorderLayout.WEST);
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 12));
        titleRow.setOpaque(false);
        JLabel title = new JLabel(NAME);
        title.setForeground(INK);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 29f));
        titleRow.add(title);
        header.add(titleRow, BorderLayout.CENTER);
        return header;
    }

    private static JPanel fullWidth(java.awt.Component component, int height) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setPreferredSize(new Dimension(298, height));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        row.add(component);
        return row;
    }

    private void setupTray() {
        if (!SystemTray.isSupported()) {
            return;
        }
        PopupMenu menu = new PopupMenu();
        MenuItem open = new MenuItem("Zobrazit okno");
        open.addActionListener(event -> SwingUtilities.invokeLater(this::show));
        MenuItem quit = new MenuItem("Ukončit");
        quit.addActionListener(event -> SwingUtilities.invokeLater(this::exit));
        menu.add(open);
        menu.addSeparator();
        menu.add(quit);
        try {
            TrayIcon icon = new TrayIcon(CatIcon.draw(32), NAME, menu);
            icon.setImageAutoSize(true);
            icon.addActionListener(event -> SwingUtilities.invokeLater(this::show));
            SystemTray tray = SystemTray.getSystemTray();
            tray.add(icon);
            trayIcon = icon;
            tray.addPropertyChangeListener("trayIcons", event -> SwingUtilities.invokeLater(() -> {
                if (trayIcon == icon && !List.of(tray.getTrayIcons()).contains(icon)) {
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

    private void show() {
        frame.setExtendedState(Frame.NORMAL);
        frame.setVisible(true);
        frame.toFront();
    }

    private void poll() {
        long now = System.nanoTime();
        motion.tick(now);
        if (motion.active()) {
            updateStatus(now);
        } else {
            refresh();
        }
    }

    private void refresh() {
        boolean active = motion.active();
        toggle.setText(active ? "Vypnout pohyb kurzoru" : "Zapnout pohyb kurzoru");
        toggle.setForeground(active ? ACCENT : Color.WHITE);
        limit.setEnabled(!active);
        limitLabel.setForeground(active ? MUTED : INK);
        if (trayIcon != null) {
            trayIcon.setToolTip(NAME + (active ? " · zapnuto" : " · vypnuto"));
        }
        updateStatus(System.nanoTime());
        if (active) {
            timer.start();
        } else {
            timer.stop();
        }
    }

    private void updateStatus(long now) {
        String text = "Pohyb kurzoru je vypnutý";
        if (motion.active()) {
            long seconds = motion.secondsLeft(now);
            text = seconds < 0 ? "Pohyb po 59 s nečinnosti myši"
                    : "Vypnutí za %d:%02d:%02d".formatted(seconds / 3600, seconds / 60 % 60, seconds % 60);
        }
        if (!text.equals(status.getText())) {
            status.setText(text);
        }
    }

    private void exit() {
        motion.stop();
        timer.stop();
        if (trayIcon != null) {
            SystemTray.getSystemTray().remove(trayIcon);
        }
        frame.dispose();
        System.exit(0);
    }

    private static final class ToggleButton extends JButton {
        private static final long serialVersionUID = 1L;

        ToggleButton() {
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setFont(getFont().deriveFont(Font.BOLD, 14f));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean filled = Color.WHITE.equals(getForeground());
            Color background = filled ? ACCENT : new Color(0xF5E4DB);
            if (getModel().isRollover() || getModel().isPressed()) {
                background = filled ? new Color(0x76301C) : new Color(0xEFD6C8);
            }
            g.setColor(background);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            if (hasFocus()) {
                g.setColor(filled ? Color.WHITE : ACCENT);
                g.drawRoundRect(3, 3, getWidth() - 7, getHeight() - 7, 9, 9);
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    public static void main(String[] args) {
        if (System.getProperty("os.name", "").startsWith("Mac")
                && System.getProperty("apple.awt.UIElement") == null) {
            // Use the menu bar icon as the app's entry point on macOS.
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
