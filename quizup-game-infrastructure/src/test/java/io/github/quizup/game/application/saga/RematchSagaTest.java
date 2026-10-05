package io.github.quizup.game.application.saga;

import io.github.quizup.game.domain.command.GameCommand;
import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.game.domain.model.GameDeadline;
import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.microservice.core.domain.model.i18n.Language;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.test.matchers.Matchers;
import org.axonframework.test.saga.SagaTestFixture;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Test in-memory de l'orchestration {@link RematchSaga} via {@link SagaTestFixture}.
 * L'acceptation crée la nouvelle partie puis confirme la revanche ; l'échec de création,
 * le refus, l'annulation et l'expiration closent la demande.
 */
class RematchSagaTest {

    private static final String GAME_ID = "game-1";
    private static final String TOPIC_ID = "topic-1";
    private static final String PLAYER_1 = "player-1";
    private static final String PLAYER_2 = "player-2";
    private static final Set<Language> LANGUAGES = Set.of(Language.FR, Language.EN);

    private final SagaTestFixture<RematchSaga> fixture =
            new SagaTestFixture<>(RematchSaga.class);

    @Test
    void accepted_createsNewGame_thenConfirmsRematch() {
        fixture.givenAPublished(rematchRequested())
                .whenPublishingA(rematchAccepted())
                .expectDispatchedCommandsMatching(acceptedRematchCommands());
    }

    @Test
    void creationFailure_abortsRematch() {
        fixture.setCallbackBehavior((payload, metaData) -> {
            if (payload instanceof GameCommand.CreateGameCommand) {
                throw new IllegalStateException("Aucune question disponible pour ce thème");
            }
            return null;
        });

        fixture.givenAPublished(rematchRequested())
                .whenPublishingA(rematchAccepted())
                .expectDispatchedCommandsMatching(createFailureCommands());
    }

    @Test
    void declined_endsSaga() {
        fixture.givenAPublished(rematchRequested())
                .whenPublishingA(new GameEvent.RematchDeclinedEvent(GAME_ID, PLAYER_2, Instant.now()))
                .expectActiveSagas(0)
                .expectNoDispatchedCommands();
    }

    @Test
    void cancelled_endsSaga() {
        fixture.givenAPublished(rematchRequested())
                .whenPublishingA(new GameEvent.RematchCancelledEvent(GAME_ID, "PLAYER_CANCELLED", Instant.now()))
                .expectActiveSagas(0)
                .expectNoDispatchedCommands();
    }

    @Test
    void expiry_abortsRematch() {
        fixture.givenAPublished(rematchRequested())
                .whenTimeElapses(GameDeadline.REMATCH_EXPIRY_DURATION)
                .expectDispatchedCommands(new GameCommand.AbortRematchCommand(GAME_ID, "EXPIRED"));
    }

    private static GameEvent.RematchRequestedEvent rematchRequested() {
        return new GameEvent.RematchRequestedEvent(
                GAME_ID, PLAYER_1, "Alpha", PLAYER_2, "Bravo", TOPIC_ID, LANGUAGES, Instant.now());
    }

    private static GameEvent.RematchAcceptedEvent rematchAccepted() {
        return new GameEvent.RematchAcceptedEvent(GAME_ID, PLAYER_2, Instant.now());
    }

    private static Matcher<? extends List<? super CommandMessage<?>>> acceptedRematchCommands() {
        return Matchers.payloadsMatching(Matchers.matches((List<?> payloads) -> {
            if (payloads.size() != 2) {
                return false;
            }
            if (!(payloads.get(0) instanceof GameCommand.CreateGameCommand create)) {
                return false;
            }
            if (!(payloads.get(1) instanceof GameCommand.ConfirmRematchCommand confirm)) {
                return false;
            }
            return GAME_ID.equals(confirm.gameId())
                    && create.gameId().equals(confirm.newGameId())
                    && TOPIC_ID.equals(create.topicId())
                    && PLAYER_1.equals(create.player1Id())
                    && "Alpha".equals(create.player1Name())
                    && PLAYER_2.equals(create.player2Id())
                    && "Bravo".equals(create.player2Name())
                    && LANGUAGES.equals(create.languages())
                    && GamePlayerType.HUMAN.equals(create.player2Type());
        }));
    }

    private static Matcher<? extends List<? super CommandMessage<?>>> createFailureCommands() {
        return Matchers.payloadsMatching(Matchers.matches((List<?> payloads) -> {
            if (payloads.size() != 2 || !(payloads.get(0) instanceof GameCommand.CreateGameCommand)) {
                return false;
            }
            return payloads.get(1) instanceof GameCommand.AbortRematchCommand abort
                    && GAME_ID.equals(abort.gameId())
                    && "CREATE_FAILED".equals(abort.reason());
        }));
    }
}
