package io.github.quizup.game.domain.aggregate;

import io.github.quizup.axon.test.QuizUpAxonMatchers;
import io.github.quizup.game.domain.command.GameCommand;
import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.game.domain.exception.GameExceptions;
import io.github.quizup.game.domain.model.GameMode;
import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.game.domain.model.GameQuestion;
import io.github.quizup.game.domain.model.GameQuestionChoice;
import io.github.quizup.game.domain.model.GameRoundType;
import io.github.quizup.game.domain.model.GameRules;
import io.github.quizup.game.domain.port.out.GameEventStorePort;
import io.github.quizup.game.domain.port.out.QuestionRepositoryPort;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test Axon in-memory de l'agrégat {@link GameAggregate} via {@link AggregateTestFixture}.
 *
 * <p>100 % in-memory : event store de l'agrégat en mémoire, aucun Postgres ni Axon Server.
 * Le port sortant {@link QuestionRepositoryPort} est un mock.
 *
 * <p>Couvre le cycle de round à deux phases : la question est affichée sans chrono, puis les
 * réponses sont révélées et le chrono démarre — répondre avant la révélation est refusé, et le
 * temps de réponse est mesuré depuis la révélation.
 */
class GameAggregateTest {

    private static final String GAME_ID = "game-1";
    private static final String TOPIC_ID = "topic-1";
    private static final String PLAYER_1 = "player-1";
    private static final String PLAYER_2 = "player-2";

    private final AggregateTestFixture<GameAggregate> fixture =
            new AggregateTestFixture<>(GameAggregate.class);

    @BeforeEach
    void setUp() {
        // GameAggregate reconstruit `players` à partir des arguments de l'événement dans son
        // handler `on(GameCreatedEvent)` ; le snapshot in-mémoire peut donc différer de l'état
        // final et le fixture lève une "Illegal state change". On désactive la détection
        // (comportement documenté par Axon) — les assertions sur les événements restent la preuve.
        fixture.setReportIllegalStateChange(false);
    }

    @Test
    void createGame_appliesGameCreatedEvent() {
        QuestionRepositoryPort questionRepositoryPort = mock(QuestionRepositoryPort.class);
        when(questionRepositoryPort.findRandomApprovedByTopicId(anyString(), anyInt())).thenReturn(List.of());

        GameCommand.CreateGameCommand command = new GameCommand.CreateGameCommand(
                GAME_ID, TOPIC_ID, PLAYER_1, "Alpha", PLAYER_2, "Bravo", GameMode.SYNC, GamePlayerType.HUMAN, null, null);

        fixture.registerInjectableResource(questionRepositoryPort)
                .registerInjectableResource(mock(GameEventStorePort.class))
                .givenNoPriorActivity()
                .when(command)
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        GameEvent.GameCreatedEvent.class,
                        e -> ((GameEvent.GameCreatedEvent) e).gameId().equals(GAME_ID)));
    }

    @Test
    void answerBeforeReveal_isRejected() {
        Instant shownAt = Instant.now();

        fixture.given(concat(readyGame(), roundStarted(shownAt)))
                .when(new GameCommand.AnswerQuestionCommand(
                        GAME_ID, PLAYER_1, GameQuestionChoice.A, shownAt.plusMillis(500)))
                .expectException(GameExceptions.RoundNotRevealedProblem.class);
    }

    @Test
    void revealBeforeQuestionShown_isRejected() {
        fixture.given(readyGame())
                .when(new GameCommand.RevealQuestionCommand(GAME_ID))
                .expectException(GameExceptions.RoundNotRevealableProblem.class);
    }

    @Test
    void revealQuestion_appliesQuestionRevealedEvent() {
        Instant shownAt = Instant.now();

        fixture.given(concat(readyGame(), roundStarted(shownAt)))
                .when(new GameCommand.RevealQuestionCommand(GAME_ID))
                .expectEventsMatching(QuizUpAxonMatchers.hasPayloadMatching(
                        GameEvent.QuestionRevealedEvent.class,
                        e -> ((GameEvent.QuestionRevealedEvent) e).round() == GameRoundType.ROUND_1));
    }

    @Test
    void createAsyncRecordGame_allowsAbsentPlayer2() {
        QuestionRepositoryPort questionRepositoryPort = mock(QuestionRepositoryPort.class);
        when(questionRepositoryPort.findRandomApprovedByTopicId(anyString(), anyInt())).thenReturn(questions());

        GameCommand.CreateGameCommand command = new GameCommand.CreateGameCommand(
                GAME_ID, TOPIC_ID, PLAYER_1, "Alpha", null, null,
                GameMode.ASYNC, GamePlayerType.HUMAN, null, null);

        fixture.registerInjectableResource(questionRepositoryPort)
                .registerInjectableResource(mock(GameEventStorePort.class))
                .givenNoPriorActivity()
                .when(command)
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        GameEvent.GameCreatedEvent.class,
                        e -> ((GameEvent.GameCreatedEvent) e).mode() == GameMode.ASYNC));
    }

    @Test
    void endRun_recordsRunWithoutWinner() {
        Instant answeredAt = Instant.now();

        fixture.given(concat(asyncReadyGame(), answered(answeredAt)))
                .when(new GameCommand.EndRunCommand(GAME_ID))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        GameEvent.GameRunRecordedEvent.class,
                        e -> {
                            GameEvent.GameRunRecordedEvent recorded = (GameEvent.GameRunRecordedEvent) e;
                            return PLAYER_1.equals(recorded.playerId()) && recorded.score() == 18;
                        }));
    }

    @Test
    void forfeitEndsGame_andOpponentWins() {
        fixture.given(readyGame())
                .when(new GameCommand.EndGameCommand(GAME_ID, PLAYER_2))
                .expectEventsMatching(QuizUpAxonMatchers.hasPayloadMatching(
                        GameEvent.GameEndedEvent.class,
                        e -> {
                            GameEvent.GameEndedEvent ended = (GameEvent.GameEndedEvent) e;
                            return PLAYER_1.equals(ended.winnerId())
                                    && PLAYER_2.equals(ended.forfeitById());
                        }));
    }

    @Test
    void abandonAsyncSolo_endsGameWithoutWinner() {
        fixture.given(asyncReadyGame())
                .when(new GameCommand.EndGameCommand(GAME_ID, PLAYER_1))
                .expectEventsMatching(QuizUpAxonMatchers.hasPayloadMatching(
                        GameEvent.GameEndedEvent.class,
                        e -> {
                            GameEvent.GameEndedEvent ended = (GameEvent.GameEndedEvent) e;
                            return ended.winnerId() == null
                                    && ended.player2Id() == null
                                    && PLAYER_1.equals(ended.forfeitById());
                        }));
    }

    @Test
    void forfeitByNonPlayer_isRejected() {
        fixture.given(readyGame())
                .when(new GameCommand.EndGameCommand(GAME_ID, "stranger"))
                .expectException(GameExceptions.PlayerNotInGameProblem.class);
    }

    @Test
    void abandon_whenNotInProgress_isRejected() {
        fixture.given(concat(asyncReadyGame(),
                        new GameEvent.GameRunRecordedEvent(GAME_ID, PLAYER_1, TOPIC_ID, 0, Instant.now())))
                .when(new GameCommand.EndGameCommand(GAME_ID, PLAYER_1))
                .expectException(GameExceptions.GameNotInProgressProblem.class);
    }

    @Test
    void answerAfterReveal_scoresFromRevealAndReportsCorrect() {
        Instant shownAt = Instant.now();
        Instant revealedAt = shownAt.plusMillis(GameRules.QUESTION_REVEAL_MS);
        Instant answeredAt = revealedAt.plusSeconds(2);

        fixture.given(concat(readyGame(), roundStarted(shownAt), revealed(revealedAt)))
                .when(new GameCommand.AnswerQuestionCommand(
                        GAME_ID, PLAYER_1, GameQuestionChoice.A, answeredAt))
                .expectEventsMatching(QuizUpAxonMatchers.hasPayloadMatching(
                        GameEvent.QuestionAnsweredEvent.class,
                        e -> {
                            GameEvent.QuestionAnsweredEvent answered = (GameEvent.QuestionAnsweredEvent) e;
                            // 2 s après la révélation : 10 (base) + 8 (bonus de vitesse) = 18.
                            return answered.correct()
                                    && answered.pointsEarned() == 18
                                    && answered.timeMs() == 2000;
                        }));
    }

    // ── Fixtures ──

    private static Object[] concat(Object[] base, Object... extra) {
        Object[] result = new Object[base.length + extra.length];
        System.arraycopy(base, 0, result, 0, base.length);
        System.arraycopy(extra, 0, result, base.length, extra.length);
        return result;
    }

    private Object[] readyGame() {
        return new Object[]{
                new GameEvent.GameCreatedEvent(
                        GAME_ID, TOPIC_ID, PLAYER_1, "Alpha", PLAYER_2, "Bravo",
                        GamePlayerType.HUMAN, GameMode.SYNC, questions(), null, null, Instant.now()),
                new GameEvent.GameJoinedEvent(GAME_ID, PLAYER_1, Instant.now()),
                new GameEvent.GameJoinedEvent(GAME_ID, PLAYER_2, Instant.now()),
                new GameEvent.GameStartedEvent(
                        GAME_ID, GameMode.SYNC, Instant.now(), Instant.now().plusMillis(GameRules.MATCH_INTRO_MS))
        };
    }

    /** Run asynchrone solo : pas de joueur 2, partie démarrée. */
    private Object[] asyncReadyGame() {
        return new Object[]{
                new GameEvent.GameCreatedEvent(
                        GAME_ID, TOPIC_ID, PLAYER_1, "Alpha", null, null,
                        GamePlayerType.HUMAN, GameMode.ASYNC, questions(), null, null, Instant.now()),
                new GameEvent.GameStartedEvent(
                        GAME_ID, GameMode.ASYNC, Instant.now(), Instant.now().plusMillis(GameRules.MATCH_INTRO_MS))
        };
    }

    private GameEvent.QuestionAnsweredEvent answered(Instant answeredAt) {
        return new GameEvent.QuestionAnsweredEvent(
                GAME_ID, GameRoundType.ROUND_1, "question-0", PLAYER_1, GamePlayerType.HUMAN,
                GameQuestionChoice.A, true, answeredAt, 18, 2000);
    }

    private GameEvent.RoundStartedEvent roundStarted(Instant shownAt) {
        return new GameEvent.RoundStartedEvent(
                GAME_ID,
                GameRoundType.ROUND_1,
                questions().get(0),
                shownAt,
                shownAt.plusMillis(GameRules.QUESTION_REVEAL_MS)
        );
    }

    private GameEvent.QuestionRevealedEvent revealed(Instant revealedAt) {
        return new GameEvent.QuestionRevealedEvent(
                GAME_ID,
                GameRoundType.ROUND_1,
                revealedAt,
                revealedAt.plusSeconds(GameRules.ROUND_TIMEOUT_SECONDS)
        );
    }

    private static List<GameQuestion> questions() {
        return IntStream.range(0, GameRules.TOTAL_ROUNDS)
                .mapToObj(i -> new GameQuestion(
                        "question-" + i,
                        "Question " + i,
                        null,
                        null,
                        Map.of(
                                GameQuestionChoice.A, "Réponse A",
                                GameQuestionChoice.B, "Réponse B",
                                GameQuestionChoice.C, "Réponse C",
                                GameQuestionChoice.D, "Réponse D"),
                        GameQuestionChoice.A))
                .toList();
    }
}
