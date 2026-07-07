package pcd.ass01.common;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public final class CommandBuffer {

    private final LinkedList<GameCommand> commands = new LinkedList<>();
    private final int maxSize;

    public CommandBuffer(int maxSize) {
        this.maxSize = maxSize;
    }

    public synchronized boolean offer(GameCommand command) {
        if (commands.size() == maxSize) {
            return false;
        }
        commands.addLast(command);
        notifyAll();
        return true;
    }

    public synchronized List<GameCommand> drainToList() {
        List<GameCommand> drained = new ArrayList<>(commands);
        commands.clear();
        notifyAll();
        return drained;
    }

    public synchronized int size() {
        return commands.size();
    }
}
