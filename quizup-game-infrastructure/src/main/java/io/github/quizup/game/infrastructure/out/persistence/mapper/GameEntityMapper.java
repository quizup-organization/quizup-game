package io.github.quizup.game.infrastructure.out.persistence.mapper;

import io.github.quizup.game.domain.model.Game;
import io.github.quizup.game.domain.model.PlayerProgressSnapshot;
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
                .player1Name(entity.getPlayer1Name())
                .player2Id(entity.getPlayer2Id())
                .player2Name(entity.getPlayer2Name())
                .opponent(entity.getOpponent())
                .botDifficulty(entity.getBotDifficulty())
                .player1Progress(progress(entity.getPlayer1Level(), entity.getPlayer1XpTotal()))
                .player2Progress(progress(entity.getPlayer2Level(), entity.getPlayer2XpTotal()))
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
        entity.setPlayer1Name(game.player1Name());
        entity.setPlayer2Id(game.player2Id());
        entity.setPlayer2Name(game.player2Name());
        entity.setOpponent(game.opponent());
        entity.setBotDifficulty(game.botDifficulty());
        entity.setPlayer1Level(level(game.player1Progress()));
        entity.setPlayer1XpTotal(xpTotal(game.player1Progress()));
        entity.setPlayer2Level(level(game.player2Progress()));
        entity.setPlayer2XpTotal(xpTotal(game.player2Progress()));
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

    private static PlayerProgressSnapshot progress(Integer level, Integer xpTotal) {
        if (level == null) {
            return null;
        }
        return new PlayerProgressSnapshot(level, xpTotal == null ? 0 : xpTotal);
    }

    private static Integer level(PlayerProgressSnapshot progress) {
        return progress == null ? null : progress.level();
    }

    private static Integer xpTotal(PlayerProgressSnapshot progress) {
        return progress == null ? null : progress.xpTotal();
    }
}

