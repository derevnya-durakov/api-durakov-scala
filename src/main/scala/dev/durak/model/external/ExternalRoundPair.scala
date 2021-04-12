package dev.durak.model.external

import dev.durak.model.Card

import java.util.Optional

case class ExternalRoundPair(
  attack: Card,
  defence: Optional[Card]
)
