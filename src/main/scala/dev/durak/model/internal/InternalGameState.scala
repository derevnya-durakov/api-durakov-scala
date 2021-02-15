package dev.durak.model.internal

import dev.durak.model.{CardDeck, Identifiable}

import java.util.UUID

case class InternalGameState(id: UUID,
                             seed: Long,
                             nonce: Long,
                             deck: CardDeck,
                             discardPileSize: Int,
                             players: List[InternalPlayer],
                             round: List[InternalRoundPair],
                             defendingId: UUID) extends Identifiable