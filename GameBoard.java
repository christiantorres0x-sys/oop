import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameBoard extends JPanel implements ActionListener {
    Paddle paddle;
    List<Ball> balls;
    List<Brick> bricks;
    Timer timer;
    SoundManager soundManager;

    // Gameplay state
    boolean gameOver = false;
    boolean win = false;
    int lives = 3;
    int score = 0;

    // Smooth movement flags
    private boolean movingLeft = false;
    private boolean movingRight = false;
    private final int PADDLE_SPEED = 6;

    // MVP doesn't focus on score; single-ball for classic behavior
    private final int BALL_COUNT = 1; // single ball

    public GameBoard() {
        setFocusable(true);
        setPreferredSize(new Dimension(500, 500));

        soundManager = new SoundManager();
        paddle = new Paddle(200, 450, 100, 18);
        balls = new ArrayList<>();
        bricks = new ArrayList<>();

        // Click to focus and request focus on EDT
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                requestFocusInWindow();
            }
        });
        SwingUtilities.invokeLater(() -> requestFocusInWindow());

    // Setup key bindings for smooth movement (pressed and released)
    setupKeyBindings();

        // Mouse motion to control paddle position
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                // center paddle on mouse x
                paddle.x = e.getX() - paddle.width / 2;
                // clamp
                paddle.x = Math.max(0, Math.min(paddle.x, getWidth() - paddle.width));
            }
        });

        timer = new Timer(16, this); // ~60 FPS
        // Do not initialize bricks here — wait until the game is explicitly started
    }

    

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver && !win) {
            // Smooth paddle movement while key is held
            if (movingLeft) paddle.x -= PADDLE_SPEED;
            if (movingRight) paddle.x += PADDLE_SPEED;

            // Keep paddle within bounds
            paddle.x = Math.max(0, Math.min(paddle.x, getWidth() - paddle.width));

            // Update balls
            for (Ball b : new ArrayList<>(balls)) {
                b.move();
            }
            checkCollisions();
        }
        repaint();
    }

    private void checkCollisions() {
        // Process collisions per ball
        List<Brick> toRemove = new ArrayList<>();
        for (Ball b : new ArrayList<>(balls)) {
            // Wall collisions
            if (b.x <= 0) { b.x = 0; b.dx = Math.abs(b.dx); soundManager.playSFX("ballHit"); }
            if (b.x + b.width >= getWidth()) { b.x = getWidth() - b.width; b.dx = -Math.abs(b.dx); soundManager.playSFX("ballHit"); }
            if (b.y <= 0) { b.y = 0; b.dy = Math.abs(b.dy); soundManager.playSFX("ballHit"); }

            // Paddle collision
            if (b.getBounds().intersects(paddle.getBounds())) {
                soundManager.playSFX("ballHit");
                b.y = paddle.y - b.height - 1;
                b.dy = -Math.abs(b.dy);
                // alter dx depending on hit position
                int paddleCenter = paddle.x + paddle.width / 2;
                int ballCenter = b.x + b.width / 2;
                int diff = ballCenter - paddleCenter;
                b.dx = diff / Math.max(1, paddle.width / 8);
                if (b.dx == 0) b.dx = (new Random().nextBoolean()) ? -2 : 2;
            }

            // Brick collisions
            for (Brick brick : bricks) {
                if (!brick.destroyed && b.getBounds().intersects(brick.getBounds())) {
                    brick.destroyed = true;
                    soundManager.playSFX("boxHit");
                    // maintain speed magnitude when bouncing off bricks
                    double speed = Math.sqrt(b.dx * b.dx + b.dy * b.dy);
                    b.dy *= -1;
                    // normalize to maintain consistent speed
                    double currentSpeed = Math.sqrt(b.dx * b.dx + b.dy * b.dy);
                    if (currentSpeed > 0) {
                        b.dx = (int) (b.dx * speed / currentSpeed);
                        b.dy = (int) (b.dy * speed / currentSpeed);
                    }
                    toRemove.add(brick);
                    score += 100;
                    break; // one brick per ball per tick
                }
            }
        }

        // Remove destroyed bricks from list
        if (!toRemove.isEmpty()) {
            bricks.removeAll(toRemove);
        }

        // Win condition: when no bricks
        if (bricks.isEmpty()) {
            win = true;
            soundManager.playSFX("win");
            timer.stop();
        }

        // Remove balls that fell below
        List<Ball> fallen = new ArrayList<>();
        for (Ball b : balls) {
            if (b.y > getHeight()) fallen.add(b);
        }
        if (!fallen.isEmpty()) {
            // remove fallen balls
            balls.removeAll(fallen);
            // decrement lives (for single-ball mode, one life per fall)
            lives -= fallen.size();
            if (lives > 0) {
                // respawn balls and paddle
                resetPositions();
            } else {
                gameOver = true;
                soundManager.playSFX("lose");
                timer.stop();
            }
        }
    }
    private void resetPositions() {
        // Reset paddle and balls to starting positions
        paddle.x = (getWidth() - paddle.width) / 2;
        paddle.y = getHeight() - 50;
        balls.clear();
        // spawn BALL_COUNT balls near paddle with slight angle variations
        for (int i = 0; i < BALL_COUNT; i++) {
            Ball b = new Ball(paddle.x + paddle.width/2 - 6, paddle.y - 12 - i*2, 12);
            int speed = 5;
            b.dx = (i % 2 == 0) ? -speed + i%3 : speed - i%3;
            b.dy = -Math.abs(speed);
            balls.add(b);
        }
        // ensure focus so key bindings work
        requestFocusInWindow();
    }

    // Pause/resume controls used by the menu
    public void pauseGame() {
        paused = true;
        soundManager.pauseBackgroundMusic();
        if (timer != null) timer.stop();
        repaint();
    }

    public void resumeGame() {
        paused = false;
        soundManager.resumeBackgroundMusic();
        if (timer != null && !timer.isRunning()) timer.start();
        requestFocusInWindow();
    }

    private void setupKeyBindings() {
        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        // Left pressed
        im.put(KeyStroke.getKeyStroke("LEFT"), "left.pressed");
        am.put("left.pressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                movingLeft = true;
            }
        });

        // Left released
        im.put(KeyStroke.getKeyStroke("released LEFT"), "left.released");
        am.put("left.released", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                movingLeft = false;
            }
        });

        // Right pressed
        im.put(KeyStroke.getKeyStroke("RIGHT"), "right.pressed");
        am.put("right.pressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                movingRight = true;
            }
        });

        // Right released
        im.put(KeyStroke.getKeyStroke("released RIGHT"), "right.released");
        am.put("right.released", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                movingRight = false;
            }
        });

        // Pause (Space)
        im.put(KeyStroke.getKeyStroke("SPACE"), "pause.toggle");
        am.put("pause.toggle", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                togglePause();
            }
        });

        // Restart (R) — reinitialize and start
        im.put(KeyStroke.getKeyStroke('R', 0), "restart");
        am.put("restart", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startGame();
            }
        });
    }

    private boolean paused = false;

    private void togglePause() {
        paused = !paused;
        if (paused) {
            timer.stop();
        } else {
            timer.start();
        }
        requestFocusInWindow();
    }

    private void restartGame() {
        // reset state
        // reset minimal state
        gameOver = false;
        win = false;
        paused = false;
        // reset score & lives
        lives = 3;
        score = 0;
        // recreate bricks using the MVP-specified 5-row palette
        bricks.clear();
        Color[] palette = {
            new Color(0xB4, 0x4C, 0xE4), // Purple
            new Color(0x5F, 0xA7, 0xFF), // Blue
            new Color(0x79, 0xEB, 0x5C), // Green
            new Color(0xFF, 0xE0, 0x66), // Yellow
            new Color(0xFF, 0x5D, 0x5D)  // Red
        };

        int rows = palette.length; // 5 rows per MVP spec
            int cols = 12; // More columns for smaller bricks
            int boardW = Math.max(getWidth(), getPreferredSize().width);
            int brickSize = Math.max(1, boardW / cols); // Smaller brick size, never 0
            int startX = 0;
            int startY = 0;

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int bx = startX + col * brickSize;
                int by = startY + row * brickSize;
                bricks.add(new Brick(bx, by, brickSize, brickSize, palette[row]));
            }
        }

        resetPositions();
        // don't auto-start here; menu will call startGame()
        requestFocusInWindow();
    }

    // Start the game (called by the main menu or restart)
    public void startGame() {
        // (re)initialize level so bricks/paddle/balls are laid out using actual component size
        restartGame();
        if (!timer.isRunning()) {
            timer.start();
        }
        soundManager.playBackgroundMusic();
        requestFocusInWindow();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // Dark navy background
    g2.setColor(new Color(0x0D, 0x1B, 0x2A));
    g2.fillRect(0, 0, getWidth(), getHeight());

        // Draw bricks
        for (Brick brick : bricks) {
            brick.draw(g2);
        }

    // Draw paddle & balls
    for (Ball b : balls) b.draw(g2);
    paddle.draw(g2);

    // HUD: score and lives
    g2.setColor(Color.WHITE);
    g2.setFont(new Font("Arial", Font.BOLD, 14));
    String hud = "Score: " + score;
    g2.drawString(hud, 12, 20);
    String life = "Lives: " + lives;
    int lw = g2.getFontMetrics().stringWidth(life);
    g2.drawString(life, getWidth() - lw - 12, 20);

        // Paused or end overlay
        if (paused) {
            g2.setColor(new Color(255, 255, 255, 120));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 28));
            g2.drawString("Paused", getWidth() / 2 - 50, getHeight() / 2);
        }

        if (gameOver || win) {
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 36));
            String msg = win ? "You Win!" : "Game Over";
            g2.drawString(msg, getWidth() / 2 - 100, getHeight() / 2 - 10);
            g2.setFont(new Font("Arial", Font.PLAIN, 18));
            g2.drawString("Press R to restart", getWidth() / 2 - 80, getHeight() / 2 + 20);
        }
    }

    // Cleanup resources on app exit
    public void cleanup() {
        soundManager.cleanup();
    }
}
