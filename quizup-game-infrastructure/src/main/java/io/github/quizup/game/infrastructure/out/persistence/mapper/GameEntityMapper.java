package io.github.quizup.game.infrastructure.out.persistence.mapper;

import io.github.quizup.game.domain.model.Game;
import io.github.quizup.game.infrastructure.out.persistence.entity.GameEntity;

import java.util.List;
import java.util.Optional;

public final class GameEntityMapper {

    private GameEntityMapper() {
    }

    public static Game toDomain(GameEntity entity) {
        return Game.builder()
                .gameId(entity.getGameId())
                .topicId(entity.getTopicId())
                .player1Id(entity.getPlayer1Id())
                .player2Id(entity.getPlayer2Id())
                .mode(entity.getMode())
                .opponent(entity.getOpponent())
                .status(entity.getStatus())
                .player1Score(entity.getPlayer1Score())
                .player2Score(entity.getPlayer2Score())
                .winnerId(entity.getWinnerId())
                .createdAt(entity.getCreatedAt())
                .startedAt(entity.getStartedAt())
                .endedAt(entity.getEndedAt())
                .rounds(entity.getRounds().stream().map(GameRoundEntityMapper::toDomain).toList())
                .build();
    }

    public static GameEntity toEntity(Game game) {
        GameEntity entity = new GameEntity();
        entity.setGameId(game.gameId());
        entity.setTopicId(game.topicId());
        entity.setPlayer1Id(game.player1Id());
        entity.setPlayer2Id(game.player2Id());
        entity.setMode(game.mode());
        entity.setOpponent(game.opponent());
        entity.setStatus(game.status());
        entity.setPlayer1Score(game.player1Score());
        entity.setPlayer2Score(game.player2Score());
        entity.setWinnerId(game.winnerId());
        entity.setCreatedAt(game.createdAt());
        entity.setStartedAt(game.startedAt());
        entity.setEndedAt(game.endedAt());
        Optional.ofNullable(game.rounds()).orElse(List.of()).forEach(round -> entity.getRounds().add(
                GameRoundEntityMapper.toEntity(round, entity)
        ));
        return entity;
    }
}

