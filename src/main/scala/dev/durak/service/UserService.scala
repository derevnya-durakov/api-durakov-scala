package dev.durak.service

import dev.durak.graphql.Constants
import dev.durak.model.{Auth, User, UserEvent}
import dev.durak.repo.{ICrudRepository, RepositoryWrapper, UserMongoRepository}
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

import java.util.UUID

@Service
class UserService(eventPublisher: ApplicationEventPublisher,
                  userRepo: RepositoryWrapper[User],
                  authRepo: ICrudRepository[Auth]) {
  private val lock = new Object

  userRepo.saveAll(
    Seq(
      User("sergo"),
      User("kolya"),
      User("sasha")
    )
  ).map(Auth(_)).foreach(authRepo.create)

  def users: Iterable[User] = userRepo.findAll()

  def findUser(id: String): Option[User] = userRepo.find(UUID.fromString(id))

  def addUser(nickname: String): User = {
    val createdUser = internalCreateUser(nickname)
    eventPublisher.publishEvent(new UserEvent(Constants.USER_CREATED, createdUser))
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