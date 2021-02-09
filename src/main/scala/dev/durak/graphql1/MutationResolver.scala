package dev.durak.graphql1

import dev.durak.model.Player
import dev.durak.service.{AuthService, GameService, PlayerService}
import graphql.kickstart.tools.GraphQLMutationResolver
import graphql.schema.DataFetchingEnvironment
import org.springframework.stereotype.Component

@Component
class MutationResolver(gameService: GameService,
                       playerService: PlayerService,
                       authService: AuthService) extends GraphQLMutationResolver {
  def addPlayer(nickname: String, env: DataFetchingEnvironment): Player =
    authService.authenticated(env) { _ => playerService.addPlayer(nickname) }
}