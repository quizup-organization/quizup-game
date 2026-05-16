package io.github.quizup.game.domain.exception;

import io.github.quizup.common.domain.exception.ProblemCategory;
import io.github.quizup.game.domain.model.GamePlayer;

import java.util.Map;

/**
 * Exceptions spécifiques au domaine Game.
 * Chaque exception décrit un cas métier précis — pas d'exception générique paramétrée.
 */
public interface GameExceptions {

    // ── Création ──

    class MissingTopicProblem extends GameProblem {
        public MissingTopicProblem(String gameId) {
            super(gameId, "urn:quizup:game:missingTopic",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Topic required",
                    "A topic is required to create game " + gameId, null);
        }
    }

    class MissingPlayerProblem extends GameProblem {
        public MissingPlayerProblem(String gameId, GamePlayer gamePlayer) {
            super(gameId, "urn:quizup:game:missingPlayer",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Player required",
                    gamePlayer.name() + " is required to create game " + gameId,
                    Map.of("player", gamePlayer.name()));
        }
    }

    class MissingPlayerIdProblem extends GameProblem {
        public MissingPlayerIdProblem(String gameId) {
            super(gameId, "urn:quizup:game:missingPlayerId",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Player ID required",
                    "A player ID is required to perform this action on game " + gameId, null);
        }
    }


    class MissingTimestampProblem extends GameProblem {
        public MissingTimestampProblem(String gameId) {
            super(gameId, "urn:quizup:game:missingTimestamp",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Timestamp required",
                    "A timestamp is required to answer a question in game " + gameId, null);
        }
    }

    // ── Lookup ──

    class GameNotFoundProblem extends GameProblem {
        public GameNotFoundProblem(String gameId) {
            super(gameId, "urn:quizup:game:notFound",
                    ProblemCategory.BUSINESS_RESOURCE_MISSING,
                    "Game not found",
                    "The game " + gameId + " was not found", null);
        }
    }

    // ── Join ──

    class GameNotJoinableProblem extends GameProblem {
        public GameNotJoinableProblem(String gameId, String currentStatus) {
            super(gameId, "urn:quizup:game:notJoinable",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Game is not joinable",
                    "Game " + gameId + " is in status " + currentStatus + " and cannot be joined",
                    Map.of("currentStatus", currentStatus));
        }
    }

    class PlayerAlreadyJoinedProblem extends GameProblem {
        public PlayerAlreadyJoinedProblem(String gameId, String playerId) {
            super(gameId, "urn:quizup:game:playerAlreadyJoined",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Player already joined",
                    "Player " + playerId + " has already joined game " + gameId,
                    Map.of("playerId", playerId));
        }
    }

    class PlayerNotInGameProblem extends GameProblem {
        public PlayerNotInGameProblem(String gameId, String playerId) {
            super(gameId, "urn:quizup:game:playerNotInGame",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Player not in game",
                    "Player " + playerId + " is not a participant of game " + gameId,
                    Map.of("playerId", playerId));
        }
    }

    // ── Start ──

    class GameNotReadyProblem extends GameProblem {
        public GameNotReadyProblem(String gameId, String currentStatus) {
            super(gameId, "urn:quizup:game:notReady",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Game is not ready to start",
                    "Game " + gameId + " is in status " + currentStatus + " but both players must be present",
                    Map.of("currentStatus", currentStatus));
        }
    }

    class GameNotStartableProblem extends GameProblem {
        public GameNotStartableProblem(String gameId, String currentStatus) {
            super(gameId, "urn:quizup:game:notStartable",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Game cannot be started",
                    "Game " + gameId + " is in status " + currentStatus + " and cannot be started",
                    Map.of("currentStatus", currentStatus));
        }
    }

    // ── Rounds ──

    class GameNotInProgressProblem extends GameProblem {
        public GameNotInProgressProblem(String gameId, String currentStatus) {
            super(gameId, "urn:quizup:game:notInProgress",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Game is not in progress",
                    "Game " + gameId + " is in status " + currentStatus + " but must be IN_PROGRESS",
                    Map.of("currentStatus", currentStatus));
        }
    }

    class RoundNotStartedProblem extends GameProblem {
        public RoundNotStartedProblem(String gameId, String round) {
            super(gameId, "urn:quizup:game:roundNotStarted",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Round not started",
                    "Round " + round + " in game " + gameId + " has not been started yet",
                    Map.of("round", round));
        }
    }

    class RoundNotStartableProblem extends GameProblem {
        public RoundNotStartableProblem(String gameId, String round, String currentStatus) {
            super(gameId, "urn:quizup:game:roundNotStartable",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Round cannot be started",
                    "Round " + round + " is in status " + currentStatus + " and cannot be started",
                    Map.of("round", round, "currentStatus", currentStatus));
        }
    }

    class RoundAlreadyAnsweredProblem extends GameProblem {
        public RoundAlreadyAnsweredProblem(String gameId, String round, String playerId) {
            super(gameId, "urn:quizup:game:roundAlreadyAnswered",
                    ProblemCategory.BUSINESS_INVALID_COMMAND,
                    "Round already answered",
                    "Player " + playerId + " has already answered round " + round + " in game " + gameId,
                    Map.of("round", round, "playerId", playerId));
        }
    }
}
