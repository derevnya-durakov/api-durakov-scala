package dev.durak.service

import dev.durak.exceptions.GameException
import dev.durak.graphql.Constants
import dev.durak.model.external.{ExternalGameState, ExternalPlayer, ExternalRoundPair}
import dev.durak.model.{GameEvent, _}
import dev.durak.repo.ICrudRepository
import dev.durak.util.GameCheckUtils
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

import java.util.UUID
import scala.annotation.tailrec
import scala.collection.mutable
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

@Service
class GameService(
  eventPublisher: ApplicationEventPublisher,
  userService: UserService,
  gameRepo: ICrudRepository[GameState]
) {
  private val lock = new Object // todo: separate locks for each game
  // just for testing
  startGame(null, userService.users.slice(0, 3).map(_.id.toString).toList)

  def getGameState(auth: Auth, id: String): Option[GameState] =
    gameRepo.find(UUID.fromString(id))

  def testRestartGame(): Boolean = {
    val userIds = getGameState(null, GameService.TestGameId)
      .get
      .players
      .map(_.user.id.toString)
    startGame(auth = null, userIds)
    true
  }

  private def withGame[T](gameId: String)(action: GameState => T): T =
    action(gameRepo
      .find(UUID.fromString(gameId))
      .getOrElse(throw new GameException("Game not found")))

  private def withMe[T](auth: Auth, game: GameState)(action: Player => T): T =
    action(game
      .players
      .find(_.user.id == auth.user.id)
      .getOrElse(throw new GameException("You are not in the game")))

  private def withGameAndMe[T](
    auth: Auth,
    gameId: String
  )(action: (GameState, Player) => T): T = {
    withGame(gameId) { game =>
      withMe(auth, game) { me =>
        action(game, me)
      }
    }
  }

  def nextGame(auth: Auth, gameId: String): GameState =
    lock synchronized {
      withGameAndMe(auth, gameId) { (game, _) =>
        GameCheckUtils.iCanStartNextGame(game)
        val playersWithEmptyHands = game.players
          .map(p => p.copy(hand = Nil, saidBeat = false, done = None))
        val (players, deck) = dealCards(playersWithEmptyHands, CardDeck(game.seed), game)
        val defender = findActualPlayer(game.durak.get, players)
        val attacker = players
          .find(p => GameCheckUtils.playersEqual(findNextPlayerWithCards(p, players), defender))
          .get
        val updatedState = gameRepo.update(
          game.copy(
            nonce = game.nonce + 1,
            deck = deck,
            discardPileSize = 0,
            players = players,
            round = Nil,
            attacker = attacker,
            defender = defender,
            isTaking = false,
            durak = None
          )
        )
        eventPublisher.publishEvent(new GameEvent(Constants.GAME_NEXT, updatedState))
        updatedState
      }
    }

  def take(auth: Auth, gameId: String): GameState =
    lock synchronized {
      withGameAndMe(auth, gameId) { (game, me) =>
        GameCheckUtils.iCanTake(me, game)
        val updatedState = internalTakeAction(game)
        eventPublisher.publishEvent(new GameEvent(Constants.GAME_TAKE, updatedState))
        updatedState
      }
    }

  private def internalTakeAction(game: GameState): GameState = {
    val updatedPlayers = game.players.map { p =>
      if (p.hand.isEmpty || p.done.isDefined) {
        p.copy(saidBeat = true)
      } else {
        p
      }
    }
    val updatedAttacker = findActualPlayer(game.attacker, updatedPlayers)
    gameRepo.update(
      game.copy(
        nonce = game.nonce + 1,
        players = updatedPlayers,
        attacker = updatedAttacker,
        isTaking = true,
      )
    )
  }

  def sayBeat(auth: Auth, gameId: String): GameState =
    lock synchronized {
      withGameAndMe(auth, gameId) { (game, me) =>
        GameCheckUtils.iCanSayBeat(me, game)
        internalSayBeatAction(me, game)
      }
    }

  private def internalSayBeatAction(me: Player, game: GameState): GameState = {
    val hasAllOthersSaidBeat = game.players
      .filterNot(_ == me)
      .filterNot(_.hand.isEmpty)
      .filterNot(_.user.id == game.defender.user.id)
      .forall(_.saidBeat)
    if (hasAllOthersSaidBeat)
      internalLastSayBeatAction(game)
    else
      internalNotLastSayBeatAction(me, game)
  }

  private def internalLastSayBeatAction(game: GameState): GameState = {
    val cardsInRound = game.round.flatMap { pair =>
      if (pair.defence.isDefined)
        pair.attack :: pair.defence.get :: Nil
      else
        pair.attack :: Nil
    }
    if (game.isTaking) {
      val playersTakenRound = game.players.map { p =>
        if (p == game.defender) p.copy(hand = p.hand ::: cardsInRound) else p
      }
      val (updatedPlayers, updatedDeck) = dealCards(playersTakenRound, game.deck, game)
      val skippingAttackPlayer = findNextPlayerWithCards(game.attacker, updatedPlayers)
      val newAttacker = findNextPlayerWithCards(skippingAttackPlayer, updatedPlayers)
      val newDefender = findNextPlayerWithCards(newAttacker, updatedPlayers)
      val updatedState = gameRepo.update(
        game.copy(
          nonce = game.nonce + 1,
          deck = updatedDeck,
          players = updatedPlayers,
          round = Nil,
          attacker = newAttacker,
          defender = newDefender,
          isTaking = false,
        )
      )
      eventPublisher.publishEvent(new GameEvent(Constants.GAME_TAKEN, updatedState))
      updatedState
    } else {
      val (updatedPlayers, updatedDeck) = dealCards(game.players, game.deck, game)
      val newAttacker = findNextPlayerWithCards(game.attacker, updatedPlayers)
      val newDefender = findNextPlayerWithCards(newAttacker, updatedPlayers)
      val updatedState = gameRepo.update(
        game.copy(
          nonce = game.nonce + 1,
          deck = updatedDeck,
          discardPileSize = game.discardPileSize + cardsInRound.size,
          players = updatedPlayers,
          round = Nil,
          attacker = newAttacker,
          defender = newDefender,
          isTaking = false,
        )
      )
      eventPublisher.publishEvent(new GameEvent(Constants.GAME_BEAT, updatedState))
      updatedState
    }
  }

  private def internalNotLastSayBeatAction(me: Player, game: GameState): GameState = {
    val updatedPlayers = game.players.map { p =>
      if (p == me) p.copy(saidBeat = true) else p
    }
    val updatedAttacker = findActualPlayer(game.attacker, updatedPlayers)
    val updatedState = gameRepo.update(
      game.copy(
        nonce = game.nonce + 1,
        players = updatedPlayers,
        attacker = updatedAttacker,
      )
    )
    eventPublisher.publishEvent(new GameEvent(Constants.GAME_BEAT, updatedState))
    updatedState
  }

  def defend(auth: Auth, gameId: String, attackCard: Card, defenceCard: Card): GameState =
    lock synchronized {
      withGameAndMe(auth, gameId) { (game, me) =>
        GameCheckUtils.iCanDefend(me, game, attackCard, defenceCard)
        internalDefendAction(attackCard, defenceCard, game)
      }
    }

  private def internalDefendAction(
    attackCard: Card,
    defenceCard: Card,
    game: GameState
  ): GameState = {
    val round = game.round.map { pair =>
      if (pair.attack == attackCard)
        RoundPair(attackCard, Some(defenceCard))
      else
        pair
    }
    // todo make some common logic for defend and attack
    val defender = game.defender
    val myHand = defender.hand.filterNot(_ == defenceCard)
    val done = if (myHand.isEmpty && game.deck.isEmpty) {
      Some(game.players.count(_.done.isDefined) + 1)
    } else {
      None
    }
    val updatedDefender = defender.copy(
      hand = myHand,
      saidBeat = false,
      done = done
    )
    val rankAlreadyWasInRound = GameCheckUtils.getRoundRanks(game.round).contains(defenceCard.rank)
    val updatedPlayers = game.players.map { p =>
      if (p.user == defender.user) {
        updatedDefender
      } else {
        p.copy(
          saidBeat = if (p.saidBeat) rankAlreadyWasInRound else false
        )
      }
    }
    var updatedGame = gameRepo.update(
      game.copy(
        nonce = game.nonce + 1,
        players = updatedPlayers,
        round = round,
        defender = updatedDefender,
        isTaking = false
      )
    )
    eventPublisher.publishEvent(new GameEvent(Constants.GAME_DEFEND, updatedGame))
    val allAttackersDone = updatedPlayers
      .filterNot(GameCheckUtils.playersEqual(_, game.attacker))
      .forall(_.done.isDefined)
    if (allAttackersDone) {
      updatedGame = gameRepo.update(
        updatedGame.copy(durak = Some(updatedGame.attacker))
      )
      eventPublisher.publishEvent(new GameEvent(Constants.GAME_END, updatedGame))
    } else if (myHand.isEmpty) {
      updatedPlayers
        .filterNot(GameCheckUtils.playersEqual(_, game.defender))
        .filterNot(_.saidBeat)
        .foreach(player => updatedGame = internalSayBeatAction(player, updatedGame))
    }
    updatedGame
  }

  def attack(auth: Auth, gameId: String, card: Card): GameState =
    lock synchronized {
      withGameAndMe(auth, gameId) { (game, me) =>
        GameCheckUtils.iCanAttack(me, game, card)
        internalAttackAction(me, card, game)
      }
    }

  private def internalAttackAction(me: Player, card: Card, game: GameState): GameState = {
    val round = game.round :+ RoundPair(card, None)
    val myHand = me.hand.filterNot(_ == card);
    val done = if (myHand.isEmpty && game.deck.isEmpty) {
      Some(game.players.count(_.done.isDefined) + 1)
    } else {
      None
    }
    val updatedMe = me.copy(
      hand = myHand,
      saidBeat = false,
      done = done
    )
    val updatedPlayers = game.players.map { p =>
      if (p.user == me.user) updatedMe else p
    }
    val allAttackersDone = updatedPlayers
      .filterNot(GameCheckUtils.playersEqual(_, game.defender))
      .forall(_.done.isDefined)
    val updatedAttacker = findActualPlayer(game.attacker, updatedPlayers)
    val (durak, event) = if (allAttackersDone) {
      (Some(game.defender), Constants.GAME_END)
    } else {
      (None, Constants.GAME_ATTACK)
    }
    val updatedState = gameRepo.update(
      game.copy(
        nonce = game.nonce + 1,
        players = updatedPlayers,
        round = round,
        attacker = updatedAttacker,
        durak = durak
      )
    )
    eventPublisher.publishEvent(new GameEvent(event, updatedState))
    updatedState
  }

  def startGame(auth: Auth, userIds: List[String]): GameState = {
    if (userIds.size < 2 || userIds.size > 6) {
      throw new GameException("Players size must be 2 <= x <= 6")
    }
    val id = UUID.fromString(GameService.TestGameId) // hardcoded for tests
    val seed = 123 // hardcoded for tests
    val playersWithEmptyHands = loadUsers(userIds)
      .map(Player(_, Nil, saidBeat = false, done = None))
    val sourceDeck = CardDeck(seed)
    val (players, deck) = initialDealCards(playersWithEmptyHands, sourceDeck)
    val attacker = initialDecideWhoAttacker(players, deck.trumpSuit)
    val defender = findNextPlayerWithCards(attacker, players)
    val gameState = GameState(
      id = id,
      seed = seed,
      deck = deck,
      players = players,
      attacker = attacker,
      defender = defender
    )
    eventPublisher.publishEvent(new GameEvent(Constants.GAME_CREATED, gameState))
    // for testing
    if (gameRepo.exists(id))
      gameRepo.update(gameState)
    else
      gameRepo.create(gameState)
  }

  @tailrec
  private def findNextPlayerWithCards(currentPlayer: Player, players: List[Player]): Player = {
    if (players.map(_.hand).forall(_.isEmpty))
      throw new GameException("Try to get next player when all players have empty hands")
    val realCurrentPlayer = findActualPlayer(currentPlayer, players)
    val nextPlayer = players(findNextCircleIndex(realCurrentPlayer, players))
    if (nextPlayer.hand.nonEmpty)
      nextPlayer
    else
      findNextPlayerWithCards(nextPlayer, players)
  }

  private def findNextCircleIndex[T](currentElem: T, elements: List[T]): Int = {
    val nextIndex = elements.indexOf(currentElem) + 1
    if (nextIndex < elements.size) nextIndex else 0
  }

  private def findMinRankInHand(cards: List[Card], suit: Suit): Option[Rank] =
    cards
      .filter(_.suit == suit)
      .map(_.rank)
      .minOption

  private def initialDecideWhoAttacker(players: List[Player], trumpSuit: Suit): Player =
    players.map { player =>
      player.user -> findMinRankInHand(player.hand, trumpSuit)
    }
      .filter(_._2.isDefined)
      .minByOption(_._2.get)
      .map(_._1)
      .flatMap(user => players.find(_.user == user))
      .getOrElse(players.head)

  private def initialDealCards(
    sourcePlayers: List[Player],
    sourceDeck: CardDeck
  ): (List[Player], CardDeck) = {
    var deck = sourceDeck
    val players = for (player <- sourcePlayers) yield {
      val (updatedHand, updatedDeck) = deck.fillHand(player.hand, targetHandSize = 6)
      deck = updatedDeck
      player.copy(hand = updatedHand, saidBeat = false)
    }
    (players, deck)
  }

  private def dealCards(
    sourcePlayers: List[Player],
    sourceDeck: CardDeck,
    game: GameState
  ): (List[Player], CardDeck) = {
    val playersMap = mutable.Map(sourcePlayers.map(p => (p.user, p)): _*)
    var deck = sourceDeck
    val attacker = findActualPlayer(game.attacker, sourcePlayers)
    val defender = findActualPlayer(game.defender, sourcePlayers)
    val restPlayers = sourcePlayers
      .filterNot(GameCheckUtils.playersEqual(_, game.attacker))
      .filterNot(GameCheckUtils.playersEqual(_, game.defender))
    ((attacker :: restPlayers) :+ defender) foreach { player =>
      val (updatedHand, updatedDeck) = deck.fillHand(player.hand)
      deck = updatedDeck
      playersMap.put(player.user, player.copy(hand = updatedHand, saidBeat = false))
    }
    var players: List[Player] = Nil
    sourcePlayers.map(_.user).foreach(user => players = players :+ playersMap(user))
    (players, deck)
  }

  private def findActualPlayer(nonActualPlayer: Player, actualPlayers: List[Player]): Player =
    actualPlayers
      .find(GameCheckUtils.playersEqual(_, nonActualPlayer))
      .getOrElse(throw new GameException("Not found actual player in list"))

  private def loadUsers(userIds: List[String]): List[User] =
    userIds
      .map { id =>
        userService.findUser(id)
          .getOrElse(throw new GameException(s"User $id not found"))
      }
}

object GameService {
  val TestGameId = "0c52f37c-399c-4304-9d39-34d08b3ae1ba"

  def toExternal(state: GameState, user: User): ExternalGameState = {
    val hand = state.players.find(_.user == user).map(_.hand)
      .getOrElse(throw new GameException("User is not player of the game")).asJava
    ExternalGameState(
      state.id.toString,
      state.nonce,
      state.deck.trumpSuit,
      state.deck.lastTrump.toJava,
      state.deck.deckSize,
      state.discardPileSize,
      hand,
      state.players.map(toExternal).asJava,
      state.round.map(toExternal).asJava,
      toExternal(state.attacker),
      toExternal(state.defender),
      state.isTaking,
      state.durak.map(toExternal).toJava
    )
  }

  def toExternal(roundPair: RoundPair): ExternalRoundPair =
    ExternalRoundPair(roundPair.attack, roundPair.defence.toJava)

  def toExternal(player: Player): ExternalPlayer =
    ExternalPlayer(player.user, player.hand.size, player.saidBeat, player.done.toJava)
}