package pcd.ass04.alarm;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.time.Duration;
import java.util.Set;
import org.apache.pekko.actor.typed.ActorRef;
import org.apache.pekko.actor.typed.ActorSystem;
import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

public final class AlarmClusterNode {

    private static final String PIN = "1234";
    private static final Set<String> ZONES = Set.of("perimeter", "living", "night");

    private AlarmClusterNode() {
    }

    public static void main(String[] args) {
        String role = args.length > 0 ? args[0] : "control";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 25520;
        ActorSystem<Void> system = ActorSystem.create(root(role, args), "alarm-cluster", config(role, port));
        system.log().info("Started alarm cluster node role={} port={}", role, port);
    }

    private static Behavior<Void> root(String role, String[] args) {
        return Behaviors.setup(context -> {
            switch (role) {
                case "control" -> context.spawn(ControlPanelEntity.create(
                    PIN, Duration.ofSeconds(20), Duration.ofSeconds(10), ZONES), "control-panel");
                case "sensor" -> {
                    String sensorId = args.length > 2 ? args[2] : "front-door";
                    String zone = args.length > 3 ? args[3] : "perimeter";
                    SensorKind kind = args.length > 4 ? SensorKind.valueOf(args[4]) : SensorKind.DOOR;
                    ActorRef<DistributedSensorActor.Command> sensor = context.spawn(
                        DistributedSensorActor.create(sensorId, zone, kind), "sensor-" + sensorId);
                    if (args.length > 5 && "demo".equals(args[5])) {
                        context.scheduleOnce(Duration.ofSeconds(8), sensor, DistributedSensorActor.Trigger.INSTANCE);
                    }
                }
                case "keypad" -> {
                    ActorRef<DistributedKeypadActor.Command> keypad = context.spawn(DistributedKeypadActor.create(), "keypad");
                    if (args.length > 2 && "demo".equals(args[2])) {
                        context.scheduleOnce(Duration.ofSeconds(3), keypad, new DistributedKeypadActor.Disarm(PIN));
                        context.scheduleOnce(Duration.ofSeconds(5), keypad,
                            new DistributedKeypadActor.Arm(PIN, Set.of("perimeter")));
                    }
                }
                default -> throw new IllegalArgumentException("Unknown role: " + role);
            }
            return Behaviors.empty();
        });
    }

    private static Config config(String role, int port) {
        String overrides = """
            pekko.remote.artery.canonical.port = %d
            pekko.cluster.roles = [%s]
            """.formatted(port, role);
        return ConfigFactory.parseString(overrides).withFallback(ConfigFactory.load());
    }
}
