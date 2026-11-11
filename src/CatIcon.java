import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
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

    static BufferedImage draw(int size, boolean hunting) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.scale(size / 64.0, size / 64.0);
        Shape head = CatArt.polygon(5, 4, 22, 15, 32, 13, 42, 15, 59, 4, 58, 28,
                62, 38, 56, 39, 60, 46, 54, 45, 48, 54, 32, 60, 16, 54, 10, 45, 4, 46, 8, 39, 2, 38, 6, 28);
        g.setPaint(new GradientPaint(0, 0, new Color(0x24222C), 0, 60, new Color(0x09090D)));
        g.fill(head);
        g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(hunting ? new Color(0x9B494D) : new Color(0x665D74));
        g.draw(head);
        g.setColor(hunting ? new Color(0xFFB24F) : new Color(0xB39BD6));
        g.fill(CatArt.polygon(11, 28, 29, 35, 23, 40, 16, 37));
        g.fill(CatArt.polygon(53, 28, 35, 35, 41, 40, 48, 37));
        g.setColor(new Color(0x09090D));
        g.fill(CatArt.polygon(22, 31, 25, 35, 21, 40, 19, 35));
        g.fill(CatArt.polygon(42, 31, 39, 35, 43, 40, 45, 35));
        g.setColor(hunting ? new Color(0xF08786) : new Color(0xA77D9F));
        g.fill(CatArt.polygon(27, 43, 37, 43, 32, 48));
        g.dispose();
        return image;
    }

    public static void main(String[] args) throws Exception {
        int[] sizes = {16, 24, 32, 48, 64, 128, 256};
        ByteBuffer directory = ByteBuffer.allocate(6 + 16 * sizes.length).order(ByteOrder.LITTLE_ENDIAN);
        directory.putShort((short) 0).putShort((short) 1).putShort((short) sizes.length);
        ByteArrayOutputStream images = new ByteArrayOutputStream();
        for (int size : sizes) {
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(draw(size, false), "PNG", png);
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
