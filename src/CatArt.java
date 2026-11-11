import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

final class CatArt {
    static final int WIDTH = 408;
    static final int HEIGHT = 382;

    private static final Shape HEAD = head();
    private static final Shape LEFT_EAR = polygon(39, 33, 57, 97, 105, 76);
    private static final Shape LEFT_EYE = eye();
    private static final Shape LEFT_IRIS = new Ellipse2D.Double(105, 129, 39, 49);
    private static final Shape LEFT_PUPIL = polygon(127, 134, 133, 154, 124, 176, 118, 154);
    private static final Shape LEFT_BROW = polygon(57, 119, 96, 122, 163, 153, 111, 140, 76, 134);

    private CatArt() {}

    static Shape silhouette() {
        return HEAD;
    }

    static void paint(Graphics2D g, boolean hunting) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 28, new Color(0x202029), 0, 355, new Color(0x09090D)));
        g.fill(HEAD);

        Graphics2D face = (Graphics2D) g.create();
        face.clip(HEAD);
        pair(face, LEFT_EAR, new Color(0x35232E));
        pair(face, polygon(43, 43, 58, 91, 72, 83), new Color(0x51313F));
        pair(face, LEFT_BROW, new Color(0x2E2935));

        Color eyeTop = hunting ? new Color(0xFFB754) : new Color(0xAD95D5);
        Color eyeBottom = hunting ? new Color(0xF04C46) : new Color(0x69558B);
        pair(face, LEFT_EYE, new Color(0x0A080D));
        for (boolean mirrored : new boolean[] {false, true}) {
            Graphics2D eye = (Graphics2D) face.create();
            if (mirrored) {
                eye.translate(WIDTH, 0);
                eye.scale(-1, 1);
            }
            eye.setStroke(new BasicStroke(8, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            eye.setColor(hunting ? new Color(0x4D2428) : new Color(0x30253D));
            eye.draw(LEFT_EYE);
            eye.clip(LEFT_EYE);
            eye.setPaint(new GradientPaint(0, 131, eyeTop, 0, 173, eyeBottom));
            eye.fill(LEFT_EYE);
            eye.setColor(hunting ? new Color(0xFFCF74) : new Color(0xC6ACEF));
            eye.fill(LEFT_IRIS);
            eye.setColor(new Color(0x140C17));
            eye.fill(LEFT_PUPIL);
            eye.fill(new Ellipse2D.Double(119, 139, 11, 23));
            eye.setColor(new Color(0xFFF5DB));
            eye.fill(new Ellipse2D.Double(111, 142, 6, 6));
            eye.dispose();
        }

        pair(face, polygon(46, 177, 103, 194, 55, 189), new Color(0x332B39));
        pair(face, polygon(42, 197, 99, 207, 55, 206), new Color(0x302735));
        pair(face, polygon(53, 218, 103, 220, 61, 226), new Color(0x26212F));
        face.setColor(new Color(0x6E3C53));
        face.fill(polygon(188, 184, 197, 180, 211, 180, 220, 184, 204, 197));
        face.setColor(new Color(0x241825));
        face.setStroke(new BasicStroke(2));
        face.drawLine(204, 195, 204, 206);
        face.dispose();

        Graphics2D edge = (Graphics2D) g.create();
        edge.clip(HEAD);
        edge.setStroke(new BasicStroke(5));
        edge.setColor(hunting ? new Color(0x663037) : new Color(0x403847));
        edge.draw(HEAD);
        edge.dispose();
    }

    private static void pair(Graphics2D g, Shape shape, Color color) {
        g.setColor(color);
        g.fill(shape);
        AffineTransform mirror = new AffineTransform(-1, 0, 0, 1, WIDTH, 0);
        g.fill(mirror.createTransformedShape(shape));
    }

    private static Shape head() {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(25, 13);
        p.quadTo(24, 4, 35, 7);
        p.quadTo(89, 21, 144, 63);
        p.quadTo(204, 47, 264, 63);
        p.quadTo(319, 21, 373, 7);
        p.quadTo(384, 4, 383, 13);
        p.quadTo(390, 71, 375, 125);
        p.quadTo(393, 160, 395, 187);
        p.lineTo(407, 217);
        p.lineTo(388, 215);
        p.lineTo(400, 244);
        p.lineTo(379, 240);
        p.curveTo(369, 324, 296, 375, 204, 378);
        p.curveTo(112, 375, 39, 324, 29, 240);
        p.lineTo(8, 244);
        p.lineTo(20, 215);
        p.lineTo(1, 217);
        p.lineTo(13, 187);
        p.quadTo(15, 160, 33, 125);
        p.quadTo(18, 71, 25, 13);
        p.closePath();
        return p;
    }

    private static Shape eye() {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(58, 130);
        p.quadTo(104, 132, 165, 158);
        p.quadTo(142, 181, 109, 173);
        p.quadTo(75, 169, 58, 130);
        p.closePath();
        return p;
    }

    static Shape polygon(int... coordinates) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(coordinates[0], coordinates[1]);
        for (int i = 2; i < coordinates.length; i += 2) {
            p.lineTo(coordinates[i], coordinates[i + 1]);
        }
        p.closePath();
        return p;
    }
}
