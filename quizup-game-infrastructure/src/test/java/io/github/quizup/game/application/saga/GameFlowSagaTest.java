package io.github.quizup.game.application.saga;

import io.github.quizup.game.domain.command.GameCommand;
import io.github.quizup.game.domain.event.GameEvent;
import io.github.quizup.game.domain.model.GameMode;
import io.github.quizup.game.domain.model.GamePlayerType;
import io.github.quizup.game.domain.port.out.GameEventStorePort;
import org.axonframework.test.saga.SagaTestFixture;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.mock;

/**
 * Test in-memory de l'orchestration {@link GameFlowSaga} via {@link SagaTestFixture}.
 * Couvre le démarrage d'un run asynchrone solo (joueur 2 absent) : la saga fait rejoindre le
 * joueur présent puis démarre la partie, sans passer par l'attente des deux joueurs du mode SYNC.
 */
class GameFlowSagaTest {

    private final SagaTestFixture<GameFlowSaga> fixture =
            new SagaTestFixture<>(GameFlowSaga.class);

    @Test
    void asyncRecordRun_joinsPlayerAndStartsGame() {
        fixture.registerResource(mock(GameEventStorePort.class));

        fixture.givenNoPriorActivity()
                .whenPublishingA(new GameEvent.GameCreatedEvent(
                        "game-1",
                        "topic-1",
                        "player-1",
                        "Alpha",
                        null,
                        null,
                        GamePlayerType.HUMAN,
                        GameMode.ASYNC,
                        List.of(),
                        null,
                        null,
                        Instant.now()
                ))
                .expectDispatchedCommands(
                        new GameCommand.JoinGameCommand("game-1", "player-1"),
                        new GameCommand.StartGameCommand("game-1")
                );
    }
}
