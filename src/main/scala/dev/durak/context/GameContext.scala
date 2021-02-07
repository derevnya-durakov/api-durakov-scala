//package dev.durak.context
//
//import dev.durak.exceptions.DurakException
//import dev.durak.model._
//import dev.durak.repo.Repository
//
//import java.util.UUID
//import scala.collection.mutable
//import scala.util.Random
//
//class GameContext(val gameRepository: Repository[Game],
//                  val playerRepository: Repository[Player]) {
//  private val InitialCardsList: List[Card] =
//    Ranks.values.toList.flatMap(
//      rank => Suits.values.toList.map(suit => Card(suit, rank)))
//  private val seed = 123L;
//
//  private def authenticated[T](accessToken: UUID)(func: Player => T): T =
//    func(playerRepository
//      .find(_.accessToken == accessToken)
//      .getOrElse(throw DurakException("Incorrect access token")))
//
//  def currentGame(accessToken: UUID): Option[Game] =
//    authenticated(accessToken) { signedPlayer =>
//      gameRepository.find(_.players.contains(signedPlayer))
//    }
//
//  // todo maybe?
//  // create game
//  // join game
//
//  def startGame(accessToken: UUID, playerIds: List[UUID]): Game =
//    authenticated(accessToken) { signedPlayer =>
//      val players: List[Player] = playerIds.map(
//        id => playerRepository
//          .findById(id)
//          .getOrElse(throw DurakException(s"Player $id not found")))
//      val deck = new Random(seed).shuffle(InitialCardsList)
//      val id = UUID.randomUUID()
//      val initialGame = new Game(
//        id,
//        seed,
//        deck,
//        trumpSuit = deck.last.suit,
//        players,
//        hands = players.map(Hand(_, Nil)),
//        field = Nil,
//        reboundSize = 0,
//        attacker = players.head,
//        defender = players.tail.head
//      )
//      val gameWithDealedCards = dealCards(initialGame)
//      val gameWithAttackerAndDefender = initAttackerAndDefender(gameWithDealedCards)
////      games += (id -> gameWithAttackerAndDefender) // todo
//      gameWithAttackerAndDefender
//    }
//
//  private def dealCards(game: Game): Game = {
////    val playerHands: mutable.Map[UUID, List[Card]] = {
////      val immutableMap = game.hands
////        .groupMap(_.player.id)(hand => hand)
////        .map(e => e._1 -> e._2)
////      mutable.Map(immutableMap.toSeq: _*)
////    }
////    val sortedPlayers = game.attacker :: game.players.filterNot(_ == game.attacker).filterNot()
//    var deck = game.deck
//
//    while (deck.nonEmpty) {
//
//
//    }
//
//
//    game // todo
//  }
//
//  private def initAttackerAndDefender(game: Game): Game = {
//    game // todo
//  }
//
//  private def getInitialAttackerAndDefender(game: Game): (Player, Player) = {
//    //    game.trumpSuit
//    (game.attacker, game.defender) // todo
//  }
//
//  private def nextPlayer(currentPlayer: Player, game: Game): Player = {
//    val currentIndex = game.players.indexOf(currentPlayer)
//    val nextIndex = if (currentIndex + 1 < game.players.size) currentIndex + 1 else 0
//    game.players(nextIndex)
//  }
//}
