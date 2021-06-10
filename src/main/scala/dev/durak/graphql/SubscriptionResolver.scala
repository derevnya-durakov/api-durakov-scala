package dev.durak.graphql

import dev.durak.model.UserEvent
import dev.durak.model.external.ExternalGameEvent
import dev.durak.service.{AuthService, GameService, UserService}
import graphql.kickstart.tools.GraphQLSubscriptionResolver
import graphql.schema.DataFetchingEnvironment
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component

@Component
class SubscriptionResolver(
  eventPublisher: EventPublisher,
  authService: AuthService
) extends GraphQLSubscriptionResolver {
  def usersUpdated(env: DataFetchingEnvironment): Publisher[UserEvent] =
    authService.authenticated(env) { _ => eventPublisher.getUserCreatedPublisher }

  def gameUpdated(gameId: String, env: DataFetchingEnvironment): Publisher[ExternalGameEvent] =
    authService.authenticated(env) { auth =>
      eventPublisher
        .getGameEventPublisher(gameId)
        .map { event =>
          ExternalGameEvent(event.name, GameService.toExternal(event.state, auth.user))
        }
    }
}
