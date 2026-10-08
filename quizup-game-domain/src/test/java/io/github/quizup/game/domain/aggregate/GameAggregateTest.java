package io.github.quizup.game.domain.aggregate;

import io.github.quizup.axon.test.QuizUpAxonMatchers;
import io.github.quizup.game.domain.command.GameCommand;
import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.game.domain.exception.GameExceptions;
import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.game.domain.model.GameQuestion;
import io.github.quizup.game.domain.model.GameQuestionChoice;
import io.github.quizup.game.domain.model.GameQuestionContent;
import io.github.quizup.game.domain.model.GameRoundType;
import io.github.quizup.game.domain.model.GameRules;
import io.github.quizup.game.domain.model.PlayerProgressSnapshot;
import io.github.quizup.game.domain.port.out.QuestionRepositoryPort;
import io.github.quizup.microservice.core.domain.model.i18n.Language;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test Axon in-memory de l'agrégat {@link GameAggregate} via {@link AggregateTestFixture}.
 *
 * <p>100 % in-memory : event store de l'agrégat en mémoire, aucun Postgres ni Axon Server.
 * Le port sortant {@link QuestionRepositoryPort} est un mock. La partie démarre immédiatement à
 * la création (émission de {@code GameCreatedEvent} puis {@code GameStartedEvent}).</p>
 */
class GameAggregateTest {

    private static final String GAME_ID = "game-1";
    private static final String TOPIC_ID = "topic-1";
    private static final String PLAYER_1 = "player-1";
    private static final String PLAYER_2 = "player-2";
    private static final PlayerProgressSnapshot PLAYER_1_PROGRESS = new PlayerProgressSnapshot(2, 150);
    private static final PlayerProgressSnapshot PLAYER_2_PROGRESS = new PlayerProgressSnapshot(1, 0);

    private final AggregateTestFixture<GameAggregate> fixture =
            new AggregateTestFixture<>(GameAggregate.class);

    @BeforeEach
    void setUp() {
        fixture.setReportIllegalStateChange(false);
    }

    @Test
    void createGame_appliesCreatedThenStarted() {
        QuestionRepositoryPort questionRepositoryPort = mock(QuestionRepositoryPort.class);
        when(questionRepositoryPort.findRandomApprovedByTopicId(anyString(), anyInt(), anySet())).thenReturn(questions());

        fixture.registerInjectableResource(questionRepositoryPort)
                .givenNoPriorActivity()
                .when(createCommand())
                .expectEventsMatching(QuizUpAxonMatchers.hasPayloadMatching(
                        GameEvent.GameCreatedEvent.class,
                        e -> ((GameEvent.GameCreatedEvent) e).gameId().equals(GAME_ID)))
                .expectEventsMatching(QuizUpAxonMatchers.hasPayloadMatching(
                        GameEvent.GameStartedEvent.class,
                        e -> ((GameEvent.GameStartedEvent) e).gameId().equals(GAME_ID)));
    }

    @Test
    void createGame_carriesPlayerProgressSnapshot() {
        QuestionRepositoryPort questionRepositoryPort = mock(QuestionRepositoryPort.class);
        when(questionRepositoryPort.findRandomApprovedByTopicId(anyString(), anyInt(), anySet())).thenReturn(questions());

        fixture.registerInjectableResource(questionRepositoryPort)
                .givenNoPriorActivity()
                .when(createCommand())
                .expectEventsMatching(QuizUpAxonMatchers.hasPayloadMatching(
                        GameEvent.GameCreatedEvent.class,
                        e -> {
                            GameEvent.GameCreatedEvent created = (GameEvent.GameCreatedEvent) e;
                            return PLAYER_1_PROGRESS.equals(created.player1Progress())
                                    && PLAYER_2_PROGRESS.equals(created.player2Progress());
                        }));
    }

    @Test
    void createGame_withFewerThanSevenQuestions_isRejected() {
        QuestionRepositoryPort questionRepositoryPort = mock(QuestionRepositoryPort.class);
        when(questionRepositoryPort.findRandomApprovedByTopicId(anyString(), anyInt(), anySet()))
                .thenReturn(questions().subList(0, GameRules.TOTAL_ROUNDS - 1));

        fixture.registerInjectableResource(questionRepositoryPort)
                .givenNoPriorActivity()
                .when(createCommand())
                .expectException(GameExceptions.NotEnoughQuestionsProblem.class);
    }

    @Test
    void createGame_withBlankPlayer2_isRejected() {
        GameCommand.CreateGameCommand command = new GameCommand.CreateGameCommand(
                GAME_ID, TOPIC_ID, PLAYER_1, "Alpha", " ", "Bravo",
                Set.of(Language.FR), GamePlayerType.HUMAN, null,
                PLAYER_1_PROGRESS, PLAYER_2_PROGRESS);

        fixture.registerInjectableResource(mock(QuestionRepositoryPort.class))
                .givenNoPriorActivity()
                .when(command)
                .expectException(GameExceptions.MissingPlayerProblem.class);
    }

    @Test
    void answerBeforeReveal_isRejected() {
        Instant shownAt = Instant.now();

        fixture.given(concat(created(), roundStarted(shownAt)))
                .when(new GameCommand.AnswerQuestionCommand(
                        GAME_ID, PLAYER_1, GameQuestionChoice.A, shownAt.plusMillis(500)))
                .expectException(GameExceptions.RoundNotRevealedProblem.class);
    }

    @Test
    void revealBeforeQuestionShown_isRejected() {
        fixture.given(created())
                .when(new GameCommand.RevealQuestionCommand(GAME_ID))
                .expectException(GameExceptions.RoundNotRevealableProblem.class);
    }

    @Test
    void revealQuestion_appliesQuestionRevealedEvent() {
        Instant shownAt = Instant.now();

        fixture.given(concat(created(), roundStarted(shownAt)))
                .when(new GameCommand.RevealQuestionCommand(GAME_ID))
                .expectEventsMatching(QuizUpAxonMatchers.hasPayloadMatching(
                        GameEvent.QuestionRevealedEvent.class,
                        e -> ((GameEvent.QuestionRevealedEvent) e).round() == GameRoundType.ROUND_1));
    }

    @Test
    void forfeitEndsGame_andOpponentWins() {
        fixture.given(created())
                .when(new GameCommand.ForfeitGameCommand(GAME_ID, PLAYER_2))
                .expectEventsMatching(QuizUpAxonMatchers.hasPayloadMatching(
                        GameEvent.GameEndedEvent.class,
                        e -> {
                            GameEvent.GameEndedEvent ended = (GameEvent.GameEndedEvent) e;
                            return PLAYER_1.equals(ended.winnerId());
                        }));
    }

    @Test
    void forfeitByNonPlayer_isRejected() {
        fixture.given(created())
                .when(new GameCommand.ForfeitGameCommand(GAME_ID, "stranger"))
                .expectException(GameExceptions.PlayerNotInGameProblem.class);
    }

    @Test
    void forfeit_whenNotInProgress_isRejected() {
        fixture.given(finished())
                .when(new GameCommand.ForfeitGameCommand(GAME_ID, PLAYER_1))
                .expectException(GameExceptions.GameNotInProgressProblem.class);
    }

    @Test
    void answerAfterReveal_scoresFromRevealAndReportsCorrect() {
        Instant shownAt = Instant.now();
        Instant revealedAt = shownAt.plusMillis(GameRules.QUESTION_REVEAL_MS);
        Instant answeredAt = revealedAt.plusSeconds(2);

        fixture.given(concat(created(), roundStarted(shownAt), revealed(revealedAt)))
                .when(new GameCommand.AnswerQuestionCommand(
                        GAME_ID, PLAYER_1, GameQuestionChoice.A, answeredAt))
                .expectEventsMatching(QuizUpAxonMatchers.hasPayloadMatching(
                        GameEvent.QuestionAnsweredEvent.class,
                        e -> {
                            GameEvent.QuestionAnsweredEvent answered = (GameEvent.QuestionAnsweredEvent) e;
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

    private GameCommand.CreateGameCommand createCommand() {
        return new GameCommand.CreateGameCommand(
                GAME_ID, TOPIC_ID, PLAYER_1, "Alpha", PLAYER_2, "Bravo",
                Set.of(Language.FR), GamePlayerType.HUMAN, null,
                PLAYER_1_PROGRESS, PLAYER_2_PROGRESS);
    }

    /** La partie naît démarrée : {@code GameCreatedEvent} puis {@code GameStartedEvent}. */
    private Object[] created() {
        Instant now = Instant.now();
        return new Object[]{
                new GameEvent.GameCreatedEvent(
                        GAME_ID, TOPIC_ID, PLAYER_1, "Alpha", PLAYER_2, "Bravo",
                        GamePlayerType.HUMAN, questions(), null,
                        PLAYER_1_PROGRESS, PLAYER_2_PROGRESS, now),
                new GameEvent.GameStartedEvent(GAME_ID, now, now.plusMillis(GameRules.MATCH_INTRO_MS))
        };
    }

    private Object[] finished() {
        return concat(created(), ended());
    }

    private GameEvent.GameEndedEvent ended() {
        return new GameEvent.GameEndedEvent(
                GAME_ID,
                PLAYER_1,
                PLAYER_1, "Alpha",
                PLAYER_2, "Bravo",
                TOPIC_ID,
                20, 10,
                2, 1,
                1, 0,
                Instant.now());
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
                        Map.of(
                                Language.FR, new GameQuestionContent(
                                        "Question " + i,
                                        Map.of(
                                                GameQuestionChoice.A, "Réponse A",
                                                GameQuestionChoice.B, "Réponse B",
                                                GameQuestionChoice.C, "Réponse C",
                                                GameQuestionChoice.D, "Réponse D")),
                                Language.EN, new GameQuestionContent(
                                        "Question " + i,
                                        Map.of(
                                                GameQuestionChoice.A, "Answer A",
                                                GameQuestionChoice.B, "Answer B",
                                                GameQuestionChoice.C, "Answer C",
                                                GameQuestionChoice.D, "Answer D"))),
                        null,
                        null,
                        GameQuestionChoice.A))
                .toList();
    }
}
