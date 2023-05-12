package gmu.iit.gameboy;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

public class Screen extends JPanel
{
    private static final int PREF_ZOOM = 2;

    private static final int PREF_W = 160 * PREF_ZOOM;
    private static final int PREF_H = 144 * PREF_ZOOM;

    int[][] defaultPalette = new int[][] {
        {8, 24, 32},
        {52, 104, 86},
        {136, 192, 112},
        {224, 248, 208}
    };

    int[][] currentpalette = defaultPalette;

    // Image
    private BufferedImage image = new BufferedImage(160, 144, BufferedImage.TYPE_INT_RGB);

    /**
     *
     * @param palette
     */
    public void changePalette(int[][] palette) { currentpalette = palette; }

    /**
     *
     * @param x
     * @param y
     * @param width
     * @param height
     * @param color
     */
    public void setPixel(int x, int y, int color) {
        Color c = new Color(currentpalette[3-color][0], currentpalette[3-color][1], currentpalette[3-color][2]);
        image.setRGB(x, y, c.getRGB());
    }

    @Override
    public Dimension getPreferredSize() { return new Dimension(PREF_W, PREF_H); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        drawScaledImage(image, this, g);
    }

    public void clearScreen() {
        for(int i = 0; i < 23040; i++) {
            image.setRGB(i % 144, i / 160, Color.black.getRGB());
        }
    }

    /**
     * Taken from here:
     * https://www.codejava.net/java-se/graphics/drawing-an-image-with-automatic-scaling
     * @param image
     * @param canvas
     * @param g
     */
    public static void drawScaledImage(Image image, Component canvas, Graphics g) {
        int imgWidth = image.getWidth(null);
        int imgHeight = image.getHeight(null);

        double imgAspect = (double) imgHeight / imgWidth;

        int canvasWidth = canvas.getWidth();
        int canvasHeight = canvas.getHeight();

        double canvasAspect = (double) canvasHeight / canvasWidth;

        int x1 = 0; // top left X position
        int y1 = 0; // top left Y position
        int x2 = 0; // bottom right X position
        int y2 = 0; // bottom right Y position

        if (canvasAspect > imgAspect) {
            y1 = canvasHeight;
            // keep image aspect ratio
            canvasHeight = (int) (canvasWidth * imgAspect);
            y1 = (y1 - canvasHeight) / 2;
        } else {
            x1 = canvasWidth;
            // keep image aspect ratio
            canvasWidth = (int) (canvasHeight / imgAspect);
            x1 = (x1 - canvasWidth) / 2;
        }
        x2 = canvasWidth + x1;
        y2 = canvasHeight + y1;

        g.drawImage(image, x1, y1, x2, y2, 0, 0, imgWidth, imgHeight, null);
    }
}
