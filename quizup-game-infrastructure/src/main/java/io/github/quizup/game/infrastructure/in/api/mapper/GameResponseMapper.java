package io.github.quizup.game.infrastructure.in.api.mapper;

import io.github.quizup.common.domain.model.search.PageResult;
import io.github.quizup.common.infrastructure.in.api.response.PageResponse;
import io.github.quizup.common.infrastructure.mapper.SearchResponseMapper;
import io.github.quizup.game.domain.model.Game;
import io.github.quizup.game.domain.model.GameRound;
import io.github.quizup.game.domain.model.GameRoundStatus;
import io.github.quizup.game.infrastructure.mapper.GameQuestionChoiceMapper;
import io.github.quizup.game.infrastructure.in.api.response.GameResponse;
import io.github.quizup.game.infrastructure.in.api.response.GameRoundResponse;

public final class GameResponseMapper {

    private GameResponseMapper() {
    }

    public static GameResponse toResponse(Game game) {
        return new GameResponse(
                game.gameId(),
                game.topicId(),
                game.player1Id(),
                game.player2Id(),
                game.mode(),
                game.opponent(),
                game.status(),
                game.player1Score(),
                game.player2Score(),
                game.winnerId(),
                game.createdAt(),
                game.rounds().stream().map(GameResponseMapper::toResponse).toList()
        );
    }

    public static GameRoundResponse toResponse(GameRound round) {
        return new GameRoundResponse(
                round.round(),
                round.questionText(),
                round.status(),
                GameQuestionChoiceMapper.toTopic(round.player1Choice()),
                round.player1Points(),
                GameQuestionChoiceMapper.toTopic(round.player2Choice()),
                round.player2Points(),
                round.status() == GameRoundStatus.CLOSED ? GameQuestionChoiceMapper.toTopic(round.correctAnswer()) : null
        );
    }

    public static PageResponse<GameResponse> toResponse(PageResult<Game> pageResult) {
        return SearchResponseMapper.toSearchResponse(pageResult, GameResponseMapper::toResponse);
    }
}

