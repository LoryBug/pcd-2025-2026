package pcd.ass01.common;

public record KickBotCommand(Vec2 impulse) implements GameCommand {

    @Override
    public void execute(GameModel model) {
        model.kickBot(impulse);
    }
}
