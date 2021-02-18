package dev.durak.model

import java.util.UUID

case class GameState(id: UUID,
                     seed: Long,
                     nonce: Long,
                     deck: CardDeck,
                     discardPileSize: Int,
                     players: List[Player],
                     round: List[RoundPair],
                     defendingId: UUID,
                     isTaking: Boolean) extends Identifiable
