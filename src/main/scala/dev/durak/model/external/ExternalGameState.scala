package dev.durak.model.external

import dev.durak.model.{Card, Suit}

import java.util.Optional

case class ExternalGameState(
  id: String,
  nonce: Long,
  trumpSuit: Suit,
  lastTrump: Optional[Card],
  deckSize: Int,
  discardPileSize: Int,
  hand: java.util.List[Card],
  players: java.util.List[ExternalPlayer],
  round: java.util.List[ExternalRoundPair],
  attacker: ExternalPlayer,
  defender: ExternalPlayer,
  isTaking: Boolean,
  durak: Optional[ExternalPlayer]
)
