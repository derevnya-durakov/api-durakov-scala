//package dev.durak.model
//
//import dev.durak.model.Suits.Suit
//
//import java.util.UUID
//
//case class Game(id: UUID,
//                seed: Long,
//                deck: List[Card],
//                trumpSuit: Suit,
//                players: List[Player],
//                hands: List[Hand],
//                field: List[CardPair],
//                reboundSize: Int = 0,
//                attacker: Player,
//                defender: Player) extends Identifiable
//
////object Game {
////  private val InitialCardsList: List[Card] =
////    Ranks.values.toList.flatMap(rank => Suits.values.toList.map(suit => new Card(suit, rank)))
////
////  def apply(seed: Long, players: List[Player]): Game = createGame(seed, players)
////
////  private def createGame(seed: Long, players: List[Player]): Game = {
////    val deck = new Random(seed).shuffle(InitialCardsList)
////    new Game(
////      id = UUID.randomUUID(),
////      seed,
////      deck,
////      trumpSuit = None,
////      players,
////      hands = Nil,
////      field = Nil,
////      reboundSize = 0,
////      attacker = None,
////      defender = None
////    )
////  }
////}