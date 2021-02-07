package dev.durak.service

import dev.durak.model.{Auth, Player, PlayerEvent}
import dev.durak.repo.ICrudRepository
import org.springframework.jms.core.JmsTemplate
import org.springframework.stereotype.Service

import java.util.UUID

@Service
class PlayerService(jmsTemplate: JmsTemplate,
                    playerRepo: ICrudRepository[Player],
                    authRepo: ICrudRepository[Auth]) {
  private val lock = new Object

  Seq("kolya", "sergo", "sasha").foreach(internalCreatePlayer)

  def players: List[Player] = playerRepo.findAll()

  def findPlayer(id: String): Option[Player] = playerRepo.find(UUID.fromString(id))

  def addPlayer(nickname: String): Player = {
    val createdPlayer = internalCreatePlayer(nickname)
    jmsTemplate.convertAndSend(
      "PLAYER_CREATED", new PlayerEvent("PLAYER_CREATED", createdPlayer))
    createdPlayer
  }

  private def internalCreatePlayer(nickname: String): Player =
    lock synchronized {
      if (playerRepo.findAll().exists(_.nickname == nickname))
        throw new RuntimeException(s"Player $nickname already exists")
      else {
        val createdPlayer = playerRepo.create(Player(nickname))
        authRepo.create(Auth(createdPlayer))
        createdPlayer
      }
    }

  def accessToken(nickname: String): Option[String] =
    authRepo.findAll()
      .find(_.player.nickname == nickname)
      .map(_.accessToken.toString)

  def auth(accessToken: String): Option[Auth] =
    authRepo.findAll()
      .find(_.accessToken.toString == accessToken)
}

object PlayerService {
  //  val PlayerCreatedEvent = "PLAYER_CREATED"
  //  val PlayerUpdatedEvent = "PLAYER_UPDATED"
  //  val PlayerDeletedEvent = "PLAYER_DELETED"
}