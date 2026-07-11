package pcd.ass01.view;

import pcd.ass01.common.GameSnapshot;

/** Thread-safe holder of the last immutable snapshot rendered by Swing. */
public final class ViewModel {

    private GameSnapshot snapshot;

    /** Replaces the snapshot published by the game loop. */
    public synchronized void update(GameSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    /**
     * @return the latest snapshot available to the EDT
     */
    public synchronized GameSnapshot snapshot() {
        return snapshot;
    }
}
