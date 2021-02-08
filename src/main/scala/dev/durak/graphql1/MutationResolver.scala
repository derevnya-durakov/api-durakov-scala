package dev.durak.graphql1

import dev.durak.model.Player
import dev.durak.service.{GameService, PlayerService}
import graphql.kickstart.tools.GraphQLMutationResolver
import graphql.schema.DataFetchingEnvironment
import org.springframework.stereotype.Component

@Component
class MutationResolver(gameService: GameService,
                       playerService: PlayerService) extends GraphQLMutationResolver {
  def addPlayer(nickname: String, env: DataFetchingEnvironment): Player =
    playerService.authenticated(env) { _ => playerService.addPlayer(nickname) }
}