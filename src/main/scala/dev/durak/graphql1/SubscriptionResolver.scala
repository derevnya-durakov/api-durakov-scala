package dev.durak.graphql1

import dev.durak.model.PlayerEvent
import dev.durak.service.{AuthService, PlayerService}
import graphql.kickstart.tools.GraphQLSubscriptionResolver
import graphql.schema.DataFetchingEnvironment
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component

@Component
class SubscriptionResolver(chatPublisher: ChatPublisher,
                           playerService: PlayerService,
                           authService: AuthService) extends GraphQLSubscriptionResolver {
  def playersUpdated(env: DataFetchingEnvironment): Publisher[PlayerEvent] =
    authService.authenticated(env) { _ => chatPublisher.getUserCreatedPublisher }
}
