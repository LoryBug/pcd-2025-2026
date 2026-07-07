package pcd.ass04.ttt;

import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public final class TicTacToeGameImpl implements TicTacToeGame {

    private final String name;
    private final Mark[][] board = new Mark[3][3];
    private final CopyOnWriteArrayList<TicTacToeListener> listeners = new CopyOnWriteArrayList<>();
    private String playerX;
    private String playerO;
    private Mark turn = Mark.X;
    private GamePhase phase = GamePhase.WAITING_FOR_OPPONENT;
    private Optional<Mark> winner = Optional.empty();

    public TicTacToeGameImpl(String name, String firstPlayer) {
        this.name = name;
        this.playerX = firstPlayer;
        for (Mark[] row : board) {
            Arrays.fill(row, Mark.EMPTY);
        }
    }

    public synchronized Mark join(String playerName) {
        if (playerName.equals(playerX)) {
            return Mark.X;
        }
        if (playerName.equals(playerO)) {
            return Mark.O;
        }
        if (playerO != null) {
            throw new IllegalStateException("Game is full");
        }
        playerO = playerName;
        phase = GamePhase.IN_PROGRESS;
        notifyJoined(playerName, Mark.O);
        return Mark.O;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public synchronized GameSnapshot snapshot() {
        return new GameSnapshot(name, board, playerX, playerO, turn, phase, winner);
    }

    @Override
    public synchronized MoveResult makeMove(String playerName, int row, int col)
            throws InvalidMoveException {
        validateMove(playerName, row, col);
        board[row][col] = turn;
        Mark played = turn;
        notifyMove(playerName, row, col, played);

        Optional<Mark> foundWinner = findWinner();
        if (foundWinner.isPresent()) {
            winner = foundWinner;
            phase = GamePhase.FINISHED;
            notifyGameOver();
            return new MoveResult(true, phase, winner, "Player " + playerName + " wins");
        }
        if (isBoardFull()) {
            phase = GamePhase.FINISHED;
            notifyGameOver();
            return new MoveResult(true, phase, Optional.empty(), "Draw");
        }
        turn = turn == Mark.X ? Mark.O : Mark.X;
        return new MoveResult(true, phase, Optional.empty(), "Move accepted");
    }

    @Override
    public void addListener(TicTacToeListener listener) {
        if (listener != null) {
            listeners.addIfAbsent(listener);
        }
    }

    @Override
    public void removeListener(TicTacToeListener listener) {
        listeners.remove(listener);
    }

    private void validateMove(String playerName, int row, int col) throws InvalidMoveException {
        if (phase != GamePhase.IN_PROGRESS) {
            throw new InvalidMoveException("Game is not in progress");
        }
        if (row < 0 || row >= 3 || col < 0 || col >= 3) {
            throw new InvalidMoveException("Move out of board");
        }
        if (board[row][col] != Mark.EMPTY) {
            throw new InvalidMoveException("Cell already occupied");
        }
        if (markOf(playerName) != turn) {
            throw new InvalidMoveException("Not your turn");
        }
    }

    private Mark markOf(String playerName) throws InvalidMoveException {
        if (playerName.equals(playerX)) {
            return Mark.X;
        }
        if (playerName.equals(playerO)) {
            return Mark.O;
        }
        throw new InvalidMoveException("Unknown player");
    }

    private Optional<Mark> findWinner() {
        int[][] lines = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8},
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8},
            {0, 4, 8}, {2, 4, 6}
        };
        for (int[] line : lines) {
            Mark a = at(line[0]);
            if (a != Mark.EMPTY && a == at(line[1]) && a == at(line[2])) {
                return Optional.of(a);
            }
        }
        return Optional.empty();
    }

    private Mark at(int index) {
        return board[index / 3][index % 3];
    }

    private boolean isBoardFull() {
        for (Mark[] row : board) {
            for (Mark cell : row) {
                if (cell == Mark.EMPTY) {
                    return false;
                }
            }
        }
        return true;
    }

    private void notifyJoined(String playerName, Mark mark) {
        for (TicTacToeListener listener : listeners) {
            try {
                listener.onPlayerJoined(name, playerName, mark);
            } catch (RemoteException e) {
                listeners.remove(listener);
            }
        }
    }

    private void notifyMove(String playerName, int row, int col, Mark mark) {
        for (TicTacToeListener listener : listeners) {
            try {
                listener.onMove(name, playerName, row, col, mark);
            } catch (RemoteException e) {
                listeners.remove(listener);
            }
        }
    }

    private void notifyGameOver() {
        GameSnapshot snapshot = snapshot();
        for (TicTacToeListener listener : listeners) {
            try {
                listener.onGameOver(name, snapshot);
            } catch (RemoteException e) {
                listeners.remove(listener);
            }
        }
    }
}
