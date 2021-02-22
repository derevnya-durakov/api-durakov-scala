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

  private def withGame[T](gameId: String)(action: GameState => T): T =
    action(gameRepo
      .find(UUID.fromString(gameId))
      .getOrElse(throw new GameException("Game not found")))

  private def withMe[T](auth: Auth, game: GameState)(action: Player => T): T =
    action(game
      .players
      .find(_.user.id == auth.user.id)
      .getOrElse(throw new GameException("You are not in the game")))

  private def withGameAndMe[T](auth: Auth, gameId: String)
                              (action: (GameState, Player) => T): T = {
    withGame(gameId) { game =>
      withMe(auth, game) { me =>
        action(game, me)
      }
    }
  }

  def take(auth: Auth, gameId: String): GameState =
    lock synchronized {
      withGameAndMe(auth, gameId) { (state, me) =>
        GameCheckUtils.iCanTake(me, state)
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

  def sayBeat(auth: Auth, gameId: String): GameState =
    lock synchronized {
      withGameAndMe(auth, gameId) { (game, me) =>
        GameCheckUtils.iCanSayBeat(me, game)
        val hasAllSayBeat = game.players
          .filterNot(_ == me)
          .filterNot(_.hand.isEmpty)
          .filterNot(_.user.id == game.defender.user.id)
          .forall(_.saidBeat)
        if (hasAllSayBeat) {
          val cardsInRound = game.round.flatMap { pair =>
            if (pair.defence.isDefined)
              pair.attack :: pair.defence.get :: Nil
            else
              pair.attack :: Nil
          }
          if (game.isTaking) {
            val playersTakenRound = game.players.map { p =>
              if (p == game.defender)
                Player(p.user, p.hand ::: cardsInRound, p.saidBeat, p.done)
              else
                p
            }
            val (updatedPlayers, updatedDeck) = dealCards(playersTakenRound, game.deck)
            val skippingAttackPlayer = findNextPlayerWithCards(game.attacker, updatedPlayers)
            val newAttacker = findNextPlayerWithCards(skippingAttackPlayer, updatedPlayers)
            val newDefender = findNextPlayerWithCards(newAttacker, updatedPlayers)
            val updatedState = gameRepo.update(GameState(
              game.id,
              game.seed,
              game.nonce + 1,
              updatedDeck,
              game.discardPileSize,
              updatedPlayers,
              Nil,
              newAttacker,
              newDefender,
              isTaking = false
            ))
            eventPublisher.publishEvent(new GameEvent(Constants.GAME_TAKEN, updatedState))
            updatedState
          } else {
            val (updatedPlayers, updatedDeck) = dealCards(game.players, game.deck)
            val newAttacker = findNextPlayerWithCards(game.attacker, updatedPlayers)
            val newDefender = findNextPlayerWithCards(newAttacker, updatedPlayers)
            val updatedState = gameRepo.update(GameState(
              game.id,
              game.seed,
              game.nonce + 1,
              updatedDeck,
              game.discardPileSize + cardsInRound.size,
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
          val updatedPlayers = game.players.map { p =>
            if (p == me)
              Player(p.user, p.hand, saidBeat = true, p.done)
            else
              p
          }
          val updatedAttacker = updatedPlayers.find(_.user == game.attacker.user).get
          val updatedState = gameRepo.update(GameState(
            game.id,
            game.seed,
            game.nonce + 1,
            game.deck,
            game.discardPileSize,
            updatedPlayers,
            game.round,
            updatedAttacker,
            game.defender,
            game.isTaking
          ))
          eventPublisher.publishEvent(new GameEvent(Constants.GAME_BEAT, updatedState))
          updatedState
        }
      }
    }

  def defend(auth: Auth, gameId: String, attackCard: Card, defenceCard: Card): GameState =
    lock synchronized {
      withGameAndMe(auth, gameId) { (game, me) =>
        GameCheckUtils.iCanDefend(me, game, attackCard, defenceCard)
        val round = game.round.map { pair =>
          if (pair.attack == attackCard)
            RoundPair(attackCard, Some(defenceCard))
          else
            pair
        }
        val defender = game.defender
        val updatedDefender = Player(
          defender.user,
          defender.hand.filterNot(_ == defenceCard),
          saidBeat = false,
          done = None
        )
        val updatedPlayers = game.players.map { p =>
          if (p.user == defender.user) updatedDefender else p
        }
        val updatedState = gameRepo.update(GameState(
          game.id,
          game.seed,
          game.nonce + 1,
          game.deck,
          game.discardPileSize,
          updatedPlayers,
          round,
          game.attacker,
          updatedDefender,
          isTaking = false
        ))
        eventPublisher.publishEvent(new GameEvent(Constants.GAME_DEFEND, updatedState))
        updatedState
      }
    }

  def attack(auth: Auth, gameId: String, card: Card): GameState =
    lock synchronized {
      withGameAndMe(auth, gameId) { (game, me) =>
        GameCheckUtils.iCanAttack(me, game, card)
        val round = game.round :+ RoundPair(card, None)
        val updatedPlayer = Player(
          me.user,
          me.hand.filterNot(_ == card),
          saidBeat = false,
          done = None
        )
        val updatedPlayers = game.players.map { p =>
          if (p.user == me.user) updatedPlayer else p
        }
        val updatedAttacker = updatedPlayers.find(_.user == game.attacker.user).get
        val updatedState = gameRepo.update(GameState(
          game.id,
          game.seed,
          game.nonce + 1,
          game.deck,
          game.discardPileSize,
          updatedPlayers,
          round,
          updatedAttacker,
          game.defender,
          game.isTaking
        ))
        eventPublisher.publishEvent(new GameEvent(Constants.GAME_ATTACK, updatedState))
        updatedState
      }
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