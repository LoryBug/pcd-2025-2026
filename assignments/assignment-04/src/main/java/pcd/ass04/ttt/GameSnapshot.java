package pcd.ass04.ttt;

import java.io.Serializable;
import java.util.Optional;

public record GameSnapshot(
        String gameName,
        Mark[][] board,
        String playerX,
        String playerO,
        Mark turn,
        GamePhase phase,
        Optional<Mark> winner) implements Serializable {

    public GameSnapshot {
        board = copy(board);
        winner = winner == null ? Optional.empty() : winner;
    }

    public static Mark[][] copy(Mark[][] source) {
        Mark[][] result = new Mark[3][3];
        for (int row = 0; row < 3; row++) {
            System.arraycopy(source[row], 0, result[row], 0, 3);
        }
        return result;
    }
}
