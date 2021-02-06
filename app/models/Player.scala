package models

import java.util.UUID

case class Player(id: UUID, accessToken: UUID, nickname: String)

object Player {
  def apply(nickname: String): Player =
    Player(id = UUID.randomUUID(), accessToken = UUID.randomUUID(), nickname)
}