package dev.durak.model

import dev.durak.model.Suits.Suit

import java.util.UUID

class GameState(val id: UUID,
                val deck: List[Card],
                val trumpSuit: Suit /* todo think about trumpCard with rank */ ,
                val players: List[Player],
                val hands: List[Hand],
                val field: List[CardPair],
                val reboundSize: Int,
                val attacker: UUID,
                val defender: UUID)
