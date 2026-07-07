package main

import "testing"

func TestTournamentRequiresPowerOfTwo(t *testing.T) {
	_, _, err := RunTournament(defaultPlayers(6))
	if err == nil {
		t.Fatal("expected an error for a non power-of-two number of players")
	}
}

func TestTournamentRunsExpectedRounds(t *testing.T) {
	winner, history, err := RunTournament(defaultPlayers(8))
	if err != nil {
		t.Fatal(err)
	}
	if winner.ID != 7 {
		t.Fatalf("unexpected winner: got %d", winner.ID)
	}
	if len(history) != 3 {
		t.Fatalf("expected 3 rounds, got %d", len(history))
	}
	if len(history[0].Matches) != 4 || len(history[1].Matches) != 2 || len(history[2].Matches) != 1 {
		t.Fatalf("unexpected round structure: %#v", history)
	}
}

func TestWinnerOfUsesFirstPlayerParity(t *testing.T) {
	first := Move{Player: Player{ID: 1, Name: "odd"}, Number: 3, Parity: Odd}
	second := Move{Player: Player{ID: 2, Name: "even"}, Number: 2, Parity: Even}
	if winner := winnerOf(first, second); winner.ID != first.Player.ID {
		t.Fatalf("expected first player to win, got %v", winner)
	}
}
