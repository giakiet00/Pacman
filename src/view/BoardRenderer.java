package view;

import model.Board;
import util.Constants;
import java.awt.*;

/**
 * BoardRenderer class - Responsible for rendering the maze and items. Placed in
 * the view package as it is purely for display and contains no game logic.
 *
 * Color scheme: - Wall (1): Dark blue with lighter border - Small dot (0):
 * Light yellow - Power pellet (2): White, blinking every 300ms - Empty (3):
 * Nothing is drawn
 *
 * @author M4N
 */
public class BoardRenderer {

    /**
     * Draws the entire board on the graphics context. Called every frame from
     * GamePanel.
     *
     * @param g Graphics context to draw on
     * @param board the board data to render
     */
    public void draw(Graphics g, Board board) {
        int ts = Constants.TILE_SIZE;
        for (int r = 0; r < board.rows(); r++) {
            for (int c = 0; c < board.cols(); c++) {
                int cell = board.getCell(r, c);
                int px = c * ts;
                int py = r * ts;

                switch (cell) {
                    case 1:
                        drawWall(g, px, py, ts);
                        break;
                    case 0:
                        drawDot(g, px, py, ts);
                        break;
                    case 2:
                        drawPowerPellet(g, px, py, ts);
                        break;
                    // case 3: empty space - draw nothing
                }
            }
        }
    }

    /**
     * Draws a wall tile: dark blue background with lighter border.
     */
    private void drawWall(Graphics g, int px, int py, int ts) {
        g.setColor(new Color(0, 0, 170));
        g.fillRect(px, py, ts, ts);
        g.setColor(new Color(100, 100, 255)); // Lighter border
        g.drawRect(px + 1, py + 1, ts - 2, ts - 2);
    }

    /**
     * Draws a small dot in the center of the tile.
     */
    private void drawDot(Graphics g, int px, int py, int ts) {
        g.setColor(new Color(255, 220, 150));
        int dot = 4;
        g.fillOval(
                px + ts / 2 - dot / 2,
                py + ts / 2 - dot / 2,
                dot, dot
        );
    }

    /**
     * Draws a power pellet (large dot) that blinks every 300ms.
     */
    private void drawPowerPellet(Graphics g, int px, int py, int ts) {
        long t = System.currentTimeMillis();
        if ((t / 300) % 2 == 0) { // Blink every 300ms
            g.setColor(Color.WHITE);
            int pellet = 10;
            g.fillOval(
                    px + ts / 2 - pellet / 2,
                    py + ts / 2 - pellet / 2,
                    pellet, pellet
            );
        }
    }
}
