package dev.durak.model

import java.util.UUID

case class Player(id: UUID, accessToken: UUID, nickname: String) extends Identifiable

object Player {
  def apply(nickname: String): Player =
    Player(UUID.randomUUID(), UUID.randomUUID(), nickname)
}
