import java.awt.BasicStroke;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicComboBoxUI;

final class CatFace extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Color TEXT = new Color(0xF7EFF8);
    private static final Color MUTED = new Color(0xC0ABCC);
    private static final Color WELL = new Color(0x201925);
    private static final Color BORDER = new Color(0x45344F);
    private static final Color HIGHLIGHT = new Color(0x573747);
    private static final String PICKER = "picker";
    private static final String COUNTDOWN = "countdown";
    private static final BufferedImage STALK_MOUSE = MouseOverlay.draw();
    private static final int PAW_START = 142;
    private static final int PAW_END = 228;

    private final CardLayout timerLayout = new CardLayout();
    private final JPanel timerCard = new JPanel(timerLayout);
    private final JLabel countdown = new JLabel("", SwingConstants.CENTER);
    private final JLabel timerLabel = new JLabel("Dej myšce milost za", SwingConstants.CENTER);
    private boolean hunting;
    private boolean pouncing;
    private int pawX = PAW_START;
    private Point gaze;

    CatFace(JFrame frame, JButton toggle, JComboBox<?> limit, Runnable minimize, Runnable close) {
        super(null);
        setPreferredSize(new Dimension(CatArt.WIDTH, CatArt.HEIGHT));
        setOpaque(false);

        toggle.setBounds(65, 214, 278, 47);
        add(toggle);

        timerLabel.setFont(timerLabel.getFont().deriveFont(Font.BOLD, 13f));
        timerLabel.setForeground(MUTED);
        timerLabel.setLabelFor(limit);
        timerLabel.setBounds(104, 274, 200, 20);
        add(timerLabel);

        stylePicker(limit);
        countdown.setOpaque(true);
        countdown.setBackground(WELL);
        countdown.setForeground(TEXT);
        countdown.setFont(countdown.getFont().deriveFont(Font.BOLD, 13f));
        countdown.setBorder(BorderFactory.createLineBorder(BORDER));
        timerCard.setOpaque(false);
        timerCard.add(limit, PICKER);
        timerCard.add(countdown, COUNTDOWN);
        timerCard.setBounds(105, 299, 198, 32);
        add(timerCard);

        JButton min = windowButton(Glyph.MINIMIZE, "Minimalizovat", minimize);
        min.setBounds(164, 72, 35, 30);
        add(min);
        JButton quit = windowButton(Glyph.CLOSE, "Ukončit", close);
        quit.setBounds(209, 72, 35, 30);
        add(quit);

        MouseAdapter drag = new MouseAdapter() {
            private Point offset;

            @Override public void mousePressed(MouseEvent event) {
                if (event.getButton() == MouseEvent.BUTTON1) {
                    offset = event.getPoint();
                }
            }

            @Override public void mouseDragged(MouseEvent event) {
                if (offset != null) {
                    frame.setLocation(event.getXOnScreen() - offset.x, event.getYOnScreen() - offset.y);
                }
            }

            @Override public void mouseReleased(MouseEvent event) {
                offset = null;
            }
        };
        addMouseListener(drag);
        addMouseMotionListener(drag);
    }

    void setMode(boolean active, boolean engaged, String label) {
        hunting = active;
        timerLabel.setText(label);
        timerLayout.show(timerCard, engaged ? COUNTDOWN : PICKER);
        repaint();
    }

    void setCountdown(String text) {
        if (!text.equals(countdown.getText())) {
            countdown.setText(text);
        }
    }

    void setStalkProgress(double progress, boolean pouncing) {
        int nextX = pouncing ? 247 : PAW_START + (int) Math.round(progress * (PAW_END - PAW_START));
        if (pawX != nextX || this.pouncing != pouncing) {
            pawX = nextX;
            this.pouncing = pouncing;
            repaint(127, 333, 155, 34);
        }
    }

    void lookAt(Point screenPointer) {
        Point next = null;
        if (screenPointer != null) {
            Point location = getLocationOnScreen();
            next = new Point(screenPointer.x - location.x, screenPointer.y - location.y);
        }
        if (!java.util.Objects.equals(gaze, next)) {
            gaze = next;
            repaint(46, 112, 316, 73);
        }
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        CatArt.paint(g, hunting, gaze);
        if (hunting) {
            paintStalk(g);
        }
        g.dispose();
    }

    private void paintStalk(Graphics2D g) {
        if (pouncing) {
            g.setColor(new Color(0xFFB754));
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(259, 340, 265, 336);
            g.drawLine(262, 346, 270, 346);
            g.drawLine(259, 352, 265, 356);
        } else {
            g.setColor(BORDER);
            for (int x = 142; x <= 244; x += 8) {
                g.fill(new Ellipse2D.Double(x, 348, 2.6, 2.6));
            }
            g.drawImage(STALK_MOUSE, 243, 336, this);
        }

        g.setColor(pouncing ? new Color(0xFFB754) : new Color(0xC38EBC));
        g.fill(new Ellipse2D.Double(pawX - 8, 345, 12, 11));
        g.fill(new Ellipse2D.Double(pawX - 2, 339, 5, 6));
        g.fill(new Ellipse2D.Double(pawX + 4, 343, 5, 6));
        g.fill(new Ellipse2D.Double(pawX + 4, 350, 5, 6));
    }

    private static JButton windowButton(Glyph glyph, String description, Runnable action) {
        JButton button = new FaceButton(glyph);
        button.setToolTipText(description);
        button.getAccessibleContext().setAccessibleName(description);
        button.addActionListener(event -> action.run());
        return button;
    }

    private static void stylePicker(JComboBox<?> picker) {
        picker.setLightWeightPopupEnabled(false);
        picker.setUI(new BasicComboBoxUI() {
            @Override protected JButton createArrowButton() {
                JButton arrow = new FaceButton(Glyph.DOWN);
                arrow.setFocusable(false);
                return arrow;
            }

            @Override public void paintCurrentValueBackground(Graphics g, java.awt.Rectangle bounds, boolean focus) {
                g.setColor(focus ? HIGHLIGHT : WELL);
                g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        });
        picker.setBackground(WELL);
        picker.setForeground(TEXT);
        picker.setFont(picker.getFont().deriveFont(Font.BOLD, 13f));
        picker.setBorder(BorderFactory.createLineBorder(BORDER));
        picker.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        picker.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                    boolean selected, boolean focus) {
                super.getListCellRendererComponent(list, value, index, selected, focus);
                setBackground(selected ? HIGHLIGHT : WELL);
                setForeground(TEXT);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(BorderFactory.createEmptyBorder(5, 6, 5, 6));
                return this;
            }
        });
    }

    private enum Glyph { MINIMIZE, CLOSE, DOWN }

    static final class FaceButton extends JButton {
        private static final long serialVersionUID = 1L;
        private final Glyph glyph;

        FaceButton() {
            this(null);
        }

        private FaceButton(Glyph glyph) {
            this.glyph = glyph;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(getFont().deriveFont(Font.BOLD, 17f));
            setForeground(TEXT);
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean main = glyph == null;
            boolean hover = getModel().isRollover() || getModel().isPressed();
            boolean active = main && getModel().isSelected();
            int arc = main ? 22 : 12;
            if (main) {
                Color top = active ? new Color(0x792B39) : new Color(0x56385E);
                Color bottom = active ? new Color(0x501C2A) : new Color(0x34243F);
                g.setPaint(new GradientPaint(0, 0, hover ? top.brighter() : top, 0, getHeight(), bottom));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                g.setColor(active ? new Color(0xB85157) : new Color(0x805788));
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            } else {
                g.setColor(hover ? HIGHLIGHT : WELL);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                g.setColor(BORDER);
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            }
            if (hasFocus()) {
                g.setColor(MUTED);
                g.drawRoundRect(3, 3, getWidth() - 7, getHeight() - 7, arc, arc);
            }
            if (!main) {
                paintGlyph(g);
            }
            g.dispose();
            if (main) {
                super.paintComponent(graphics);
            }
        }

        private void paintGlyph(Graphics2D g) {
            g.setColor(MUTED);
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int x = getWidth() / 2;
            int y = getHeight() / 2;
            switch (glyph) {
                case MINIMIZE:
                    g.drawLine(x - 5, y, x + 5, y);
                    break;
                case CLOSE:
                    g.drawLine(x - 4, y - 4, x + 4, y + 4);
                    g.drawLine(x + 4, y - 4, x - 4, y + 4);
                    break;
                case DOWN:
                    g.drawPolyline(new int[] {x - 4, x, x + 4}, new int[] {y - 2, y + 2, y - 2}, 3);
                    break;
            }
        }
    }
}
