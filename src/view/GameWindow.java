package view;

import javax.swing.*;

/**
 * Main game window (JFrame) that hosts the GamePanel. Placed in the view
 * package as it is a UI component.
 *
 * Responsibilities: create the JFrame, attach the GamePanel, and display the
 * window.
 *
 * @author M4N
 */
public class GameWindow extends JFrame {

    /**
     * Sets up and displays the game window. Configures the title, size, close
     * behavior, and keyboard focus.
     */
    public GameWindow() {
        setTitle("PAC-MAN | FPT University - CSD201 - Group4"); // window title bar text
        setResizable(false);                                      // prevent user from resizing the window
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);          // exit the program when the window is closed

        GamePanel gamePanel = new GamePanel();
        add(gamePanel);         // attach the game panel as the main content

        pack();                          // auto-size the window to fit the panel's preferred size
        setLocationRelativeTo(null);     // center the window on the screen
        gamePanel.requestFocusInWindow(); // ensure the panel receives keyboard input immediately
    }
}
