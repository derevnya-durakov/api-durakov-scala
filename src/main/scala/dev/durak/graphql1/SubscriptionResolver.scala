package dev.durak.graphql1

import dev.durak.model.PlayerEvent
import dev.durak.service.PlayerService
import graphql.kickstart.tools.GraphQLSubscriptionResolver
import graphql.schema.DataFetchingEnvironment
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component

@Component
class SubscriptionResolver(chatPublisher: ChatPublisher,
                           playerService: PlayerService) extends GraphQLSubscriptionResolver {
  def playersUpdated(env: DataFetchingEnvironment): Publisher[PlayerEvent] =
    playerService.authenticated(env) {auth =>
      chatPublisher.getUserCreatedPublisher
    }
}
