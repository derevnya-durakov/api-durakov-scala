package dev.durak.graphql1

import dev.durak.service.AuthService
import graphql.kickstart.execution.subscriptions.SubscriptionSession
import graphql.kickstart.execution.subscriptions.apollo.{ApolloSubscriptionConnectionListener, OperationMessage}
import org.springframework.stereotype.Component

import java.util
import java.util.Collections

@Component
class WsAuthTokenListener extends ApolloSubscriptionConnectionListener {
  override def onConnect(session: SubscriptionSession, message: OperationMessage): Unit = {
    val payload = message.getPayload
    val authToken: String = payload match {
      case payloadMap: util.Map[String, util.Map[String, String]] =>
        payloadMap
          .getOrDefault("headers", Collections.emptyMap[String, String]())
          .get(AuthService.AuthTokenHeader)
      case _ => null
    }
    if (authToken != null)
      session.getUserProperties.put(AuthService.AuthTokenHeader, authToken)
  }
}
