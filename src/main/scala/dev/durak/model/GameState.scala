package dev.durak.model

import java.util.UUID

case class GameState(id: UUID,
                     seed: Long,
                     nonce: Long,
                     deck: CardDeck,
                     discardPileSize: Int,
                     players: List[Player],
                     round: List[RoundPair],
                     attacker: Player,
                     defender: Player,
                     isTaking: Boolean,
                     durak: Option[Player]) extends Identifiable
