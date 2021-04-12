package dev.durak.graphql

import dev.durak.model.external.ExternalGameState
import dev.durak.model.{Auth, User}
import dev.durak.service.{AuthService, GameService, UserService}
import graphql.kickstart.tools.GraphQLQueryResolver
import graphql.schema.DataFetchingEnvironment
import org.springframework.stereotype.Component

import java.lang
import java.util.Optional
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

@Component
class QueryResolver(
  gameService: GameService,
  userService: UserService,
  authService: AuthService
) extends GraphQLQueryResolver {
  def users(env: DataFetchingEnvironment): lang.Iterable[User] =
    authService.authenticated(env) { _ => userService.users.asJava }

  def findUser(id: String, env: DataFetchingEnvironment): Optional[User] =
    authService.authenticated(env) { _ => userService.findUser(id).toJava }

  def accessToken(nickname: String): Optional[String] =
    authService.accessToken(nickname).toJava

  def auth(env: DataFetchingEnvironment): Auth =
    authService.authenticated(env) { auth => auth }

  def getGameState(id: String, env: DataFetchingEnvironment): Optional[ExternalGameState] =
    authService.authenticated(env) { auth =>
      gameService
        .getGameState(auth, id)
        .map(GameService.toExternal(_, auth.user))
        .toJava
    }
}
