package model;

import util.Constants;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

/**
 * Represents a ghost enemy with individual AI behavior matching the original
 * Pac-Man arcade game.
 *
 * Ghost behaviors in CHASE mode: BLINKY (red) - Targets Pac-Man's current tile
 * directly. PINKY (pink) - Targets 4 tiles ahead of Pac-Man (ambush) + original
 * overflow bug when facing UP. INKY (cyan) - Targets the point reflected
 * through Blinky from 2 tiles ahead of Pac-Man. CLYDE (orange)- Chases like
 * Blinky if far (>8 tiles), retreats to scatter corner if close.
 *
 * Chase/Scatter cycle (based on original Level 1 timing): SCATTER 7s -> CHASE
 * 20s -> SCATTER 7s -> CHASE 20s -> SCATTER 5s -> CHASE 20s -> SCATTER 5s ->
 * CHASE forever
 *
 * @author M4N
 */
public class Ghost {

    /**
     * All possible states a ghost can be in at any time.
     */
    public enum GhostState {
        /**
         * Actively hunting Pac-Man using individual AI targeting.
         */
        CHASE,
        /**
         * Vulnerable after Pac-Man eats a power pellet; moves randomly.
         */
        FRIGHTENED,
        /**
         * Just eaten by Pac-Man; travels back to the ghost house to respawn.
         */
        EATEN,
        /**
         * Retreating to a fixed corner of the maze.
         */
        SCATTER
    }

    /**
     * Ghost name constants used to identify each ghost's AI behavior.
     */
    public static final String BLINKY = "Blinky";

    /**
     *
     */
    public static final String PINKY = "Pinky";

    /**
     *
     */
    public static final String INKY = "Inky";

    /**
     *
     */
    public static final String CLYDE = "Clyde";

    // Scatter corner targets for each ghost (tile col, tile row)
    private static final int[] SCATTER_BLINKY = {18, 0};
    private static final int[] SCATTER_PINKY = {2, 0};
    private static final int[] SCATTER_INKY = {18, 22};
    private static final int[] SCATTER_CLYDE = {2, 22};

    /**
     * Duration of each Chase/Scatter phase in milliseconds, matching original
     * Level 1 timing. Order: scatter, chase, scatter, chase, scatter, chase,
     * scatter, chase (forever).
     */
    private static final long[] PHASE_DURATIONS = {
        7000, 20000,
        7000, 20000,
        5000, 20000,
        5000, Long.MAX_VALUE // final chase phase lasts indefinitely
    };

    /**
     * Whether each phase index is a SCATTER phase (true) or CHASE phase
     * (false). Parallel array to PHASE_DURATIONS.
     */
    private static final boolean[] PHASE_IS_SCATTER = {
        true, false,
        true, false,
        true, false,
        true, false
    };

    /**
     * Distance threshold (in tiles) used by Clyde to switch between chase and
     * scatter.
     */
    private static final double CLYDE_THRESHOLD = 8.0;

    /**
     * Pixel position of the ghost on screen.
     */
    private int x, y;

    /**
     * Current tile position of the ghost on the board.
     */
    private int tileCol, tileRow;

    /**
     * Starting tile position used when resetting after death.
     */
    private final int startCol, startRow;

    /**
     * Current movement direction.
     */
    private Direction currentDir;

    /**
     * Display color of this ghost.
     */
    private final Color color;

    /**
     * Current behavioral state.
     */
    private GhostState state;

    /**
     * Remaining time in FRIGHTENED state, in milliseconds.
     */
    private long frightenedTimer;

    /**
     * Index into PHASE_DURATIONS / PHASE_IS_SCATTER for the current
     * Chase/Scatter phase.
     */
    private int phaseIndex = 0;

    /**
     * Time elapsed in the current Chase/Scatter phase, in milliseconds.
     */
    private long phaseTimer = 0;

    /**
     * Random number generator used for movement in FRIGHTENED state.
     */
    private final Random random = new Random();

    /**
     * Name of this ghost (used to select AI behavior).
     */
    private final String name;

    /**
     * Frame counter used to calculate speed adjustments per level.
     */
    private long frameCount = 0;

    /**
     * Reference to Blinky, required by Inky's targeting calculation.
     */
    private Ghost blinky = null;

    /**
     * Constructs a ghost with a given starting position, color, and name.
     *
     * @param startCol starting tile column
     * @param startRow starting tile row
     * @param color display color of this ghost
     * @param name ghost name (use the BLINKY/PINKY/INKY/CLYDE constants)
     */
    public Ghost(int startCol, int startRow, Color color, String name) {
        this.startCol = startCol;
        this.startRow = startRow;
        this.color = color;
        this.name = name;
        reset();
    }

    /**
     * Sets the Blinky reference required for Inky's targeting logic. Must be
     * called on Inky before the game starts.
     *
     * @param blinky the Blinky ghost instance
     */
    public void setBlinky(Ghost blinky) {
        this.blinky = blinky;
    }

    /**
     * Resets this ghost to its starting position and initial state. Called at
     * game start and after Pac-Man dies.
     */
    public void reset() {
        tileCol = startCol;
        tileRow = startRow;
        x = tileCol * Constants.TILE_SIZE;
        y = tileRow * Constants.TILE_SIZE;
        currentDir = Direction.LEFT;
        state = GhostState.SCATTER; // ghosts begin in SCATTER, matching the original game
        frightenedTimer = 0;
        phaseIndex = 0;
        phaseTimer = 0;
        frameCount = 0;
    }

    /**
     * Updates the ghost's AI state and moves it on the board. Called once per
     * game tick.
     *
     * @param board the current game board
     * @param pacman the player character
     * @param deltaMs time elapsed since the last tick in milliseconds
     * @param level current game level (affects ghost speed)
     */
    public void update(Board board, Pacman pacman, long deltaMs, int level) {
        frameCount++;

        // Advance the Chase/Scatter phase timer (only when not FRIGHTENED or EATEN)
        if (state != GhostState.FRIGHTENED && state != GhostState.EATEN) {
            phaseTimer += deltaMs;
            if (phaseTimer >= PHASE_DURATIONS[phaseIndex]) {
                phaseTimer = 0;
                if (phaseIndex < PHASE_DURATIONS.length - 1) {
                    phaseIndex++; // advance to next phase
                }
                currentDir = currentDir.opposite(); // reverse direction on phase change (original behavior)
            }
            // Sync state to the current phase
            state = PHASE_IS_SCATTER[phaseIndex] ? GhostState.SCATTER : GhostState.CHASE;
        }

        // Count down FRIGHTENED timer and return to normal when it expires
        if (state == GhostState.FRIGHTENED) {
            frightenedTimer -= deltaMs;
            if (frightenedTimer <= 0) {
                state = PHASE_IS_SCATTER[phaseIndex] ? GhostState.SCATTER : GhostState.CHASE;
                frightenedTimer = 0;
            }
        }

        // Move the ghost (extra step granted at higher levels for increased speed)
        moveOnce(board, pacman);
        if (Constants.ghostExtraStep(level, frameCount) || ghostSpeedForLevel(level) >= 2) {
            moveOnce(board, pacman);
        }

        // Respawn ghost when it reaches home after being eaten
        if (state == GhostState.EATEN && isNearHome()) {
            tileCol = startCol;
            tileRow = startRow;
            x = tileCol * Constants.TILE_SIZE;
            y = tileRow * Constants.TILE_SIZE;
            state = PHASE_IS_SCATTER[phaseIndex] ? GhostState.SCATTER : GhostState.CHASE;
        }
    }

    /**
     * Returns the ghost's movement speed for the given level.
     *
     * @param level current level number
     * @return speed value from Constants
     */
    private int ghostSpeedForLevel(int level) {
        return Constants.ghostSpeedForLevel(level);
    }

    /**
     * Moves the ghost one step in its current direction. Chooses a new
     * direction first if the ghost is aligned to the tile grid.
     *
     * @param board the current game board
     * @param pacman the player character
     */
    private void moveOnce(Board board, Pacman pacman) {
        if (isAlignedToGrid()) {
            chooseDirection(board, pacman); // only pick a new direction at tile boundaries
        }

        // Move only if the full leading edge of the ghost's sprite is clear of walls.
        // (Previously this only checked the single center point of the next position,
        // which let the ghost's body visually overlap into a wall tile by several
        // pixels before the check kicked in - most noticeable at corners.)
        if (canMove(currentDir, board)) {
            x += currentDir.dx * Constants.GHOST_SPEED;
            y += currentDir.dy * Constants.GHOST_SPEED;
        }

        // Tunnel wrap: ghosts cross screen edges just like Pac-Man
        int maxX = board.cols() * Constants.TILE_SIZE;
        if (x < -Constants.TILE_SIZE) {
            x = maxX;
        }
        if (x > maxX) {
            x = -Constants.TILE_SIZE;
        }
        // Update tile position based on pixel center
        tileCol = (x + Constants.TILE_SIZE / 2) / Constants.TILE_SIZE;
        tileRow = (y + Constants.TILE_SIZE / 2) / Constants.TILE_SIZE;

        // Safety net: if the ghost ever ends up centered on a wall tile (should not
        // happen given the check above, but guards against any future edge case),
        // snap it back to the last tile-aligned position instead of leaving it
        // visually stuck inside a wall.
        if (board.isWall(tileRow, tileCol)) {
            x = tileCol * Constants.TILE_SIZE - currentDir.dx * Constants.GHOST_SPEED;
            y = tileRow * Constants.TILE_SIZE - currentDir.dy * Constants.GHOST_SPEED;
            tileCol = (x + Constants.TILE_SIZE / 2) / Constants.TILE_SIZE;
            tileRow = (y + Constants.TILE_SIZE / 2) / Constants.TILE_SIZE;
        }
    }

    /**
     * Checks if the ghost can move one step in the given direction without any
     * part of its sprite overlapping a wall. Mirrors {@code Pacman.canMove}:
     * checks both leading corners of the sprite's bounding box (not just its
     * center), so the ghost stops flush against a wall instead of visually
     * poking into it - especially important at corners where two walls meet.
     *
     * @param dir direction to test
     * @param board the current game board
     * @return true if the move is clear of walls
     */
    private boolean canMove(Direction dir, Board board) {
        if (dir == Direction.NONE) {
            return false;
        }
        int ts = Constants.TILE_SIZE;
        int speed = Constants.GHOST_SPEED;
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

        // Wrap column indices around the board width so stepping past the
        // left/right edge (the tunnel) checks the matching column on the
        // opposite side instead of being treated as an out-of-bounds wall -
        // otherwise a ghost could freeze in the tunnel entrance the same way
        // Pacman could before this fix.
        int cols = board.cols();
        col1 = ((col1 % cols) + cols) % cols;
        col2 = ((col2 % cols) + cols) % cols;

        return !board.isWall(row1, col1) && !board.isWall(row2, col2);
    }

    /**
     * Returns true if the ghost is perfectly aligned to the tile grid.
     * Direction changes are only allowed at grid-aligned positions.
     *
     * @return true if both x and y are multiples of TILE_SIZE
     */
    private boolean isAlignedToGrid() {
        return (x % Constants.TILE_SIZE == 0) && (y % Constants.TILE_SIZE == 0);
    }

    /**
     * Returns true if the ghost is within 1 tile of its starting position. Used
     * to detect when an EATEN ghost has returned home.
     *
     * @return true if near the ghost house spawn tile
     */
    private boolean isNearHome() {
        return Math.abs(tileCol - startCol) <= 1 && Math.abs(tileRow - startRow) <= 1;
    }

    /**
     * Chooses the ghost's next movement direction based on its current state.
     * Ghosts cannot reverse direction except when forced (e.g., dead end).
     *
     * @param board the current game board
     * @param pacman the player character
     */
    private void chooseDirection(Board board, Pacman pacman) {
        List<Direction> possible = getPossibleDirections(board);

        // Fallback: if no valid direction found (e.g., stuck), allow any non-wall direction
        if (possible.isEmpty()) {
            possible = new ArrayList<>();
            for (Direction d : Direction.values()) {
                if (d == Direction.NONE) {
                    continue;
                }
                if (!board.isWall(tileRow + d.dy, tileCol + d.dx)) {
                    possible.add(d);
                }
            }
            if (possible.isEmpty()) {
                return; // truly stuck, do nothing
            }
            currentDir = possible.get(0);
            return;
        }

        // FRIGHTENED: move randomly
        if (state == GhostState.FRIGHTENED) {
            currentDir = possible.get(random.nextInt(possible.size()));
            return;
        }

        // EATEN: head straight back to the ghost house
        if (state == GhostState.EATEN) {
            moveToTarget(possible, startCol, startRow);
            return;
        }

        // SCATTER: retreat to this ghost's assigned corner
        if (state == GhostState.SCATTER) {
            int[] corner = getScatterCorner();
            moveToTarget(possible, corner[0], corner[1]);
            return;
        }

        // CHASE: use this ghost's individual AI targeting
        int[] target = calcTarget(pacman);
        moveToTarget(possible, target[0], target[1]);
    }

    /**
     * Returns the scatter corner tile (col, row) assigned to this ghost.
     *
     * @return int array {col, row} of the scatter target
     */
    private int[] getScatterCorner() {
        switch (name) {
            case BLINKY:
                return SCATTER_BLINKY;
            case PINKY:
                return SCATTER_PINKY;
            case INKY:
                return SCATTER_INKY;
            case CLYDE:
                return SCATTER_CLYDE;
            default:
                return SCATTER_BLINKY;
        }
    }

    /**
     * Calculates the chase target tile for this ghost based on its individual
     * AI.
     *
     * @param pacman the player character
     * @return int array {targetCol, targetRow}
     */
    private int[] calcTarget(Pacman pacman) {
        int px = pacman.getTileCol();
        int py = pacman.getTileRow();

        switch (name) {

            // Blinky: target is exactly Pac-Man's current tile
            case BLINKY:
                return new int[]{px, py};

            // Pinky: target is 4 tiles ahead of Pac-Man
            // Includes the original overflow bug: when Pac-Man faces UP, target shifts 4 tiles left as well
            case PINKY: {
                Direction d = pacman.getCurrentDir();
                int targetCol = px + d.dx * 4;
                int targetRow = py + d.dy * 4;
                if (d == Direction.UP) {
                    targetCol -= 4; // original UP overflow bug
                }
                return new int[]{targetCol, targetRow};
            }

            // Inky: target is the point reflected through Blinky from 2 tiles ahead of Pac-Man
            // Also replicates the UP overflow bug for the intermediate point
            case INKY: {
                Direction d = pacman.getCurrentDir();
                int midCol = px + d.dx * 2;
                int midRow = py + d.dy * 2;
                if (d == Direction.UP) {
                    midCol -= 2; // same overflow bug as Pinky, scaled to 2 tiles
                }
                if (blinky != null) {
                    int bc = blinky.getTileCol();
                    int br = blinky.getTileRow();
                    // Reflect the midpoint through Blinky's position
                    return new int[]{
                        midCol + (midCol - bc),
                        midRow + (midRow - br)
                    };
                }
                return new int[]{px, py}; // fallback if Blinky reference is missing
            }

            // Clyde: chases like Blinky when far, retreats to scatter corner when close
            case CLYDE: {
                double dist = Math.hypot(tileCol - px, tileRow - py);
                if (dist > CLYDE_THRESHOLD) {
                    return new int[]{px, py}; // far: chase Pac-Man
                } else {
                    return new int[]{SCATTER_CLYDE[0], SCATTER_CLYDE[1]}; // close: go to corner
                }
            }

            default:
                return new int[]{px, py};
        }
    }

    /**
     * Returns a list of valid directions the ghost can move from its current
     * tile. Excludes walls and the direct reverse of the current direction
     * (ghosts cannot U-turn). If no valid directions exist, allows the reverse
     * as a last resort.
     *
     * @param board the current game board
     * @return list of valid directions
     */
    private List<Direction> getPossibleDirections(Board board) {
        List<Direction> list = new ArrayList<>();
        for (Direction d : Direction.values()) {
            if (d == Direction.NONE) {
                continue;
            }
            if (d == currentDir.opposite()) {
                continue; // no U-turns allowed
            }
            if (!board.isWall(tileRow + d.dy, tileCol + d.dx)) {
                list.add(d);
            }
        }

        // If completely blocked, allow reversing as a last resort (e.g., dead end)
        if (list.isEmpty()) {
            Direction opp = currentDir.opposite();
            if (opp != Direction.NONE && !board.isWall(tileRow + opp.dy, tileCol + opp.dx)) {
                list.add(opp);
            }
        }
        return list;
    }

    /**
     * Chooses the direction from the possible list that minimizes distance to
     * the target tile. This is the standard ghost pathfinding used in all
     * non-random states.
     *
     * @param possible list of valid directions to choose from
     * @param targetCol target tile column
     * @param targetRow target tile row
     */
    private void moveToTarget(List<Direction> possible, int targetCol, int targetRow) {
        Direction best = possible.get(0);
        double bestDist = Double.MAX_VALUE;
        for (Direction d : possible) {
            double dist = Math.hypot(
                    (tileCol + d.dx) - targetCol,
                    (tileRow + d.dy) - targetRow
            );
            if (dist < bestDist) {
                bestDist = dist;
                best = d;
            }
        }
        currentDir = best;
    }

    // ── Frightened / Eaten ────────────────────────────────────────────────────
    /**
     * Triggers FRIGHTENED state for this ghost when Pac-Man eats a power
     * pellet. Duration decreases at higher levels. Does nothing if the ghost is
     * already EATEN.
     *
     * @param level current game level (affects frightened duration)
     */
    public void frighten(int level) {
        if (state != GhostState.EATEN) {
            state = GhostState.FRIGHTENED;
            frightenedTimer = Constants.frightenedDuration(level);
            currentDir = currentDir.opposite(); // reverse direction immediately
        }
    }

    /**
     * Marks this ghost as EATEN when Pac-Man collides with it in FRIGHTENED
     * state. Returns the score awarded for eating this ghost.
     *
     * @return score points, or 0 if the ghost was not in FRIGHTENED state
     */
    public int eat() {
        if (state == GhostState.FRIGHTENED) {
            state = GhostState.EATEN;
            return Constants.SCORE_GHOST;
        }
        return 0;
    }

    // ── Rendering ─────────────────────────────────────────────────────────────
    /**
     * Draws this ghost at its current pixel position. Appearance varies by
     * state: normal color, blue (frightened), flashing (about to recover), or
     * eyes only (eaten).
     *
     * @param g the Graphics context to draw on
     */
    public void draw(Graphics g) {
        int ts = Constants.TILE_SIZE;
        int px = x + 1;
        int py = y + 1;
        int size = ts - 2;

        // EATEN: only draw the eyes floating back to the ghost house
        if (state == GhostState.EATEN) {
            drawEyes(g, px, py, size);
            return;
        }

        // Determine body color based on state
        Color bodyColor;
        if (state == GhostState.FRIGHTENED) {
            // Flash white when frightenedTimer is nearly expired (last 2 seconds)
            boolean flash = frightenedTimer < 2000 && (frightenedTimer / 300) % 2 == 0;
            bodyColor = flash ? Color.WHITE : new Color(0, 0, 200);
        } else {
            bodyColor = color; // normal ghost color
        }

        // Draw ghost body: semicircle head + rectangular lower body
        g.setColor(bodyColor);
        g.fillArc(px, py, size, size, 0, 180);
        g.fillRect(px, py + size / 2, size, size / 2);

        // Draw bottom "skirt" notches to simulate legs
        int legCount = 3;
        int legW = size / legCount;
        for (int i = 0; i < legCount; i++) {
            g.setColor(Color.BLACK);
            int notchX = px + i * legW + legW / 2 - 1;
            g.fillRect(notchX, py + size - 4, 3, 5);
        }

        // Draw outline arc for the head
        g.setColor(bodyColor.darker());
        g.drawArc(px, py, size, size, 0, 180);

        // Draw eyes or frightened face
        if (state != GhostState.FRIGHTENED) {
            drawEyes(g, px, py, size); // normal directional eyes
        } else {
            // Frightened face: simple dot eyes and wavy mouth
            g.setColor(Color.WHITE);
            int eyeY = py + size / 3;
            int eyeSize = Math.max(2, size / 8);
            g.fillRect(px + size / 4 - eyeSize / 2, eyeY, eyeSize, eyeSize);
            g.fillRect(px + size * 3 / 4 - eyeSize / 2, eyeY, eyeSize, eyeSize);
            // Draw wavy mouth
            int mouthY = eyeY + eyeSize + 2;
            for (int i = 0; i < 5; i++) {
                int mx = px + size / 5 + i * (size / 6);
                g.fillRect(mx, mouthY + (i % 2 == 0 ? 0 : 2), size / 10 + 1, size / 10 + 1);
            }
        }
    }

    /**
     * Draws the ghost's eyes, with pupils shifted in the current movement
     * direction.
     *
     * @param g the Graphics context
     * @param px left pixel x of the ghost bounding box
     * @param py top pixel y of the ghost bounding box
     * @param size pixel size of the ghost
     */
    private void drawEyes(Graphics g, int px, int py, int size) {
        // White eyeballs
        g.setColor(Color.WHITE);
        g.fillOval(px + size / 5, py + size / 5, size / 4, size / 4);
        g.fillOval(px + size * 3 / 5, py + size / 5, size / 4, size / 4);

        // Blue pupils shifted toward current movement direction
        g.setColor(new Color(0, 100, 255));
        int pdx = currentDir.dx * 2;
        int pdy = currentDir.dy * 2;
        g.fillOval(px + size / 5 + pdx + 2, py + size / 5 + pdy + 2, size / 8, size / 8);
        g.fillOval(px + size * 3 / 5 + pdx + 2, py + size / 5 + pdy + 2, size / 8, size / 8);
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    /**
     * @return pixel X coordinate of the ghost's center
     */
    public int getCenterX() {
        return x + Constants.TILE_SIZE / 2;
    }

    /**
     * @return pixel Y coordinate of the ghost's center
     */
    public int getCenterY() {
        return y + Constants.TILE_SIZE / 2;
    }

    /**
     * @return current tile column of the ghost
     */
    public int getTileCol() {
        return tileCol;
    }

    /**
     * @return current tile row of the ghost
     */
    public int getTileRow() {
        return tileRow;
    }

    /**
     * @return current behavioral state of the ghost
     */
    public GhostState getState() {
        return state;
    }

    /**
     * @return name of this ghost (e.g., "Blinky")
     */
    public String getName() {
        return name;
    }
}
