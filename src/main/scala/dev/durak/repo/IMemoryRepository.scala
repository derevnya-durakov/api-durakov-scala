package dev.durak.repo

import dev.durak.model.Identifiable

import java.util.UUID
import scala.collection.mutable

trait IMemoryRepository[T <: Identifiable] extends ICrudRepository[T] {
  protected val entities: mutable.Map[UUID, T] = mutable.Map()
  protected val lock = new Object

  override def create(entity: T): T =
    lock synchronized {
      if (exists(entity.id))
        throw new RuntimeException(s"Entity with id $entity.id already exists")
      else {
        entities.put(entity.id, entity)
        entity
      }
    }

  override def find(id: UUID): Option[T] = entities.get(id)

  override def findAll(): List[T] = entities.values.toList

  override def update(entity: T): T = {
    lock synchronized {
      if (!exists(entity.id))
        throw new RuntimeException(s"Entity with id $entity.id not exists")
      else {
        entities.put(entity.id, entity)
        entity
      }
    }
  }

  override def delete(id: UUID): Option[T] = entities.remove(id)

  override def exists(id: UUID): Boolean = entities.contains(id)
}
