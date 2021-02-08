package dev.durak.model.internal

import dev.durak.model.{Identifiable, State}

import java.util.UUID

case class InternalGameState(id: UUID,
                             nonce: Long,
                             //                seed: Long,
                             //                deck: Set[Card],
                             //                trumpSuit: Suit,
                             //                players: Seq[Player],
                             //                hands: Seq[Hand],
                             //                field: Set[CardPair],
                             //                reboundSize: Int = 0,
                             //                attacker: Player,
                             //                defender: Player
               ) extends Identifiable with State

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