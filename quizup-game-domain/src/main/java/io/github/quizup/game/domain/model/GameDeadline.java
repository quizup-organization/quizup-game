package io.github.quizup.game.domain.model;

import java.time.Duration;

import static io.github.quizup.game.domain.model.GameRules.GAME_TIMEOUT_HOURS;
import static io.github.quizup.game.domain.model.GameRules.QUESTION_REVEAL_MS;
import static io.github.quizup.game.domain.model.GameRules.ROUND_TIMEOUT_SECONDS;

/**
 * Noms et durées des deadlines d'une partie.
 *
 * <p>Seules les deadlines à durée fixe exposent une constante de durée. Les transitions de phase
 * ({@link #MATCH_INTRO}, {@link #NEXT_ROUND_STARTS}) et la réponse du bot ({@link #BOT_ANSWERS})
 * sont pilotées par les instants absolus portés par les événements (ou par la difficulté du bot) :
 * la saga calcule le délai à l'exécution plutôt que de figer une constante.
 */
public interface GameDeadline {

    String GAME_EXPIRED = "game-expired";
    Duration GAME_EXPIRED_TIMEOUT = Duration.ofHours(GAME_TIMEOUT_HOURS);

    /**
     * Révélation des réponses : la question est affichée seule pendant {@code QUESTION_REVEAL_MS},
     * puis la saga émet la révélation et arme le chrono du round.
     */
    String QUESTION_REVEAL = "question-reveal";
    Duration QUESTION_REVEAL_TIMEOUT = Duration.ofMillis(QUESTION_REVEAL_MS);

    String ROUND_EXPIRED = "round-expired";
    Duration ROUND_EXPIRED_TIMEOUT = Duration.ofSeconds(ROUND_TIMEOUT_SECONDS);

    /** Transition vers le premier round (écran VS + intro) — durée portée par {@code firstRoundAt}. */
    String MATCH_INTRO = "match-intro";

    /** Transition entre deux rounds (révélation + intro) — durée portée par {@code nextRoundAt}. */
    String NEXT_ROUND_STARTS = "next-round-starts";

    /** Réponse du bot — délai aléatoire borné par la difficulté, après révélation. */
    String BOT_ANSWERS = "bot-answers";

    /** Réponse du fantôme — rejouée au temps enregistré du run, après révélation. */
    String GHOST_ANSWERS = "ghost-answers";
}
