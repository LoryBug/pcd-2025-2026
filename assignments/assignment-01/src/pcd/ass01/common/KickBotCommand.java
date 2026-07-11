package pcd.ass01.common;

/** Command produced by the bot thread when it decides to shoot. */
public record KickBotCommand(Vec2 impulse) implements GameCommand {

    @Override
    public void execute(GameModel model) {
        model.kickBot(impulse);
    }
}
