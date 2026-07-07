package main

import (
	"errors"
	"fmt"
	"math/bits"
	"os"
	"strconv"
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
	ID   int
	Name string
	Seed int
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

func playerAgent(player Player, inbox <-chan PlayRequest) {
	for request := range inbox {
		number := (player.Seed + request.Round*3 + request.Match + player.ID) % 6
		request.ReplyTo <- Move{Player: player, Number: number, Parity: request.AssignedParity}
	}
}

func runMatch(round int, match int, first Player, second Player, firstInbox chan<- PlayRequest,
	secondInbox chan<- PlayRequest, results chan<- MatchResult) {
	moves := make(chan Move)
	firstInbox <- PlayRequest{Round: round, Match: match, AssignedParity: Odd, ReplyTo: moves}
	secondInbox <- PlayRequest{Round: round, Match: match, AssignedParity: Even, ReplyTo: moves}

	firstMove := <-moves
	secondMove := <-moves
	winner := winnerOf(firstMove, secondMove)
	results <- MatchResult{Round: round, Match: match, FirstMove: firstMove, SecondMove: secondMove, Winner: winner}
}

func winnerOf(first Move, second Move) Player {
	if Parity((first.Number+second.Number)%2) == first.Parity {
		return first.Player
	}
	return second.Player
}

func RunTournament(players []Player) (Player, []RoundResult, error) {
	if len(players) == 0 || bits.OnesCount(uint(len(players))) != 1 {
		return Player{}, nil, errors.New("number of players must be a positive power of two")
	}

	inboxes := make(map[int]chan PlayRequest, len(players))
	for _, player := range players {
		inbox := make(chan PlayRequest)
		inboxes[player.ID] = inbox
		go playerAgent(player, inbox)
	}
	defer func() {
		for _, inbox := range inboxes {
			close(inbox)
		}
	}()

	current := append([]Player(nil), players...)
	history := make([]RoundResult, 0)
	for round := 1; len(current) > 1; round++ {
		matchCount := len(current) / 2
		results := make(chan MatchResult)
		for match := 0; match < matchCount; match++ {
			first := current[match*2]
			second := current[match*2+1]
			go runMatch(round, match, first, second, inboxes[first.ID], inboxes[second.ID], results)
		}

		roundMatches := make([]MatchResult, matchCount)
		nextRound := make([]Player, matchCount)
		for i := 0; i < matchCount; i++ {
			result := <-results
			roundMatches[result.Match] = result
			nextRound[result.Match] = result.Winner
		}
		history = append(history, RoundResult{Round: round, Matches: roundMatches})
		current = nextRound
	}

	return current[0], history, nil
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
