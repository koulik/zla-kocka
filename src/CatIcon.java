import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Paths;
import javax.imageio.ImageIO;

final class CatIcon {
    private CatIcon() {}

    static BufferedImage draw(int size) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.scale(size / 64.0, size / 64.0);
        g.setColor(new Color(0x35201F));
        g.fill(new RoundRectangle2D.Double(1, 1, 62, 62, 17, 17));
        g.setColor(new Color(0xFFAC7E));
        Path2D face = new Path2D.Double();
        face.moveTo(12, 12);
        face.quadTo(13, 10, 16, 12);
        face.lineTo(25, 20);
        face.quadTo(32, 18, 39, 20);
        face.lineTo(48, 12);
        face.quadTo(51, 10, 52, 12);
        face.lineTo(53, 33);
        face.curveTo(55, 43, 47, 50, 32, 53);
        face.curveTo(17, 50, 9, 43, 11, 33);
        face.closePath();
        g.fill(face);
        g.setColor(new Color(0x9B4027));
        triangle(g, 16, 17, 22, 23, 16, 27);
        triangle(g, 48, 17, 42, 23, 48, 27);
        g.setColor(new Color(0x35201F));
        triangle(g, 16, 30, 30, 37, 19, 40);
        triangle(g, 48, 30, 34, 37, 45, 40);
        triangle(g, 27, 43, 37, 43, 32, 48);
        g.dispose();
        return image;
    }

    private static void triangle(Graphics2D g, int x1, int y1, int x2, int y2, int x3, int y3) {
        g.fillPolygon(new int[]{x1, x2, x3}, new int[]{y1, y2, y3}, 3);
    }

    public static void main(String[] args) throws Exception {
        int[] sizes = {16, 24, 32, 48, 64, 128, 256};
        ByteBuffer directory = ByteBuffer.allocate(6 + 16 * sizes.length).order(ByteOrder.LITTLE_ENDIAN);
        directory.putShort((short) 0).putShort((short) 1).putShort((short) sizes.length);
        ByteArrayOutputStream images = new ByteArrayOutputStream();
        for (int size : sizes) {
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(draw(size), "PNG", png);
            directory.put((byte) size).put((byte) size).putShort((short) 0);
            directory.putShort((short) 1).putShort((short) 32);
            directory.putInt(png.size()).putInt(directory.capacity() + images.size());
            png.writeTo(images);
        }
        try (OutputStream out = Files.newOutputStream(Paths.get(args[0]))) {
            out.write(directory.array());
            images.writeTo(out);
        }
    }
}
