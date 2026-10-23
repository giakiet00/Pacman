package util;

/**
 * Game constants and utility methods. Contains all configurable values and
 * level-based calculations.
 *
 * @author M4N
 */
public class Constants {

    /**
     * Size of each tile in pixels
     */
    public static final int TILE_SIZE = 24;

    /**
     * Number of columns in the maze
     */
    public static final int COLS = 21;

    /**
     * Number of rows in the maze
     */
    public static final int ROWS = 23;

    /**
     * Screen width in pixels
     */
    public static final int SCREEN_WIDTH = COLS * TILE_SIZE; // 504 px

    /**
     * Screen height in pixels (including HUD space)
     */
    public static final int SCREEN_HEIGHT = ROWS * TILE_SIZE + 60; // + HUD

    /**
     * Pacman's movement speed (pixels per frame)
     */
    public static final int PACMAN_SPEED = 2;

    /**
     * Base ghost movement speed (pixels per frame)
     */
    public static final int GHOST_SPEED = 1;

    /**
     * Delay between frames in milliseconds (~60 FPS)
     */
    public static final int FRAME_DELAY = 16;

    /**
     * Default duration of frightened mode in milliseconds
     */
    public static final int FRIGHTENED_DURATION = 7000;

    /**
     * Score awarded for eating a small dot
     */
    public static final int SCORE_DOT = 10;

    /**
     * Score awarded for eating a power pellet
     */
    public static final int SCORE_PELLET = 50;

    /**
     * Score awarded for eating a ghost in frightened mode
     */
    public static final int SCORE_GHOST = 200;

    /**
     * Number of lives Pacman starts with
     */
    public static final int INITIAL_LIVES = 3;

    /**
     * Maximum number of levels in the game
     */
    public static final int MAX_LEVEL = 50;

    /**
     * Change maze layout every this number of levels
     */
    public static final int LEVELS_PER_LAYOUT = 5;

    /**
     * Returns ghost speed for the given level. Ghosts become faster starting
     * from certain positions in the layout cycle.
     *
     * @param level current level
     * @return ghost speed
     */
    public static int ghostSpeedForLevel(int level) {
        int pos = ((level - 1) % LEVELS_PER_LAYOUT) + 1; // 1..5
        if (pos >= 4) {
            return 2;
        }
        return 1;
    }

    /**
     * Checks if ghost should take an extra step on this frame (for speed
     * variation).
     *
     * @param level current level
     * @param frameCount current frame count
     * @return true if ghost should move extra step
     */
    public static boolean ghostExtraStep(int level, long frameCount) {
        int pos = ((level - 1) % LEVELS_PER_LAYOUT) + 1;
        return (pos == 3) && (frameCount % 2 == 0);
    }

    /**
     * Returns frightened mode duration based on level. Duration decreases as
     * level increases.
     *
     * @param level current level
     * @return frightened duration in milliseconds
     */
    public static int frightenedDuration(int level) {
        int pos = ((level - 1) % LEVELS_PER_LAYOUT) + 1;
        if (pos >= 5) {
            return 3000;
        }
        if (pos >= 4) {
            return 5000;
        }
        return 7000;
    }

    /**
     * Returns layout index (0..9) based on current level. Layout changes every
     * 5 levels.
     *
     * @param level current level
     * @return layout index to use
     */
    public static int layoutIndexForLevel(int level) {
        return ((level - 1) / LEVELS_PER_LAYOUT) % 10;
    }
}
