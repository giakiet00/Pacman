package model;

/**
 * Enum Direction - Defines all possible movement directions in the game. Placed
 * in the model package as it is core game data used by Pac-Man and ghosts.
 *
 * Each constant holds a dx/dy offset representing one tile step in that
 * direction.
 *
 * @author M4N
 */
public enum Direction {

    /**
     * Up: row decreases
     */
    UP(0, -1),
    /**
     * Down: row increases
     */
    DOWN(0, 1),
    /**
     *
     */
    LEFT(-1, 0),
    /**
     * Left: column decreases
     */
    RIGHT(1, 0),
    /**
     * Right: column increases
     */
    NONE(0, 0);   // No movement

    /**
     * Change along the X axis (column).
     */
    public final int dx;

    /**
     * Change along the Y axis (row).
     */
    public final int dy;

    /**
     * Constructs a direction with the given axis offsets.
     *
     * @param dx column delta (-1, 0, or 1)
     * @param dy row delta (-1, 0, or 1)
     */
    Direction(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    /**
     * Returns the opposite of this direction. Example: UP.opposite() returns
     * DOWN. NONE returns NONE.
     *
     * @return the opposite direction
     */
    public Direction opposite() {
        switch (this) {
            case UP:
                return DOWN;
            case DOWN:
                return UP;
            case LEFT:
                return RIGHT;
            case RIGHT:
                return LEFT;
            default:
                return NONE; // NONE has no opposite
        }
    }
}
