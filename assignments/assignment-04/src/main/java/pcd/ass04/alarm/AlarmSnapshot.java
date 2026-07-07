package pcd.ass04.alarm;

import java.util.Set;

public record AlarmSnapshot(AlarmState state, Set<String> activeZones, boolean sirenOn) implements JsonSerializable {

    public AlarmSnapshot {
        activeZones = Set.copyOf(activeZones);
    }
}
