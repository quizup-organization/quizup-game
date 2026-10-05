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
import io.github.quizup.game.domain.model.GameStatus;
import io.github.quizup.game.domain.port.out.QuestionRepositoryPort;
import io.github.quizup.microservice.core.domain.model.i18n.Language;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.axonframework.test.matchers.Matchers;
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
 * Le port sortant {@link QuestionRepositoryPort} est un mock.
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
        fixture.setReportIllegalStateChange(false);
    }

    @Test
    void createGame_appliesGameCreatedEvent() {
        QuestionRepositoryPort questionRepositoryPort = mock(QuestionRepositoryPort.class);
        when(questionRepositoryPort.findRandomApprovedByTopicId(anyString(), anyInt(), anySet())).thenReturn(questions());

        fixture.registerInjectableResource(questionRepositoryPort)
                .givenNoPriorActivity()
                .when(createCommand())
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        GameEvent.GameCreatedEvent.class,
                        e -> ((GameEvent.GameCreatedEvent) e).gameId().equals(GAME_ID)));
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
                Set.of(Language.FR), GamePlayerType.HUMAN, null);

        fixture.registerInjectableResource(mock(QuestionRepositoryPort.class))
                .givenNoPriorActivity()
                .when(command)
                .expectException(GameExceptions.MissingPlayerProblem.class);
    }

    @Test
    void joinGame_isIdempotent() {
        fixture.given(concat(created(), joined(PLAYER_1), joined(PLAYER_2)))
                .when(new GameCommand.JoinGameCommand(GAME_ID, PLAYER_1))
                .expectNoEvents();
    }

    @Test
    void leaveBeforeStart_cancelsGame() {
        fixture.given(concat(created(), joined(PLAYER_1)))
                .when(new GameCommand.LeaveGameCommand(GAME_ID, PLAYER_1))
                .expectEventsMatching(QuizUpAxonMatchers.hasPayloadMatching(
                        GameEvent.GameCancelledEvent.class,
                        e -> "PLAYER_LEFT".equals(((GameEvent.GameCancelledEvent) e).reason())));
    }

    @Test
    void leaveAfterStart_isRejected() {
        fixture.given(readyGame())
                .when(new GameCommand.LeaveGameCommand(GAME_ID, PLAYER_1))
                .expectException(GameExceptions.GameAlreadyStartedProblem.class);
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
    void forfeitEndsGame_andOpponentWins() {
        fixture.given(readyGame())
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
        fixture.given(readyGame())
                .when(new GameCommand.ForfeitGameCommand(GAME_ID, "stranger"))
                .expectException(GameExceptions.PlayerNotInGameProblem.class);
    }

    @Test
    void forfeit_whenNotInProgress_isRejected() {
        fixture.given(created())
                .when(new GameCommand.ForfeitGameCommand(GAME_ID, PLAYER_1))
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
                            return answered.correct()
                                    && answered.pointsEarned() == 18
                                    && answered.timeMs() == 2000;
                        }));
    }

    // ── Présence écran de résultat ──

    @Test
    void joinAfterFinished_appliesGameJoinedEvent() {
        fixture.given(finishedWithoutPresence())
                .when(new GameCommand.JoinGameCommand(GAME_ID, PLAYER_1))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        GameEvent.GameJoinedEvent.class,
                        e -> PLAYER_1.equals(((GameEvent.GameJoinedEvent) e).playerId())));
    }

    @Test
    void joinAfterFinished_doesNotBecomeReady() {
        fixture.given(concat(finishedWithoutPresence(), joined(PLAYER_1), joined(PLAYER_2)))
                .when(new GameCommand.StartGameCommand(GAME_ID))
                .expectException(GameExceptions.GameNotReadyProblem.class);
    }

    @Test
    void leaveAfterFinished_appliesGameLeftOnly() {
        fixture.given(finishedWithPresence())
                .when(new GameCommand.LeaveGameCommand(GAME_ID, PLAYER_1))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        GameEvent.GameLeftEvent.class,
                        e -> PLAYER_1.equals(((GameEvent.GameLeftEvent) e).playerId())));
    }

    @Test
    void leaveAfterFinished_whenAbsent_isNoOp() {
        fixture.given(finishedWithoutPresence())
                .when(new GameCommand.LeaveGameCommand(GAME_ID, PLAYER_1))
                .expectNoEvents();
    }

    // ── Revanche ──

    @Test
    void requestRematch_appliesRematchRequestedEvent() {
        fixture.given(finishedWithPresence())
                .when(new GameCommand.RequestRematchCommand(GAME_ID, PLAYER_1, Set.of(Language.FR)))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        GameEvent.RematchRequestedEvent.class,
                        e -> {
                            GameEvent.RematchRequestedEvent requested = (GameEvent.RematchRequestedEvent) e;
                            return PLAYER_1.equals(requested.requesterId())
                                    && PLAYER_2.equals(requested.opponentId())
                                    && TOPIC_ID.equals(requested.topicId())
                                    && Set.of(Language.FR).equals(requested.languages());
                        }));
    }

    @Test
    void requestRematch_whenNotFinished_isRejected() {
        fixture.given(readyGame())
                .when(new GameCommand.RequestRematchCommand(GAME_ID, PLAYER_1, Set.of(Language.FR)))
                .expectException(GameExceptions.GameNotFinishedProblem.class);
    }

    @Test
    void requestRematch_againstBot_isRejected() {
        fixture.given(finishedBotWithoutPresence())
                .when(new GameCommand.RequestRematchCommand(GAME_ID, PLAYER_1, Set.of(Language.FR)))
                .expectException(GameExceptions.BotRematchNotAllowedProblem.class);
    }

    @Test
    void requestRematch_withoutPresence_isRejected() {
        fixture.given(finishedWithoutPresence())
                .when(new GameCommand.RequestRematchCommand(GAME_ID, PLAYER_1, Set.of(Language.FR)))
                .expectException(GameExceptions.RematchPlayersNotPresentProblem.class);
    }

    @Test
    void requestRematch_withoutLanguages_isRejected() {
        fixture.given(finishedWithPresence())
                .when(new GameCommand.RequestRematchCommand(GAME_ID, PLAYER_1, Set.of()))
                .expectException(GameExceptions.MissingLanguagesProblem.class);
    }

    @Test
    void acceptRematch_byOpponent_appliesRematchAcceptedEvent() {
        fixture.given(concat(finishedWithPresence(), rematchRequested(PLAYER_1)))
                .when(new GameCommand.AcceptRematchCommand(GAME_ID, PLAYER_2))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        GameEvent.RematchAcceptedEvent.class,
                        e -> PLAYER_2.equals(((GameEvent.RematchAcceptedEvent) e).playerId())));
    }

    @Test
    void acceptRematch_withoutRequest_isRejected() {
        fixture.given(finishedWithPresence())
                .when(new GameCommand.AcceptRematchCommand(GAME_ID, PLAYER_2))
                .expectException(GameExceptions.RematchNotRequestedProblem.class);
    }

    @Test
    void acceptRematch_byRequester_isNoOp() {
        fixture.given(concat(finishedWithPresence(), rematchRequested(PLAYER_1)))
                .when(new GameCommand.AcceptRematchCommand(GAME_ID, PLAYER_1))
                .expectNoEvents();
    }

    @Test
    void secondRequest_appliesRematchAcceptedEvent() {
        fixture.given(concat(finishedWithPresence(), rematchRequested(PLAYER_1)))
                .when(new GameCommand.RequestRematchCommand(GAME_ID, PLAYER_2, Set.of(Language.FR)))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        GameEvent.RematchAcceptedEvent.class,
                        e -> PLAYER_2.equals(((GameEvent.RematchAcceptedEvent) e).playerId())));
    }

    @Test
    void declineRematch_appliesRematchDeclinedEvent() {
        fixture.given(concat(finishedWithPresence(), rematchRequested(PLAYER_1)))
                .when(new GameCommand.DeclineRematchCommand(GAME_ID, PLAYER_2))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        GameEvent.RematchDeclinedEvent.class,
                        e -> PLAYER_2.equals(((GameEvent.RematchDeclinedEvent) e).playerId())));
    }

    @Test
    void cancelRematch_byRequester_appliesRematchCancelledEvent() {
        fixture.given(concat(finishedWithPresence(), rematchRequested(PLAYER_1)))
                .when(new GameCommand.CancelRematchCommand(GAME_ID, PLAYER_1))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        GameEvent.RematchCancelledEvent.class,
                        e -> "PLAYER_CANCELLED".equals(((GameEvent.RematchCancelledEvent) e).reason())));
    }

    @Test
    void cancelRematch_byOpponent_isRejected() {
        fixture.given(concat(finishedWithPresence(), rematchRequested(PLAYER_1)))
                .when(new GameCommand.CancelRematchCommand(GAME_ID, PLAYER_2))
                .expectException(GameExceptions.NotRematchRequesterProblem.class);
    }

    @Test
    void confirmRematch_afterAccept_appliesRematchStartedEvent() {
        fixture.given(concat(finishedWithPresence(), rematchRequested(PLAYER_1), rematchAccepted(PLAYER_2)))
                .when(new GameCommand.ConfirmRematchCommand(GAME_ID, "new-game"))
                .expectEventsMatching(QuizUpAxonMatchers.singlePayloadMatching(
                        GameEvent.RematchStartedEvent.class,
                        e -> "new-game".equals(((GameEvent.RematchStartedEvent) e).newGameId())));
    }

    @Test
    void confirmRematch_withoutAccept_isRejected() {
        fixture.given(concat(finishedWithPresence(), rematchRequested(PLAYER_1)))
                .when(new GameCommand.ConfirmRematchCommand(GAME_ID, "new-game"))
                .expectException(GameExceptions.RematchNotAcceptedProblem.class);
    }

    @Test
    void leaveDuringRematch_cancelsRematchAndLeaves() {
        fixture.given(concat(finishedWithPresence(), rematchRequested(PLAYER_1)))
                .when(new GameCommand.LeaveGameCommand(GAME_ID, PLAYER_2))
                .expectEventsMatching(Matchers.payloadsMatching(Matchers.exactSequenceOf(
                        Matchers.matches(m -> m instanceof GameEvent.RematchCancelledEvent cancelled
                                && "PLAYER_LEFT".equals(cancelled.reason())),
                        Matchers.matches(m -> m instanceof GameEvent.GameLeftEvent left
                                && PLAYER_2.equals(left.playerId())),
                        Matchers.andNoMore())));
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
                Set.of(Language.FR), GamePlayerType.HUMAN, null);
    }

    private Object[] created() {
        return new Object[]{new GameEvent.GameCreatedEvent(
                GAME_ID, TOPIC_ID, PLAYER_1, "Alpha", PLAYER_2, "Bravo",
                GamePlayerType.HUMAN, questions(), null, Instant.now())};
    }

    private Object[] createdBot() {
        return new Object[]{new GameEvent.GameCreatedEvent(
                GAME_ID, TOPIC_ID, PLAYER_1, "Alpha", PLAYER_2, "Bot",
                GamePlayerType.BOT, questions(), null, Instant.now())};
    }

    private GameEvent.GameJoinedEvent joined(String playerId) {
        return new GameEvent.GameJoinedEvent(GAME_ID, playerId, Instant.now());
    }

    private Object[] readyGame() {
        return concat(created(), joined(PLAYER_1), joined(PLAYER_2),
                new GameEvent.GameStartedEvent(GAME_ID, Instant.now(), Instant.now().plusMillis(GameRules.MATCH_INTRO_MS)));
    }

    private Object[] finishedWithoutPresence() {
        return concat(readyGame(), ended());
    }

    private Object[] finishedBotWithoutPresence() {
        return concat(createdBot(), joined(PLAYER_1), joined(PLAYER_2),
                new GameEvent.GameStartedEvent(GAME_ID, Instant.now(), Instant.now().plusMillis(GameRules.MATCH_INTRO_MS)),
                ended());
    }

    private Object[] finishedWithPresence() {
        return concat(finishedWithoutPresence(), joined(PLAYER_1), joined(PLAYER_2));
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

    private GameEvent.RematchRequestedEvent rematchRequested(String requesterId) {
        return new GameEvent.RematchRequestedEvent(
                GAME_ID, requesterId, "Alpha", PLAYER_2, "Bravo", TOPIC_ID, Set.of(Language.FR), Instant.now());
    }

    private GameEvent.RematchAcceptedEvent rematchAccepted(String playerId) {
        return new GameEvent.RematchAcceptedEvent(GAME_ID, playerId, Instant.now());
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
