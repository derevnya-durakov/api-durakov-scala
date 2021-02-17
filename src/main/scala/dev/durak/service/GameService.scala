package dev.durak.service

import dev.durak.exceptions.GameException
import dev.durak.graphql.Constants
import dev.durak.model._
import dev.durak.repo.ICrudRepository
import org.springframework.jms.core.JmsTemplate
import org.springframework.stereotype.Service

import java.util.UUID
import scala.annotation.tailrec
import scala.collection.mutable

@Service
class GameService(jmsTemplate: JmsTemplate,
                  authService: AuthService,
                  gameRepo: ICrudRepository[GameState],
                  userRepo: ICrudRepository[User],
                  authRepo: ICrudRepository[Auth]) {
  private val lock = new Object

  def getGameState(auth: Auth, id: String): Option[GameState] =
    gameRepo.find(UUID.fromString(id))

  //  def defend(auth: Auth, gameId: String, )

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
              if (state.round.isEmpty) {
                val isAttacker = findNextPlayer(player, state.players).user.id == state.defendingId
                if (!isAttacker) {
                  throw new GameException("You can not attack")
                }
              } else {
                if (state.round.size > 5) {
                  throw new GameException("Game already have 6 cards")
                }
                if (!getAvailableCardRanks(state.round).contains(card.rank)) {
                  throw new GameException("No such card rank in round")
                }
              }
              val round = RoundPair(card, None) :: state.round
              val updatedPlayers = state.players.map { p =>
                if (p.user == auth.user)
                  p
                else
                  Player(p.user, p.hand.filterNot(_ == card))
              }
              val updatedState = gameRepo.update(GameState(
                state.id,
                state.seed,
                state.nonce + 1,
                state.deck,
                state.discardPileSize,
                updatedPlayers,
                round,
                state.defendingId
              ))
              jmsTemplate.convertAndSend(
                Constants.GAME_ATTACK, new GameEvent(Constants.GAME_ATTACK, state.id))
              updatedState
          }
      }
    }

  private def getAvailableCardRanks(round: List[RoundPair]): Set[Rank] = {
    round.flatMap { pair =>
      val cards = pair.attack :: Nil
      if (pair.beaten.isDefined)
        pair.beaten.get :: cards
      else
        cards
    }.map(_.rank).toSet
  }

  def startGame(auth: Auth, userIds: List[String]): GameState = {
    if (userIds.size < 2 || userIds.size > 6) {
      throw new GameException("Players size must be 2 <= x <= 6")
    }
    val id = UUID.fromString("0c52f37c-399c-4304-9d39-34d08b3ae1ba") // hardcoded for tests
    val seed = 123 // hardcoded for tests
    val playersWithEmptyHands = loadUsers(userIds).map(Player(_, Nil))
    val sourceDeck = CardDeck(seed)
    val (players, deck) = dealCards(playersWithEmptyHands, sourceDeck)
    val defendingId = findNextPlayer(identifyAttacker(players, deck.trumpSuit), players).user.id
    val gameState = GameState(
      id,
      seed,
      nonce = 1,
      deck,
      discardPileSize = 0,
      players,
      round = Nil,
      defendingId
    )
    jmsTemplate.convertAndSend(
      Constants.GAME_UPDATED, new GameEvent(Constants.GAME_CREATED, id))
    // for testing
    if (gameRepo.exists(id))
      gameRepo.update(gameState)
    else
      gameRepo.create(gameState)
  }

  @tailrec
  private def findNextPlayer(currentPlayer: Player,
                             players: List[Player]): Player = {
    if (!players.contains(currentPlayer))
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

  private def identifyAttacker(players: List[Player], trumpSuit: Suit): Player =
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
    (playersMap.map(e => Player(e._1, e._2)).toList, deck)
  }

  private def loadUsers(userIds: List[String]): List[User] =
    try {
      userIds
        .map(UUID.fromString)
        .map(id => userRepo.find(id).getOrElse(throw new GameException(s"User $id not found")))
    } catch {
      case e: IllegalArgumentException => throw new GameException(e.getMessage)
    }
}