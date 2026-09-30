package io.github.quizup.game.domain.model;

import io.github.quizup.microservice.core.domain.model.i18n.Language;

import java.util.Map;
import java.util.Set;

/**
 * Snapshot immuable multilingue d'une question, embarqué dans les events pour autonomie
 * event-sourcing. Le repli de lecture est déterministe : langue demandée, puis français,
 * puis anglais, puis premier contenu disponible.
 */
public record GameQuestion(
        String questionId,
        Map<Language, GameQuestionContent> translations,
        String imageUrl,
        String difficulty,
        GameQuestionChoice correctAnswer
) {

    /** Langues disponibles pour cette question. */
    public Set<Language> availableLanguages() {
        return Set.copyOf(translations.keySet());
    }

    /** Contenu dans la langue demandée, avec repli déterministe (FR, puis EN, puis premier). */
    public GameQuestionContent content(Language language) {
        GameQuestionContent content = translations.get(language);
        if (content != null) {
            return content;
        }
        content = translations.get(Language.FR);
        if (content != null) {
            return content;
        }
        content = translations.get(Language.EN);
        if (content != null) {
            return content;
        }
        return translations.values().iterator().next();
    }

    /** Texte de repli (FR prioritaire), utilisé par le contrat BFF. */
    public String text() {
        return content(Language.FR).text();
    }

    /** Réponses de repli (FR prioritaire), utilisées par le contrat BFF. */
    public Map<GameQuestionChoice, String> answers() {
        return content(Language.FR).answers();
    }
}
