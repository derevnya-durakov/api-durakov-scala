package dev.durak.graphql

import dev.durak.model.{GameEvent, UserEvent}
import dev.durak.service.{AuthService, UserService}
import graphql.kickstart.tools.GraphQLSubscriptionResolver
import graphql.schema.DataFetchingEnvironment
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component

@Component
class SubscriptionResolver(eventPublisher: EventPublisher,
                           userService: UserService,
                           authService: AuthService) extends GraphQLSubscriptionResolver {
  def usersUpdated(env: DataFetchingEnvironment): Publisher[UserEvent] =
    authService.authenticated(env) { _ => eventPublisher.getUserCreatedPublisher }

  def gameUpdated(gameId: String, env: DataFetchingEnvironment): Publisher[GameEvent] =
    authService.authenticated(env) { _ => eventPublisher.getGameEventPublisher(gameId) }
}
