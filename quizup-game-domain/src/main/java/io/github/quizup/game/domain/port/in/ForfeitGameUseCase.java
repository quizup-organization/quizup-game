package io.github.quizup.game.domain.port.in;

import io.github.quizup.game.domain.command.GameCommand;

import java.util.concurrent.CompletableFuture;

/**
 * Port entrant — abandon d'une partie en cours (l'adversaire est déclaré vainqueur).
 */
public interface ForfeitGameUseCase {

    CompletableFuture<String> forfeit(GameCommand.ForfeitGameCommand command);

    default CompletableFuture<String> forfeit(String gameId, String playerId) {
        return forfeit(new GameCommand.ForfeitGameCommand(gameId, playerId));
    }
}
