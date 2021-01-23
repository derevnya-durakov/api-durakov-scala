package dev.durak.model

import java.util.UUID

class Hand(val id: UUID, val playerId: UUID, val cards: List[Card])