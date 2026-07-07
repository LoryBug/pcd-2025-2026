package pcd.ass03.alarm;

import java.time.Duration;
import java.util.Set;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.TimerScheduler;

public final class ControlPanelActor {

    public sealed interface Command permits Arm, Disarm, SensorTriggered, GetState, ExitDelayExpired,
            EntryDelayExpired {
    }

    public record Arm(String pin, Set<String> zones) implements Command {
        public Arm {
            zones = Set.copyOf(zones);
        }
    }

    public record Disarm(String pin) implements Command {
    }

    public record SensorTriggered(String sensorId, String zone, SensorKind kind) implements Command {
    }

    public record GetState(ActorRef<StateSnapshot> replyTo) implements Command {
    }

    private enum ExitDelayExpired implements Command {
        INSTANCE
    }

    private enum EntryDelayExpired implements Command {
        INSTANCE
    }

    private static final String EXIT_TIMER = "exit-delay";
    private static final String ENTRY_TIMER = "entry-delay";

    private ControlPanelActor() {
    }

    public static Behavior<Command> create(String correctPin, Duration exitDelay, Duration entryDelay,
            Set<String> allZones) {
        return Behaviors.setup(context -> Behaviors.withTimers(timers ->
            disarmed(correctPin, exitDelay, entryDelay, Set.copyOf(allZones), timers)));
    }

    private static Behavior<Command> disarmed(String pin, Duration exitDelay, Duration entryDelay, Set<String> allZones,
            TimerScheduler<Command> timers) {
        return Behaviors.receive(Command.class)
            .onMessage(Arm.class, msg -> {
                if (!pin.equals(msg.pin())) {
                    return Behaviors.same();
                }
                Set<String> activeZones = msg.zones().isEmpty() ? allZones : msg.zones();
                timers.startSingleTimer(EXIT_TIMER, ExitDelayExpired.INSTANCE, exitDelay);
                return exitDelay(pin, exitDelay, entryDelay, allZones, activeZones, timers);
            })
            .onMessage(Disarm.class, msg -> Behaviors.same())
            .onMessage(SensorTriggered.class, msg -> Behaviors.same())
            .onMessage(GetState.class, msg -> {
                msg.replyTo().tell(new StateSnapshot(AlarmState.DISARMED, Set.of(), false));
                return Behaviors.same();
            })
            .build();
    }

    private static Behavior<Command> exitDelay(String pin, Duration exitDelay, Duration entryDelay, Set<String> allZones,
            Set<String> activeZones, TimerScheduler<Command> timers) {
        return Behaviors.receive(Command.class)
            .onMessage(ExitDelayExpired.class,
                msg -> armed(pin, exitDelay, entryDelay, allZones, activeZones, timers))
            .onMessage(Disarm.class, msg -> {
                if (!pin.equals(msg.pin())) {
                    return Behaviors.same();
                }
                timers.cancel(EXIT_TIMER);
                return disarmed(pin, exitDelay, entryDelay, allZones, timers);
            })
            .onMessage(Arm.class, msg -> Behaviors.same())
            .onMessage(SensorTriggered.class, msg -> Behaviors.same())
            .onMessage(GetState.class, msg -> {
                msg.replyTo().tell(new StateSnapshot(AlarmState.EXIT_DELAY, activeZones, false));
                return Behaviors.same();
            })
            .build();
    }

    private static Behavior<Command> armed(String pin, Duration exitDelay, Duration entryDelay, Set<String> allZones,
            Set<String> activeZones, TimerScheduler<Command> timers) {
        return Behaviors.receive(Command.class)
            .onMessage(SensorTriggered.class, msg -> {
                if (!activeZones.contains(msg.zone())) {
                    return Behaviors.same();
                }
                timers.startSingleTimer(ENTRY_TIMER, EntryDelayExpired.INSTANCE, entryDelay);
                return entryDelay(pin, exitDelay, entryDelay, allZones, activeZones, timers);
            })
            .onMessage(Disarm.class, msg -> {
                if (!pin.equals(msg.pin())) {
                    return Behaviors.same();
                }
                return disarmed(pin, exitDelay, entryDelay, allZones, timers);
            })
            .onMessage(Arm.class, msg -> Behaviors.same())
            .onMessage(GetState.class, msg -> {
                msg.replyTo().tell(new StateSnapshot(AlarmState.ARMED, activeZones, false));
                return Behaviors.same();
            })
            .build();
    }

    private static Behavior<Command> entryDelay(String pin, Duration exitDelay, Duration entryDelay, Set<String> allZones,
            Set<String> activeZones, TimerScheduler<Command> timers) {
        return Behaviors.receive(Command.class)
            .onMessage(EntryDelayExpired.class,
                msg -> alarm(pin, exitDelay, entryDelay, allZones, activeZones, timers))
            .onMessage(Disarm.class, msg -> {
                if (!pin.equals(msg.pin())) {
                    return Behaviors.same();
                }
                timers.cancel(ENTRY_TIMER);
                return disarmed(pin, exitDelay, entryDelay, allZones, timers);
            })
            .onMessage(Arm.class, msg -> Behaviors.same())
            .onMessage(SensorTriggered.class, msg -> Behaviors.same())
            .onMessage(GetState.class, msg -> {
                msg.replyTo().tell(new StateSnapshot(AlarmState.ENTRY_DELAY, activeZones, false));
                return Behaviors.same();
            })
            .build();
    }

    private static Behavior<Command> alarm(String pin, Duration exitDelay, Duration entryDelay, Set<String> allZones,
            Set<String> activeZones, TimerScheduler<Command> timers) {
        return Behaviors.receive(Command.class)
            .onMessage(Disarm.class, msg -> {
                if (!pin.equals(msg.pin())) {
                    return Behaviors.same();
                }
                return disarmed(pin, exitDelay, entryDelay, allZones, timers);
            })
            .onMessage(Arm.class, msg -> Behaviors.same())
            .onMessage(SensorTriggered.class, msg -> Behaviors.same())
            .onMessage(GetState.class, msg -> {
                msg.replyTo().tell(new StateSnapshot(AlarmState.ALARM, activeZones, true));
                return Behaviors.same();
            })
            .build();
    }
}
