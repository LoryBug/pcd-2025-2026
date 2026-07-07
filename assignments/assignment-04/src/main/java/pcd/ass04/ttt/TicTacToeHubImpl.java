package pcd.ass04.ttt;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TicTacToeHubImpl implements TicTacToeHub {

    private final Map<String, HostedGame> games = new ConcurrentHashMap<>();
    private final GameExporter exporter;

    public TicTacToeHubImpl() {
        this(TicTacToeHubImpl::exportForRmi);
    }

    public TicTacToeHubImpl(GameExporter exporter) {
        this.exporter = exporter;
    }

    @Override
    public TicTacToeGame createGame(String gameName, String playerName, TicTacToeListener listener)
            throws RemoteException {
        HostedGame hosted = new HostedGame(new TicTacToeGameImpl(gameName, playerName), null);
        HostedGame previous = games.putIfAbsent(gameName, hosted.withRemote(exporter.export(hosted.impl())));
        if (previous != null) {
            throw new IllegalArgumentException("Game already exists: " + gameName);
        }
        HostedGame created = games.get(gameName);
        created.impl().addListener(listener);
        return created.remote();
    }

    @Override
    public TicTacToeGame joinGame(String gameName, String playerName, TicTacToeListener listener)
            throws RemoteException {
        HostedGame hosted = games.get(gameName);
        if (hosted == null) {
            throw new IllegalArgumentException("Game not found: " + gameName);
        }
        hosted.impl().addListener(listener);
        hosted.impl().join(playerName);
        return hosted.remote();
    }

    @Override
    public List<String> listGames() {
        return new ArrayList<>(games.keySet());
    }

    private static TicTacToeGame exportForRmi(TicTacToeGameImpl game) throws RemoteException {
        return (TicTacToeGame) UnicastRemoteObject.exportObject(game, 0);
    }

    public interface GameExporter {
        TicTacToeGame export(TicTacToeGameImpl game) throws RemoteException;
    }

    private record HostedGame(TicTacToeGameImpl impl, TicTacToeGame remote) {
        private HostedGame withRemote(TicTacToeGame remote) {
            return new HostedGame(impl, remote);
        }
    }
}
