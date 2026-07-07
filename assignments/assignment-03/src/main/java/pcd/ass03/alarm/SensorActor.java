package pcd.ass03.alarm;

import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

public final class SensorActor {

    public sealed interface Command permits Trigger, GetDefinition {
    }

    public enum Trigger implements Command {
        INSTANCE
    }

    public record GetDefinition(ActorRef<SensorDefinition> replyTo) implements Command {
    }

    private SensorActor() {
    }

    public static Behavior<Command> create(SensorDefinition definition,
            ActorRef<ControlPanelActor.Command> controlPanel) {
        return Behaviors.receive(Command.class)
            .onMessage(Trigger.class, msg -> {
                controlPanel.tell(new ControlPanelActor.SensorTriggered(
                    definition.id(), definition.zone(), definition.kind()));
                return Behaviors.same();
            })
            .onMessage(GetDefinition.class, msg -> {
                msg.replyTo().tell(definition);
                return Behaviors.same();
            })
            .build();
    }
}
