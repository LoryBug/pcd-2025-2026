package pcd.ass04.ttt;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public final class TicTacToeServer {

    public static final String HUB_NAME = "TicTacToeHub";

    private TicTacToeServer() {
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 1099;
        Registry registry = createOrGetRegistry(port);
        TicTacToeHub hub = (TicTacToeHub) UnicastRemoteObject.exportObject(new TicTacToeHubImpl(), 0);
        registry.rebind(HUB_NAME, hub);
        System.out.printf("TicTacToe RMI server ready on port %d, binding %s%n", port, HUB_NAME);
    }

    private static Registry createOrGetRegistry(int port) throws Exception {
        try {
            return LocateRegistry.createRegistry(port);
        } catch (Exception e) {
            return LocateRegistry.getRegistry(port);
        }
    }
}
