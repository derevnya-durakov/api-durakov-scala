package dev.durak.model

import java.util.UUID

case class Auth(id: UUID, accessToken: UUID, player: User) extends Identifiable

object Auth {
  def apply(player: User): Auth = Auth(
    id = UUID.randomUUID(),
    accessToken = UUID.randomUUID(),
    player)
}