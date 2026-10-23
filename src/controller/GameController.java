package controller;

import model.*;
import util.Constants;
import util.SoundManager;

import java.util.*;

/**
 * Main controller that manages all game logic, state transitions, input
 * handling, and coordination between game components.
 *
 * @author M4N
 */
public class GameController {

    /**
     * Represents the possible states the game can be in at any moment.
     */
    public enum GameState {
        /**
         * Showing the main menu screen.
         */
        MENU,
        /**
         * Game is actively running.
         */
        PLAYING,
        /**
         * Game is paused by the player.
         */
        PAUSED,
        /**
         * Pac-Man just died; waiting before respawn or game over.
         */
        DYING,
        /**
         * All dots collected; waiting before loading the next level.
         */
        LEVEL_WIN,
        /**
         * Player has run out of lives.
         */
        GAME_OVER,
        /**
         * Player has beaten all levels.
         */
        VICTORY,
        /**
         * Showing the How To Play info screen.
         */
        HOW_TO_PLAY,
        /**
         * Showing the About Us info screen.
         */
        ABOUT_US,
        /**
         * Player confirmed quit; waiting for final exit.
         */
        QUIT
    }

    /**
     * Currently highlighted menu item index (0=Play, 1=HowToPlay, 2=AboutUs,
     * 3=Quit).
     */
    private int menuSelection = 0;

    /**
     * Total number of items in the main menu.
     */
    public static final int MENU_ITEMS = 4;

    /**
     * The game board holding cell/tile data.
     */
    private Board board;

    /**
     * The player-controlled Pac-Man character.
     */
    private Pacman pacman;

    /**
     * List of all ghost enemies in the game.
     */
    private List<Ghost> ghosts;

    /**
     * Current player score.
     */
    private int score;

    /**
     * Highest score achieved across sessions.
     */
    private int highScore;

    /**
     * Remaining player lives.
     */
    private int lives;

    /**
     * Number of dots eaten in the current level.
     */
    private int dotsEaten;

    /**
     * Current level number (starts at 1).
     */
    private int level;

    /**
     * Current state of the game.
     */
    private GameState gameState;

    /**
     * Countdown counter used for timed delays (e.g., death/level transitions).
     */
    private int delayCounter;

    /**
     * Total time elapsed while playing, in milliseconds.
     */
    private long elapsedMs = 0;

    /**
     * Handles all sound effects and music.
     */
    private SoundManager sound;

    /**
     * Tracks whether Pac-Man was moving in the previous tick (for waka sound
     * control).
     */
    private boolean wasMoving = false;
    /**
     * Number of ghosts eaten in the current power pellet chain (resets each
     * pellet/death/level).
     */
    private int ghostEatChain = 0;

    /**
     * Constructs a new GameController and initializes all game components. Sets
     * the initial state to MENU.
     */
    public GameController() {
        sound = new SoundManager();
        board = new Board();
        pacman = new Pacman();
        ghosts = createGhosts();
        highScore = 0;
        score = 0;
        lives = Constants.INITIAL_LIVES;
        dotsEaten = 0;
        level = 1;
        gameState = GameState.MENU;
    }

    /**
     * Creates and returns the four ghosts (Blinky, Pinky, Inky, Clyde) with
     * their starting positions and colors.
     *
     * @return list of initialized Ghost objects
     */
    private List<Ghost> createGhosts() {
        List<Ghost> list = new ArrayList<>();

        // Blinky (red) - chases Pac-Man directly
        Ghost blinky = new Ghost(10, 9, java.awt.Color.RED, Ghost.BLINKY);
        // Pinky (pink) - aims ahead of Pac-Man
        Ghost pinky = new Ghost(9, 10, new java.awt.Color(255, 180, 255), Ghost.PINKY);
        // Inky (cyan) - uses Blinky's position for targeting
        Ghost inky = new Ghost(10, 10, new java.awt.Color(0, 255, 255), Ghost.INKY);
        // Clyde (orange) - alternates between chase and scatter
        Ghost clyde = new Ghost(11, 10, new java.awt.Color(255, 180, 100), Ghost.CLYDE);

        // Inky needs a reference to Blinky for its targeting logic
        inky.setBlinky(blinky);

        list.add(blinky);
        list.add(pinky);
        list.add(inky);
        list.add(clyde);
        return list;
    }

    /**
     * Called every game frame to advance the game logic based on the current
     * state.
     *
     * @param deltaMs time elapsed since the last tick, in milliseconds
     */
    public void tick(long deltaMs) {
        switch (gameState) {
            case PLAYING:
                elapsedMs += deltaMs; // accumulate play time
                updateGame(deltaMs);
                break;

            case DYING:
                delayCounter--;
                if (delayCounter <= 0) {
                    if (lives > 0) {
                        respawn(); // still has lives, reset positions
                    } else {
                        gameState = GameState.GAME_OVER; // no lives left
                    }
                }
                break;

            case LEVEL_WIN:
                delayCounter--;
                if (delayCounter <= 0) {
                    nextLevel(); // advance to the next level
                }
                break;

            default:
                break; // no update needed for menu/pause/etc.
        }
    }

    /**
     * Runs all in-game updates for a single frame: moves Pac-Man and ghosts,
     * checks for dot eating, ghost collisions, and level completion.
     *
     * @param deltaMs time elapsed since the last frame, in milliseconds
     */
    private void updateGame(long deltaMs) {
        int prevX = pacman.getX(), prevY = pacman.getY();
        pacman.update(board); // move Pac-Man based on current direction

        // Detect if Pac-Man actually moved this frame
        boolean moving = (pacman.getX() != prevX || pacman.getY() != prevY);

        // Start waka sound when movement begins
        if (moving && !wasMoving) {
            sound.startWaka();
        }
        // Stop waka sound when movement stops
        if (!moving && wasMoving) {
            sound.stopWaka();
        }
        wasMoving = moving;

        // Update each ghost's AI and movement
        for (Ghost g : ghosts) {
            g.update(board, pacman, deltaMs, level);
        }

        checkDotEating();      // award score and trigger frightened mode if needed
        checkGhostCollision(); // handle eating ghosts or losing a life

        // Check if all dots have been collected
        if (dotsEaten >= board.getTotalDots()) {
            sound.stopAll();
            sound.playLevelWin();
            gameState = GameState.LEVEL_WIN;
            delayCounter = 180; // ~3 seconds before advancing
        }
    }

    /**
     * Checks if Pac-Man is on a cell with a dot or power pellet, awards points,
     * and activates frightened mode for ghosts if a pellet is eaten.
     */
    private void checkDotEating() {
        int r = pacman.getTileRow(), c = pacman.getTileCol();
        int cell = board.getCell(r, c);

        if (cell == 0) {
            // Regular dot eaten
            board.setCell(r, c, 3); // mark cell as empty
            score += Constants.SCORE_DOT;
            dotsEaten++;
            sound.playEatDot();
        } else if (cell == 2) {
            // Power pellet eaten - ghosts become frightened
            board.setCell(r, c, 3);
            score += Constants.SCORE_PELLET;
            dotsEaten++;
            sound.playPellet();
            ghostEatChain = 0; // reset chain on new pellet
            for (Ghost g : ghosts) {
                g.frighten(level); // duration depends on current level
            }
        }
    }

    /**
     * Checks if Pac-Man collides with any ghost. If the ghost is frightened, it
     * gets eaten for bonus points. If the ghost is active (chase/scatter),
     * Pac-Man loses a life.
     */
    private void checkGhostCollision() {
        int px = pacman.getCenterX(), py = pacman.getCenterY();
        int collisionDist = Constants.TILE_SIZE - 4; // pixel threshold for collision

        for (Ghost g : ghosts) {
            int dx = px - g.getCenterX(), dy = py - g.getCenterY();
            double dist = Math.sqrt(dx * dx + dy * dy);

            if (dist < collisionDist) {
                if (g.getState() == Ghost.GhostState.FRIGHTENED) {
                    // Pac-Man eats the frightened ghost; score doubles per ghost in chain
                    ghostEatChain++;
                    score += g.eat() * (1 << (ghostEatChain - 1)); // 200, 400, 800, 1600
                    sound.playEatGhost();
                } else if (g.getState() == Ghost.GhostState.CHASE
                        || g.getState() == Ghost.GhostState.SCATTER) {
                    // Ghost kills Pac-Man
                    lives--;
                    pacman.setAlive(false);
                    sound.stopAll();
                    sound.playDie();

                    // Save high score if beaten
                    if (score > highScore) {
                        highScore = score;
                    }

                    gameState = GameState.DYING;
                    delayCounter = 150; // ~2.5 seconds before respawn/game over
                    return; // stop checking other ghosts
                }
            }
        }
    }

    /**
     * Resets Pac-Man and all ghosts to their starting positions and resumes
     * gameplay after a death.
     */
    private void respawn() {
        pacman.reset();
        for (Ghost g : ghosts) {
            g.reset();
        }
        ghostEatChain = 0; // reset chain on death
        wasMoving = false;
        gameState = GameState.PLAYING;
    }

    /**
     * Advances the game to the next level. If the maximum level is exceeded,
     * triggers the VICTORY state.
     */
    private void nextLevel() {
        level++;

        if (level > Constants.MAX_LEVEL) {
            // Player has beaten all levels
            if (score > highScore) {
                highScore = score;
            }
            gameState = GameState.VICTORY;
            return;
        }

        // Load the new level layout and reset all entities
        board.setLayout(Constants.layoutIndexForLevel(level));
        pacman.reset();
        for (Ghost g : ghosts) {
            g.reset();
        }
        ghostEatChain = 0; // reset chain on new level
        dotsEaten = 0;
        wasMoving = false;
        gameState = GameState.PLAYING;
    }

    /**
     * Starts a new game from the beginning: resets score, lives, level, board
     * layout, and all entity positions.
     */
    public void startGame() {
        // Save current score to high score if it qualifies
        if (score > highScore) {
            highScore = score;
        }

        // Reset all game state
        score = 0;
        lives = Constants.INITIAL_LIVES;
        dotsEaten = 0;
        level = 1;
        elapsedMs = 0;

        board.setLayout(0); // load the first level layout
        pacman.reset();
        for (Ghost g : ghosts) {
            g.reset();
        }
        ghostEatChain = 0; // reset chain on new game
        wasMoving = false;
        sound.stopAll();
        gameState = GameState.PLAYING;
    }

    /**
     * Handles a key press event by delegating to the appropriate handler based
     * on the current game state.
     *
     * @param keyCode the AWT KeyEvent key code
     */
    public void handleKey(int keyCode) {
        switch (gameState) {
            case MENU:
                handleMenuKey(keyCode);
                break;

            case HOW_TO_PLAY:
            case ABOUT_US:
                // Any of these keys returns to the main menu
                if (keyCode == java.awt.event.KeyEvent.VK_ESCAPE
                        || keyCode == java.awt.event.KeyEvent.VK_BACK_SPACE
                        || keyCode == java.awt.event.KeyEvent.VK_ENTER) {
                    gameState = GameState.MENU;
                }
                break;

            case QUIT:
                // Confirm quit on Enter or Escape
                if (keyCode == java.awt.event.KeyEvent.VK_ENTER
                        || keyCode == java.awt.event.KeyEvent.VK_ESCAPE) {
                    System.exit(0);
                }
                break;

            case GAME_OVER:
            case VICTORY:
                if (keyCode == java.awt.event.KeyEvent.VK_ENTER) {
                    gameState = GameState.MENU; // return to menu
                }
                if (keyCode == java.awt.event.KeyEvent.VK_R) {
                    startGame(); // restart immediately
                }
                break;

            case LEVEL_WIN:
                break; // no input accepted during level transition

            case PLAYING:
                handlePlayKey(keyCode);
                break;

            case PAUSED:
                // Resume with P or Enter
                if (keyCode == java.awt.event.KeyEvent.VK_P
                        || keyCode == java.awt.event.KeyEvent.VK_ENTER) {
                    gameState = GameState.PLAYING;
                }
                break;

            default:
                break;
        }
    }

    /**
     * Handles key input while on the main menu. Supports arrow keys, WASD,
     * Enter/Space for navigation, and number keys 1-4 as shortcuts.
     *
     * @param keyCode the AWT KeyEvent key code
     */
    private void handleMenuKey(int keyCode) {
        // Navigate up
        if (keyCode == java.awt.event.KeyEvent.VK_UP || keyCode == java.awt.event.KeyEvent.VK_W) {
            menuSelection = (menuSelection - 1 + MENU_ITEMS) % MENU_ITEMS;
        }
        // Navigate down
        if (keyCode == java.awt.event.KeyEvent.VK_DOWN || keyCode == java.awt.event.KeyEvent.VK_S) {
            menuSelection = (menuSelection + 1) % MENU_ITEMS;
        }

        // Confirm selection with Enter or Space
        if (keyCode == java.awt.event.KeyEvent.VK_ENTER || keyCode == java.awt.event.KeyEvent.VK_SPACE) {
            switch (menuSelection) {
                case 0:
                    startGame();
                    break;
                case 1:
                    gameState = GameState.HOW_TO_PLAY;
                    break;
                case 2:
                    gameState = GameState.ABOUT_US;
                    break;
                case 3:
                    gameState = GameState.QUIT;
                    break;
            }
        }

        // Number key shortcuts for direct menu access
        if (keyCode == java.awt.event.KeyEvent.VK_1) {
            startGame();
        }
        if (keyCode == java.awt.event.KeyEvent.VK_2) {
            gameState = GameState.HOW_TO_PLAY;
        }
        if (keyCode == java.awt.event.KeyEvent.VK_3) {
            gameState = GameState.ABOUT_US;
        }
        if (keyCode == java.awt.event.KeyEvent.VK_4) {
            gameState = GameState.QUIT;
        }
    }

    /**
     * Handles key input during active gameplay. Arrow keys and WASD change
     * Pac-Man's direction; P pauses the game.
     *
     * @param keyCode the AWT KeyEvent key code
     */
    private void handlePlayKey(int keyCode) {
        if (keyCode == java.awt.event.KeyEvent.VK_UP || keyCode == java.awt.event.KeyEvent.VK_W) {
            pacman.setNextDir(Direction.UP);
        }
        if (keyCode == java.awt.event.KeyEvent.VK_DOWN || keyCode == java.awt.event.KeyEvent.VK_S) {
            pacman.setNextDir(Direction.DOWN);
        }
        if (keyCode == java.awt.event.KeyEvent.VK_LEFT || keyCode == java.awt.event.KeyEvent.VK_A) {
            pacman.setNextDir(Direction.LEFT);
        }
        if (keyCode == java.awt.event.KeyEvent.VK_RIGHT || keyCode == java.awt.event.KeyEvent.VK_D) {
            pacman.setNextDir(Direction.RIGHT);
        }
        if (keyCode == java.awt.event.KeyEvent.VK_P) {
            sound.stopAll();
            gameState = GameState.PAUSED; // pause the game
        }
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------
    /**
     * @return the game board
     */
    public Board getBoard() {
        return board;
    }

    /**
     * @return the Pac-Man entity
     */
    public Pacman getPacman() {
        return pacman;
    }

    /**
     * @return list of all ghosts
     */
    public List<Ghost> getGhosts() {
        return ghosts;
    }

    /**
     * @return current player score
     */
    public int getScore() {
        return score;
    }

    /**
     * @return highest score recorded
     */
    public int getHighScore() {
        return highScore;
    }

    /**
     * @return remaining player lives
     */
    public int getLives() {
        return lives;
    }

    /**
     * @return current level number
     */
    public int getLevel() {
        return level;
    }

    /**
     * @return current game state
     */
    public GameState getGameState() {
        return gameState;
    }

    /**
     * @return currently selected menu item index
     */
    public int getMenuSelection() {
        return menuSelection;
    }

    /**
     * @return total milliseconds elapsed during active gameplay
     */
    public long getElapsedMs() {
        return elapsedMs;
    }
}
