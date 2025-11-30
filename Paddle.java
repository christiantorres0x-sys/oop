import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class Paddle extends GameObject {

    public Paddle(int x, int y, int width, int height) {
        super(x, y, width, height);
    }

    public void moveLeft() {
        x -= 10;
    }

    public void moveRight() {
        x += 10;
    }

    @Override
    public void draw(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // soft shadow
        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRoundRect(x + 2, y + 6, width, height, 18, 18);

        // white rounded paddle
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(x, y, width, height, 18, 18);

        // subtle outline
        g2.setColor(new Color(180, 180, 180));
        g2.drawRoundRect(x, y, width, height, 18, 18);
    }
}
