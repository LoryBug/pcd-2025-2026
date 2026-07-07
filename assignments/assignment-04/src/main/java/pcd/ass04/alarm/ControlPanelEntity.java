package pcd.ass04.alarm;

import java.time.Duration;
import java.util.Set;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.javadsl.TimerScheduler;
import org.apache.pekko.actor.typed.receptionist.Receptionist;
import org.apache.pekko.actor.typed.receptionist.ServiceKey;

public final class ControlPanelEntity {

    public static final ServiceKey<Command> SERVICE_KEY = ServiceKey.create(Command.class, "alarm-control-panel");

    public sealed interface Command extends JsonSerializable permits Arm, Disarm, SensorEvent, GetState,
            ExitDelayExpired, EntryDelayExpired {
    }

    public record Arm(String pin, Set<String> zones) implements Command {
        public Arm {
            zones = Set.copyOf(zones);
        }
    }

    public record Disarm(String pin) implements Command {
    }

    public record SensorEvent(String sensorId, String zone, SensorKind kind) implements Command {
    }

    public record GetState(ActorRef<AlarmSnapshot> replyTo) implements Command {
    }

    private enum ExitDelayExpired implements Command {
        INSTANCE
    }

    private enum EntryDelayExpired implements Command {
        INSTANCE
    }

    private static final String EXIT_TIMER = "exit-delay";
    private static final String ENTRY_TIMER = "entry-delay";

    private ControlPanelEntity() {
    }

    public static Behavior<Command> create(String pin, Duration exitDelay, Duration entryDelay, Set<String> allZones) {
        return Behaviors.setup(context -> {
            context.getSystem().receptionist().tell(Receptionist.register(SERVICE_KEY, context.getSelf()));
            return Behaviors.withTimers(timers -> recovery(pin, exitDelay, entryDelay, Set.copyOf(allZones), timers));
        });
    }

    private static Behavior<Command> recovery(String pin, Duration exitDelay, Duration entryDelay, Set<String> allZones,
            TimerScheduler<Command> timers) {
        return Behaviors.receive(Command.class)
            .onMessage(Disarm.class, msg -> {
                if (pin.equals(msg.pin())) {
                    return disarmed(pin, exitDelay, entryDelay, allZones, timers);
                }
                return Behaviors.same();
            })
            .onMessage(Arm.class, msg -> Behaviors.same())
            .onMessage(SensorEvent.class, msg -> Behaviors.same())
            .onMessage(GetState.class, msg -> {
                msg.replyTo().tell(new AlarmSnapshot(AlarmState.RECOVERY, Set.of(), false));
                return Behaviors.same();
            })
            .build();
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
            .onMessage(SensorEvent.class, msg -> Behaviors.same())
            .onMessage(GetState.class, msg -> {
                msg.replyTo().tell(new AlarmSnapshot(AlarmState.DISARMED, Set.of(), false));
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
            .onMessage(SensorEvent.class, msg -> Behaviors.same())
            .onMessage(GetState.class, msg -> {
                msg.replyTo().tell(new AlarmSnapshot(AlarmState.EXIT_DELAY, activeZones, false));
                return Behaviors.same();
            })
            .build();
    }

    private static Behavior<Command> armed(String pin, Duration exitDelay, Duration entryDelay, Set<String> allZones,
            Set<String> activeZones, TimerScheduler<Command> timers) {
        return Behaviors.receive(Command.class)
            .onMessage(SensorEvent.class, msg -> {
                if (!activeZones.contains(msg.zone())) {
                    return Behaviors.same();
                }
                timers.startSingleTimer(ENTRY_TIMER, EntryDelayExpired.INSTANCE, entryDelay);
                return entryDelay(pin, exitDelay, entryDelay, allZones, activeZones, timers);
            })
            .onMessage(Disarm.class, msg -> {
                if (pin.equals(msg.pin())) {
                    return disarmed(pin, exitDelay, entryDelay, allZones, timers);
                }
                return Behaviors.same();
            })
            .onMessage(Arm.class, msg -> Behaviors.same())
            .onMessage(GetState.class, msg -> {
                msg.replyTo().tell(new AlarmSnapshot(AlarmState.ARMED, activeZones, false));
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
                if (pin.equals(msg.pin())) {
                    timers.cancel(ENTRY_TIMER);
                    return disarmed(pin, exitDelay, entryDelay, allZones, timers);
                }
                return Behaviors.same();
            })
            .onMessage(Arm.class, msg -> Behaviors.same())
            .onMessage(SensorEvent.class, msg -> Behaviors.same())
            .onMessage(GetState.class, msg -> {
                msg.replyTo().tell(new AlarmSnapshot(AlarmState.ENTRY_DELAY, activeZones, false));
                return Behaviors.same();
            })
            .build();
    }

    private static Behavior<Command> alarm(String pin, Duration exitDelay, Duration entryDelay, Set<String> allZones,
            Set<String> activeZones, TimerScheduler<Command> timers) {
        return Behaviors.receive(Command.class)
            .onMessage(Disarm.class, msg -> {
                if (pin.equals(msg.pin())) {
                    return disarmed(pin, exitDelay, entryDelay, allZones, timers);
                }
                return Behaviors.same();
            })
            .onMessage(Arm.class, msg -> Behaviors.same())
            .onMessage(SensorEvent.class, msg -> Behaviors.same())
            .onMessage(GetState.class, msg -> {
                msg.replyTo().tell(new AlarmSnapshot(AlarmState.ALARM, activeZones, true));
                return Behaviors.same();
            })
            .build();
    }
}
