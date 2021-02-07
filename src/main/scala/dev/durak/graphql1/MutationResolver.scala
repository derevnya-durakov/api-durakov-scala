package dev.durak.graphql1

import dev.durak.model.Player
import dev.durak.service.PlayerService
import graphql.kickstart.tools.GraphQLMutationResolver
import org.springframework.stereotype.Component

@Component
class MutationResolver(service: PlayerService) extends GraphQLMutationResolver {
  def addPlayer(nickname: String): Player = service.addPlayer(nickname)
}