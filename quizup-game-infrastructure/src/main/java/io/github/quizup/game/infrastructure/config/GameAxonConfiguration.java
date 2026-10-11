package io.github.quizup.game.infrastructure.config;

import org.axonframework.common.caching.Cache;
import org.axonframework.common.caching.WeakReferenceCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cache d'agrégats Axon ({@code @Aggregate(cache = "gameAggregateCache")} sur {@code GameAggregate}).
 * <p>
 * Une partie dure 7 rounds (~100+ événements) et chaque commande/deadline relisait l'agrégat :
 * sans cache, l'event store encaissait un replay complet par commande. Sous charge (test 1000
 * parties), c'est le principal coût Postgres du service. Le cache est local à l'instance
 * (le routage distribué garantit qu'une partie est toujours servie par le même nœud) ;
 * les références sont faibles : éviction GC sous pression mémoire, rechargement transparent.
 */
@Configuration
public class GameAxonConfiguration {

    @Bean
    public Cache gameAggregateCache() {
        return new WeakReferenceCache();
    }
}
