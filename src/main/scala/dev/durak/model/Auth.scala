package dev.durak.model

import java.util.UUID

case class Auth(id: UUID, accessToken: UUID, player: Player) extends Identifiable

object Auth {
  def apply(player: Player): Auth = Auth(
    id = UUID.randomUUID(),
    accessToken = UUID.randomUUID(),
    player)
}