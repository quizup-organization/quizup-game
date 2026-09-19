package io.github.quizup.game.domain.port.in;

import io.github.quizup.game.domain.command.GameCommand;
import io.github.quizup.game.domain.model.BotDifficulty;
import io.github.quizup.game.domain.model.GameMode;
import io.github.quizup.game.domain.model.GamePlayerType;

import java.util.concurrent.CompletableFuture;

public interface CreateGameUseCase {

    CompletableFuture<String> create(GameCommand.CreateGameCommand command);

    default CompletableFuture<String> create(String gameId,
                                             String topicId,
                                             String player1Id,
                                             String player1Name,
                                             String player2Id,
                                             String player2Name,
                                             GameMode mode,
                                             GamePlayerType player2Type) {
        return create(gameId, topicId, player1Id, player1Name, player2Id, player2Name, mode, player2Type, null, null);
    }

    default CompletableFuture<String> create(String gameId,
                                             String topicId,
                                             String player1Id,
                                             String player1Name,
                                             String player2Id,
                                             String player2Name,
                                             GameMode mode,
                                             GamePlayerType player2Type,
                                             BotDifficulty botDifficulty) {
        return create(gameId, topicId, player1Id, player1Name, player2Id, player2Name, mode, player2Type, botDifficulty, null);
    }

    default CompletableFuture<String> create(String gameId,
                                             String topicId,
                                             String player1Id,
                                             String player1Name,
                                             String player2Id,
                                             String player2Name,
                                             GameMode mode,
                                             GamePlayerType player2Type,
                                             BotDifficulty botDifficulty,
                                             String ghostGameId) {
        return create(
                new GameCommand.CreateGameCommand(
                        gameId,
                        topicId,
                        player1Id,
                        player1Name,
                        player2Id,
                        player2Name,
                        mode,
                        player2Type,
                        botDifficulty,
                        ghostGameId
                )
        );
    }
}
