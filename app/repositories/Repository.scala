package repositories

import scala.concurrent.Future

/**
 * Simple CRUD repository which provides basic operations.
 */
trait Repository[K, V] {
  def create(entity: V): Future[V]

  def find(id: K): Future[Option[V]]

  def findAll(): Future[List[V]]

  def update(entity: V): Future[V]

  def delete(id: K): Future[Option[V]]
}