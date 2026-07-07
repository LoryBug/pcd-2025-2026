package pcd.ass04.alarm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.javadsl.AskPattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

class ControlPanelEntityTest {

    private static final ActorTestKit TEST_KIT = ActorTestKit.create();
    private static final String PIN = "1234";

    @AfterAll
    static void tearDown() {
        TEST_KIT.shutdownTestKit();
    }

    @Test
    void startsInSafeRecoveryAndRequiresPinToDisarm() throws InterruptedException {
        ActorRef<ControlPanelEntity.Command> panel = spawn("recovery");

        assertEquals(AlarmState.RECOVERY, state(panel).state());
        panel.tell(new ControlPanelEntity.Arm(PIN, Set.of("perimeter")));
        assertEquals(AlarmState.RECOVERY, state(panel).state());

        panel.tell(new ControlPanelEntity.Disarm(PIN));
        assertEquals(AlarmState.DISARMED, awaitState(panel, AlarmState.DISARMED).state());
    }

    @Test
    void activeZoneSensorRaisesAlarmAfterEntryDelay() throws InterruptedException {
        ActorRef<ControlPanelEntity.Command> panel = spawn("alarm-flow");

        panel.tell(new ControlPanelEntity.Disarm(PIN));
        awaitState(panel, AlarmState.DISARMED);
        panel.tell(new ControlPanelEntity.Arm(PIN, Set.of("perimeter")));
        awaitState(panel, AlarmState.ARMED);

        panel.tell(new ControlPanelEntity.SensorEvent("front-door", "perimeter", SensorKind.DOOR));
        awaitState(panel, AlarmState.ENTRY_DELAY);
        AlarmSnapshot snapshot = awaitState(panel, AlarmState.ALARM);

        assertTrue(snapshot.sirenOn());
    }

    @Test
    void inactiveZoneSensorIsIgnored() throws InterruptedException {
        ActorRef<ControlPanelEntity.Command> panel = spawn("zones");

        panel.tell(new ControlPanelEntity.Disarm(PIN));
        awaitState(panel, AlarmState.DISARMED);
        panel.tell(new ControlPanelEntity.Arm(PIN, Set.of("perimeter")));
        awaitState(panel, AlarmState.ARMED);

        panel.tell(new ControlPanelEntity.SensorEvent("living-motion", "living", SensorKind.MOTION));
        AlarmSnapshot snapshot = state(panel);

        assertEquals(AlarmState.ARMED, snapshot.state());
        assertFalse(snapshot.sirenOn());
    }

    private static ActorRef<ControlPanelEntity.Command> spawn(String name) {
        return TEST_KIT.spawn(ControlPanelEntity.create(
            PIN, Duration.ofMillis(60), Duration.ofMillis(60), Set.of("perimeter", "living")), name);
    }

    private static AlarmSnapshot state(ActorRef<ControlPanelEntity.Command> panel) {
        CompletionStage<AlarmSnapshot> state = AskPattern.ask(
            panel,
            ControlPanelEntity.GetState::new,
            Duration.ofSeconds(2),
            TEST_KIT.system().scheduler());
        return state.toCompletableFuture().join();
    }

    private static AlarmSnapshot awaitState(ActorRef<ControlPanelEntity.Command> panel, AlarmState expected)
            throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        AlarmSnapshot snapshot = state(panel);
        while (snapshot.state() != expected && System.nanoTime() < deadline) {
            Thread.sleep(20);
            snapshot = state(panel);
        }
        if (snapshot.state() != expected) {
            fail("expected state " + expected + " but got " + snapshot);
        }
        return snapshot;
    }
}
