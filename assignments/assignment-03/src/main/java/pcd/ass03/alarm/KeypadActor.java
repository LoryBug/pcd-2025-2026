package pcd.ass03.alarm;

import java.util.Set;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

public final class KeypadActor {

    public sealed interface Command permits ArmFull, ArmPartial, Disarm {
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

    private KeypadActor() {
    }

    public static Behavior<Command> create(ActorRef<ControlPanelActor.Command> controlPanel) {
        return Behaviors.receive(Command.class)
            .onMessage(ArmFull.class, msg -> {
                controlPanel.tell(new ControlPanelActor.Arm(msg.pin(), Set.of()));
                return Behaviors.same();
            })
            .onMessage(ArmPartial.class, msg -> {
                controlPanel.tell(new ControlPanelActor.Arm(msg.pin(), msg.zones()));
                return Behaviors.same();
            })
            .onMessage(Disarm.class, msg -> {
                controlPanel.tell(new ControlPanelActor.Disarm(msg.pin()));
                return Behaviors.same();
            })
            .build();
    }
}
