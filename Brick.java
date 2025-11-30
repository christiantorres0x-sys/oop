import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;

public class Brick extends GameObject {
    boolean destroyed = false;
    Color color = Color.ORANGE;

    public Brick(int x, int y, int w, int h, Color color) {
        super(x, y, w, h);
        if (color != null) this.color = color;
    }

    @Override
    public void draw(Graphics g) {
        if (!destroyed) {
            Graphics2D g2 = (Graphics2D) g;
            GradientPaint gp = new GradientPaint(x, y, color.brighter(), x + width, y + height, color.darker());
            g2.setPaint(gp);
            g2.fillRect(x, y, width, height);
            g2.setColor(Color.DARK_GRAY);
            g2.drawRect(x, y, width, height);
        }
    }
}
