package dev.durak.repo

import dev.durak.model.Identifiable

import java.util.UUID

trait ICrudRepository[T <: Identifiable] {
  def create(entity: T): T

  def find(id: UUID): Option[T]

  def findAll(): Iterable[T]

  def update(entity: T): T

  def delete(id: UUID): Option[T]

  def exists(id: UUID): Boolean
}
