package util;

import javax.sound.sampled.*;

/**
 * SoundManager class - Handles all game sound effects using procedural audio.
 * No external sound files needed - all sounds are generated in real-time.
 *
 * Sounds are created by generating sine/square wave byte arrays and playing
 * them through Java's AudioSystem.
 *
 * @author M4N
 */
public class SoundManager {

    // Audio format: 44100 Hz, 8-bit, mono, signed, big-endian
    private static final float SAMPLE_RATE = 44100f;
    private static final AudioFormat FORMAT
            = new AudioFormat(SAMPLE_RATE, 8, 1, true, true);

    // Sound clips
    private Clip clipEatDot;     // Eating small dot
    private Clip clipPellet;     // Eating power pellet
    private Clip clipEatGhost;   // Eating ghost
    private Clip clipDie;        // Pacman death
    private Clip clipLevelWin;   // Level complete
    private Clip clipWaka;       // Movement sound (waka waka)

    private boolean wakaPlaying = false;

    /**
     * Initializes all sound clips by generating audio data.
     */
    public SoundManager() {
        try {
            clipEatDot = buildClip(genEatDot());
            clipPellet = buildClip(genPellet());
            clipEatGhost = buildClip(genEatGhost());
            clipDie = buildClip(genDie());
            clipLevelWin = buildClip(genLevelWin());
            clipWaka = buildClip(genWaka());
        } catch (Exception e) {
            // Game continues without sound if audio fails to initialize
            System.err.println("SoundManager: Could not initialize audio - " + e.getMessage());
        }
    }

    // ====================== PLAY SOUND METHODS ======================
    /**
     * Plays sound for eating a small dot.
     */
    public void playEatDot() {
        playOnce(clipEatDot);
    }

    /**
     * Plays sound for eating a power pellet.
     */
    public void playPellet() {
        playOnce(clipPellet);
    }

    /**
     * Plays sound for eating a ghost.
     */
    public void playEatGhost() {
        playOnce(clipEatGhost);
    }

    /**
     * Plays Pacman death sound and stops movement sound.
     */
    public void playDie() {
        stopWaka();
        playOnce(clipDie);
    }

    /**
     * Plays level complete sound and stops movement sound.
     */
    public void playLevelWin() {
        stopWaka();
        playOnce(clipLevelWin);
    }

    /**
     * Starts looping waka waka sound while Pacman is moving.
     */
    public void startWaka() {
        if (clipWaka != null && !wakaPlaying) {
            clipWaka.setFramePosition(0);
            clipWaka.loop(Clip.LOOP_CONTINUOUSLY);
            wakaPlaying = true;
        }
    }

    /**
     * Stops the waka waka movement sound.
     */
    public void stopWaka() {
        if (clipWaka != null && wakaPlaying) {
            clipWaka.stop();
            wakaPlaying = false;
        }
    }

    /**
     * Stops all currently playing sounds.
     */
    public void stopAll() {
        stopWaka();
        stopClip(clipEatDot);
        stopClip(clipPellet);
        stopClip(clipEatGhost);
        stopClip(clipDie);
        stopClip(clipLevelWin);
    }

    // ====================== HELPER METHODS ======================
    /**
     * Plays a clip once from the beginning.
     */
    private void playOnce(Clip clip) {
        if (clip == null) {
            return;
        }
        clip.stop();
        clip.setFramePosition(0);
        clip.start();
    }

    /**
     * Stops a clip if it is currently playing.
     */
    private void stopClip(Clip clip) {
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }

    /**
     * Builds a Clip from raw audio byte data.
     */
    private Clip buildClip(byte[] data) throws Exception {
        AudioInputStream ais = new AudioInputStream(
                new java.io.ByteArrayInputStream(data),
                FORMAT,
                data.length
        );
        Clip clip = AudioSystem.getClip();
        clip.open(ais);
        return clip;
    }

    // ====================== SOUND GENERATION ======================
    /**
     * Generates a sine wave sound.
     *
     * @param freq frequency in Hz
     * @param durationMs duration in milliseconds
     * @param volume volume level (0.0 - 1.0)
     */
    private byte[] genSine(double freq, int durationMs, double volume) {
        int n = (int) (SAMPLE_RATE * durationMs / 1000.0);
        byte[] buf = new byte[n];
        for (int i = 0; i < n; i++) {
            double t = i / SAMPLE_RATE;
            buf[i] = (byte) (Math.sin(2 * Math.PI * freq * t) * 127 * volume);
        }
        return buf;
    }

    /**
     * Generates a square wave sound (retro game style).
     */
    private byte[] genSquare(double freq, int durationMs, double volume) {
        int n = (int) (SAMPLE_RATE * durationMs / 1000.0);
        byte[] buf = new byte[n];
        for (int i = 0; i < n; i++) {
            double t = i / SAMPLE_RATE;
            double sin = Math.sin(2 * Math.PI * freq * t);
            buf[i] = (byte) ((sin >= 0 ? 1 : -1) * 127 * volume);
        }
        return buf;
    }

    /**
     * Concatenates multiple byte arrays into one.
     */
    private byte[] concat(byte[]... parts) {
        int total = 0;
        for (byte[] p : parts) {
            total += p.length;
        }
        byte[] out = new byte[total];
        int pos = 0;
        for (byte[] p : parts) {
            System.arraycopy(p, 0, out, pos, p.length);
            pos += p.length;
        }
        return out;
    }

    // ====================== SPECIFIC SOUND DEFINITIONS ======================
    private byte[] genEatDot() {
        return genSquare(600, 30, 0.25);
    }

    private byte[] genPellet() {
        return concat(
                genSquare(300, 80, 0.5),
                genSquare(500, 80, 0.5),
                genSquare(700, 80, 0.5)
        );
    }

    private byte[] genEatGhost() {
        return concat(
                genSquare(200, 60, 0.6),
                genSquare(400, 60, 0.6),
                genSquare(600, 60, 0.6),
                genSquare(800, 80, 0.6)
        );
    }

    private byte[] genDie() {
        return concat(
                genSquare(500, 80, 0.7),
                genSquare(450, 80, 0.7),
                genSquare(400, 80, 0.7),
                genSquare(350, 80, 0.7),
                genSquare(300, 80, 0.7),
                genSquare(250, 100, 0.7),
                genSquare(200, 100, 0.7),
                genSquare(150, 150, 0.7)
        );
    }

    private byte[] genLevelWin() {
        return concat(
                genSquare(523, 100, 0.5), // C5
                genSquare(659, 100, 0.5), // E5
                genSquare(784, 100, 0.5), // G5
                genSquare(1047, 200, 0.5) // C6
        );
    }

    private byte[] genWaka() {
        return concat(
                genSquare(400, 60, 0.15),
                silence(40),
                genSquare(300, 60, 0.15),
                silence(40)
        );
    }

    /**
     * Creates a silent audio segment.
     */
    private byte[] silence(int durationMs) {
        int n = (int) (SAMPLE_RATE * durationMs / 1000.0);
        return new byte[n]; // Zero bytes = silence
    }
}
