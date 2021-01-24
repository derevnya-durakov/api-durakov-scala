package dev.durak.model

import dev.durak.model.Suits.Suit

import java.util.UUID

class Game( val id: UUID,
           val seed: Long,
           val deck: List[Card],
           val trumpSuit: Suit,
           val players: List[Player],
           val hands: List[Hand],
           val field: List[CardPair],
           val reboundSize: Int = 0,
           val attacker: Player,
           val defender: Player) extends Identifiable

//object Game {
//  private val InitialCardsList: List[Card] =
//    Ranks.values.toList.flatMap(rank => Suits.values.toList.map(suit => new Card(suit, rank)))
//
//  def apply(seed: Long, players: List[Player]): Game = createGame(seed, players)
//
//  private def createGame(seed: Long, players: List[Player]): Game = {
//    val deck = new Random(seed).shuffle(InitialCardsList)
//    new Game(
//      id = UUID.randomUUID(),
//      seed,
//      deck,
//      trumpSuit = None,
//      players,
//      hands = Nil,
//      field = Nil,
//      reboundSize = 0,
//      attacker = None,
//      defender = None
//    )
//  }
//}