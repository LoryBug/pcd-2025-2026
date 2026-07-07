package pcd.ass04.cs;

public record RabbitConfig(String host, int port, String username, String password) {

    public static RabbitConfig localhost() {
        return new RabbitConfig("localhost", 5672, "guest", "guest");
    }
}
