package dev.durak.model

import java.util.UUID

case class User(
  id: UUID,
  nickname: String
) extends Identifiable

object User {
  def apply(nickname: String): User =
    User(id = UUID.randomUUID(), nickname)
}
