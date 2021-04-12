package dev.durak.repo

import org.springframework.data.domain.{Example, Page, Pageable, Sort}

trait RepositoryWrapper[T] {
  def saveAll[S <: T](entities: Iterable[S]): List[S]

  def findAll(): List[T]

  def findAll(sort: Sort): List[T]

  def insert[S <: T](entity: S): S

  def insert[S <: T](entities: Iterable[S]): List[S]

  def findAll[S <: T](example: Example[S]): List[S]

  def findAll[S <: T](example: Example[S], sort: Sort): List[S]

  def findAll(pageable: Pageable): Page[T]

  def save[S <: T](entity: S): S

  def findById(id: Long): Option[T]

  def existsById(id: Long): Boolean

  def findAllById(ids: Iterable[Long]): Iterable[T]

  def count(): Long

  def deleteById(id: Long): Unit

  def delete(entity: T): Unit

  def deleteAll(entities: Iterable[_ <: T]): Unit

  def deleteAll(): Unit

  def findOne[S <: T](example: Example[S]): Option[S]

  def findAll[S <: T](example: Example[S], pageable: Pageable): Page[S]

  def count[S <: T](example: Example[S]): Long

  def exists[S <: T](example: Example[S]): Boolean
}
