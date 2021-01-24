package dev.durak.repo

import dev.durak.model.Game
import dev.durak.repo.GameRepository.Games

import java.util.UUID
import scala.collection.mutable

class GameRepository extends Repository[Game] {
  override def save(game: Game): Game = {
    Games.put(game.id, game)
    game
  }

  override def find(f: Game => Boolean): Option[Game] =
    Games.values.find(f)

  override def getAll: Iterable[Game] =
    Games.values

  override def findById(id: UUID): Option[Game] =
    Games.get(id)
}

object GameRepository {
  private val Games: mutable.Map[UUID, Game] = mutable.Map()
}
