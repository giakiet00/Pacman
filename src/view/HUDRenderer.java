package view;

import model.Direction;
import util.Constants;
import java.awt.*;

/**
 * Renders the HUD (Heads-Up Display) strip shown below the maze.
 *
 * The HUD is divided into three columns: Left - current score Center - high
 * score and elapsed time Right - current level and remaining lives
 *
 * A directional pad is also drawn on the far right to show Pac-Man's current
 * and queued movement direction.
 *
 * @author M4N
 */
public class HUDRenderer {

    /**
     * Draws the full HUD below the game board.
     *
     * @param g the Graphics context
     * @param score current player score
     * @param highScore highest score achieved
     * @param level current level number
     * @param lives remaining player lives
     * @param width total screen width in pixels
     * @param rows number of tile rows on the board (used to find HUD Y
     * position)
     * @param currentDir Pac-Man's active movement direction
     * @param nextDir Pac-Man's queued next direction
     * @param elapsedMs total milliseconds elapsed during active gameplay
     */
    public void draw(Graphics g, int score, int highScore, int level,
            int lives, int width, int rows,
            Direction currentDir, Direction nextDir,
            long elapsedMs) {

        int hudY = rows * Constants.TILE_SIZE; // HUD starts immediately below the board
        int hudH = Constants.SCREEN_HEIGHT - hudY; // height of the HUD strip
        int col = width / 3;                 // width of each of the three columns

        // Draw black background for the HUD strip
        g.setColor(Color.BLACK);
        g.fillRect(0, hudY, width, hudH);

        // Separator line between board and HUD
        g.setColor(new Color(60, 60, 60));
        g.drawLine(0, hudY, width, hudY);

        int labelY = hudY + 16; // Y position for column header labels
        int valueY = hudY + 35; // Y position for column value text

        // ── Left column: SCORE ────────────────────────────────────────────────
        g.setFont(new Font("Arial", Font.BOLD, 11));
        g.setColor(Color.WHITE);
        g.drawString("SCORE", 8, labelY);          // label

        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.setColor(Color.YELLOW);
        g.drawString(String.valueOf(score), 8, valueY); // score value

        // ── Center column: HIGH SCORE + elapsed timer ─────────────────────────
        int centerX = col + col / 2; // horizontal center of the middle column

        g.setFont(new Font("Arial", Font.BOLD, 11));
        g.setColor(Color.WHITE);
        drawCenteredAt(g, "HIGH SCORE", centerX, labelY); // label

        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.setColor(new Color(255, 210, 0));
        drawCenteredAt(g, String.valueOf(highScore), centerX, valueY); // high score value

        // Elapsed time formatted as m:ss, shown below high score
        long secs = elapsedMs / 1000;
        String timeStr = String.format("%d:%02d", secs / 60, secs % 60);
        g.setFont(new Font("Arial", Font.PLAIN, 11));
        g.setColor(new Color(140, 200, 255));
        drawCenteredAt(g, "[T] " + timeStr, centerX, hudY + 52); // timer with clock prefix

        // ── Right column: LEVEL + lives ───────────────────────────────────────
        int rightX = col * 2 + 8; // left edge of the right column

        g.setFont(new Font("Arial", Font.BOLD, 11));
        g.setColor(Color.WHITE);
        g.drawString("LVL " + level + "/" + Constants.MAX_LEVEL, rightX, labelY); // level progress

        // Draw one Pac-Man icon per remaining life
        int iconSize = 16, iconGap = 20, iconY = hudY + 20;
        g.setColor(Color.YELLOW);
        for (int i = 0; i < lives; i++) {
            g.fillArc(rightX + i * iconGap, iconY, iconSize, iconSize, 30, 300); // Pac-Man silhouette
        }

        // ── Direction pad (far right) ─────────────────────────────────────────
        // Shows 4 arrow buttons; highlights current direction and queued next direction
        int padCx = width - 45;       // center X of the d-pad
        int padCy = hudY + hudH / 2;  // center Y of the d-pad
        int btnSize = 16, gap = 2;      // button size and spacing

        drawDirButton(g, Direction.UP, padCx, padCy - btnSize - gap, btnSize, currentDir, nextDir);
        drawDirButton(g, Direction.DOWN, padCx, padCy + gap, btnSize, currentDir, nextDir);
        drawDirButton(g, Direction.LEFT, padCx - btnSize - gap, padCy - btnSize / 2, btnSize, currentDir, nextDir);
        drawDirButton(g, Direction.RIGHT, padCx + btnSize + gap, padCy - btnSize / 2, btnSize, currentDir, nextDir);
    }

    /**
     * Draws a single direction button on the d-pad. The button is highlighted
     * yellow if it is the queued next direction, or outlined in cyan if it is
     * the current active direction.
     *
     * @param g the Graphics context
     * @param dir the direction this button represents
     * @param x top-left X position of the button
     * @param y top-left Y position of the button
     * @param size width and height of the button in pixels
     * @param currentDir Pac-Man's current movement direction
     * @param nextDir Pac-Man's queued next direction
     */
    private void drawDirButton(Graphics g, Direction dir, int x, int y, int size,
            Direction currentDir, Direction nextDir) {

        boolean isNext = (dir == nextDir);    // this button is the queued direction
        boolean isCurrent = (dir == currentDir); // this button is the active direction

        // Fill color: yellow if queued, dark grey if current, darker grey otherwise
        g.setColor(isNext ? new Color(255, 220, 0)
                : isCurrent ? new Color(80, 80, 80)
                        : new Color(45, 45, 45));
        g.fillRoundRect(x, y, size, size, 5, 5);

        // Border color: cyan if current, dark yellow if queued, grey otherwise
        g.setColor(isCurrent ? new Color(0, 200, 255)
                : isNext ? new Color(200, 160, 0)
                        : new Color(90, 90, 90));
        g.drawRoundRect(x, y, size, size, 5, 5);

        // Arrow color: black on yellow (queued), cyan (current), or light grey (inactive)
        Color arrowColor = isNext ? Color.BLACK
                : isCurrent ? new Color(0, 200, 255)
                        : new Color(140, 140, 140);
        drawArrow(g, dir, x, y, size, arrowColor);
    }

    /**
     * Draws a filled triangle arrow inside a button pointing in the given
     * direction.
     *
     * @param g the Graphics context
     * @param dir the direction the arrow should point
     * @param bx top-left X of the button bounding box
     * @param by top-left Y of the button bounding box
     * @param size button size in pixels
     * @param c arrow fill color
     */
    private void drawArrow(Graphics g, Direction dir, int bx, int by, int size, Color c) {
        g.setColor(c);
        int m = size / 5;         // inner margin from button edge
        int cx = bx + size / 2;    // horizontal center of button
        int cy = by + size / 2;    // vertical center of button
        int hs = size / 2 - m;     // half-span of the arrow base

        int[] xs, ys; // triangle vertex arrays

        switch (dir) {
            case UP: // triangle pointing up
                xs = new int[]{cx, cx - hs, cx + hs};
                ys = new int[]{by + m, by + size - m, by + size - m};
                break;
            case DOWN: // triangle pointing down
                xs = new int[]{cx, cx - hs, cx + hs};
                ys = new int[]{by + size - m, by + m, by + m};
                break;
            case LEFT: // triangle pointing left
                xs = new int[]{bx + m, bx + size - m, bx + size - m};
                ys = new int[]{cy, cy - hs, cy + hs};
                break;
            default: // RIGHT: triangle pointing right
                xs = new int[]{bx + size - m, bx + m, bx + m};
                ys = new int[]{cy, cy - hs, cy + hs};
                break;
        }

        g.fillPolygon(xs, ys, 3); // draw the filled triangle
    }

    /**
     * Draws a string horizontally centered at the given X coordinate and Y
     * baseline.
     *
     * @param g the Graphics context
     * @param s the string to draw
     * @param cx the horizontal center position
     * @param y the Y coordinate of the text baseline
     */
    private void drawCenteredAt(Graphics g, String s, int cx, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, cx - fm.stringWidth(s) / 2, y);
    }
}
