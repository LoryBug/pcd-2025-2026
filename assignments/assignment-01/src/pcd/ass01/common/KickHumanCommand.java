package pcd.ass01.common;

public record KickHumanCommand(Vec2 impulse) implements GameCommand {

    @Override
    public void execute(GameModel model) {
        model.kickHuman(impulse);
    }
}
