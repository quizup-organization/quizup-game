package io.github.quizup.game.domain.model;

/**
 * Popularité d'un thème : nombre de parties créées sur une fenêtre glissante.
 */
public record TopicPopularity(String topicId, long games) {
}
