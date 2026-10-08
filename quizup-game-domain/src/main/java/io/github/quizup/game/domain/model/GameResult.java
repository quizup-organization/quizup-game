package io.github.quizup.game.domain.model;

import io.github.quizup.game.domain.exception.GameExceptions;

import static java.util.Objects.isNull;

/**
 * Résultat d'une partie du point de vue d'un joueur (écran de fin) : identités, scores et
 * statistiques de réponses recalculées depuis les rounds.
 */
public record GameResult(
        String gameId,
        String topicId,
        String player1Id,
        String player1Name,
        String player2Id,
        String player2Name,
        String playerId,
        String opponentId,
        int myScore,
        int opponentScore,
        String winnerId,
        boolean botGame,
        int basePoints,
        int speedBonus,
        int correctAnswers,
        int fastAnswers,
        int answeredRounds,
        int totalRounds,
        int myLevel,
        int myXpTotal,
        int opponentLevel,
        int opponentXpTotal
) {

    /**
     * Construit le résultat de {@code game} du point de vue du joueur {@code playerId}.
     *
     * @throws GameExceptions.PlayerNotInGameProblem si le joueur ne participe pas à la partie
     */
    public static GameResult from(Game game, String playerId) {
        boolean isPlayer1 = playerId.equals(game.player1Id());

        if (!isPlayer1 && !playerId.equals(game.player2Id())) {
            throw new GameExceptions.PlayerNotInGameProblem(game.gameId(), playerId);
        }

        int basePoints = 0;
        int speedBonus = 0;
        int correctAnswers = 0;
        int fastAnswers = 0;
        int answeredRounds = 0;

        for (GameRound round : game.rounds()) {
            GameQuestionChoice choice = isPlayer1 ? round.player1Choice() : round.player2Choice();
            int points = isPlayer1 ? round.player1Points() : round.player2Points();
            Long timeMs = isPlayer1 ? round.player1TimeMs() : round.player2TimeMs();

            if (isNull(choice)) {
                continue;
            }

            answeredRounds++;

            if (choice != round.correctAnswer()) {
                continue;
            }

            correctAnswers++;
            int roundBasePoints = GameRules.getBasePoints(round.round().isBonus());
            basePoints += roundBasePoints;
            speedBonus += Math.max(0, points - roundBasePoints);

            if (!isNull(timeMs) && timeMs < GameRules.FAST_ANSWER_SECONDS * 1000) {
                fastAnswers++;
            }
        }

        PlayerProgressSnapshot myProgress = isPlayer1 ? game.player1Progress() : game.player2Progress();
        PlayerProgressSnapshot opponentProgress = isPlayer1 ? game.player2Progress() : game.player1Progress();

        return new GameResult(
                game.gameId(),
                game.topicId(),
                game.player1Id(),
                game.player1Name(),
                game.player2Id(),
                game.player2Name(),
                playerId,
                isPlayer1 ? game.player2Id() : game.player1Id(),
                isPlayer1 ? game.player1Score() : game.player2Score(),
                isPlayer1 ? game.player2Score() : game.player1Score(),
                game.winnerId(),
                GamePlayerType.BOT.equals(game.opponent()),
                basePoints,
                speedBonus,
                correctAnswers,
                fastAnswers,
                answeredRounds,
                game.rounds().size(),
                myProgress == null ? 0 : myProgress.level(),
                myProgress == null ? 0 : myProgress.xpTotal(),
                opponentProgress == null ? 0 : opponentProgress.level(),
                opponentProgress == null ? 0 : opponentProgress.xpTotal()
        );
    }
}
