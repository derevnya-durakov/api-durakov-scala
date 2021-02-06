package graphql.resolvers

import com.google.inject.Inject
import models.Player
import repositories.PlayerRepository

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

/**
 * A resolver class that contains all resolver methods for the Post model.
 *
 * @param playerRepository an repository which provides basic operations for Post entity
 * @param executionContext execute program logic asynchronously, typically but not necessarily on a thread pool
 */
class PlayerResolver @Inject()(playerRepository: PlayerRepository,
                               implicit val executionContext: ExecutionContext) {

  def players: Future[List[Player]] = playerRepository.findAll()

  def addPlayer(nickname: String): Future[Player] = playerRepository.create(Player(nickname))

  def findPlayer(id: String): Future[Option[Player]] = playerRepository.find(UUID.fromString(id))

  //  def updatePost(post: Player): Future[Player] = playerRepository.update(post)

  //  def deletePost(id: String): Future[Option[Player]] = playerRepository.delete(UUID.fromString(id))

  def accessTokenByNickname(nickname: String): Future[Option[String]] =
    players.map {
      _.find(_.nickname == nickname)
        .map(_.accessToken.toString)
    }

  def playerByAccessToken(accessToken: String): Future[Option[Player]] =
    players.map {
      _.find(_.accessToken.toString == accessToken)
    }
}
