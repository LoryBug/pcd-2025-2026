package pcd.ass04.ttt;

import java.io.Serializable;
import java.util.Optional;

public record MoveResult(boolean accepted, GamePhase phase, Optional<Mark> winner, String message)
        implements Serializable {

    public MoveResult {
        winner = winner == null ? Optional.empty() : winner;
    }
}
