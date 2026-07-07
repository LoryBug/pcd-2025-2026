package pcd.ass01.common;

public interface GameModel {
    void updateState(long dt);

    void kickHuman(Vec2 impulse);

    void kickBot(Vec2 impulse);

    boolean isBotStopped();

    GameStatus status();

    GameSnapshot snapshot(int fps);
}
