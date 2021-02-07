package dev.durak.model

import java.util.UUID

case class Player(id: UUID, nickname: String) extends Identifiable

object Player {
  def apply(nickname: String): Player =
    Player(id = UUID.randomUUID(), nickname)
}
