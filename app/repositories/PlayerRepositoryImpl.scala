package repositories

import models.Player
import repositories.PlayerRepositoryImpl.Players

import java.util.UUID
import scala.collection.mutable
import scala.concurrent.Future

class PlayerRepositoryImpl extends PlayerRepository {
  override def create(entity: Player): Future[Player] = {
    Players.put(entity.id, entity)
    Future.successful(entity)
  }

  override def find(id: UUID): Future[Option[Player]] =
    Future.successful(Players.get(id))

  override def findAll(): Future[List[Player]] =
    Future.successful(Players.values.toList)

  override def update(entity: Player): Future[Player] =
    create(entity)

  override def delete(id: UUID): Future[Option[Player]] =
    Future.successful(Players.remove(id))
}

object PlayerRepositoryImpl {
  private val Players: mutable.Map[UUID, Player] = mutable.Map()
  Seq("kolya", "sergo", "sasha").map(Player(_)).foreach(p => Players.put(p.id, p))
}
