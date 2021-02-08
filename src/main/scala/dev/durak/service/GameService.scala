package dev.durak.service

import dev.durak.model.internal.InternalGameState
import dev.durak.model.{Auth, Player}
import dev.durak.repo.ICrudRepository
import org.springframework.stereotype.Service

@Service
class GameService(
                   //                   roomRepo: ICrudRepository[Room],
                   gameRepo: ICrudRepository[InternalGameState],
                   playerRepo: ICrudRepository[Player],
                   authRepo: ICrudRepository[Auth]) {
  private val lock = new Object

  private def authenticated[T](accessToken: String)(op: Auth => T): T = {
    val auth = authRepo.findAll().find(_.accessToken.toString == accessToken)
    require(auth.isDefined, "Unauthorized access")
    op(auth.get)
  }

  //  def getRooms: Iterable[Room] = roomRepo.findAll()
  //
  //  def getRoom(id: String): Option[Room] = roomRepo.find(UUID.fromString(id))

  //  def createRoom(accessToken: String): Room =
  //    authenticated(accessToken) { auth => roomRepo.create(Room(auth.player)) }

  //  private def withRoom[T](roomId: String)(op: Room => T): T = {
  //    val roomOpt = getRoom(roomId)
  //    require(roomOpt.isDefined, "Room doesn't exist")
  //    op(roomOpt.get)
  //  }
  //
  //  def joinRoom(accessToken: String, roomId: String): Room =
  //    authenticated(accessToken) { auth =>
  //      lock synchronized {
  //        withRoom(roomId) { room =>
  //          require(!room.players.contains(auth.player), "Player already in room")
  //          require(room.game.isEmpty, "Game in the room already started")
  //          require(room.players.size < 6, "Room is already full")
  //          val updatedPlayersSet = (auth.player :: room.players.toList).toSet
  //          roomRepo.update(Room(room.id, room.creator, updatedPlayersSet, room.game))
  //        }
  //      }
  //    }
  //
  //  def startGame(accessToken: String, roomId: String): Room =
  //    authenticated(accessToken) { auth =>
  //      lock synchronized {
  //        withRoom(roomId) { room =>
  //          require(room.creator == auth.player, "You are not creator of the room")
  //          require(room.players.size >= 2, "Room should have at least 2 players")
  //          val game = gameRepo.create(InternalGameState(UUID.randomUUID()))
  //          roomRepo.update(Room(room.id, room.creator, room.players, Some(game)))
  //        }
  //      }
  //    }
}
