package pcd.ass01.common;

/** Command produced by the Swing EDT when the human player presses a key. */
public record KickHumanCommand(Vec2 impulse) implements GameCommand {

    @Override
    public void execute(GameModel model) {
        model.kickHuman(impulse);
    }
}
