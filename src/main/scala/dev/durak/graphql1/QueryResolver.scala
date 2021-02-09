package dev.durak.graphql1

import dev.durak.model.{Auth, Player}
import dev.durak.service.{AuthService, GameService, PlayerService}
import graphql.kickstart.tools.GraphQLQueryResolver
import graphql.schema.DataFetchingEnvironment
import org.springframework.stereotype.Component

import java.lang
import java.util.Optional
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

@Component
class QueryResolver(gameService: GameService,
                    playerService: PlayerService,
                    authService: AuthService) extends GraphQLQueryResolver {
  def players(env: DataFetchingEnvironment): lang.Iterable[Player] =
    authService.authenticated(env) { _ => playerService.players.asJava }

  def findPlayer(id: String, env: DataFetchingEnvironment): Optional[Player] =
    authService.authenticated(env) { _ => playerService.findPlayer(id).toJava }

  def accessToken(nickname: String): Optional[String] =
    authService.accessToken(nickname).toJava

  def auth(env: DataFetchingEnvironment): Auth =
    authService.authenticated(env) { auth => auth }
}
