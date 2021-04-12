package dev.durak.model

import java.util.UUID

case class GameState(
  id: UUID,
  seed: Int,
  nonce: Long = 1,
  deck: CardDeck,
  discardPileSize: Int = 0,
  players: List[Player],
  round: List[RoundPair] = Nil,
  attacker: Player,
  defender: Player,
  isTaking: Boolean = false,
  durak: Option[Player] = None
) extends Identifiable
