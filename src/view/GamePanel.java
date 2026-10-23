package view;

import controller.GameController;
import controller.GameController.GameState;
import util.Constants;
import model.Ghost;
import model.Direction;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Main game panel that handles rendering all screens and receiving keyboard
 * input.
 *
 * Extends JPanel to act as the primary drawing surface. Implements
 * ActionListener for the game timer and KeyListener for player input.
 *
 * Screens managed: MENU, HOW_TO_PLAY, ABOUT_US, QUIT, PLAYING, DYING,
 * LEVEL_WIN, GAME_OVER, VICTORY, PAUSED
 *
 * @author M4N
 */
public class GamePanel extends JPanel implements ActionListener, KeyListener {

    /**
     * Controls all game logic and state.
     */
    private final GameController controller;

    /**
     * Responsible for rendering the maze/board.
     */
    private final BoardRenderer boardRenderer;

    /**
     * Responsible for rendering the HUD (score, lives, level, timer).
     */
    private final HUDRenderer hudRenderer;

    /**
     * Swing timer that drives the game loop at a fixed frame rate.
     */
    private final Timer gameTimer;

    /**
     * Timestamp of the last frame, used to calculate deltaMs.
     */
    private long lastTime;

    /**
     * Tick counter used for menu animations (incremented each frame).
     */
    private int animTick = 0;

    /**
     * Constructs the GamePanel: sets up size, background, input listeners,
     * renderers, and starts the game loop timer.
     */
    public GamePanel() {
        setPreferredSize(new Dimension(Constants.SCREEN_WIDTH, Constants.SCREEN_HEIGHT));
        setBackground(Color.BLACK);
        setFocusable(true);         // required to receive keyboard events
        addKeyListener(this);

        controller = new GameController();
        boardRenderer = new BoardRenderer();
        hudRenderer = new HUDRenderer();

        lastTime = System.currentTimeMillis();
        gameTimer = new Timer(Constants.FRAME_DELAY, this); // fires every FRAME_DELAY ms
        gameTimer.start();
    }

    /**
     * Called by the game timer every frame. Calculates elapsed time, ticks the
     * game controller, and requests a repaint.
     *
     * @param e the action event fired by the timer
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        long now = System.currentTimeMillis();
        long delta = now - lastTime; // time since last frame in ms
        lastTime = now;
        animTick++;                  // advance animation tick for menu effects
        controller.tick(delta);      // update game logic
        repaint();                   // trigger paintComponent
    }

    /**
     * Renders the correct screen based on the current game state.
     *
     * @param g the Graphics context provided by Swing
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // clear previous frame
        GameState state = controller.getGameState();

        switch (state) {
            case MENU:
                drawMenu(g);
                break;
            case HOW_TO_PLAY:
                drawHowToPlay(g);
                break;
            case ABOUT_US:
                drawAboutUs(g);
                break;
            case QUIT:
                drawQuit(g);
                break;
            case PLAYING:
            case DYING:
                drawGame(g); // same view for both; DYING just freezes input
                break;
            case LEVEL_WIN:
                drawGame(g);
                drawLevelWin(g); // overlay on top of the game
                break;
            case GAME_OVER:
                drawGame(g);
                drawGameOver(g); // overlay on top of the game
                break;
            case VICTORY:
                drawGame(g);
                drawVictory(g);  // overlay on top of the game
                break;
            case PAUSED:
                drawGame(g);
                drawCenteredOverlay(g, "PAUSED", "Press P or ENTER to continue", Color.WHITE);
                break;
        }
    }

    // ── SCREEN: MENU ──────────────────────────────────────────────────────────
    /**
     * Draws the main menu screen with an animated title, menu buttons, high
     * score display, animated ghosts, and a chasing Pac-Man.
     *
     * @param g the Graphics context
     */
    private void drawMenu(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Black background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        int cx = getWidth() / 2;

        // Animated title: pulses between orange and yellow using a sine wave
        float pulse = (float) (0.6 + 0.4 * Math.sin(animTick * 0.05));
        Color titleColor = new Color(1f, pulse, 0f);
        g.setFont(new Font("Arial", Font.BOLD, 52));
        g.setColor(new Color(80, 40, 0));           // dark shadow offset by 3px
        drawCenteredString(g, "PAC-MAN", 80 + 3);
        g.setColor(titleColor);                     // glowing title on top
        drawCenteredString(g, "PAC-MAN", 80);

        // High score display below title
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.setColor(new Color(255, 200, 0));
        drawCenteredString(g, "HIGH SCORE: " + controller.getHighScore(), 108);

        // Project subtitle
        g.setFont(new Font("Arial", Font.ITALIC, 13));
        g.setColor(new Color(150, 150, 200));
        drawCenteredString(g, "FPT University - CSD201 - SU26", 128);

        // Menu option buttons
        String[] labels = {"1. PLAY", "2. HOW TO PLAY", "3. ABOUT US", "4. QUIT"};
        Color[] colors = {
            new Color(80, 255, 80), // green  for PLAY
            new Color(80, 200, 255), // cyan   for HOW TO PLAY
            new Color(255, 200, 80), // yellow for ABOUT US
            new Color(255, 80, 80) // red    for QUIT
        };
        int sel = controller.getMenuSelection();
        int boxW = 260, boxH = 52, boxX = cx - boxW / 2, baseY = 155;

        for (int i = 0; i < labels.length; i++) {
            int by = baseY + i * (boxH + 8);
            boolean selected = (i == sel);

            if (selected) {
                // Highlighted button: dark background + colored border
                g.setColor(new Color(30, 30, 60));
                g.fillRoundRect(boxX - 2, by - 2, boxW + 4, boxH + 4, 14, 14);
                g.setColor(colors[i]);
                g.drawRoundRect(boxX - 2, by - 2, boxW + 4, boxH + 4, 14, 14);

                // Blinking arrow indicator (toggles every 15 ticks)
                if ((animTick / 15) % 2 == 0) {
                    g.setColor(colors[i]);
                    g.setFont(new Font("Arial", Font.BOLD, 20));
                    g.drawString("\u25B6", boxX - 28, by + boxH / 2 + 7); // Unicode right-pointing triangle
                }
            } else {
                // Normal (unselected) button: dimmer style
                g.setColor(new Color(20, 20, 40));
                g.fillRoundRect(boxX, by, boxW, boxH, 12, 12);
                g.setColor(new Color(60, 60, 80));
                g.drawRoundRect(boxX, by, boxW, boxH, 12, 12);
            }

            // Button label text, centered inside the box
            g.setFont(new Font("Arial", Font.BOLD, selected ? 20 : 17));
            g.setColor(selected ? colors[i] : new Color(160, 160, 160));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(labels[i],
                    cx - fm.stringWidth(labels[i]) / 2,
                    by + (boxH + fm.getAscent() - fm.getDescent()) / 2);
        }

        // Navigation hint at the bottom
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.setColor(new Color(100, 100, 100));
        drawCenteredString(g, "\u2191\u2193 Navigate    ENTER / 1-4 Select", getHeight() - 18); // up/down arrows

        // Animated mini ghosts bobbing near the bottom
        int gy = getHeight() - 55;
        int[] ghostColors = {0xFF0000, 0xFF69B4, 0x00FFFF, 0xFFB064}; // red, pink, cyan, orange
        for (int i = 0; i < 4; i++) {
            // Each ghost bobs up/down slightly with a phase offset
            int gx = cx - 60 + i * 36 + (int) (Math.sin(animTick * 0.08 + i) * 5);
            g.setColor(new Color(ghostColors[i]));
            g.fillOval(gx, gy, 24, 24); // ghost body
            // Draw white eyes
            g.setColor(Color.WHITE);
            g.fillOval(gx + 3, gy + 5, 6, 7);
            g.fillOval(gx + 12, gy + 5, 6, 7);
            // Draw black pupils that shift slightly over time
            g.setColor(Color.BLACK);
            int ex = (int) (2 * Math.cos(animTick * 0.05));
            g.fillOval(gx + 4 + ex, gy + 7, 3, 3);
            g.fillOval(gx + 13 + ex, gy + 7, 3, 3);
        }

        // Pac-Man chasing the ghosts (scrolls left to right and loops)
        int pmx = cx - 100 + (animTick * 2) % 200 - 10;
        int mouthAngle = 30 + (int) (20 * Math.abs(Math.sin(animTick * 0.2))); // animated chomping
        g.setColor(Color.YELLOW);
        g.fillArc(pmx, gy, 26, 26, mouthAngle / 2, 360 - mouthAngle);
    }

    // ── SCREEN: HOW TO PLAY ───────────────────────────────────────────────────
    /**
     * Renders the How To Play instruction screen. Covers objective, controls,
     * scoring, power pellets, ghosts, lives, and level progression.
     *
     * @param g the Graphics context
     */
    private void drawHowToPlay(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Black background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        // Title bar at the top
        g.setColor(new Color(20, 20, 80));
        g.fillRect(0, 0, getWidth(), 50);
        g.setColor(new Color(80, 200, 255));
        g.drawLine(0, 50, getWidth(), 50); // separator line
        g.setFont(new Font("Arial", Font.BOLD, 26));
        g.setColor(new Color(80, 200, 255));
        drawCenteredString(g, "HOW TO PLAY", 36);

        int y = 75;  // current Y position for drawing text
        int lineH = 26;  // vertical spacing between lines

        // Each entry: first string is a section header, rest are bullet points
        String[][] steps = {
            {"STEP 1 - OBJECTIVE",
                "Eat all the dots on the map to complete each level.",
                "Clear all 50 levels to WIN the game!"},
            {"STEP 2 - CONTROLS",
                "Move: Arrow Keys  or  W A S D",
                "Pause / Resume: P  or  ENTER"},
            {"STEP 3 - SCORING",
                "Small dot = 10 pts    Power Pellet = 50 pts",
                "Eat a frightened ghost = 200 pts"},
            {"STEP 4 - POWER PELLET",
                "Eating a Power Pellet frightens all ghosts (blue).",
                "Chase and eat them for bonus points!"},
            {"STEP 5 - GHOSTS",
                "4 ghosts: Blinky (red), Pinky (pink), Inky (cyan), Clyde (orange).",
                "Avoid ghosts when they are NOT blue -- you will lose a life!"},
            {"STEP 6 - LIVES",
                "You start with 3 lives (shown in HUD).",
                "Lose a life each time a ghost catches you."},
            {"STEP 7 - LEVELS & MAPS",
                "Every 5 levels the maze layout changes (10 unique maps).",
                "Ghosts get faster as levels progress -- stay sharp!"}
        };

        for (String[] step : steps) {
            // Section header in yellow
            g.setFont(new Font("Arial", Font.BOLD, 13));
            g.setColor(new Color(255, 200, 0));
            g.drawString(step[0], 30, y);
            y += lineH - 4;

            // Bullet point lines in white
            g.setFont(new Font("Arial", Font.PLAIN, 12));
            g.setColor(Color.WHITE);
            for (int i = 1; i < step.length; i++) {
                g.drawString("  - " + step[i], 30, y);
                y += lineH - 5;
            }
            y += 8; // extra gap between sections
        }

        // Back hint at the bottom
        g.setFont(new Font("Arial", Font.BOLD, 13));
        g.setColor(new Color(100, 100, 100));
        drawCenteredString(g, "Press ENTER or ESC to go back", getHeight() - 18);
    }

    // ── SCREEN: ABOUT US ──────────────────────────────────────────────────────
    /**
     * Draws the About Us screen showing university info, course details, and
     * team member cards.
     *
     * @param g the Graphics context
     */
    private void drawAboutUs(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Black background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        int cx = getWidth() / 2;

        // Title bar
        g.setColor(new Color(40, 20, 60));
        g.fillRect(0, 0, getWidth(), 50);
        g.setColor(new Color(200, 100, 255));
        g.drawLine(0, 50, getWidth(), 50); // separator line
        g.setFont(new Font("Arial", Font.BOLD, 26));
        g.setColor(new Color(200, 100, 255));
        drawCenteredString(g, "ABOUT US", 36);

        // University info card
        int cardX = 30, cardW = getWidth() - 60;
        int cardY = 65;
        g.setColor(new Color(15, 15, 40));
        g.fillRoundRect(cardX, cardY, cardW, 70, 12, 12);
        g.setColor(new Color(80, 80, 200));
        g.drawRoundRect(cardX, cardY, cardW, 70, 12, 12);

        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.setColor(new Color(200, 180, 255));
        drawCenteredString(g, "FPT University", cardY + 25);
        g.setFont(new Font("Arial", Font.PLAIN, 13));
        g.setColor(new Color(160, 160, 220));
        drawCenteredString(g, "Course: CSD201  |  Term: SU26  |  Class: SE2001", cardY + 50);
        drawCenteredString(g, "Mentor: LanLTT11", cardY + 68);

        // Team members section header
        int teamY = cardY + 95;
        g.setFont(new Font("Arial", Font.BOLD, 15));
        g.setColor(new Color(255, 200, 0));
        drawCenteredString(g, "-- TEAM MEMBERS --", teamY);
        teamY += 28;

        // Team member data: student ID, name, role
        String[][] members = {
            {"CE200244", "Nguyen Van Man", "Team Leader"},
            {"CE200123", "Nguyen Hoang Gia Kiet", "Developer"},
            {"CE201027", "Phan Vo Anh Hao", "Developer"},
            {"CE200489", "Pham Thi Bich Tram", "Developer"},
            {"CE200322", "Nguyen Thanh Triet", "Developer"},
            {"CE200389", "Nguyen Nhan Tam", "Developer"},};

        // Each member gets a unique accent color
        Color[] memberColors = {
            new Color(255, 120, 120),
            new Color(120, 220, 255),
            new Color(120, 255, 180),
            new Color(255, 220, 100),
            new Color(200, 150, 255),
            new Color(255, 180, 100)
        };

        for (int i = 0; i < members.length; i++) {
            int my = teamY + i * 42;

            // Card background and border
            g.setColor(new Color(20, 20, 50));
            g.fillRoundRect(cardX, my, cardW, 34, 8, 8);
            g.setColor(memberColors[i]);
            g.drawRoundRect(cardX, my, cardW, 34, 8, 8);

            // Student ID in accent color
            g.setFont(new Font("Arial", Font.BOLD, 13));
            g.setColor(memberColors[i]);
            g.drawString("[" + members[i][0] + "]", cardX + 14, my + 22);

            // Full name in white
            g.setFont(new Font("Arial", Font.PLAIN, 13));
            g.setColor(Color.WHITE);
            g.drawString(members[i][1], cardX + 120, my + 22);

            // Role in grey italic
            g.setFont(new Font("Arial", Font.ITALIC, 11));
            g.setColor(new Color(160, 160, 160));
            g.drawString(members[i][2], cardX + 260, my + 22);
        }

        // Decorative Pac-Man icon below member list
        int py = teamY + members.length * 42 + 20;
        g.setColor(Color.YELLOW);
        g.fillArc(cx - 15, py, 30, 30, 30, 300); // Pac-Man with open mouth

        // Back hint
        g.setFont(new Font("Arial", Font.BOLD, 13));
        g.setColor(new Color(100, 100, 100));
        drawCenteredString(g, "Press ENTER or ESC to go back", getHeight() - 18);
    }

    // ── SCREEN: QUIT ──────────────────────────────────────────────────────────
    /**
     * Draws the quit confirmation screen with a sad animated Pac-Man and a
     * farewell message.
     *
     * @param g the Graphics context
     */
    private void drawQuit(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Black background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        int cx = getWidth() / 2;
        int cy = getHeight() / 2;

        // Sad Pac-Man with pulsing color
        float alpha = (float) (0.5 + 0.5 * Math.sin(animTick * 0.07));
        g.setColor(new Color(1f, alpha, 0f)); // yellow-orange pulse
        g.fillArc(cx - 40, cy - 120, 80, 80, 200, 320); // body with open mouth facing down

        // Sad eyes
        g.setColor(Color.BLACK);
        g.fillOval(cx - 18, cy - 105, 7, 7);
        g.fillOval(cx + 8, cy - 105, 7, 7);

        // Sad mouth (arc curving downward)
        g.setColor(new Color(150, 60, 0));
        g.drawArc(cx - 15, cy - 86, 30, 20, 0, -180);

        // Farewell text
        g.setFont(new Font("Arial", Font.BOLD, 30));
        g.setColor(new Color(255, 180, 0));
        drawCenteredString(g, "Goodbye!", cy - 20);

        g.setFont(new Font("Arial", Font.PLAIN, 17));
        g.setColor(Color.WHITE);
        drawCenteredString(g, "Thank you for playing PAC-MAN!", cy + 14);

        g.setFont(new Font("Arial", Font.ITALIC, 14));
        g.setColor(new Color(180, 180, 255));
        drawCenteredString(g, "See you again!", cy + 38);

        // Exit instruction
        g.setFont(new Font("Arial", Font.PLAIN, 13));
        g.setColor(new Color(100, 100, 100));
        drawCenteredString(g, "Press ENTER to exit", cy + 75);
    }

    // ── SCREEN: GAME ──────────────────────────────────────────────────────────
    /**
     * Renders the active gameplay view: board, all ghosts, Pac-Man, and the
     * HUD. Used by PLAYING, DYING, LEVEL_WIN, GAME_OVER, VICTORY, and PAUSED
     * states.
     *
     * @param g the Graphics context
     */
    private void drawGame(Graphics g) {
        boardRenderer.draw(g, controller.getBoard()); // draw maze tiles

        // Draw all ghosts
        for (Ghost ghost : controller.getGhosts()) {
            ghost.draw(g);
        }

        controller.getPacman().draw(g); // draw Pac-Man on top

        // Draw HUD: score, high score, level, lives, directions, elapsed time
        hudRenderer.draw(
                g,
                controller.getScore(),
                controller.getHighScore(),
                controller.getLevel(),
                controller.getLives(),
                getWidth(),
                controller.getBoard().rows(),
                controller.getPacman().getCurrentDir(),
                controller.getPacman().getNextDir(),
                controller.getElapsedMs()
        );
    }

    // ── SCREEN: LEVEL WIN ─────────────────────────────────────────────────────
    /**
     * Draws the level-cleared overlay shown on top of the game board. Displays
     * the cleared level number and a preview of what comes next.
     *
     * @param g the Graphics context
     */
    private void drawLevelWin(Graphics g) {
        // Semi-transparent dark overlay over the game
        g.setColor(new Color(0, 0, 0, 140));
        g.fillRect(0, 0, getWidth(), getHeight());

        // Centered popup box
        int cx = getWidth() / 2, cy = getHeight() / 2;
        int bw = 300, bh = 140, bx = cx - bw / 2, by = cy - bh / 2;
        g.setColor(new Color(10, 40, 10));
        g.fillRoundRect(bx, by, bw, bh, 16, 16);
        g.setColor(new Color(0, 220, 80));
        g.drawRoundRect(bx, by, bw, bh, 16, 16);

        // Level clear title
        g.setFont(new Font("Arial", Font.BOLD, 26));
        g.setColor(new Color(0, 255, 100));
        drawCenteredString(g, "LEVEL " + controller.getLevel() + " CLEAR!", by + 45);

        // Next level hint or final level message
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.setColor(Color.WHITE);
        if (controller.getLevel() < Constants.MAX_LEVEL) {
            drawCenteredString(g, "Get ready for Level " + (controller.getLevel() + 1) + "...", by + 80);
        } else {
            drawCenteredString(g, "Final level - hang tight!", by + 80);
        }

        // Current score
        g.setFont(new Font("Arial", Font.PLAIN, 12));
        g.setColor(new Color(120, 120, 120));
        drawCenteredString(g, "Score: " + controller.getScore(), by + 112);
    }

    // ── SCREEN: GAME OVER ─────────────────────────────────────────────────────
    /**
     * Draws the game over overlay showing final score, elapsed time, and high
     * score (or a new record message).
     *
     * @param g the Graphics context
     */
    private void drawGameOver(Graphics g) {
        // Semi-transparent dark overlay
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRect(0, 0, getWidth(), getHeight());

        // Centered red-bordered popup box
        int cx = getWidth() / 2, cy = getHeight() / 2;
        int bw = 320, bh = 230, bx = cx - bw / 2, by = cy - bh / 2;
        g.setColor(new Color(40, 5, 5));
        g.fillRoundRect(bx, by, bw, bh, 20, 20);
        g.setColor(Color.RED);
        g.drawRoundRect(bx, by, bw, bh, 20, 20);

        // GAME OVER title
        g.setFont(new Font("Arial", Font.BOLD, 32));
        g.setColor(Color.RED);
        drawCenteredString(g, "GAME OVER", by + 48);

        // Final score
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.setColor(Color.WHITE);
        drawCenteredString(g, "Score: " + controller.getScore(), by + 85);

        // Elapsed time formatted as mm:ss
        long ms = controller.getElapsedMs();
        String time = String.format("Time: %d:%02d", ms / 60000, (ms / 1000) % 60);
        drawCenteredString(g, time, by + 108);

        // Show NEW HIGH SCORE banner or current high score
        boolean newRecord = controller.getScore() > 0
                && controller.getScore() >= controller.getHighScore();
        g.setFont(new Font("Arial", Font.BOLD, 17));
        if (newRecord) {
            g.setColor(Color.YELLOW);
            drawCenteredString(g, "* NEW HIGH SCORE! *", by + 140);
        } else {
            g.setColor(new Color(255, 200, 0));
            drawCenteredString(g, "High Score: " + controller.getHighScore(), by + 140);
        }

        // Navigation options
        g.setFont(new Font("Arial", Font.PLAIN, 13));
        g.setColor(new Color(160, 160, 160));
        drawCenteredString(g, "ENTER -> Menu    R -> Replay", by + 185);
    }

    // ── SCREEN: VICTORY ───────────────────────────────────────────────────────
    /**
     * Draws the victory screen overlay shown when the player clears all 50
     * levels. Displays final score, total time, and high score.
     *
     * @param g the Graphics context
     */
    private void drawVictory(Graphics g) {
        // Semi-transparent dark overlay
        g.setColor(new Color(0, 0, 0, 190));
        g.fillRect(0, 0, getWidth(), getHeight());

        // Centered green-bordered popup box
        int cx = getWidth() / 2, cy = getHeight() / 2;
        int bw = 360, bh = 240, bx = cx - bw / 2, by = cy - bh / 2;
        g.setColor(new Color(10, 40, 10));
        g.fillRoundRect(bx, by, bw, bh, 20, 20);
        g.setColor(new Color(0, 255, 100));
        g.drawRoundRect(bx, by, bw, bh, 20, 20);

        // Victory title
        g.setFont(new Font("Arial", Font.BOLD, 28));
        g.setColor(new Color(0, 255, 100));
        drawCenteredString(g, "YOU WIN!", by + 48);

        // Completion message
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.setColor(Color.WHITE);
        drawCenteredString(g, "All 50 Levels Completed!", by + 76);

        // Final score
        g.setFont(new Font("Arial", Font.BOLD, 15));
        g.setColor(Color.WHITE);
        drawCenteredString(g, "Final Score: " + controller.getScore(), by + 108);

        // Total time formatted as mm:ss
        long ms = controller.getElapsedMs();
        drawCenteredString(g, String.format("Total Time: %d:%02d", ms / 60000, (ms / 1000) % 60), by + 130);

        // High score
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.setColor(new Color(255, 220, 0));
        drawCenteredString(g, "High Score: " + controller.getHighScore(), by + 160);

        // Navigation options
        g.setFont(new Font("Arial", Font.PLAIN, 13));
        g.setColor(new Color(160, 160, 160));
        drawCenteredString(g, "ENTER -> Menu    R -> Replay", by + 200);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    /**
     * Draws a simple centered overlay band (used for the PAUSED screen).
     * Renders a dark translucent bar with a title and subtitle centered on
     * screen.
     *
     * @param g the Graphics context
     * @param title large text displayed in the center
     * @param sub smaller hint text displayed below the title
     * @param col color used for the title text
     */
    private void drawCenteredOverlay(Graphics g, String title, String sub, Color col) {
        // Dark translucent horizontal band
        g.setColor(new Color(0, 0, 0, 160));
        g.fillRect(0, getHeight() / 2 - 42, getWidth(), 84);

        // Title text
        g.setFont(new Font("Arial", Font.BOLD, 26));
        g.setColor(col);
        drawCenteredString(g, title, getHeight() / 2 + 2);

        // Subtitle hint text
        g.setFont(new Font("Arial", Font.PLAIN, 13));
        g.setColor(new Color(180, 180, 180));
        drawCenteredString(g, sub, getHeight() / 2 + 28);
    }

    /**
     * Draws a string horizontally centered on the panel at the given Y
     * coordinate.
     *
     * @param g the Graphics context
     * @param s the string to draw
     * @param y the Y coordinate of the text baseline
     */
    private void drawCenteredString(Graphics g, String s, int y) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(s, (getWidth() - fm.stringWidth(s)) / 2, y);
    }

    // ── Input handling ────────────────────────────────────────────────────────
    /**
     * Forwards key press events to the game controller for handling.
     *
     * @param e the key event
     */
    @Override
    public void keyPressed(KeyEvent e) {
        controller.handleKey(e.getKeyCode());
    }

    /**
     * Not used but required by the KeyListener interface. Key typed events are
     * ignored; input is handled in keyPressed instead.
     * @param e
     */
    @Override
    public void keyTyped(KeyEvent e) {
    }

    /**
     * Not used but required by the KeyListener interface. Key release events
     * are ignored; input is handled in keyPressed instead.
     * @param e
     */
    @Override
    public void keyReleased(KeyEvent e) {
    }
}
