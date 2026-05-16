package io.github.quizup.game.domain.exception;

import io.github.quizup.common.domain.exception.BaseProblem;
import io.github.quizup.common.domain.exception.ProblemCategory;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Classe de base pour toutes les exceptions métier liées au domaine Game
 */
@Getter
public abstract class GameProblem extends BaseProblem {

    private final String gameId;

    protected GameProblem(
            String gameId,
            String type,
            ProblemCategory category,
            String title,
            String detail,
            Map<String, Object> context) {
        super(
                type,
                category,
                title,
                detail,
                mergeContext(context, gameId)
        );
        this.gameId = gameId;
    }

    protected GameProblem(
            String gameId,
            String type,
            String title,
            String detail,
            Map<String, Object> context) {
        this(
                gameId,
                type,
                ProblemCategory.BUSINESS_INVALID_COMMAND,
                title,
                detail,
                context
        );
    }

    protected GameProblem(
            String gameId,
            String type,
            String title,
            String detail) {
        this(
                gameId,
                type,
                ProblemCategory.BUSINESS_INVALID_COMMAND,
                title,
                detail,
                null
        );
    }

    protected GameProblem(
            String gameId,
            String type,
            String title) {
        this(
                gameId,
                type,
                title,
                null,
                null
        );
    }

    private static Map<String, Object> mergeContext(Map<String, Object> context, String gameId) {
        Map<String, Object> merged = new HashMap<>();
        if (context != null) {
            merged.putAll(context);
        }
        merged.put("gameId", gameId);
        return merged;
    }

}
