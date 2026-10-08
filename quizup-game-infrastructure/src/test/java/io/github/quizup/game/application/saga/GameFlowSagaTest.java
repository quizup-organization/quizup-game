package io.github.quizup.game.application.saga;

import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.game.domain.model.GamePlayerType;
import org.axonframework.test.saga.SagaTestFixture;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

/**
 * Test in-memory de l'orchestration {@link GameFlowSaga} via {@link SagaTestFixture}. La partie
 * démarre immédiatement à la création : la saga n'émet aucune commande à la création (elle
 * planifie l'expiration), puis l'intro ({@code MATCH_INTRO}) déclenche le premier round.
 */
class GameFlowSagaTest {

    private static final String GAME_ID = "game-1";
    private static final String PLAYER_1 = "player-1";
    private static final String PLAYER_2 = "player-2";

    private final SagaTestFixture<GameFlowSaga> fixture =
            new SagaTestFixture<>(GameFlowSaga.class);

    @Test
    void gameCreated_dispatchesNoCommand() {
        fixture.givenNoPriorActivity()
                .whenPublishingA(gameCreated(GamePlayerType.HUMAN))
                .expectNoDispatchedCommands();
    }

    private static GameEvent.GameCreatedEvent gameCreated(GamePlayerType player2Type) {
        return new GameEvent.GameCreatedEvent(
                GAME_ID,
                "topic-1",
                PLAYER_1,
                "Alpha",
                PLAYER_2,
                "Bravo",
                player2Type,
                List.of(),
                null,
                null,
                null,
                Instant.now()
        );
    }
}
