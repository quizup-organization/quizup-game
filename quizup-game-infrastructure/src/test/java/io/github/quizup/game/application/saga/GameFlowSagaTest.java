package io.github.quizup.game.application.saga;

import io.github.quizup.game.domain.command.GameCommand;
import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.game.domain.model.GamePlayerType;
import org.axonframework.test.saga.SagaTestFixture;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

/**
 * Test in-memory de l'orchestration {@link GameFlowSaga} via {@link SagaTestFixture}.
 * La partie ne démarre qu'une fois les deux joueurs entrés dans l'arène ; seul le bot est
 * rejoint côté serveur (il n'a pas de client).
 */
class GameFlowSagaTest {

    private static final String GAME_ID = "game-1";
    private static final String PLAYER_1 = "player-1";
    private static final String PLAYER_2 = "player-2";

    private final SagaTestFixture<GameFlowSaga> fixture =
            new SagaTestFixture<>(GameFlowSaga.class);

    @Test
    void humanGame_doesNotAutoJoinPlayers() {
        fixture.givenNoPriorActivity()
                .whenPublishingA(gameCreated(GamePlayerType.HUMAN))
                .expectNoDispatchedCommands();
    }

    @Test
    void botGame_autoJoinsBot() {
        fixture.givenNoPriorActivity()
                .whenPublishingA(gameCreated(GamePlayerType.BOT))
                .expectDispatchedCommands(
                        new GameCommand.JoinGameCommand(GAME_ID, PLAYER_2)
                );
    }

    @Test
    void bothJoined_startsGame() {
        fixture.givenAPublished(gameCreated(GamePlayerType.HUMAN))
                .andThenAPublished(new GameEvent.GameJoinedEvent(GAME_ID, PLAYER_1, Instant.now()))
                .whenPublishingA(new GameEvent.GameJoinedEvent(GAME_ID, PLAYER_2, Instant.now()))
                .expectDispatchedCommands(
                        new GameCommand.StartGameCommand(GAME_ID)
                );
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
                Instant.now()
        );
    }
}
