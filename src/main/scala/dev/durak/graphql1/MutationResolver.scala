package dev.durak.graphql1

import dev.durak.model.User
import dev.durak.service.{AuthService, GameService, UserService}
import graphql.kickstart.tools.GraphQLMutationResolver
import graphql.schema.DataFetchingEnvironment
import org.springframework.stereotype.Component

@Component
class MutationResolver(gameService: GameService,
                       playerService: UserService,
                       authService: AuthService) extends GraphQLMutationResolver {
  def addPlayer(nickname: String, env: DataFetchingEnvironment): User =
    authService.authenticated(env) { _ => playerService.addPlayer(nickname) }
}