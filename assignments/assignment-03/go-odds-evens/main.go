package main

import (
	"errors"
	"fmt"
	"math/bits"
	"os"
	"strconv"
	"time"
)

var (
	ErrTournamentCancelled = errors.New("tournament cancelled")
	ErrMatchTimeout        = errors.New("match timeout")
)

type Parity int

const (
	Even Parity = iota
	Odd
)

func (p Parity) String() string {
	if p == Odd {
		return "odd"
	}
	return "even"
}

type Player struct {
	ID        int
	Name      string
	Seed      int
	ThinkTime time.Duration
}

type PlayRequest struct {
	Round          int
	Match          int
	AssignedParity Parity
	ReplyTo        chan<- Move
}

type Move struct {
	Player Player
	Number int
	Parity Parity
}

type MatchResult struct {
	Round      int
	Match      int
	FirstMove  Move
	SecondMove Move
	Winner     Player
}

type RoundResult struct {
	Round   int
	Matches []MatchResult
}

type matchOutcome struct {
	result MatchResult
	err    error
}

func playerAgent(player Player, inbox <-chan PlayRequest, done <-chan struct{}) {
	for {
		select {
		case <-done:
			return
		case request, ok := <-inbox:
			if !ok {
				return
			}
			if player.ThinkTime > 0 {
				select {
				case <-time.After(player.ThinkTime):
				case <-done:
					return
				}
			}
			number := (player.Seed + request.Round*3 + request.Match + player.ID) % 6
			select {
			case request.ReplyTo <- Move{Player: player, Number: number, Parity: request.AssignedParity}:
			case <-done:
				return
			}
		}
	}
}

func runMatch(round int, match int, first Player, second Player, firstInbox chan<- PlayRequest,
	secondInbox chan<- PlayRequest, results chan<- matchOutcome, done <-chan struct{}, timeout time.Duration) {
	moves := make(chan Move, 2)
	if !sendRequest(firstInbox, PlayRequest{Round: round, Match: match, AssignedParity: Odd, ReplyTo: moves}, done) {
		results <- matchOutcome{err: ErrTournamentCancelled}
		return
	}
	if !sendRequest(secondInbox, PlayRequest{Round: round, Match: match, AssignedParity: Even, ReplyTo: moves}, done) {
		results <- matchOutcome{err: ErrTournamentCancelled}
		return
	}

	timer := time.NewTimer(timeout)
	defer timer.Stop()
	firstMove, ok := receiveMove(moves, done, timer.C)
	if !ok {
		results <- matchOutcome{err: timeoutOrCancelled(done)}
		return
	}
	secondMove, ok := receiveMove(moves, done, timer.C)
	if !ok {
		results <- matchOutcome{err: timeoutOrCancelled(done)}
		return
	}
	winner := winnerOf(firstMove, secondMove)
	results <- matchOutcome{result: MatchResult{Round: round, Match: match, FirstMove: firstMove, SecondMove: secondMove, Winner: winner}}
}

func sendRequest(inbox chan<- PlayRequest, request PlayRequest, done <-chan struct{}) bool {
	select {
	case inbox <- request:
		return true
	case <-done:
		return false
	}
}

func receiveMove(moves <-chan Move, done <-chan struct{}, timeout <-chan time.Time) (Move, bool) {
	select {
	case move := <-moves:
		return move, true
	case <-done:
		return Move{}, false
	case <-timeout:
		return Move{}, false
	}
}

func timeoutOrCancelled(done <-chan struct{}) error {
	select {
	case <-done:
		return ErrTournamentCancelled
	default:
		return ErrMatchTimeout
	}
}

func winnerOf(first Move, second Move) Player {
	if Parity((first.Number+second.Number)%2) == first.Parity {
		return first.Player
	}
	return second.Player
}

func RunTournament(players []Player) (Player, []RoundResult, error) {
	return RunTournamentWithCancel(players, nil, 5*time.Second)
}

func RunTournamentWithCancel(players []Player, done <-chan struct{}, matchTimeout time.Duration) (Player, []RoundResult, error) {
	if len(players) == 0 || bits.OnesCount(uint(len(players))) != 1 {
		return Player{}, nil, errors.New("number of players must be a positive power of two")
	}
	if matchTimeout <= 0 {
		return Player{}, nil, errors.New("match timeout must be positive")
	}
	internalDone := make(chan struct{})
	defer close(internalDone)
	combinedDone := (<-chan struct{})(internalDone)
	if done != nil {
		combinedDone = mergeDone(done, internalDone)
	}

	inboxes := make(map[int]chan PlayRequest, len(players))
	for _, player := range players {
		inbox := make(chan PlayRequest)
		inboxes[player.ID] = inbox
		go playerAgent(player, inbox, combinedDone)
	}

	current := append([]Player(nil), players...)
	history := make([]RoundResult, 0)
	for round := 1; len(current) > 1; round++ {
		matchCount := len(current) / 2
		results := make(chan matchOutcome, matchCount)
		for match := 0; match < matchCount; match++ {
			first := current[match*2]
			second := current[match*2+1]
			go runMatch(round, match, first, second, inboxes[first.ID], inboxes[second.ID], results, combinedDone, matchTimeout)
		}

		roundMatches := make([]MatchResult, matchCount)
		nextRound := make([]Player, matchCount)
		for i := 0; i < matchCount; i++ {
			var outcome matchOutcome
			select {
			case outcome = <-results:
			case <-combinedDone:
				return Player{}, history, ErrTournamentCancelled
			}
			if outcome.err != nil {
				return Player{}, history, outcome.err
			}
			result := outcome.result
			roundMatches[result.Match] = result
			nextRound[result.Match] = result.Winner
		}
		history = append(history, RoundResult{Round: round, Matches: roundMatches})
		current = nextRound
	}

	return current[0], history, nil
}

func mergeDone(first <-chan struct{}, second <-chan struct{}) <-chan struct{} {
	merged := make(chan struct{})
	go func() {
		defer close(merged)
		select {
		case <-first:
		case <-second:
		}
	}()
	return merged
}

func defaultPlayers(n int) []Player {
	players := make([]Player, n)
	for i := range players {
		players[i] = Player{ID: i, Name: fmt.Sprintf("P%d", i), Seed: i*7 + 3}
	}
	return players
}

func main() {
	n := 8
	if len(os.Args) > 1 {
		parsed, err := strconv.Atoi(os.Args[1])
		if err != nil {
			panic(err)
		}
		n = parsed
	}

	winner, history, err := RunTournament(defaultPlayers(n))
	if err != nil {
		panic(err)
	}

	for _, round := range history {
		fmt.Printf("Round %d\n", round.Round)
		for _, match := range round.Matches {
			fmt.Printf("  match %d: %s(%d,%s) vs %s(%d,%s) -> %s\n",
				match.Match,
				match.FirstMove.Player.Name,
				match.FirstMove.Number,
				match.FirstMove.Parity,
				match.SecondMove.Player.Name,
				match.SecondMove.Number,
				match.SecondMove.Parity,
				match.Winner.Name)
		}
	}
	fmt.Printf("Winner: %s (id=%d)\n", winner.Name, winner.ID)
}
