package pcd.ass03.alarm;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.javadsl.AskPattern;

public final class AlarmDemo {

    private static final String PIN = "1234";

    private AlarmDemo() {
    }

    public static void main(String[] args) throws Exception {
        List<SensorDefinition> sensors = List.of(
            new SensorDefinition("front-door", "perimeter", SensorKind.DOOR),
            new SensorDefinition("living-motion", "living", SensorKind.MOTION),
            new SensorDefinition("bedroom-window", "night", SensorKind.WINDOW)
        );
        ActorSystem<AlarmSystemActor.Command> system = ActorSystem.create(
            AlarmSystemActor.create(PIN, Duration.ofMillis(300), Duration.ofMillis(300), sensors),
            "smart-home-alarm");

        try {
            printState(system, "initial");
            system.tell(new AlarmSystemActor.ArmPartial(PIN, java.util.Set.of("perimeter")));
            Thread.sleep(350);
            printState(system, "after exit delay");
            system.tell(new AlarmSystemActor.TriggerSensor("living-motion"));
            Thread.sleep(100);
            printState(system, "inactive living sensor ignored");
            system.tell(new AlarmSystemActor.TriggerSensor("front-door"));
            Thread.sleep(100);
            printState(system, "entry delay started");
            system.tell(new AlarmSystemActor.Disarm(PIN));
            Thread.sleep(100);
            printState(system, "disarmed by keypad");
        } finally {
            system.terminate();
        }
    }

    private static void printState(ActorSystem<AlarmSystemActor.Command> system, String label) {
        Duration timeout = Duration.ofSeconds(2);
        CompletionStage<StateSnapshot> state = AskPattern.ask(
            system,
            AlarmSystemActor.GetState::new,
            timeout,
            system.scheduler());
        System.out.printf("%s: %s%n", label, state.toCompletableFuture().join());
    }
}
