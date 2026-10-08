# AGENTS.md — quizup-game

> Service de **game** : parties (duels) de quiz, rounds, scoring. **Référence implémentation des
> patterns avancés** : sous-agrégats, sagas, deadlines, event store. Architecture : Axon
> Framework (CQRS/EDA) + JPA (projections).
> Pour les règles de patterns : [
`../../best-practices/.backend/folder-structure.md`](../../best-practices/.backend/folder-structure.md).

---

## 1. Rôle

Gestion des **parties** de quiz à deux joueurs (humain ou bot) : création, présence dans la salle
d'attente (`join`/`leave`), réponse aux questions, scoring, abandon (`forfeit`), fin (`end`) et
annulation. Les questions proviennent de `quizup-theme`. Le bot est un utilisateur spécial
(`QuizUpConstants.SYSTEM_USER_ID`). Plus de mode asynchrone/ghost : la partie ne démarre qu'une
fois les deux joueurs présents.

La **présence** est également suivie sur l'**écran de résultat** : `GameEndedEvent` remet les deux
joueurs absents, `join`/`leave` restent acceptés en `FINISHED` (présence seule, sans rejouer le
statut), et une déconnexion (`PlayerWentOfflineEvent`) sort le joueur de la dernière partie terminée.
Ce socle alimente la **revanche** (`RequestRematch/AcceptRematch/DeclineRematch/CancelRematch`) :
demande possible seulement si les deux joueurs sont présents et humains ; l'acceptation crée une
nouvelle partie via `RematchSaga` (mêmes joueurs/topic/langues), avec deadline d'expiration de 60 s.

**Package** : `io.github.quizup.game`

---

## 2. Surface (headless)

Service **headless** : aucun contrôleur REST ni WebSocket. La surface applicative unique est le
**`quizup-bff`** (`/api/**` + `/ws`) ; il interroge ce service via le **query bus** Axon et consomme
ses événements. Les handlers de requête/commande, sagas et projections restent la seule surface
exposée par le service.
## 3. Use cases (ports entrants — `domain/port/in/`)

- `CreateGameUseCase` — création d'une partie
- `JoinGameUseCase` — entrée dans la salle d'attente (idempotent)
- `LeaveGameUseCase` — sortie de la salle d'attente avant démarrage (annule la partie)
- `ForfeitGameUseCase` — abandon en cours (`ForfeitGameCommand`) : l'adversaire gagne
- `AnswerQuestionUseCase` — réponse à une question d'un round
- `CancelGameUseCase` — annulation système (expiration) avant démarrage
- `GetGameUseCase` — récupération par id
- `GetGameEventsUseCase` — lecture des événements (event store)
- `SearchGameUseCase` — recherche paginée (`POST /search`, réservée aux futures surfaces d'administration)

**Queries dédiées aux vues BFF** (`GameQuery.java`) :

- `GetCurrentGameQuery(playerId)` → `Game` : partie en attente/en cours la plus récente
  (`CREATED/READY/IN_PROGRESS`) pour la bannière de reprise ; absence ⇒ `NoCurrentGameProblem` (404).
- `GetPlayerGamesQuery(playerId, topicId, opponentId, page, size)` → `PlayerGamesPage` : historique
  d'un joueur (filtres optionnels, plus récents d'abord), sans passer par le search.
- `GetPopularTopicsQuery(since, limit)` → `List<TopicPopularity>` : thèmes les plus joués
  (parties créées sur la fenêtre), group by `topic_id`.
- `GetGameEventsQuery(gameId)` → `List<EventEnvelope>` (SDK) : event store exposé avec le payload
  **typé** via `eventType` (codec du query bus distribué). Le mapping vers les notifications web
  (`GameNotification`, annotations Jackson) est fait par le **BFF**, pas par ce service.
- `GetGameResultQuery(gameId, playerId)` → `GameResult` : composition de fin de duel pour l'écran de
  résultat (scores, vainqueur, `botGame`, `basePoints`, `speedBonus`, `correctAnswers`, `fastAnswers`,
  `answeredRounds`/`totalRounds`) — les règles de scoring restent dans le domaine (`GameRules`).

---

## 4. Dépendances inter-services

| Port out                 | Service cible  | Query Axon envoyée (QueryGateway)               |
|--------------------------|----------------|-------------------------------------------------|
| `QuestionRepositoryPort` | `quizup-theme` | `QuestionQuery.GetRandomApprovedQuestionsQuery` |

Implémentation : `application/service/QuestionService` (port sortant inter-module, spec §2.7) —
interroge `quizup-theme` via le bus et ne retourne que le type local `GameQuestion`
(`GameQuestionMapper` dans la même couche). `CreateGameCommand` porte les **langues requises**
(union des langues des joueurs, résolues côté serveur) : la sélection est **stricte** (questions
disponibles dans toutes ces langues) et la partie échoue en `NotEnoughQuestionsProblem` sinon. Le
snapshot `GameQuestion` embarque **tous les contenus localisés** (`translations`) : chaque client
choisit sa langue, avec repli déterministe FR → EN → premier contenu.

**Ports sortants locaux** : `GameRepositoryPort`, `GameEventStorePort`.

### Règles de partie (lot C4)

- **Création** : la partie exige `GameRules.TOTAL_ROUNDS` (7) questions approuvées ; sinon
  `NotEnoughQuestionsProblem` (échec explicite, jamais de partie tronquée).
- **Expiration** : `GameFlowSaga` planifie `GAME_EXPIRED` (24 h) à la création ; une partie jamais
  terminée est annulée avant démarrage ou close au score après démarrage. La deadline est annulée à
  `GameEndedEvent` / `GameCancelledEvent`.
- **Garde-fou de démarrage** : `START_TIMEOUT` (90 s) annule une partie jamais démarrée
  (`NO_SHOW_START`) — plus de salle fantôme en attendant l'expiration de 24 h. Annulé à
  `GameStartedEvent`.
- **Badge Éclair côté profile** : 5 réponses < 3 s dans un même duel (voir `quizup-profile`).
- **Revanche** : `REMATCH_EXPIRY` (60 s) — sans acceptation, la demande est annulée
  (`RematchCancelledEvent("EXPIRED")`) ; un départ de l'écran de résultat annule aussi la demande
  (`PLAYER_LEFT`). `RematchSaga` crée la partie à l'acceptation puis `ConfirmRematchCommand`
  publie `RematchStartedEvent(newGameId)` (échec de création ⇒ `CREATE_FAILED`).

---

## 5. Contrats cassés / TODO

Aucun connu à ce jour.

