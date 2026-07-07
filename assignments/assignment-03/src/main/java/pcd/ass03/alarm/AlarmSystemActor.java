package pcd.ass03.alarm;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.SupervisorStrategy;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

public final class AlarmSystemActor {

    public sealed interface Command permits ArmFull, ArmPartial, Disarm, TriggerSensor, GetState {
    }

    public record ArmFull(String pin) implements Command {
    }

    public record ArmPartial(String pin, Set<String> zones) implements Command {
        public ArmPartial {
            zones = Set.copyOf(zones);
        }
    }

    public record Disarm(String pin) implements Command {
    }

    public record TriggerSensor(String sensorId) implements Command {
    }

    public record GetState(ActorRef<StateSnapshot> replyTo) implements Command {
    }

    private AlarmSystemActor() {
    }

    public static Behavior<Command> create(String pin, Duration exitDelay, Duration entryDelay,
            List<SensorDefinition> sensors) {
        return Behaviors.setup(context -> {
            Set<String> allZones = sensors.stream().map(SensorDefinition::zone).collect(Collectors.toSet());
            Behavior<ControlPanelActor.Command> supervisedControlPanel = Behaviors.supervise(
                ControlPanelActor.create(pin, exitDelay, entryDelay, allZones))
                .onFailure(SupervisorStrategy.restart());
            ActorRef<ControlPanelActor.Command> controlPanel = context.spawn(
                supervisedControlPanel, "control-panel");
            ActorRef<KeypadActor.Command> keypad = context.spawn(
                KeypadActor.create(controlPanel), "keypad");

            Map<String, ActorRef<SensorActor.Command>> sensorRefs = new HashMap<>();
            for (SensorDefinition sensor : sensors) {
                ActorRef<SensorActor.Command> ref = context.spawn(
                    SensorActor.create(sensor, controlPanel), "sensor-" + sensor.id());
                sensorRefs.put(sensor.id(), ref);
            }

            return running(keypad, controlPanel, Map.copyOf(sensorRefs));
        });
    }

    private static Behavior<Command> running(ActorRef<KeypadActor.Command> keypad,
            ActorRef<ControlPanelActor.Command> controlPanel,
            Map<String, ActorRef<SensorActor.Command>> sensors) {
        return Behaviors.receive(Command.class)
            .onMessage(ArmFull.class, msg -> {
                keypad.tell(new KeypadActor.ArmFull(msg.pin()));
                return Behaviors.same();
            })
            .onMessage(ArmPartial.class, msg -> {
                keypad.tell(new KeypadActor.ArmPartial(msg.pin(), msg.zones()));
                return Behaviors.same();
            })
            .onMessage(Disarm.class, msg -> {
                keypad.tell(new KeypadActor.Disarm(msg.pin()));
                return Behaviors.same();
            })
            .onMessage(TriggerSensor.class, msg -> {
                ActorRef<SensorActor.Command> sensor = sensors.get(msg.sensorId());
                if (sensor != null) {
                    sensor.tell(SensorActor.Trigger.INSTANCE);
                }
                return Behaviors.same();
            })
            .onMessage(GetState.class, msg -> {
                controlPanel.tell(new ControlPanelActor.GetState(msg.replyTo()));
                return Behaviors.same();
            })
            .build();
    }
}
