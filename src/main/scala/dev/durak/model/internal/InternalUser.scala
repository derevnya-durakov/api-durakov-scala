package dev.durak.model.internal

import dev.durak.model.Identifiable

import java.util.UUID

case class InternalUser(id: UUID, nickname: String) extends Identifiable

object InternalUser {
  def apply(nickname: String): InternalUser =
    InternalUser(id = UUID.randomUUID(), nickname)
}
