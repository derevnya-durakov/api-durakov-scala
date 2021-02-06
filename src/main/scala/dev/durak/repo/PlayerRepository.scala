package dev.durak.repo

import dev.durak.model.Player
import dev.durak.repo.PlayerRepository.PredefinedPlayers
import scala.concurrent.Future

import monix.execution.Ack
import monix.execution.Ack.Continue
import monix.execution.Scheduler.Implicits.global
import monix.reactive.Observer
import monix.reactive.subjects.ConcurrentSubject

import java.util.UUID

class PlayerRepository extends Repository[Player] {
  //noinspection NotImplementedCode
  override def save(player: Player): Player = ???

  override def findById(id: UUID): Option[Player] =
    find(_.id == id)

  override def find(f: Player => Boolean): Option[Player] =
    PredefinedPlayers.find(f)

  override def getAll: Iterable[Player] =
    PredefinedPlayers
}

object PlayerRepository {
  private val PredefinedPlayers: List[Player] = List(
    createPlayer(nickname = "kolya"),
    createPlayer(nickname = "sergo"),
    createPlayer(nickname = "sasha")
  )


  val observer: Observer[Player] = new Observer[Player] {
    def onNext(player: Player): Future[Ack] = {
      println(s"observer-->${player.id} - ${player.nickname}")
      Continue
    }

    def onError(ex: Throwable): Unit =
      ex.printStackTrace()

    def onComplete(): Unit =
      println("observer completed")
  }

  val source: ConcurrentSubject[Player, Player] = ConcurrentSubject.publish[Player]
  source.subscribe(observer)



  private def createPlayer(nickname: String): Player =
    Player(id = UUID.randomUUID(), accessToken = UUID.randomUUID(), nickname)
}