package dev.durak.model.internal

import dev.durak.model.{Card, User}

case class InternalPlayer(user: User, hand: List[Card])
