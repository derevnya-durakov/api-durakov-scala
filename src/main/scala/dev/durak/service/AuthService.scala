package dev.durak.service

import dev.durak.exceptions.GameException
import dev.durak.model.Auth
import dev.durak.repo.ICrudRepository
import graphql.kickstart.execution.context.GraphQLContext
import graphql.kickstart.servlet.context.{GraphQLServletContext, GraphQLWebSocketContext}
import graphql.schema.DataFetchingEnvironment
import org.springframework.stereotype.Component

@Component
class AuthService(authRepo: ICrudRepository[Auth]) {
  def authenticated[T](env: DataFetchingEnvironment)(op: Auth => T): T = {
    val context: GraphQLContext = env.getContext
    val accessToken: String = context match {
      case ctx: GraphQLServletContext =>
        ctx
          .getHttpServletRequest
          .getHeader(AuthService.AuthTokenHeader)
      case ctx: GraphQLWebSocketContext =>
        ctx
          .getSession
          .getUserProperties
          .get(AuthService.AuthTokenHeader)
          .asInstanceOf[String]
    }
    if (accessToken == null)
      throw new GameException("401")
    val authOption = auth(accessToken)
    if (authOption.isEmpty)
      throw new GameException("403")
    op(authOption.get)
  }

  def accessToken(nickname: String): Option[String] =
    authRepo.findAll()
      .find(_.player.nickname == nickname)
      .map(_.accessToken.toString)

  def auth(accessToken: String): Option[Auth] =
    authRepo.findAll()
      .find(_.accessToken.toString == accessToken)
}

object AuthService {
  val AuthTokenHeader = "x-auth-token"
}