package pcd.ass04.cs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CoordinatorStateTest {

    @Test
    void grantsLockInFifoOrder() {
        CoordinatorState state = new CoordinatorState();

        assertEquals("p1", state.onRequest("p1", "reply-1", "c1").orElseThrow().processId());
        assertTrue(state.onRequest("p2", "reply-2", "c2").isEmpty());
        assertTrue(state.onRequest("p3", "reply-3", "c3").isEmpty());
        assertEquals(2, state.waitingCount());

        assertEquals("p2", state.onRelease("p1").orElseThrow().processId());
        assertEquals("p3", state.onRelease("p2").orElseThrow().processId());
        assertTrue(state.onRelease("p3").isEmpty());
        assertTrue(state.currentHolder().isEmpty());
    }

    @Test
    void removesTimedOutWaitingProcessOnRelease() {
        CoordinatorState state = new CoordinatorState();

        state.onRequest("p1", "reply-1", "c1");
        state.onRequest("p2", "reply-2", "c2");
        assertTrue(state.onRelease("p2").isEmpty());

        assertTrue(state.onRelease("p1").isEmpty());
        assertTrue(state.currentHolder().isEmpty());
    }
}
