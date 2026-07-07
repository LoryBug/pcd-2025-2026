package pcd.ass03.alarm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.actor.testkit.typed.javadsl.ActorTestKit;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.javadsl.AskPattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

class AlarmSystemActorTest {

    private static final ActorTestKit TEST_KIT = ActorTestKit.create();
    private static final String PIN = "1234";
    private static final List<SensorDefinition> SENSORS = List.of(
        new SensorDefinition("front-door", "perimeter", SensorKind.DOOR),
        new SensorDefinition("living-motion", "living", SensorKind.MOTION)
    );

    @AfterAll
    static void tearDown() {
        TEST_KIT.shutdownTestKit();
    }

    @Test
    void sensorEventsAreIgnoredWhenDisarmed() {
        ActorRef<AlarmSystemActor.Command> system = spawnAlarm("disarmed-ignore", 80, 80);

        system.tell(new AlarmSystemActor.TriggerSensor("front-door"));

        StateSnapshot snapshot = state(system);
        assertEquals(AlarmState.DISARMED, snapshot.state());
        assertFalse(snapshot.sirenOn());
    }

    @Test
    void armedSensorStartsEntryDelayAndTimeoutRaisesAlarm() throws InterruptedException {
        ActorRef<AlarmSystemActor.Command> system = spawnAlarm("alarm-timeout", 80, 80);

        system.tell(new AlarmSystemActor.ArmFull(PIN));
        awaitState(system, AlarmState.ARMED);

        system.tell(new AlarmSystemActor.TriggerSensor("front-door"));
        awaitState(system, AlarmState.ENTRY_DELAY);

        StateSnapshot snapshot = awaitState(system, AlarmState.ALARM);
        assertEquals(AlarmState.ALARM, snapshot.state());
        assertTrue(snapshot.sirenOn());
    }

    @Test
    void correctPinDisarmsDuringEntryDelay() throws InterruptedException {
        ActorRef<AlarmSystemActor.Command> system = spawnAlarm("entry-disarm", 80, 200);

        system.tell(new AlarmSystemActor.ArmFull(PIN));
        awaitState(system, AlarmState.ARMED);
        system.tell(new AlarmSystemActor.TriggerSensor("front-door"));
        awaitState(system, AlarmState.ENTRY_DELAY);
        system.tell(new AlarmSystemActor.Disarm(PIN));

        StateSnapshot snapshot = awaitState(system, AlarmState.DISARMED);
        assertEquals(AlarmState.DISARMED, snapshot.state());
        assertFalse(snapshot.sirenOn());
    }

    @Test
    void partialArmingIgnoresInactiveZones() throws InterruptedException {
        ActorRef<AlarmSystemActor.Command> system = spawnAlarm("partial-zones", 80, 80);

        system.tell(new AlarmSystemActor.ArmPartial(PIN, Set.of("perimeter")));
        awaitState(system, AlarmState.ARMED);
        system.tell(new AlarmSystemActor.TriggerSensor("living-motion"));

        StateSnapshot snapshot = state(system);
        assertEquals(AlarmState.ARMED, snapshot.state());
        assertEquals(Set.of("perimeter"), snapshot.activeZones());
    }

    private static ActorRef<AlarmSystemActor.Command> spawnAlarm(String name, long exitMs, long entryMs) {
        return TEST_KIT.spawn(
            AlarmSystemActor.create(PIN, Duration.ofMillis(exitMs), Duration.ofMillis(entryMs), SENSORS),
            name);
    }

    private static StateSnapshot state(ActorRef<AlarmSystemActor.Command> system) {
        Duration timeout = Duration.ofSeconds(2);
        CompletionStage<StateSnapshot> state = AskPattern.ask(
            system,
            AlarmSystemActor.GetState::new,
            timeout,
            TEST_KIT.system().scheduler());
        return state.toCompletableFuture().join();
    }

    private static StateSnapshot awaitState(ActorRef<AlarmSystemActor.Command> system, AlarmState expected)
            throws InterruptedException {
        long deadline = System.nanoTime() + Duration.ofSeconds(2).toNanos();
        StateSnapshot snapshot = state(system);
        while (snapshot.state() != expected && System.nanoTime() < deadline) {
            Thread.sleep(20);
            snapshot = state(system);
        }
        if (snapshot.state() != expected) {
            fail("expected state " + expected + " but got " + snapshot);
        }
        return snapshot;
    }
}
