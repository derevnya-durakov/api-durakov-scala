package dev.durak.model.external

import dev.durak.model.User

import java.util.Optional

case class ExternalPlayer(
  user: User,
  handSize: Int,
  saidBeat: Boolean,
  done: Optional[Int]
)
