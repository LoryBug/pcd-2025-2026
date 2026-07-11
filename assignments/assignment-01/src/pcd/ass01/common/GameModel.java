package pcd.ass01.common;

/**
 * Common interface used by both implementations of the game model.
 *
 * <p>The multithreaded version uses {@link Board}; the task-based version uses
 * {@code TaskBasedBoard}. The rest of the application, especially
 * {@code GameLoopThread} and {@code BotThread}, depends only on this interface.
 */
public interface GameModel {

    /**
     * Advances the physical state of the game by the given elapsed time.
     * Implementations synchronize this method because it mutates balls, scores
     * and game status.
     *
     * @param dt elapsed time in milliseconds since the previous update
     */
    void updateState(long dt);

    /** Applies an impulse to the human-controlled ball. */
    void kickHuman(Vec2 impulse);

    /** Applies an impulse to the bot-controlled ball. */
    void kickBot(Vec2 impulse);

    /**
     * @return true when the bot ball is almost still and can receive a new shot
     */
    boolean isBotStopped();

    /**
     * @return the current game status
     */
    GameStatus status();

    /**
     * Creates an immutable snapshot for the view layer.
     *
     * @param fps current frame rate estimate to show in the HUD
     * @return a snapshot detached from the mutable model
     */
    GameSnapshot snapshot(int fps);
}
