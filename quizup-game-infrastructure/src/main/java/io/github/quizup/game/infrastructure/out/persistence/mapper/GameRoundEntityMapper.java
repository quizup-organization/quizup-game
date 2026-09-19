package io.github.quizup.game.infrastructure.out.persistence.mapper;

import io.github.quizup.game.domain.model.GameRound;
import io.github.quizup.game.infrastructure.out.persistence.entity.GameEntity;
import io.github.quizup.game.infrastructure.out.persistence.entity.GameRoundEntity;

public final class GameRoundEntityMapper {

    private GameRoundEntityMapper() {
    }

    public static GameRound toDomain(GameRoundEntity entity) {
        return GameRound.builder()
                .round(entity.getRound())
                .questionId(entity.getQuestionId())
                .questionText(entity.getQuestionText())
                .correctAnswer(entity.getCorrectAnswer())
                .player1Choice(entity.getPlayer1Choice())
                .player1Points(entity.getPlayer1Points())
                .player1TimeMs(entity.getPlayer1TimeMs())
                .player2Choice(entity.getPlayer2Choice())
                .player2Points(entity.getPlayer2Points())
                .player2TimeMs(entity.getPlayer2TimeMs())
                .status(entity.getStatus())
                .revealedAt(entity.getRevealedAt())
                .answerDeadlineAt(entity.getAnswerDeadlineAt())
                .build();
    }

    public static GameRoundEntity toEntity(GameRound round, GameEntity gameEntity) {
        GameRoundEntity entity = new GameRoundEntity();
        entity.setId(gameEntity.getGameId() + "-" + round.round().name());
        entity.setGame(gameEntity);
        entity.setRound(round.round());
        entity.setQuestionId(round.questionId());
        entity.setQuestionText(round.questionText());
        entity.setCorrectAnswer(round.correctAnswer());
        entity.setPlayer1Choice(round.player1Choice());
        entity.setPlayer1Points(round.player1Points());
        entity.setPlayer1TimeMs(round.player1TimeMs());
        entity.setPlayer2Choice(round.player2Choice());
        entity.setPlayer2Points(round.player2Points());
        entity.setPlayer2TimeMs(round.player2TimeMs());
        entity.setStatus(round.status());
        entity.setRevealedAt(round.revealedAt());
        entity.setAnswerDeadlineAt(round.answerDeadlineAt());
        return entity;
    }
}

