package io.github.quizup.game.application.handler.query;

import io.github.quizup.game.domain.model.Game;
import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.game.domain.model.GameQuestionChoice;
import io.github.quizup.game.domain.model.GameResult;
import io.github.quizup.game.domain.model.GameRound;
import io.github.quizup.game.domain.model.GameRoundStatus;
import io.github.quizup.game.domain.model.GameRoundType;
import io.github.quizup.game.domain.model.GameStatus;
import io.github.quizup.game.domain.model.PlayerGamesPage;
import io.github.quizup.game.domain.model.TopicPopularity;
import io.github.quizup.game.domain.port.out.GameEventStorePort;
import io.github.quizup.game.domain.port.out.GameRepositoryPort;
import io.github.quizup.game.domain.query.GameQuery;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameQueryHandlerTest {

    private final GameRepositoryPort repository = mock(GameRepositoryPort.class);
    private final GameEventStorePort eventStore = mock(GameEventStorePort.class);
    private final GameQueryHandler handler = new GameQueryHandler(repository, eventStore);

    @Test
    void player_games_delegates_all_filters() {
        PlayerGamesPage page = PlayerGamesPage.builder()
                .games(List.of())
                .page(1)
                .size(20)
                .totalElements(0)
                .totalPages(0)
                .build();
        when(repository.findPlayerGames("me", "topic-1", "opponent-1", 1, 20)).thenReturn(page);

        PlayerGamesPage result = handler.handle(
                new GameQuery.GetPlayerGamesQuery("me", "topic-1", "opponent-1", 1, 20));

        assertEquals(page, result);
        verify(repository).findPlayerGames("me", "topic-1", "opponent-1", 1, 20);
    }

    @Test
    void popular_topics_delegates_window_and_limit() {
        Instant since = Instant.parse("2026-09-01T00:00:00Z");
        when(repository.findPopularTopics(since, 5))
                .thenReturn(List.of(new TopicPopularity("topic-1", 12)));

        List<TopicPopularity> result = handler.handle(new GameQuery.GetPopularTopicsQuery(since, 5));

        assertEquals(List.of(new TopicPopularity("topic-1", 12)), result);
        verify(repository).findPopularTopics(since, 5);
    }

    @Test
    void game_result_computes_score_breakdown_with_bonus_round() {
        when(repository.findById("game-1")).thenReturn(Optional.of(finishedGame()));

        GameResult result = handler.handle(new GameQuery.GetGameResultQuery("game-1", "player-1"));

        GameResult expected = new GameResult(
                "game-1", "topic-1", "player-1", "Alpha", "player-2", "Bravo",
                "player-1", "player-2", 48, 10, "player-1", false,
                30, 26, 2, 2, 3, 3);
        assertEquals(expected, result);
    }

    private static Game finishedGame() {
        return Game.builder()
                .gameId("game-1")
                .topicId("topic-1")
                .player1Id("player-1")
                .player1Name("Alpha")
                .player2Id("player-2")
                .player2Name("Bravo")
                .opponent(GamePlayerType.HUMAN)
                .status(GameStatus.FINISHED)
                .player1Score(48)
                .player2Score(10)
                .winnerId("player-1")
                .createdAt(Instant.parse("2026-09-01T00:00:00Z"))
                .endedAt(Instant.parse("2026-09-01T00:05:00Z"))
                .rounds(List.of(
                        round(GameRoundType.ROUND_1, GameQuestionChoice.A, true, 18, 2000L),
                        round(GameRoundType.ROUND_7, GameQuestionChoice.A, true, 38, 1500L),
                        round(GameRoundType.ROUND_2, GameQuestionChoice.A, false, 0, 4000L)))
                .build();
    }

    private static GameRound round(GameRoundType type,
                                   GameQuestionChoice correctAnswer,
                                   boolean sameChoice,
                                   int points,
                                   Long timeMs) {
        return GameRound.builder()
                .round(type)
                .questionId("question-" + type.name())
                .questionText("Question " + type.name())
                .correctAnswer(correctAnswer)
                .player1Choice(sameChoice ? correctAnswer : GameQuestionChoice.B)
                .player1Points(points)
                .player1TimeMs(timeMs)
                .status(GameRoundStatus.CLOSED)
                .build();
    }
}
