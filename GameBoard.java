import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameBoard extends JPanel implements ActionListener {
    Paddle paddle;
    List<Ball> balls;
    List<Brick> bricks;
    List<Wall> walls = new ArrayList<>();
    Timer timer;
    SoundManager soundManager;

    // Gameplay state
    boolean gameOver = false;
    boolean win = false;
    int lives = 3;
    int score = 0;
    int currentLevel = 1;
    final int MAX_LEVELS = 7;

    // Smooth movement flags
    private boolean movingLeft = false;
    private boolean movingRight = false;
    // paddle speed adjustable by power-ups
    private int basePaddleSpeed = 6;
    private double paddleSpeedMultiplier = 1.0;
    private int paddleSpeedBoostTicks = 0;

    // MVP doesn't focus on score; single-ball for classic behavior
    private final int BALL_COUNT = 1; // single ball
    // power-ups and effects
    List<PowerUp> powerUps = new ArrayList<>();
    int ballSpeedBoostTicks = 0; // ticks remaining for ball speed boost
    double ballSpeedMultiplier = 1.0;

    public GameBoard() {
        setFocusable(true);
        setPreferredSize(new Dimension(600, 660));

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

        // Recompute layout when container is resized so bricks stay edge-to-edge
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // reload current level to recompute brick size and positions
                loadLevel(currentLevel);
                // ensure paddle stays centered
                paddle.x = Math.max(0, Math.min(paddle.x, getWidth() - paddle.width));
                resetPositions();
                repaint();
            }
        });
    }



    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver && !win) {
            // Smooth paddle movement while key is held
            int moveAmount = (int) Math.max(1, Math.round(basePaddleSpeed * paddleSpeedMultiplier));
            if (movingLeft) paddle.x -= moveAmount;
            if (movingRight) paddle.x += moveAmount;

            // update paddle size back to normal gradually
            paddle.updateSize();

            // update power-ups (falling)
            for (PowerUp pu : new ArrayList<>(powerUps)) {
                pu.update();
            }

            // paddle speed boost tick handling
            if (paddleSpeedBoostTicks > 0) {
                paddleSpeedBoostTicks--;
                if (paddleSpeedBoostTicks == 0) {
                    paddleSpeedMultiplier = 1.0;
                }
            }

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
            //undestructible walls
            for (Wall w : walls) {
                if (b.getBounds().intersects(w.getBounds())) {
                    // simple bounce logic
                    // check horizontal vs vertical collision
                    Rectangle ballRect = b.getBounds();
                    Rectangle wallRect = w.getBounds();

                    if (ballRect.y + b.height - b.dy <= wallRect.y || ballRect.y - b.dy >= wallRect.y + wallRect.height) {
                        b.dy *= -1; // vertical bounce
                    } else {
                        b.dx *= -1; // horizontal bounce
                    }
                    soundManager.playSFX("ballHit");
                }
            }
            // Wall collisions
            if (b.x <= 0) { b.x = 0; b.dx = Math.abs(b.dx); soundManager.playSFX("ballHit"); }
            if (b.x + b.width >= getWidth()) { b.x = getWidth() - b.width; b.dx = -Math.abs(b.dx); soundManager.playSFX("ballHit"); }
            if (b.y <= 0) { b.y = 0; b.dy = Math.abs(b.dy); soundManager.playSFX("ballHit"); }

            // Paddle collision
            if (b.getBounds().intersects(paddle.getBounds())) {
                soundManager.playSFX("ballHit");
                b.y = paddle.y - b.height - 1;
                // Bounce without losing overall speed: preserve magnitude
                double prevSpeed = Math.sqrt(b.dx * b.dx + b.dy * b.dy);
                // compute new horizontal component based on hit position
                int paddleCenter = paddle.x + paddle.width / 2;
                int ballCenter = b.x + b.width / 2;
                int diff = ballCenter - paddleCenter;
                int newDx = diff / Math.max(1, paddle.width / 8);
                if (newDx == 0) newDx = (new Random().nextBoolean()) ? -2 : 2;
                int newDy = -Math.abs(b.dy);
                // normalize to preserve prevSpeed
                double cur = Math.sqrt(newDx * newDx + newDy * newDy);
                if (cur > 0) {
                    double scale = prevSpeed / cur;
                    double dxD = newDx * scale;
                    double dyD = newDy * scale;
                    b.dx = (int) Math.round(dxD);
                    b.dy = (int) Math.round(dyD);
                    // fallback if rounding made both zero
                    if (b.dx == 0 && b.dy == 0) {
                        b.dy = (int) -Math.max(1, Math.round(prevSpeed));
                    }
                } else {
                    // unlikely, but ensure an upward bounce
                    b.dy = (int) -Math.max(2, (int) Math.round(prevSpeed));
                }
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
                    // spawn power-up if brick had one
                    if (brick.powerUpType != null) {
                        PowerUp.Type t = PowerUp.Type.PADDLE_GROW;
                        if ("PADDLE".equals(brick.powerUpType)) t = PowerUp.Type.PADDLE_GROW;
                        else if ("SPEED".equals(brick.powerUpType)) t = PowerUp.Type.PADDLE_SPEED;
                        else if ("CLONE".equals(brick.powerUpType)) t = PowerUp.Type.BALL_CLONE;

                        powerUps.add(new PowerUp(brick.x + brick.width/2 - 7, brick.y + brick.height/2 - 7, t));
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

        // Level progression: load next level when cleared
        if (bricks.isEmpty()) {
            if (currentLevel < MAX_LEVELS) {
                currentLevel++;
                loadLevel(currentLevel);
                resetPositions();
                soundManager.playSFX("win");
            } else {
                // true game win
                win = true;
                soundManager.playSFX("win");
                timer.stop();
            }
        }

        // Remove balls that fell below
        List<Ball> fallen = new ArrayList<>();
        int clonesFallen = 0;
        for (Ball b : balls) {
            if (b.y > getHeight()) {
                fallen.add(b);
                if (b.isClone) clonesFallen++;
            }
        }
        if (!fallen.isEmpty()) {
            // remove fallen balls
            balls.removeAll(fallen);
            // only decrement lives for non-clone balls
            int nonCloneFalls = fallen.size() - clonesFallen;
            if (nonCloneFalls > 0) {
                lives -= nonCloneFalls;
            }
            if (lives > 0 && !balls.isEmpty()) {
                // keep playing with remaining balls
            } else if (lives > 0 && balls.isEmpty()) {
                // respawn a main ball if none remain
                resetPositions();
            } else {
                if (lives <= 0) {
                    gameOver = true;
                    soundManager.playSFX("lose");
                    timer.stop();
                }
            }
        }

        // Power-up collection: if power-up hits paddle
        List<PowerUp> caught = new ArrayList<>();
        for (PowerUp pu : new ArrayList<>(powerUps)) {
            if (pu.getBounds().intersects(paddle.getBounds())) {
                caught.add(pu);
                applyPowerUp(pu.type);
                soundManager.playSFX("menuClick");
            } else if (pu.y > getHeight()) {
                // missed, remove
                caught.add(pu);
            }
        }
        if (!caught.isEmpty()) powerUps.removeAll(caught);
    }

    private void applyPowerUp(PowerUp.Type type) {
        switch (type) {
            case PADDLE_GROW:
                paddle.applySizeBoost(80);
                break;
            case PADDLE_SPEED:
                paddleSpeedMultiplier = 1.8;
                paddleSpeedBoostTicks = 60 * 5; // ~5 seconds
                break;
            case BALL_CLONE:
                // duplicate all current balls as clones
                List<Ball> clones = new ArrayList<>();
                for (Ball b : new ArrayList<>(balls)) {
                    Ball c = new Ball(b.x + 8, b.y + 8, b.width, true);
                    c.dx = -b.dx;
                    c.dy = b.dy;
                    clones.add(c);
                }
                balls.addAll(clones);
                break;
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
            b.dy = (int) (-Math.abs(speed) * ballSpeedMultiplier);
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
        // initialize level bricks
        loadLevel(currentLevel);

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

    private void loadLevel(int level) {
        bricks.clear();
        int rows = 10;
        int bestcols = 10;

        int brickSize = Math.min(getWidth() / bestcols, 30);
        int brickW = brickSize;
        int brickH = brickSize;
        Color[] palette = {
            new Color(0xB4, 0x4C, 0xE4), // Purple
            new Color(0x5F, 0xA7, 0xFF), // Blue
            new Color(0x79, 0xEB, 0x5C), // Green
            new Color(0xFF, 0xE0, 0x66), // Yellow
            new Color(0xFF, 0x5D, 0x5D)  // Red
        };
        // Choose columns/rows so that bricks are perfect squares and tile across width with no horizontal gaps.
        int boardW = Math.max(getWidth(), getPreferredSize().width);
        int minBrick = 28; // don't make bricks too small
        int maxBrick = 96; // avoid huge bricks
        int bestCols = 0;
        int bestSize = 0;
        // Try columns range 6..20 to find a brick size in range
        for (int colsTry = Math.min(20, Math.max(6, boardW / minBrick)); colsTry >= 4; colsTry--) {
            int size = boardW / colsTry;
            if (size >= minBrick && size <= maxBrick) {
                bestCols = colsTry;
                bestSize = size;
                break;
            }
            // fallback: accept largest possible size >= minBrick
            if (size >= minBrick && bestSize == 0) {
                bestCols = colsTry;
                bestSize = size;
            }
        }
        if (bestCols == 0) {
            bestCols = Math.max(6, boardW / minBrick);
            bestSize = Math.max(minBrick, boardW / bestCols);
        }
        int cols = bestCols;
        brickSize = Math.min(getWidth() / bestcols, 30);
        int brickWidth = brickSize;
        int brickHeight = brickSize;
        int startX = (getWidth() - cols * brickW) / 2; // center horizontally
        int startY = 40;

        switch (level) {
            case 1: // Straight rows
                int rows1 = 5;
                for (int r = 0; r < rows1; r++) {
                    for (int c = 0; c < cols; c++) {
                        Brick br = new Brick(startX + c*brickW, startY + r*brickH, brickW, brickH, palette[r%palette.length]);
                        bricks.add(br);
                    }
                }
                break;
            case 2: // Checkerboard
                int rows2 = 6;
                for (int r = 0; r < rows2; r++) {
                    for (int c = 0; c < cols; c++) {
                        if ((r + c) % 2 == 0) {
                            Brick br = new Brick(startX + c*brickW, startY + r*brickH, brickW, brickH, palette[r%palette.length]);
                            bricks.add(br);
                        }
                    }
                }
                break;
            case 3: // Pyramid
                for (int r = 0; r < 6; r++) {
                    int count = Math.max(1, cols - r*2);
                    int sx = startX + r * (brickW/2);
                    for (int c = 0; c < count; c++) {
                        Brick br = new Brick(sx + c*brickW, startY + r*brickH, brickW, brickH, palette[r%palette.length]);
                        bricks.add(br);
                    }
                }
                break;
            case 4: // Zigzag
                for (int r = 0; r < 6; r++) {
                    for (int c = 0; c < cols; c++) {
                        if ((r % 2 == 0 && c % 3 != 0) || (r % 2 == 1 && c % 3 == 0)) {
                            Brick br = new Brick(startX + c*brickW, startY + r*brickH, brickW, brickH, palette[r%palette.length]);
                            bricks.add(br);
                        }
                    }
                }
                break;
            case 5: // Hollow rectangle
                int rows5 = 6;
                for (int r = 0; r < rows5; r++) {
                    for (int c = 0; c < cols; c++) {
                        if (r == 0 || r == rows5-1 || c == 0 || c == cols-1) {
                            Brick br = new Brick(startX + c*brickW, startY + r*brickH, brickW, brickH, palette[r%palette.length]);
                            bricks.add(br);
                        }
                    }
                }
                break;
            case 6: // Random scattered
                Random rnd = new Random(12345 + level);
                for (int r = 0; r < 7; r++) {
                    for (int c = 0; c < cols; c++) {
                        if (rnd.nextDouble() < 0.45) {
                            Brick br = new Brick(startX + c*brickW, startY + r*brickH, brickW, brickH, palette[r%palette.length]);
                            bricks.add(br);
                        }
                    }
                }
                break;
            case 7: // Diamond pattern with internal walls
                int midCol = cols / 2;
                int rows7 = 7;


                bricks.clear();
                walls.clear();



                for (int r = 0; r < rows7; r++) {
                    int start = midCol - r;
                    int end = midCol + r;
                    for (int c = start; c <= end; c++) {
                        if (c >= 0 && c < cols) {
                            Brick br = new Brick(startX + c * brickW, startY + r * brickH, brickW, brickH, palette[r % palette.length]);
                            bricks.add(br);
                        }
                    }
                }

            case 8:
                int[][] heart = {
                        {0,1,1,0,1,1,0},
                        {1,1,1,1,1,1,1},
                        {1,1,1,1,1,1,1},
                        {0,1,1,1,1,1,0},
                        {0,0,1,1,1,0,0},
                        {0,0,0,1,0,0,0}
                };
                for (int row = 0; row < heart.length; row++) {
                    for (int col = 0; col < heart[row].length; col++) {
                        if (heart[row][col] == 1) {
                            bricks.add(new Brick(
                                    startX + col * brickWidth,
                                    startY + row * brickHeight,
                                    brickWidth,
                                    brickHeight,
                                    palette[row % palette.length]
                            ));


                        }
                    }
                }
                break;


            case 9:
                for (int row = 0; row < 8; row++) {
                    for (int col = 0; col < cols; col++) {
                        int wave = (int)(Math.sin(col * 0.6) * 2 + 3);
                        if (wave == row) {
                            bricks.add(new Brick(
                                    startX + col * brickWidth,
                                    startY + row * brickHeight,
                                    brickWidth,
                                    brickHeight,
                                    palette[row % palette.length]
                            ));

                        }
                    }
                }
                break;

// ---------------- LEVEL 10: CROSS SHAPE ----------------
            case 10:
                int mid = cols / 2;
                for (int row = 0; row < 9; row++) {
                    for (int col = 0; col < cols; col++) {
                        if (col == mid || row == 4) {
                            bricks.add(new Brick(
                                    startX + col * brickWidth,
                                    startY + row * brickHeight,
                                    brickWidth,
                                    brickHeight,
                                    palette[row % palette.length]
                            ));



                        }
                    }
                }
                break;
            default:
                loadLevel(1); // fallback to level 1
                return;



                for (int r = 1; r < rows7 - 1; r++) {
                    Wall middleWall = new Wall(startX + midCol * brickW, startY + r * brickH, brickW, brickH, Color.GRAY);
                    walls.add(middleWall);
                }


                int midRow = rows7 / 2;
                for (int c = midCol - 3; c <= midCol + 3; c++) {
                    Wall horizontalWall = new Wall(startX + c * brickW, startY + midRow * brickH, brickW, brickH, Color.GRAY);
                    walls.add(horizontalWall);
                }

                for (int r = 0; r < rows7; r++) {
                    Wall leftWall = new Wall(startX + 0*brickW, startY + r*brickH, brickW, brickH, Color.GRAY);
                    Wall rightWall = new Wall(startX + (cols-1)*brickW, startY + r*brickH, brickW, brickH, Color.GRAY);
                    walls.add(leftWall);
                    walls.add(rightWall);
                }
                break;
        }


        Random rnd = new Random();
        if (!bricks.isEmpty()) {
            int powerCount = Math.min(3, Math.max(1, bricks.size() / 20));
            for (int i = 0; i < powerCount; i++) {
                Brick b = bricks.get(rnd.nextInt(bricks.size()));
                String t = (i % 3 == 0) ? "PADDLE" : (i % 3 == 1) ? "SPEED" : "CLONE";
                b.setPowerUp(t);
            }
        }

    }

    public void selectLevel(int level) {
        if (level < 1) level = 1;
        if (level > MAX_LEVELS) level = MAX_LEVELS;
        currentLevel = level;
        loadLevel(level);
        resetPositions();
        if (!timer.isRunning()) timer.start();
        soundManager.playBackgroundMusic();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);


    g2.setColor(new Color(0x0D, 0x1B, 0x2A));
    g2.fillRect(0, 0, getWidth(), getHeight());

        for (Brick brick : bricks) {
            brick.draw(g2);
        }


        for (Wall w : walls) {
            w.draw(g2);
        }


        for (PowerUp pu : powerUps) pu.draw(g2);


    for (Ball b : balls) b.draw(g2);
    paddle.draw(g2);


    g2.setColor(Color.WHITE);
    g2.setFont(new Font("Arial", Font.BOLD, 14));
    String hud = "Score: " + score;
    g2.drawString(hud, 12, 20);
    String life = "Lives: " + lives;
    int lw = g2.getFontMetrics().stringWidth(life);
    g2.drawString(life, getWidth() - lw - 12, 20);


    String levelStr = "Level: " + currentLevel;
    int lvlw = g2.getFontMetrics().stringWidth(levelStr);
    g2.drawString(levelStr, (getWidth() - lvlw) / 2, 20);


        int hudY = 40;
        int hudX = 12;
        if (paddle.isBoosted()) {
            g2.setColor(new Color(0x4C, 0xFF, 0x79));
            g2.fillRoundRect(hudX - 6, hudY - 14, 140, 20, 8, 8);
            g2.setColor(Color.DARK_GRAY);
            g2.drawRoundRect(hudX - 6, hudY - 14, 140, 20, 8, 8);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.PLAIN, 12));
            g2.drawString("Paddle: Enlarged", hudX, hudY);
            hudY += 22;
        }

        if (paddleSpeedBoostTicks > 0) {
            int secs = Math.max(1, paddleSpeedBoostTicks / 60);
            g2.setColor(new Color(0xFF, 0xC1, 0x4C));
            g2.fillRoundRect(hudX - 6, hudY - 14, 160, 20, 8, 8);
            g2.setColor(Color.DARK_GRAY);
            g2.drawRoundRect(hudX - 6, hudY - 14, 160, 20, 8, 8);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.PLAIN, 12));
            g2.drawString("Paddle Speed: " + secs + "s", hudX, hudY);
            hudY += 22;
        }


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


    public void cleanup() {
        soundManager.cleanup();
    }
}
