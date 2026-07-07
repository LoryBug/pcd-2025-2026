package pcd.ass04.ttt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TicTacToeHubTest {

    @Test
    void createsAndJoinsRemoteGame() throws Exception {
        TicTacToeHub hub = new TicTacToeHubImpl(game -> game);
        RecordingListener listener = new RecordingListener();

        TicTacToeGame game = hub.createGame("g1", "Alice", listener);
        TicTacToeGame joined = hub.joinGame("g1", "Bob", listener);

        assertEquals(game.name(), joined.name());
        assertEquals(GamePhase.IN_PROGRESS, game.snapshot().phase());
        assertEquals(List.of("g1"), hub.listGames());
        assertTrue(listener.events().contains("join:Bob:O"));
    }

    @Test
    void acceptsOnlyLegalTurnsAndDetectsWinner() throws Exception {
        TicTacToeHub hub = new TicTacToeHubImpl(game -> game);
        TicTacToeGame game = hub.createGame("g2", "Alice", null);
        hub.joinGame("g2", "Bob", null);

        assertThrows(InvalidMoveException.class, () -> game.makeMove("Bob", 0, 0));
        game.makeMove("Alice", 0, 0);
        game.makeMove("Bob", 1, 0);
        game.makeMove("Alice", 0, 1);
        game.makeMove("Bob", 1, 1);
        MoveResult result = game.makeMove("Alice", 0, 2);

        assertEquals(GamePhase.FINISHED, result.phase());
        assertEquals(Mark.X, result.winner().orElseThrow());
    }

    private static final class RecordingListener implements TicTacToeListener {

        private final List<String> events = new ArrayList<>();

        @Override
        public void onPlayerJoined(String gameName, String playerName, Mark mark) throws RemoteException {
            events.add("join:" + playerName + ":" + mark);
        }

        @Override
        public void onMove(String gameName, String playerName, int row, int col, Mark mark) throws RemoteException {
            events.add("move:" + playerName + ":" + row + ":" + col);
        }

        @Override
        public void onGameOver(String gameName, GameSnapshot snapshot) throws RemoteException {
            events.add("over:" + snapshot.winner());
        }

        private List<String> events() {
            return List.copyOf(events);
        }
    }
}
