package model;

import util.Constants;
import java.awt.*;

/**
 * Pacman class - Represents the player character. Pacman's speed is constant
 * (PACMAN_SPEED), only ghosts get faster with levels.
 *
 * @author M4N
 */
public class Pacman {

    private int x, y;
    private int tileCol, tileRow;
    private Direction currentDir;
    private Direction nextDir;
    private int mouthAngle;
    private int mouthDelta;
    private boolean alive;
    private static final int RADIUS = Constants.TILE_SIZE / 2 - 1;

    /**
     * Constructor - Creates a new Pacman.
     */
    public Pacman() {
        reset();
    }

    /**
     * Resets Pacman to starting position and default state.
     */
    public void reset() {
        tileCol = 10;
        tileRow = 16;
        x = tileCol * Constants.TILE_SIZE;
        y = tileRow * Constants.TILE_SIZE;
        currentDir = Direction.LEFT;
        nextDir = Direction.LEFT;
        mouthAngle = 45;
        mouthDelta = -3;
        alive = true;
    }

    /**
     * Updates Pacman's position, direction, and mouth animation.
     *
     * @param board the game board
     */
    public void update(Board board) {
        if (!alive) {
            return;
        }
        int ts = Constants.TILE_SIZE;
        boolean alignedX = (x % ts == 0);
        boolean alignedY = (y % ts == 0);
        boolean aligned = alignedX && alignedY;

        // Change direction only when perfectly aligned with a tile
        if (aligned) {
            tileCol = x / ts;
            tileRow = y / ts;
            if (nextDir != currentDir && canMoveFromTile(nextDir, tileRow, tileCol, board)) {
                currentDir = nextDir;
            }
        }

        // Move if possible
        if (canMove(currentDir, board)) {
            x += currentDir.dx * Constants.PACMAN_SPEED;
            y += currentDir.dy * Constants.PACMAN_SPEED;
            tileCol = (x + RADIUS) / ts;
            tileRow = (y + RADIUS) / ts;
        }

        // Screen wrap-around (tunnel effect)
        if (x < -ts) {
            x = board.cols() * ts;
        }
        if (x > board.cols() * ts) {
            x = -ts;
        }

        // Mouth animation
        mouthAngle += mouthDelta;
        if (mouthAngle <= 0) {
            mouthAngle = 0;
            mouthDelta = 3;
        }
        if (mouthAngle >= 45) {
            mouthAngle = 45;
            mouthDelta = -3;
        }
    }

    /**
     * Checks if Pacman can move in a direction from a specific tile.
     */
    private boolean canMoveFromTile(Direction dir, int row, int col, Board board) {
        int nr = row + dir.dy;
        int nc = col + dir.dx;
        return !board.isWall(nr, nc);
    }

    /**
     * Checks if Pacman can move in the current direction without hitting a
     * wall.
     */
    private boolean canMove(Direction dir, Board board) {
        if (dir == Direction.NONE) {
            return false;
        }
        int ts = Constants.TILE_SIZE;
        int speed = Constants.PACMAN_SPEED;
        int margin = 1;
        int nx = x + dir.dx * speed;
        int ny = y + dir.dy * speed;
        int col1, row1, col2, row2;
        switch (dir) {
            case UP:
                col1 = (nx + margin) / ts;
                row1 = ny / ts;
                col2 = (nx + ts - margin - 1) / ts;
                row2 = row1;
                break;
            case DOWN:
                col1 = (nx + margin) / ts;
                row1 = (ny + ts - 1) / ts;
                col2 = (nx + ts - margin - 1) / ts;
                row2 = row1;
                break;
            case LEFT:
                col1 = nx / ts;
                row1 = (ny + margin) / ts;
                col2 = col1;
                row2 = (ny + ts - margin - 1) / ts;
                break;
            default: // RIGHT
                col1 = (nx + ts - 1) / ts;
                row1 = (ny + margin) / ts;
                col2 = col1;
                row2 = (ny + ts - margin - 1) / ts;
                break;
        }

        // Wrap column indices around the board width so that stepping past the
        // left/right edge (the tunnel) checks the matching column on the
        // opposite side instead of being treated as an out-of-bounds wall.
        // Without this, Pacman gets permanently stuck a few pixels past the
        // edge: canMove() blocks further movement before x reaches the
        // wrap-around threshold in update(), and since that stuck position is
        // no longer tile-aligned, direction changes stop being evaluated too -
        // Pacman freezes in the tunnel entrance and never comes back out.
        int cols = board.cols();
        col1 = ((col1 % cols) + cols) % cols;
        col2 = ((col2 % cols) + cols) % cols;

        return !board.isWall(row1, col1) && !board.isWall(row2, col2);
    }

    /**
     * Draws Pacman with animated mouth based on current direction.
     *
     * @param g Graphics context
     */
    public void draw(Graphics g) {
        if (!alive) {
            return;
        }
        int startAngle;
        switch (currentDir) {
            case RIGHT:
                startAngle = mouthAngle;
                break;
            case LEFT:
                startAngle = 180 + mouthAngle;
                break;
            case UP:
                startAngle = 90 + mouthAngle;
                break;
            case DOWN:
                startAngle = 270 + mouthAngle;
                break;
            default:
                startAngle = mouthAngle;
                break;
        }
        int sweepAngle = 360 - 2 * mouthAngle;
        g.setColor(Color.YELLOW);
        g.fillArc(x + 1, y + 1,
                Constants.TILE_SIZE - 2, Constants.TILE_SIZE - 2,
                startAngle, sweepAngle);
    }

    // ====================== GETTERS AND SETTERS ======================
    /**
     * @return current x position
     */
    public int getX() {
        return x;
    }

    /**
     * @return current y position
     */
    public int getY() {
        return y;
    }

    /**
     * @return current tile column
     */
    public int getTileCol() {
        return tileCol;
    }

    /**
     * @return current tile row
     */
    public int getTileRow() {
        return tileRow;
    }

    /**
     * @return center x coordinate
     */
    public int getCenterX() {
        return x + Constants.TILE_SIZE / 2;
    }

    /**
     * @return center y coordinate
     */
    public int getCenterY() {
        return y + Constants.TILE_SIZE / 2;
    }

    /**
     * Sets the next direction for Pacman.
     *
     * @param d the direction to change to
     */
    public void setNextDir(Direction d) {
        this.nextDir = d;
    }

    /**
     * @return current direction
     */
    public Direction getCurrentDir() {
        return currentDir;
    }

    /**
     * @return next direction
     */
    public Direction getNextDir() {
        return nextDir;
    }

    /**
     * @return true if Pacman is alive
     */
    public boolean isAlive() {
        return alive;
    }

    /**
     * Sets Pacman's alive status.
     *
     * @param alive true if alive, false if dead
     */
    public void setAlive(boolean alive) {
        this.alive = alive;
    }
}
