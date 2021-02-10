package dev.durak.service

import dev.durak.model.{Auth, User, UserEvent}
import dev.durak.repo.ICrudRepository
import org.springframework.jms.core.JmsTemplate
import org.springframework.stereotype.Service

import java.util.UUID

@Service
class UserService(jmsTemplate: JmsTemplate,
                  userRepo: ICrudRepository[User],
                  authRepo: ICrudRepository[Auth]) {
  private val lock = new Object

  Seq("kolya", "sergo", "sasha").foreach(internalCreateUser)

  def users: Iterable[User] = userRepo.findAll()

  def findUser(id: String): Option[User] = userRepo.find(UUID.fromString(id))

  def addPlayer(nickname: String): User = {
    val createdUser = internalCreateUser(nickname)
    jmsTemplate.convertAndSend(
      "USER_CREATED", new UserEvent("USER_CREATED", createdUser))
    createdUser
  }

  private def internalCreateUser(nickname: String): User =
    lock synchronized {
      if (userRepo.findAll().exists(_.nickname == nickname))
        throw new RuntimeException(s"Player $nickname already exists")
      else {
        val createdPlayer = userRepo.create(User(nickname))
        authRepo.create(Auth(createdPlayer))
        createdPlayer
      }
    }
}