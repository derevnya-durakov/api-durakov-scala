package dev.durak.repo

import dev.durak.model.Player
import dev.durak.repo.PlayerRepository.PredefinedPlayers

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

  private def createPlayer(nickname: String): Player =
    Player(id = UUID.randomUUID(), accessToken = UUID.randomUUID(), nickname)
}