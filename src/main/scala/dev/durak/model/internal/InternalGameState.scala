package dev.durak.model.internal

import dev.durak.model.Card
import dev.durak.model.Suits.Suit

import java.util.UUID

case class InternalGameState(id: UUID,
                             seed: Long,
                             nonce: Long,
                             trumpSuit: Suit,
                             lastTrump: Option[Card],
                             deckSize: Int,
                             discardPileSize: Int,
                             players: List[InternalPlayer],
                             round: List[InternalRoundPair],
                             defendPlayerId: String)
