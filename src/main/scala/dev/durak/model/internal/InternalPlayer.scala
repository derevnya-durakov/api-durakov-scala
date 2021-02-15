package dev.durak.model.internal

import dev.durak.model.Card

case class InternalPlayer(user: InternalUser, hand: List[Card])
