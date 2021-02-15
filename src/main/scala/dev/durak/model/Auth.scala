package dev.durak.model

import dev.durak.model.internal.InternalUser

import java.util.UUID

case class Auth(id: UUID, accessToken: UUID, user: InternalUser) extends Identifiable

object Auth {
  def apply(player: InternalUser): Auth = Auth(
    id = UUID.randomUUID(),
    accessToken = UUID.randomUUID(),
    player)
}