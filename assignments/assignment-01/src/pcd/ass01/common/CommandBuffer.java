package pcd.ass01.common;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Monitor-protected command queue between producers and the game loop.
 *
 * <p>The Swing EDT and the bot thread are producers: they enqueue logical
 * commands instead of modifying the board directly. The game loop is the only
 * consumer: once per frame it atomically drains all pending commands and then
 * applies them to the model.
 */
public final class CommandBuffer {

    private final LinkedList<GameCommand> commands = new LinkedList<>();
    private final int maxSize;

    /**
     * @param maxSize maximum number of queued commands before new commands are
     *        rejected
     */
    public CommandBuffer(int maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * Enqueues a command without blocking the caller.
     *
     * <p>This is deliberately non-blocking because one producer is the Swing
     * EDT. Blocking the EDT would make the user interface freeze. If the buffer
     * is full, the command is dropped by returning {@code false}.
     *
     * @param command command produced by the human input or by the bot
     * @return true if the command was accepted, false if the buffer is full
     */
    public synchronized boolean offer(GameCommand command) {
        if (commands.size() == maxSize) {
            return false;
        }
        commands.addLast(command);
        notifyAll();
        return true;
    }

    /**
     * Atomically returns all pending commands and clears the buffer.
     *
     * <p>This is the consumer side of the producer-consumer pattern. The game
     * loop receives a stable list for the current frame, while producers can
     * continue adding commands for future frames after this method releases the
     * monitor.
     *
     * @return commands pending at the instant this method acquired the monitor
     */
    public synchronized List<GameCommand> drainToList() {
        List<GameCommand> drained = new ArrayList<>(commands);
        commands.clear();
        notifyAll();
        return drained;
    }

    /**
     * @return current number of queued commands
     */
    public synchronized int size() {
        return commands.size();
    }
}
