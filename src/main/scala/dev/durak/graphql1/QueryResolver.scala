package dev.durak.graphql1

import dev.durak.model.{Auth, Player}
import dev.durak.service.PlayerService
import graphql.kickstart.tools.GraphQLQueryResolver
import org.springframework.stereotype.Component

import java.util
import java.util.Optional
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

@Component
class QueryResolver(service: PlayerService) extends GraphQLQueryResolver {
  def players: util.List[Player] = service.players.asJava

  def findPlayer(id: String): Optional[Player] = service.findPlayer(id).toJava

  def accessToken(nickname: String): Optional[String] = service.accessToken(nickname).toJava

  def auth(accessToken: String): Optional[Auth] = service.auth(accessToken).toJava
}
