import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class Ball extends GameObject {
    int dx = 7;
    int dy = -7;

    public Ball(int x, int y, int diameter) {
        super(x, y, diameter, diameter);
    }

    public void move() {
        x += dx;
        y += dy;
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // soft glow: draw a larger translucent red/pink ellipse behind
        int glowSize = Math.max(width, height) + 18;
        int gx = x + width/2 - glowSize/2;
        int gy = y + height/2 - glowSize/2;
        g2.setColor(new Color(255, 140, 140, 90)); // light red/pink glow
        g2.fillOval(gx, gy, glowSize, glowSize);

        // inner red ball
        g2.setColor(Color.WHITE);
        g2.fillOval(x, y, width, height);

        // subtle darker red outline
        g2.setColor(new Color(150, 30, 30));
        g2.drawOval(x, y, width, height);
    }
}
