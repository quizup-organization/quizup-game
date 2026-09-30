package io.github.quizup.game.domain.model;

import io.github.quizup.microservice.core.domain.model.i18n.Language;

import java.util.Map;

/**
 * Snapshot immuable multilingue d'une question, embarqué dans les events pour autonomie
 * event-sourcing. {@link #translations()} contient la langue source et ses traductions ;
 * le client choisit la sienne, avec repli sur la langue source.
 */
public record GameQuestion(
        String questionId,
        Language sourceLanguage,
        Map<Language, GameQuestionContent> translations,
        String imageUrl,
        String difficulty,
        GameQuestionChoice correctAnswer
) {

    /** Contenu dans la langue demandée, avec repli sur la langue source. */
    public GameQuestionContent content(Language language) {
        GameQuestionContent content = translations.get(language);
        return content != null ? content : translations.get(sourceLanguage);
    }

    /** Texte source (compatibilité lecture). */
    public String text() {
        return content(sourceLanguage).text();
    }

    /** Réponses source (compatibilité lecture). */
    public Map<GameQuestionChoice, String> answers() {
        return content(sourceLanguage).answers();
    }
}
