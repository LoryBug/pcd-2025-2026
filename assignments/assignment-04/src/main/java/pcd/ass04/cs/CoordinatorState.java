package pcd.ass04.cs;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

public final class CoordinatorState {

    private final Deque<Grant> waiting = new ArrayDeque<>();
    private String currentHolder;

    public synchronized Optional<Grant> onRequest(String processId, String replyTo, String correlationId) {
        Grant grant = new Grant(processId, replyTo, correlationId);
        if (currentHolder == null) {
            currentHolder = processId;
            return Optional.of(grant);
        }
        waiting.addLast(grant);
        return Optional.empty();
    }

    public synchronized Optional<Grant> onRelease(String processId) {
        if (!processId.equals(currentHolder)) {
            waiting.removeIf(grant -> grant.processId().equals(processId));
            return Optional.empty();
        }
        Grant next = waiting.pollFirst();
        if (next == null) {
            currentHolder = null;
            return Optional.empty();
        }
        currentHolder = next.processId();
        return Optional.of(next);
    }

    public synchronized Optional<String> currentHolder() {
        return Optional.ofNullable(currentHolder);
    }

    public synchronized int waitingCount() {
        return waiting.size();
    }

    public record Grant(String processId, String replyTo, String correlationId) {
    }
}
