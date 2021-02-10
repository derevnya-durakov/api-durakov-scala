package dev.durak.service

import dev.durak.exceptions.GameException
import dev.durak.model._
import dev.durak.model.internal.{InternalGameState, InternalPlayer}
import dev.durak.repo.ICrudRepository
import dev.durak.service.GameService.InitialCardsList
import org.springframework.stereotype.Service

import java.util.UUID
import scala.collection.mutable
import scala.util.Random

@Service
class GameService(authService: AuthService,
                  gameRepo: ICrudRepository[InternalGameState],
                  userRepo: ICrudRepository[User],
                  authRepo: ICrudRepository[Auth]) {
  private val lock = new Object

  def startGame(auth: Auth, userIds: List[String]): InternalGameState = {
    if (userIds.size < 2 || userIds.size > 6) {
      throw new GameException("Players size must be 2 <= x <= 6")
    }
    val playersWithEmptyHands = loadUsers(userIds).map(InternalPlayer(_, Nil))
    val seed = 123 // hardcoded for tests
    var shuffledDeck = new Random(seed).shuffle(InitialCardsList)


    InternalGameState(
      id = UUID.randomUUID(),
      seed,
      nonce = 1,
    )
  }

  private def dealCards(players: List[InternalPlayer],
                        deck: List[Card],
                        lastTrump: Option[Card]): (List[InternalPlayer], List[Card], Option[Card]) = {
    val playersMap = mutable.Map[User, List[Card]](players.map(p => (p.user, p.hand)): _*)
    var varDeck = deck
    var varLastTrump = lastTrump
    while ( && (varDeck.nonEmpty || varLastTrump.isDefined)) {

    }
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

object GameService {
  private val InitialCardsList: List[Card] = Ranks.values.toList.flatMap(rank => Suits.values.toList.map(suit => new Card(suit, rank)))
}