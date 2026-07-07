package pcd.ass03.alarm;

import java.util.Set;

public record StateSnapshot(AlarmState state, Set<String> activeZones, boolean sirenOn) {

    public StateSnapshot {
        activeZones = Set.copyOf(activeZones);
    }
}
