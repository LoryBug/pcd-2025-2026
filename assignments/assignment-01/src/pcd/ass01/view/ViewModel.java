package pcd.ass01.view;

import pcd.ass01.common.GameSnapshot;

public final class ViewModel {

    private GameSnapshot snapshot;

    public synchronized void update(GameSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public synchronized GameSnapshot snapshot() {
        return snapshot;
    }
}
