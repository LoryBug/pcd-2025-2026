package pcd.ass04.ttt;

import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public final class TicTacToeClient {

    private TicTacToeClient() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.out.println("Usage: <host> <port> <create|join> <game> <player> [row col]...");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String action = args[2];
        String gameName = args[3];
        String playerName = args.length > 4 ? args[4] : "player";

        TicTacToeHub hub = (TicTacToeHub) LocateRegistry.getRegistry(host, port).lookup(TicTacToeServer.HUB_NAME);
        TicTacToeListener listener = (TicTacToeListener) UnicastRemoteObject.exportObject(
            new ConsoleTicTacToeListener(), 0);
        TicTacToeGame game = switch (action) {
            case "create" -> hub.createGame(gameName, playerName, listener);
            case "join" -> hub.joinGame(gameName, playerName, listener);
            default -> throw new IllegalArgumentException("Unknown action: " + action);
        };

        for (int i = 5; i + 1 < args.length; i += 2) {
            int row = Integer.parseInt(args[i]);
            int col = Integer.parseInt(args[i + 1]);
            System.out.println(game.makeMove(playerName, row, col));
        }
        System.out.println(game.snapshot());
    }
}
