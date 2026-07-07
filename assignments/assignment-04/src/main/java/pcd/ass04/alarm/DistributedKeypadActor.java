package pcd.ass04.alarm;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.receptionist.Receptionist;

public final class DistributedKeypadActor {

    public sealed interface Command extends JsonSerializable permits Arm, Disarm, WrappedListing {
    }

    public record Arm(String pin, Set<String> zones) implements Command {
        public Arm {
            zones = Set.copyOf(zones);
        }
    }

    public record Disarm(String pin) implements Command {
    }

    private record WrappedListing(Receptionist.Listing listing) implements Command {
    }

    private DistributedKeypadActor() {
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(context -> {
            ActorRef<Receptionist.Listing> adapter = context.messageAdapter(
                Receptionist.Listing.class, WrappedListing::new);
            context.getSystem().receptionist().tell(Receptionist.subscribe(ControlPanelEntity.SERVICE_KEY, adapter));
            return running(context, Optional.empty());
        });
    }

    private static Behavior<Command> running(ActorContext<Command> context,
            Optional<ActorRef<ControlPanelEntity.Command>> controlPanel) {
        return Behaviors.receive(Command.class)
            .onMessage(WrappedListing.class, msg -> running(context, firstControlPanel(msg.listing())))
            .onMessage(Arm.class, msg -> {
                controlPanel.ifPresent(ref -> ref.tell(new ControlPanelEntity.Arm(msg.pin(), msg.zones())));
                if (controlPanel.isEmpty()) {
                    context.getLog().warn("Keypad has no discovered control panel");
                }
                return Behaviors.same();
            })
            .onMessage(Disarm.class, msg -> {
                controlPanel.ifPresent(ref -> ref.tell(new ControlPanelEntity.Disarm(msg.pin())));
                if (controlPanel.isEmpty()) {
                    context.getLog().warn("Keypad has no discovered control panel");
                }
                return Behaviors.same();
            })
            .build();
    }

    private static Optional<ActorRef<ControlPanelEntity.Command>> firstControlPanel(Receptionist.Listing listing) {
        Set<ActorRef<ControlPanelEntity.Command>> instances = listing.getServiceInstances(ControlPanelEntity.SERVICE_KEY);
        return instances.stream().min(Comparator.comparing(ActorRef::path));
    }
}
