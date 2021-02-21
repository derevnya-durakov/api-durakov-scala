package dev.durak.service

import dev.durak.exceptions.GameException
import dev.durak.graphql.Constants
import dev.durak.model.external.{ExternalGameState, ExternalPlayer, ExternalRoundPair}
import dev.durak.model.{GameEvent, _}
import dev.durak.repo.ICrudRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

import java.util.UUID
import scala.annotation.tailrec
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

@Service
class GameService(eventPublisher: ApplicationEventPublisher,
                  userService: UserService,
                  gameRepo: ICrudRepository[GameState]) {
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
    startGame(null, userIds)
    true
  }

  def take(auth: Auth, gameId: String): GameState =
    lock synchronized {
      gameRepo.find(UUID.fromString(gameId)) match {
        case None => throw new GameException("Game not found")
        case Some(state) =>
          state.players.find(_.user == auth.user) match {
            case None => throw new GameException("You are not in the game")
            case Some(player) =>
              if (player.user.id != state.defender.user.id)
                throw new GameException("You are not defending and cannot take")
              val allCardsBeaten = state.round.forall(_.defence.isDefined)
              if (allCardsBeaten)
                throw new GameException("You cannot take cards if all cards in round are beaten")
              val updatedState = gameRepo.update(GameState(
                state.id,
                state.seed,
                state.nonce + 1,
                state.deck,
                state.discardPileSize,
                state.players,
                state.round,
                state.attacker,
                state.defender,
                isTaking = true
              ))
              eventPublisher.publishEvent(new GameEvent(Constants.GAME_TAKE, updatedState))
              updatedState
          }
      }
    }

  def sayBeat(auth: Auth, gameId: String): GameState =
    lock synchronized {
      gameRepo.find(UUID.fromString(gameId)) match {
        case None => throw new GameException("Game not found")
        case Some(state) =>
          state.players.find(_.user == auth.user) match {
            case None => throw new GameException("You are not in the game")
            case Some(player) =>
              if (player.user.id == state.defender.user.id)
                throw new GameException("You are defending and cannot say beat")
              if (player.done.isDefined)
                throw new GameException("You are done and cannot say beat")
              if (state.round.isEmpty)
                throw new GameException("No cards in round. You cannot say beat")
              if (player.saidBeat)
                throw new GameException("You already said beat")
              val hasAllSayBeat = state.players
                .filterNot(_ == player)
                .filterNot(_.hand.isEmpty)
                .filterNot(_.user.id == state.defender.user.id)
                .forall(_.saidBeat)
              if (hasAllSayBeat) {
                val cardsInRound = state.round.flatMap { pair =>
                  if (pair.defence.isDefined)
                    pair.attack :: pair.defence.get :: Nil
                  else
                    pair.attack :: Nil
                }
                if (state.isTaking) {
                  val playersTakenRound = state.players.map { p =>
                    if (p == state.defender)
                      Player(p.user, p.hand ::: cardsInRound, p.saidBeat, p.done)
                    else
                      p
                  }
                  val (updatedPlayers, updatedDeck) = dealCards(playersTakenRound, state.deck)
                  val skippingAttackPlayer = findNextPlayerWithCards(state.attacker, updatedPlayers)
                  val newAttacker = findNextPlayerWithCards(skippingAttackPlayer, updatedPlayers)
                  val newDefender = findNextPlayerWithCards(newAttacker, updatedPlayers)
                  val updatedState = gameRepo.update(GameState(
                    state.id,
                    state.seed,
                    state.nonce + 1,
                    updatedDeck,
                    state.discardPileSize,
                    updatedPlayers,
                    Nil,
                    newAttacker,
                    newDefender,
                    isTaking = false
                  ))
                  eventPublisher.publishEvent(new GameEvent(Constants.GAME_TAKEN, updatedState))
                  updatedState
                } else {
                  val (updatedPlayers, updatedDeck) = dealCards(state.players, state.deck)
                  val newAttacker = findNextPlayerWithCards(state.attacker, updatedPlayers)
                  val newDefender = findNextPlayerWithCards(newAttacker, updatedPlayers)
                  val updatedState = gameRepo.update(GameState(
                    state.id,
                    state.seed,
                    state.nonce + 1,
                    updatedDeck,
                    state.discardPileSize + cardsInRound.size,
                    updatedPlayers,
                    Nil,
                    newAttacker,
                    newDefender,
                    isTaking = false
                  ))
                  eventPublisher.publishEvent(new GameEvent(Constants.GAME_BEAT, updatedState))
                  updatedState
                }
              } else {
                val updatedPlayers = state.players.map { p =>
                  if (p == player)
                    Player(p.user, p.hand, saidBeat = true, p.done)
                  else
                    p
                }
                val updatedAttacker = updatedPlayers.find(_.user == state.attacker.user).get
                val updatedState = gameRepo.update(GameState(
                  state.id,
                  state.seed,
                  state.nonce + 1,
                  state.deck,
                  state.discardPileSize,
                  updatedPlayers,
                  state.round,
                  updatedAttacker,
                  state.defender,
                  state.isTaking
                ))
                eventPublisher.publishEvent(new GameEvent(Constants.GAME_BEAT, updatedState))
                updatedState
              }
          }
      }
    }

  def defend(auth: Auth, gameId: String, attackCard: Card, defenceCard: Card): GameState =
    lock synchronized {
      gameRepo.find(UUID.fromString(gameId)) match {
        case None => throw new GameException("Game not found")
        case Some(state) =>
          state.players.find(_.user == auth.user) match {
            case None => throw new GameException("You are not in the game")
            case Some(player) =>
              if (auth.user.id != state.defender.user.id)
                throw new GameException("You are not defending")
              if (state.isTaking)
                throw new GameException("You are taking and cannot defend")
              if (!player.hand.contains(defenceCard))
                throw new GameException("You don't have this card")
              state.round.find(_.attack == attackCard) match {
                case None => throw new GameException("Round has not such card")
                case Some(roundPair) =>
                  roundPair.defence match {
                    case Some(_) => throw new GameException("Card already beaten")
                    case None =>
                      if (!Card.canBeat(attackCard, defenceCard, state.deck.trumpSuit))
                        throw new GameException("You card is weaker than attacking card")
                      val round = state.round.map { pair =>
                        if (pair == roundPair)
                          RoundPair(attackCard, Some(defenceCard))
                        else
                          pair
                      }
                      val defender = state.defender
                      val updatedDefender = Player(
                        defender.user,
                        defender.hand.filterNot(_ == defenceCard),
                        saidBeat = false,
                        done = None
                      )
                      val updatedPlayers = state.players.map { p =>
                        if (p.user == defender.user) updatedDefender else p
                      }
                      val updatedState = gameRepo.update(GameState(
                        state.id,
                        state.seed,
                        state.nonce + 1,
                        state.deck,
                        state.discardPileSize,
                        updatedPlayers,
                        round,
                        state.attacker,
                        updatedDefender,
                        isTaking = false
                      ))
                      eventPublisher.publishEvent(new GameEvent(Constants.GAME_DEFEND, updatedState))
                      updatedState
                  }
              }
          }
      }
    }

  def attack(auth: Auth, gameId: String, card: Card): GameState =
    lock synchronized {
      gameRepo.find(UUID.fromString(gameId)) match {
        case None => throw new GameException("Game not found")
        case Some(state) =>
          state.players.find(_.user == auth.user) match {
            case None => throw new GameException("You are not in the game")
            case Some(player) =>
              if (auth.user.id == state.defender.user.id)
                throw new GameException("You cannot attack. You are defending")
              if (!player.hand.contains(card))
                throw new GameException("You don't have this card")
              if (player.saidBeat)
                throw new GameException("You marked beat")
              if (state.round.isEmpty) {
                if (player.user.id != state.attacker.user.id) {
                  throw new GameException("You are tossing. Wait for first move of attacker")
                }
              } else {
                if (state.discardPileSize == 0) {
                  if (state.round.size >= 5) {
                    throw new GameException("Round already have 5 cards (first round)")
                  }
                } else {
                  if (state.round.size >= 6) {
                    throw new GameException("Round already have 6 cards")
                  }
                  val unbeatenCount = state.round.count(_.defence.isEmpty) + 1
                  if (unbeatenCount > state.defender.hand.size)
                    throw new GameException("Defending player doesn't have enough cards to beat it")
                }
                if (!getRoundRanks(state.round).contains(card.rank)) {
                  throw new GameException("No such card rank in round")
                }
              }
              val round = state.round :+ RoundPair(card, None)
              val updatedPlayer = Player(
                player.user,
                player.hand.filterNot(_ == card),
                saidBeat = false,
                done = None
              )
              val updatedPlayers = state.players.map { p =>
                if (p.user == player.user) updatedPlayer else p
              }
              val updatedAttacker = updatedPlayers.find(_.user == state.attacker.user).get
              val updatedState = gameRepo.update(GameState(
                state.id,
                state.seed,
                state.nonce + 1,
                state.deck,
                state.discardPileSize,
                updatedPlayers,
                round,
                updatedAttacker,
                state.defender,
                state.isTaking
              ))
              eventPublisher.publishEvent(new GameEvent(Constants.GAME_ATTACK, updatedState))
              updatedState
          }
      }
    }

  private def getRoundRanks(round: List[RoundPair]): Set[Rank] = {
    round.flatMap { pair =>
      val cards = pair.attack :: Nil
      if (pair.defence.isDefined)
        pair.defence.get :: cards
      else
        cards
    }.map(_.rank).toSet
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
    val (players, deck) = dealCards(playersWithEmptyHands, sourceDeck)
    val attacker = initialDecideWhoAttacker(players, deck.trumpSuit)
    val defender = findNextPlayerWithCards(attacker, players)
    val gameState = GameState(
      id,
      seed,
      nonce = 1,
      deck,
      discardPileSize = 0,
      players,
      round = Nil,
      attacker,
      defender,
      isTaking = false
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
    if (!players.exists(_.user.id == currentPlayer.user.id))
      throw new GameException("Try to find next player in list of players not containing current")
    if (players.map(_.hand).forall(_.isEmpty))
      throw new GameException("Try to get next player when all players have empty hands")
    val realCurrentPlayer = players.find(_.user.id == currentPlayer.user.id).get
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

  private def dealCards(sourcePlayers: List[Player],
                        sourceDeck: CardDeck): (List[Player], CardDeck) = {
    var doneCounter = sourcePlayers.count(_.done.isDefined)
    var deck = sourceDeck
    val players = for (player <- sourcePlayers) yield {
      val (updatedHand, updatedDeck) = deck.fillHand(player.hand, targetHandSize = 6)
      deck = updatedDeck
      val done = if (player.done.isDefined) {
        player.done
      } else if (updatedHand.isEmpty) {
        doneCounter = doneCounter + 1
        Some(doneCounter)
      } else {
        None
      }
      Player(player.user, updatedHand, saidBeat = false, done)
    }
    (players, deck)
  }

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
    val players = state.players.map(toExternal).asJava
    val round = state.round.map(toExternal).asJava
    ExternalGameState(
      state.id.toString,
      state.nonce,
      state.deck.trumpSuit,
      state.deck.lastTrump.toJava,
      state.deck.deckSize,
      state.discardPileSize,
      hand,
      players,
      round,
      toExternal(state.attacker),
      toExternal(state.defender),
      state.defender.user.id.toString,
      state.isTaking
    )
  }

  def toExternal(roundPair: RoundPair): ExternalRoundPair =
    ExternalRoundPair(roundPair.attack, roundPair.defence.toJava)

  def toExternal(player: Player): ExternalPlayer =
    ExternalPlayer(player.user, player.hand.size, player.saidBeat, player.done.toJava)
}