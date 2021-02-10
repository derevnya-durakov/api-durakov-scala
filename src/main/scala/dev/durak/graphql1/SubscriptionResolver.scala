package dev.durak.graphql1

import dev.durak.model.UserEvent
import dev.durak.service.{AuthService, UserService}
import graphql.kickstart.tools.GraphQLSubscriptionResolver
import graphql.schema.DataFetchingEnvironment
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component

@Component
class SubscriptionResolver(chatPublisher: ChatPublisher,
                           playerService: UserService,
                           authService: AuthService) extends GraphQLSubscriptionResolver {
  def playersUpdated(env: DataFetchingEnvironment): Publisher[UserEvent] =
    authService.authenticated(env) { _ => chatPublisher.getUserCreatedPublisher }
}
