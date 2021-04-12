package dev.durak.repo

import org.springframework.data.domain.{Example, Page, Pageable, Sort}

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

class RepositoryWrapperImpl[T](mongoRepository: CustomMongoRepository[T])
  extends RepositoryWrapper[T] {
  override def saveAll[S <: T](entities: Iterable[S]): List[S] =
    mongoRepository.saveAll(entities.asJava).asScala.toList

  override def findAll(): List[T] =
    mongoRepository.findAll.asScala.toList

  override def findAll(sort: Sort): List[T] =
    mongoRepository.findAll(sort).asScala.toList

  override def insert[S <: T](entity: S): S =
    mongoRepository.insert(entity)

  override def insert[S <: T](entities: Iterable[S]): List[S] =
    mongoRepository.insert(entities.asJava).asScala.toList

  override def findAll[S <: T](example: Example[S]): List[S] =
    mongoRepository.findAll(example).asScala.toList

  override def findAll[S <: T](example: Example[S], sort: Sort): List[S] =
    mongoRepository.findAll(example).asScala.toList

  override def findAll(pageable: Pageable): Page[T] =
    mongoRepository.findAll(pageable)

  override def save[S <: T](entity: S): S =
    mongoRepository.save(entity)

  override def findById(id: Long): Option[T] =
    mongoRepository.findById(id).toScala

  override def existsById(id: Long): Boolean =
    mongoRepository.existsById(id)

  override def findAllById(ids: Iterable[Long]): Iterable[T] =
    mongoRepository.findAllById(ids.asJava).asScala

  override def count(): Long =
    mongoRepository.count()

  override def deleteById(id: Long): Unit =
    mongoRepository.deleteById(id)

  override def delete(entity: T): Unit =
    mongoRepository.delete(entity)

  override def deleteAll(entities: Iterable[_ <: T]): Unit =
    mongoRepository.deleteAll(entities.asJava)

  override def deleteAll(): Unit =
    mongoRepository.deleteAll()

  override def findOne[S <: T](example: Example[S]): Option[S] =
    mongoRepository.findOne(example).toScala

  override def findAll[S <: T](example: Example[S], pageable: Pageable): Page[S] =
    mongoRepository.findAll(example, pageable)

  override def count[S <: T](example: Example[S]): Long =
    mongoRepository.count(example)

  override def exists[S <: T](example: Example[S]): Boolean =
    mongoRepository.exists(example)
}
