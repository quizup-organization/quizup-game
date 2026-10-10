# AGENTS.md — quizup-game

> Service de **game** : parties (duels) de quiz, rounds, scoring. **Référence implémentation des
> patterns avancés** : sous-agrégats, sagas, deadlines, event store. Architecture : Axon
> Framework (CQRS/EDA) + JPA (projections).
> Pour les règles de patterns : [
`../../best-practices/.backend/folder-structure.md`](../../best-practices/.backend/folder-structure.md).

---

## 1. Rôle

Gestion des **parties** de quiz à deux joueurs (humain ou bot) : création, réponse aux questions,
scoring, abandon (`forfeit`), fin (`end`) et annulation. Les **questions** proviennent de
`quizup-theme` mais sont **préparées par l'appelant** (salle, appariement ou façade) et portées par
`CreateGameCommand` : l'agrégat game ne fait **aucun I/O** (voir
[`command-preparation.md`](../../best-practices/.backend/command-preparation.md)). Le bot est un
utilisateur spécial (`QuizUpConstants.SYSTEM_USER_ID`). **La partie démarre immédiatement à la
création** (la présence des deux joueurs est garantie en amont par la salle / l'appariement de
`quizup-matchmaking`) : plus de salle d'attente dans l'agrégat game.

Le `GameCreatedEvent` porte un **snapshot de progression** par joueur (`PlayerProgressSnapshot` :
niveau + XP totale) fourni à la création : l'écran de résultat affiche la progression **à l'instant
de la partie** (le bot reçoit un niveau d'affichage dérivé de sa difficulté).

Ce service ne possède **pas** la revanche : rejouer un adversaire passe par un **défi nominatif**
(`quizup-matchmaking`, `POST /api/challenges`) → salle → partie.

**Package** : `io.github.quizup.game`

---

## 2. Surface (headless)

Service **headless** : aucun contrôleur REST ni WebSocket. La surface applicative unique est le
**`quizup-bff`** (`/api/**` + `/ws`) ; il interroge ce service via le **query bus** Axon et consomme
ses événements. Les handlers de requête/commande, sagas et projections restent la seule surface
exposée par le service.
## 3. Commandes / queries (bus)

Commandes :

- `CreateGameCommand` — création d'une partie (démarre immédiatement) ; porte les `GameQuestion`
  préparées par l'appelant.
- `ForfeitGameCommand` — abandon en cours (`ForfeitGameCommand`) : l'adversaire gagne.
- `AnswerQuestionCommand` — réponse à une question d'un round.
- `CancelGameCommand` — annulation système (expiration) avant démarrage.
- `StartRoundCommand`, `RevealQuestionCommand`, `CloseRoundCommand`, `EndGameCommand` — orchestration
  de rounds par `GameFlowSaga`.

**Queries dédiées aux vues BFF** (`GameQuery.java`) :

- `GetActiveGamesQuery(playerId)` → `List<Game>` : parties en cours (`IN_PROGRESS`, humain ou bot),
  plus récentes d'abord, pour la bannière de reprise ; collection vide si aucune — **jamais de 404**.
- `GetPlayerGamesQuery(playerId, topicId, opponentId, page, size)` → `PlayerGamesPage` : historique
  d'un joueur (filtres optionnels, plus récents d'abord), sans passer par le search.
- `GetPopularTopicsQuery(since, limit)` → `List<TopicPopularity>` : thèmes les plus joués
  (parties créées sur la fenêtre), group by `topic_id`.
- `GetGameEventsQuery(gameId)` → `List<EventEnvelope>` (SDK) : event store exposé avec le payload
  **typé** via `eventType` (codec du query bus distribué). Le mapping vers les notifications web
  (`GameNotification`, annotations Jackson) est fait par le **BFF**, pas par ce service.
- `GetGameResultQuery(gameId, playerId)` → `GameResult` : composition de fin de duel pour l'écran de
  résultat (scores, vainqueur, `botGame`, `botDifficulty`, sujet/adversaire, `basePoints`,
  `speedBonus`, `correctAnswers`, `fastAnswers`, `answeredRounds`/`totalRounds`) — les règles de
  scoring restent dans le domaine (`GameRules`).

---

## 4. Dépendances inter-services

**Aucune** : l'agrégat game ne dépend plus de `quizup-theme` (la sélection des questions est faite
par la salle/l'appariement/façade, qui transmettent les `GameQuestion` dans la commande). Le
mapping `theme.Question → game.GameQuestion` est dupliqué chez les émetteurs (un `*-domain` ne
dépend jamais d'un autre service). `CreateGameCommand` ne porte plus `languages` (seules les
questions importent) et l'absence de questions suffisantes échoue en `NotEnoughQuestionsProblem`.
Le snapshot `GameQuestion` embarque **tous les contenus localisés** (`translations`) : chaque
client choisit sa langue, avec repli déterministe FR → EN → premier contenu.

**Ports sortants locaux** : `GameRepositoryPort`, `GameEventStorePort`.

### Règles de partie (lot C4)

- **Création** : la partie exige `GameRules.TOTAL_ROUNDS` (7) questions approuvées ; sinon
  `NotEnoughQuestionsProblem` (échec explicite, jamais de partie tronquée).
- **Expiration** : `GameFlowSaga` planifie `GAME_EXPIRED` (24 h) à la création ; une partie jamais
  terminée est close au score. La deadline est annulée à `GameEndedEvent` / `GameCancelledEvent`.
- **Badge Éclair côté profile** : 5 réponses < 3 s dans un même duel (voir `quizup-profile`).

---

## 5. Contrats cassés / TODO

Aucun connu à ce jour.

