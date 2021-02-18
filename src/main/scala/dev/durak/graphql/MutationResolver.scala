package dev.durak.graphql

import dev.durak.model.external.ExternalGameState
import dev.durak.model.{Card, User}
import dev.durak.service.{AuthService, GameService, UserService}
import graphql.kickstart.tools.GraphQLMutationResolver
import graphql.schema.DataFetchingEnvironment
import org.springframework.stereotype.Component

import scala.jdk.CollectionConverters._

@Component
class MutationResolver(gameService: GameService,
                       playerService: UserService,
                       authService: AuthService) extends GraphQLMutationResolver {
  def addUser(nickname: String, env: DataFetchingEnvironment): User =
    authService.authenticated(env) { _ => playerService.addUser(nickname) }

  def startGame(userIds: java.lang.Iterable[String],
                env: DataFetchingEnvironment): ExternalGameState =
    authService.authenticated(env) { auth =>
      GameService.convertToExternal(
        gameService.startGame(auth, userIds.asScala.toList), auth.user)
    }

  def attack(gameId: String, card: Card, env: DataFetchingEnvironment): ExternalGameState =
    authService.authenticated(env) { auth =>
      GameService.convertToExternal(
        gameService.attack(auth, gameId, card), auth.user)
    }

  def defend(gameId: String,
             attackCard: Card,
             defenceCard: Card,
             env: DataFetchingEnvironment): ExternalGameState =
    authService.authenticated(env) { auth =>
      GameService.convertToExternal(
        gameService.defend(auth, gameId, attackCard, defenceCard), auth.user)
    }
}