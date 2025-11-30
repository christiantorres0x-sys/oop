import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Main {
    public static void main(String[] args) {
        JFrame frame = new JFrame("Brick Breaker");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        GameBoard board = new GameBoard();
        board.setBounds(0,0,500,500);

        // Use a layered pane so we can show a menu overlay above the game
        JLayeredPane layered = new JLayeredPane();
        layered.setPreferredSize(new Dimension(500,500));
        layered.add(board, JLayeredPane.DEFAULT_LAYER);

        // Main menu overlay (menu will snapshot the board for blur)
        MainMenu menu = new MainMenu(500, 500, board, () -> {
            // start the game when pressing Start
            board.startGame();
            board.requestFocusInWindow();
        });
        layered.add(menu, JLayeredPane.PALETTE_LAYER);

        frame.setContentPane(layered);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);

        // Clean up resources on exit
        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                board.cleanup();
            }
        });

        // Request focus after the frame is visible so the panel receives key events
        SwingUtilities.invokeLater(() -> {
            menu.showMenu();
            menu.requestFocusInWindow();
        });

        // Toggle menu with Escape: pause/resume
        JRootPane root = frame.getRootPane();
        InputMap rim = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap ram = root.getActionMap();
        rim.put(KeyStroke.getKeyStroke("ESCAPE"), "toggle.menu");
        ram.put("toggle.menu", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (menu.isVisible()) {
                    menu.hideMenu();
                    board.resumeGame();
                    board.requestFocusInWindow();
                } else {
                    // show menu and pause
                    menu.showMenu();
                    board.pauseGame();
                }
            }
        });
    }
}
