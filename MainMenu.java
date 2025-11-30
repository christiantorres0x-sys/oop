import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class MainMenu extends JPanel {
    private float alpha = 0f;
    private Timer fadeTimer;
    private final Rectangle startButton = new Rectangle();
    private boolean hover = false;
    private final Runnable onStart;
    private final Component backgroundComponent;
    private BufferedImage backdrop;
    private SoundManager soundManager;

    public MainMenu(int width, int height, Component backgroundComponent, Runnable onStart) {
        this.onStart = onStart;
        this.backgroundComponent = backgroundComponent;
        this.soundManager = new SoundManager();
        setOpaque(false);
        setBounds(0, 0, width, height);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (startButton.contains(e.getPoint())) {
                    soundManager.playSFX("menuClick");
                    if (onStart != null) onStart.run();
                    hideMenu();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                repaint();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                boolean now = startButton.contains(e.getPoint());
                if (now != hover) {
                    hover = now;
                    repaint();
                }
            }
        });
    }

    public void showMenu() {
        if (backgroundComponent != null) {
            int w = getWidth();
            int h = getHeight();
            if (w > 0 && h > 0) {
                BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = img.createGraphics();
                backgroundComponent.paintAll(g);
                g.dispose();
                backdrop = boxBlur(img, 6);
            }
        }

        alpha = 0f;
        setVisible(true);
        if (fadeTimer != null && fadeTimer.isRunning()) fadeTimer.stop();
        fadeTimer = new Timer(20, (ActionEvent ev) -> {
            alpha += 0.04f;
            if (alpha >= 1f) {
                alpha = 1f;
                ((Timer) ev.getSource()).stop();
            }
            repaint();
        });
        fadeTimer.start();
        repaint();
    }

    public void hideMenu() {
        setVisible(false);
        backdrop = null;
        if (fadeTimer != null) fadeTimer.stop();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        int w = getWidth();
        int h = getHeight();

        if (backdrop != null) {
            g2.drawImage(backdrop, 0, 0, null);
        } else {
            g2.setColor(new Color(13, 27, 42));
            g2.fillRect(0, 0, w, h);
        }

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f * alpha));
        g2.setColor(new Color(0, 10, 20));
        g2.fillRect(0, 0, w, h);

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        String title = "Brick Break";
        Font titleFont = new Font("Arial", Font.BOLD, 48);
        g2.setFont(titleFont);
        FontMetrics fm = g2.getFontMetrics();
        int tx = (w - fm.stringWidth(title)) / 2;
        int ty = 100;
        g2.setColor(new Color(100, 180, 255, (int) (140 * alpha)));
        g2.drawString(title, tx - 2, ty - 2);
        g2.setColor(new Color(255, 255, 255, (int) (255 * alpha)));
        g2.drawString(title, tx, ty);

        int bw = Math.max(160, w / 6);
        int bh = 56;
        int bx = (w - bw) / 2;
        int by = (h - bh) / 2;
        int pad = hover ? 8 : 0;
        startButton.setBounds(bx - pad / 2, by - pad / 2, bw + pad, bh + pad);

        g2.setColor(new Color(0, 0, 0, (int) (120 * alpha)));
        g2.fillRoundRect(startButton.x + 4, startButton.y + 6, startButton.width, startButton.height, 18, 18);

        if (hover) {
            g2.setColor(new Color(255, 74, 64, (int) (255 * alpha)));
        } else {
            g2.setColor(new Color(0xff3b30));
        }
        g2.fillRoundRect(startButton.x, startButton.y, startButton.width, startButton.height, 18, 18);

        g2.setColor(Color.WHITE);
        Font btnFont = new Font("Arial", Font.BOLD, 22);
        g2.setFont(btnFont);
        String label = "START";
        FontMetrics bm = g2.getFontMetrics();
        int lx = startButton.x + (startButton.width - bm.stringWidth(label)) / 2;
        int ly = startButton.y + (startButton.height + bm.getAscent()) / 2 - 4;
        g2.drawString(label, lx, ly);

        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        g2.setColor(new Color(255, 255, 255, (int) (200 * alpha)));
        String credits = "© 2025 Brick Break MVP";
        FontMetrics cm = g2.getFontMetrics();
        g2.drawString(credits, (w - cm.stringWidth(credits)) / 2, h - 20);

        g2.dispose();
    }

    private BufferedImage boxBlur(BufferedImage src, int radius) {
        if (radius < 1) return src;
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage tmp = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int passes = 3;
        for (int p = 0; p < passes; p++) {
            for (int y = 0; y < h; y++) {
                int sumA = 0, sumR = 0, sumG = 0, sumB = 0;
                for (int x = -radius; x <= radius; x++) {
                    int xx = Math.max(0, Math.min(w - 1, x));
                    int rgb = src.getRGB(xx, y);
                    sumA += (rgb >> 24) & 0xff;
                    sumR += (rgb >> 16) & 0xff;
                    sumG += (rgb >> 8) & 0xff;
                    sumB += rgb & 0xff;
                }
                for (int x = 0; x < w; x++) {
                    int a = sumA / (radius * 2 + 1);
                    int r = sumR / (radius * 2 + 1);
                    int g = sumG / (radius * 2 + 1);
                    int b = sumB / (radius * 2 + 1);
                    int argb = (a << 24) | (r << 16) | (g << 8) | b;
                    tmp.setRGB(x, y, argb);
                    int removeX = x - radius;
                    int addX = x + radius + 1;
                    if (removeX >= 0) {
                        int rgbRem = src.getRGB(removeX, y);
                        sumA -= (rgbRem >> 24) & 0xff;
                        sumR -= (rgbRem >> 16) & 0xff;
                        sumG -= (rgbRem >> 8) & 0xff;
                        sumB -= rgbRem & 0xff;
                    }
                    if (addX < w) {
                        int rgbAdd = src.getRGB(addX, y);
                        sumA += (rgbAdd >> 24) & 0xff;
                        sumR += (rgbAdd >> 16) & 0xff;
                        sumG += (rgbAdd >> 8) & 0xff;
                        sumB += rgbAdd & 0xff;
                    }
                }
            }
            for (int x = 0; x < w; x++) {
                int sumA = 0, sumR = 0, sumG = 0, sumB = 0;
                for (int y = -radius; y <= radius; y++) {
                    int yy = Math.max(0, Math.min(h - 1, y));
                    int rgb = tmp.getRGB(x, yy);
                    sumA += (rgb >> 24) & 0xff;
                    sumR += (rgb >> 16) & 0xff;
                    sumG += (rgb >> 8) & 0xff;
                    sumB += rgb & 0xff;
                }
                for (int y = 0; y < h; y++) {
                    int a = sumA / (radius * 2 + 1);
                    int r = sumR / (radius * 2 + 1);
                    int g = sumG / (radius * 2 + 1);
                    int b = sumB / (radius * 2 + 1);
                    int argb = (a << 24) | (r << 16) | (g << 8) | b;
                    dst.setRGB(x, y, argb);
                    int removeY = y - radius;
                    int addY = y + radius + 1;
                    if (removeY >= 0) {
                        int rgbRem = tmp.getRGB(x, removeY);
                        sumA -= (rgbRem >> 24) & 0xff;
                        sumR -= (rgbRem >> 16) & 0xff;
                        sumG -= (rgbRem >> 8) & 0xff;
                        sumB -= rgbRem & 0xff;
                    }
                    if (addY < h) {
                        int rgbAdd = tmp.getRGB(x, addY);
                        sumA += (rgbAdd >> 24) & 0xff;
                        sumR += (rgbAdd >> 16) & 0xff;
                        sumG += (rgbAdd >> 8) & 0xff;
                        sumB += rgbAdd & 0xff;
                    }
                }
            }
            src = dst;
            tmp = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        }
        return src;
    }
}
