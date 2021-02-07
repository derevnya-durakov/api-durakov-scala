package dev.durak.graphql1

import dev.durak.model.PlayerEvent
import graphql.kickstart.tools.GraphQLSubscriptionResolver
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component

@Component
class SubscriptionResolver(chatPublisher: ChatPublisher) extends GraphQLSubscriptionResolver {
  def playersUpdated: Publisher[PlayerEvent] =
    chatPublisher.getUserCreatedPublisher
}
