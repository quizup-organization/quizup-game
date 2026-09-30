# AGENTS.md — quizup-game

> Service de **game** : parties (duels) de quiz, rounds, scoring. **Référence implémentation des
> patterns avancés** : sous-agrégats, sagas, deadlines, event store. Architecture : Axon
> Framework (CQRS/EDA) + JPA (projections).
> Pour les règles de patterns : [
`../../best-practices/.backend/hexagonal-architecture.md`](../../best-practices/.backend/hexagonal-architecture.md).

---

## 1. Rôle

Gestion des **parties** de quiz : création (bot), participation (join), réponse aux questions,
scoring, annulation. Les questions proviennent de `quizup-theme`. Le bot est un utilisateur
spécial (`QuizUpConstants.SYSTEM_USER_ID`).

**Package** : `io.github.quizup.game`

---

## 2. Surface (headless)

Service **headless** : aucun contrôleur REST ni WebSocket. La surface applicative unique est le
**`quizup-bff`** (`/api/**` + `/ws`) ; il interroge ce service via le **query bus** Axon et consomme
ses événements. Les handlers de requête/commande, sagas et projections restent la seule surface
exposée par le service.
## 3. Use cases (ports entrants — `domain/port/in/`)

- `CreateGameUseCase` — création d'une partie
- `JoinGameUseCase` — un joueur rejoint une partie
- `AnswerQuestionUseCase` — réponse à une question d'un round
- `CancelGameUseCase` — annulation d'une partie
- `AbandonGameUseCase` — abandon/forfait d'une partie en cours (`EndGameCommand(forfeitById)`)
- `GetGameUseCase` — récupération par id
- `GetGameEventsUseCase` — lecture des événements (event store)
- `SearchGameUseCase` — recherche paginée (`POST /search`, réservée aux futures surfaces d'administration)

**Queries dédiées aux vues BFF** (`GameQuery.java`) :

- `GetPlayerGamesQuery(playerId, topicId, opponentId, page, size)` → `PlayerGamesPage` : historique
  d'un joueur (filtres optionnels, plus récents d'abord), sans passer par le search.
- `GetPopularTopicsQuery(since, limit)` → `List<TopicPopularity>` : thèmes les plus joués
  (parties créées sur la fenêtre), group by `topic_id`.
- `GetGameEventsQuery(gameId)` → `List<EventEnvelope>` (SDK) : event store exposé avec le payload
  **typé** via `eventType` (codec du query bus distribué). Le mapping vers les notifications web
  (`GameNotification`, annotations Jackson) est fait par le **BFF**, pas par ce service.

---

## 4. Dépendances inter-services

| Port out                 | Service cible  | Query Axon envoyée (QueryGateway)               |
|--------------------------|----------------|-------------------------------------------------|
| `QuestionRepositoryPort` | `quizup-theme` | `QuestionQuery.GetRandomApprovedQuestionsQuery` |

Implémentation : `application/service/QuestionService` (port sortant inter-module, spec §2.7) —
interroge `quizup-theme` via le bus et ne retourne que le type local `GameQuestion`
(`GameQuestionMapper` dans la même couche). Le snapshot `GameQuestion` embarque **toutes les
traductions** (`sourceLanguage` + `translations`) : chaque client choisit sa langue, avec repli sur
la langue source (les parties créées avant l'i18n n'ont qu'un contenu).

**Ports sortants locaux** : `GameRepositoryPort`, `GameEventStorePort`.

### Règles de partie (lot C4)

- **Création** : la partie exige `GameRules.TOTAL_ROUNDS` (7) questions approuvées ; sinon
  `NotEnoughQuestionsProblem` (échec explicite, jamais de partie tronquée).
- **Expiration** : `GameFlowSaga` planifie `GAME_EXPIRED` (24 h) à la création ; une partie jamais
  terminée est annulée avant démarrage ou close avec forfait implicite après démarrage. La
  deadline est annulée à `GameEndedEvent` / `GameRunRecordedEvent` / `GameCancelledEvent`.
- **Badge Éclair côté profile** : 5 réponses < 3 s dans un même duel (voir `quizup-profile`).

---

## 5. Contrats cassés / TODO

Aucun connu à ce jour.

