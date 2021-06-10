package dev.durak.service

import dev.durak.graphql.Constants
import dev.durak.model.{Auth, User, UserEvent}
import dev.durak.repo.ICrudRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

import java.util.UUID

@Service
class UserService(
  eventPublisher: ApplicationEventPublisher,
  userRepo: ICrudRepository[User],
  authRepo: ICrudRepository[Auth]
) {
  private val lock = new Object

  Seq(
    User(UUID.fromString("832e3859-c1f9-4a2e-803e-427850570800"), "sergo"),
    User(UUID.fromString("e7f1e462-c04c-4ae4-bc9d-989f648433f6"), "kolya"),
    User(UUID.fromString("ca46a32a-615a-4d87-80b9-d2c3aa174581"), "sasha"),
//    User(UUID.fromString("aae099d4-9ce2-46f3-a74f-f420f86c83ee"), "ilyan"),
//    User(UUID.fromString("22f51ea3-7609-4d0d-97bd-7385610c66d7"), "user1"),
//    User(UUID.fromString("efe3768d-b9a2-4562-b21a-2beffe74b759"), "user2"),
  ).map(userRepo.create).map(Auth(_)).foreach(authRepo.create)

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