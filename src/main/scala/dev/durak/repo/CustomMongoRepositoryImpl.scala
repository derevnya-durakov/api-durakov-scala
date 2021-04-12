package dev.durak.repo

import dev.durak.model.Identifiable
import org.springframework.data.mongodb.core.MongoOperations
import org.springframework.data.mongodb.repository.query.MongoEntityInformation
import org.springframework.data.mongodb.repository.support.SimpleMongoRepository

import java.util.UUID
import java.{lang, util}
import scala.jdk.CollectionConverters._

class CustomMongoRepositoryImpl[T <: Identifiable](entityInformation: MongoEntityInformation[T, Long],
                                                   mongoOperations: MongoOperations)
  extends SimpleMongoRepository[T, Long](entityInformation, mongoOperations)
    with CustomMongoRepository[T] {
  override def save[S <: T](entity: S): S =
    super.save(withGeneratedId(entity))

  override def saveAll[S <: T](entities: lang.Iterable[S]): util.List[S] =
    super.saveAll(entities.asScala.map(withGeneratedId).asJava)

  override def insert[S <: T](entity: S): S =
    super.insert(withGeneratedId(entity))

  override def insert[S <: T](entities: lang.Iterable[S]): util.List[S] =
    super.insert(entities.asScala.map(withGeneratedId).asJava)

  protected def withGeneratedId[S <: T](entity: S): S = {
    if (entity.id == null) {
      entity.setId(UUID.randomUUID)
    }
    entity
  }
}
