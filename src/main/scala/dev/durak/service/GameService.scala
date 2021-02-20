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
import scala.collection.mutable

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
              if (player.user.id != state.defendingId)
                throw new GameException("You are not defending and cannot take")
              val allCardsBeaten = state.round.forall(_.defence.isDefined)
              if (!allCardsBeaten)
                throw new GameException("You cannot take cards if all cards in round are beaten")
              val updatedState = gameRepo.update(GameState(
                state.id,
                state.seed,
                state.nonce + 1,
                state.deck,
                state.discardPileSize,
                state.players,
                state.round,
                state.defendingId,
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
              if (player.user.id == state.defendingId)
                throw new GameException("You are defending and cannot say beat")
              if (state.round.isEmpty)
                throw new GameException("No cards in round. You cannot say beat")
              if (player.saidBeat)
                throw new GameException("You already said beat")
              val hasAllSayBeat = state.players
                .filterNot(_ == player)
                .filterNot(_.user.id == state.defendingId)
                .forall(_.saidBeat)
              if (hasAllSayBeat) {
                val cardsInRound = state.round.flatMap { pair =>
                  if (pair.defence.isDefined)
                    pair.attack :: pair.defence.get :: Nil
                  else
                    pair.attack :: Nil
                }
                if (state.isTaking) {
                  val defender = getDefender(state)
                  val playersTakenRound = state.players.map { p =>
                    if (p == defender)
                      Player(p.user, p.hand ::: cardsInRound, p.saidBeat)
                    else
                      p
                  }
                  val (updatedPlayers, updatedDeck) = dealCards(playersTakenRound, state.deck)
                  val newDefender = findNextPlayer(defender, updatedPlayers)
                  val updatedState = gameRepo.update(GameState(
                    state.id,
                    state.seed,
                    state.nonce + 1,
                    updatedDeck,
                    state.discardPileSize,
                    updatedPlayers,
                    Nil,
                    newDefender.user.id,
                    isTaking = false
                  ))
                  eventPublisher.publishEvent(new GameEvent(Constants.GAME_TAKEN, updatedState))
                  updatedState
                } else {
                  val (updatedPlayers, updatedDeck) = dealCards(state.players, state.deck)
                  val currentDefender = getDefender(state)
                  val newDefender = findNextPlayer(currentDefender, updatedPlayers)
                  val updatedState = gameRepo.update(GameState(
                    state.id,
                    state.seed,
                    state.nonce + 1,
                    updatedDeck,
                    state.discardPileSize + cardsInRound.size,
                    updatedPlayers,
                    Nil,
                    newDefender.user.id,
                    isTaking = false
                  ))
                  eventPublisher.publishEvent(new GameEvent(Constants.GAME_BEAT, updatedState))
                  updatedState
                }
              } else {
                val updatedPlayers = state.players.map { p =>
                  if (p == player)
                    Player(p.user, p.hand, saidBeat = true)
                  else
                    p
                }
                val updatedState = gameRepo.update(GameState(
                  state.id,
                  state.seed,
                  state.nonce + 1,
                  state.deck,
                  state.discardPileSize,
                  updatedPlayers,
                  state.round,
                  state.defendingId,
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
              if (auth.user.id != state.defendingId)
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
                      val updatedPlayers = state.players
                        .map(p => Player(p.user, p.hand.filterNot(_ == defenceCard), saidBeat = false))
                      val updatedState = gameRepo.update(GameState(
                        state.id,
                        state.seed,
                        state.nonce + 1,
                        state.deck,
                        state.discardPileSize,
                        updatedPlayers,
                        round,
                        state.defendingId,
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
              if (auth.user.id == state.defendingId)
                throw new GameException("You cannot attack. You are defending")
              if (!player.hand.contains(card))
                throw new GameException("You don't have this card")
              if (player.saidBeat)
                throw new GameException("You marked beat")
              val attacker = getAttacker(state)
              if (player.user.id != attacker.user.id) {
                if (state.round.isEmpty) {
                  throw new GameException("You are tossing. Wait for first move of attacker")
                }
                if (!attacker.saidBeat) {
                  throw new GameException("Wait while the attacker will say beat")
                }
              }
              if (state.round.isEmpty) {
                if (player.user.id != attacker.user.id) {
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
                  val defender = getDefender(state)
                  val unbeatenCount = state.round.count(_.defence.isEmpty) + 1
                  if (unbeatenCount > defender.hand.size)
                    throw new GameException("Defending player doesn't have enough cards to beat it")
                }
                if (!getAvailableCardRanks(state.round).contains(card.rank)) {
                  throw new GameException("No such card rank in round")
                }
              }
              val round = RoundPair(card, None) :: state.round
              val updatedPlayers = state.players
                .map(p => Player(p.user, p.hand.filterNot(_ == card), saidBeat = false))
              val updatedState = gameRepo.update(GameState(
                state.id,
                state.seed,
                state.nonce + 1,
                state.deck,
                state.discardPileSize,
                updatedPlayers,
                round,
                state.defendingId,
                state.isTaking
              ))
              eventPublisher.publishEvent(new GameEvent(Constants.GAME_ATTACK, updatedState))
              updatedState
          }
      }
    }

  private def getAttacker(state: GameState): Player =
    state.players
      .find(findNextPlayer(_, state.players).user.id == state.defendingId)
      .getOrElse(throw new GameException("Attacking player not found in state"))

  private def getDefender(state: GameState): Player =
    state.players
      .find(_.user.id == state.defendingId)
      .getOrElse(throw new GameException("Defending player not found in state"))

  private def getAvailableCardRanks(round: List[RoundPair]): Set[Rank] = {
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
    val playersWithEmptyHands = loadUsers(userIds).map(Player(_, Nil, saidBeat = false))
    val sourceDeck = CardDeck(seed)
    val (players, deck) = dealCards(playersWithEmptyHands, sourceDeck)
    val defendingId = findNextPlayer(initialIdentifyAttacker(players, deck.trumpSuit), players).user.id
    val gameState = GameState(
      id,
      seed,
      nonce = 1,
      deck,
      discardPileSize = 0,
      players,
      round = Nil,
      defendingId,
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
  private def findNextPlayer(currentPlayer: Player,
                             players: List[Player]): Player = {
    if (!players.exists(_.user.id == currentPlayer.user.id))
      throw new GameException("Try to find next player in list of players not containing current")
    if (players.map(_.hand).forall(_.isEmpty))
      throw new GameException("Try to get next player when all players have empty hands")
    val nextPlayer = players(findNextCircleIndex(currentPlayer, players))
    if (nextPlayer.hand.nonEmpty)
      nextPlayer
    else
      findNextPlayer(nextPlayer, players)
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

  private def initialIdentifyAttacker(players: List[Player], trumpSuit: Suit): Player =
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
    val playersMap = mutable.Map[User, List[Card]](sourcePlayers.map(p => (p.user, p.hand)): _*)
    var deck = sourceDeck
    for (user <- playersMap.keySet) {
      val (updatedHand, updatedDeck) = deck.fillHand(playersMap(user), targetHandSize = 6)
      playersMap.put(user, updatedHand)
      deck = updatedDeck
    }
    (playersMap.map(e => Player(e._1, e._2, saidBeat = false)).toList, deck)
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

  def convertToExternal(state: GameState, user: User): ExternalGameState = {
    import scala.jdk.CollectionConverters._
    import scala.jdk.OptionConverters._
    val hand = state.players.find(_.user == user).map(_.hand)
      .getOrElse(throw new GameException("User is not player of the game")).asJava
    val players = state.players.map(p => ExternalPlayer(p.user, p.hand.size, p.saidBeat)).asJava
    val round = state.round.map(r => ExternalRoundPair(r.attack, r.defence.toJava)).asJava
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
      state.defendingId.toString,
      state.isTaking
    )
  }
}