package pcd.ass04.alarm;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.ActorContext;
import org.apache.pekko.actor.typed.javadsl.Behaviors;
import org.apache.pekko.actor.typed.receptionist.Receptionist;

public final class DistributedSensorActor {

    public sealed interface Command extends JsonSerializable permits Trigger, WrappedListing {
    }

    public enum Trigger implements Command {
        INSTANCE
    }

    private record WrappedListing(Receptionist.Listing listing) implements Command {
    }

    private DistributedSensorActor() {
    }

    public static Behavior<Command> create(String sensorId, String zone, SensorKind kind) {
        return Behaviors.setup(context -> {
            ActorRef<Receptionist.Listing> adapter = context.messageAdapter(
                Receptionist.Listing.class, WrappedListing::new);
            context.getSystem().receptionist().tell(Receptionist.subscribe(ControlPanelEntity.SERVICE_KEY, adapter));
            return running(context, sensorId, zone, kind, Optional.empty());
        });
    }

    private static Behavior<Command> running(ActorContext<Command> context, String sensorId, String zone, SensorKind kind,
            Optional<ActorRef<ControlPanelEntity.Command>> controlPanel) {
        return Behaviors.receive(Command.class)
            .onMessage(WrappedListing.class, msg -> running(context, sensorId, zone, kind, firstControlPanel(msg.listing())))
            .onMessage(Trigger.class, msg -> {
                controlPanel.ifPresent(ref -> ref.tell(new ControlPanelEntity.SensorEvent(sensorId, zone, kind)));
                if (controlPanel.isEmpty()) {
                    context.getLog().warn("Sensor {} has no discovered control panel", sensorId);
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
