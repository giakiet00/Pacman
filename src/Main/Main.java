package Main;


import javax.swing.SwingUtilities;
import view.GameWindow;

/**
 * Main class of the Pacman game.
 *
 * This is the entry point of the application. It creates and displays the main
 * game window.
 *
 * @author M4N
 */
public class Main {

    /**
     * Starts the Pacman game.
     *
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {

        // Run the game on the Swing Event Dispatch Thread (EDT).
        // This ensures that all Swing components are created safely.
        SwingUtilities.invokeLater(() -> {

            // Create the main game window.
            GameWindow window = new GameWindow();

            // Display the window on the screen.
            window.setVisible(true);
        });
    }
}
