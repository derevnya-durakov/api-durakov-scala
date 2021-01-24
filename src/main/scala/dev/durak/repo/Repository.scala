package dev.durak.repo

import dev.durak.model.Identifiable

import java.util.UUID

trait Repository[T <: Identifiable] {
  def save(entity: T): T

  def findById(id: UUID): Option[T]

  def find(f: T => Boolean): Option[T]

  def getAll: Iterable[T]
}
