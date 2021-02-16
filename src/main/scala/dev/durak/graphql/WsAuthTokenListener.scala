package dev.durak.graphql

import dev.durak.service.AuthService
import graphql.kickstart.execution.subscriptions.SubscriptionSession
import graphql.kickstart.execution.subscriptions.apollo.{ApolloSubscriptionConnectionListener, OperationMessage}
import org.springframework.stereotype.Component

import java.util
import java.util.Collections
import scala.jdk.CollectionConverters._

@Component
class WsAuthTokenListener extends ApolloSubscriptionConnectionListener {
  override def onConnect(session: SubscriptionSession, message: OperationMessage): Unit = {
    val payload = message.getPayload
    payload match {
      case payloadMap: util.Map[String, util.Map[String, String]] =>
        val headersKey = payloadMap
          .keySet().asScala
          .find(_.equalsIgnoreCase("headers"))
        if (headersKey.isDefined) {
          val headers = payloadMap.getOrDefault(headersKey.get, Collections.emptyMap())
          val authHeaderKey = headers
            .keySet().asScala
            .find(_.equalsIgnoreCase(AuthService.AuthTokenHeader))
          if (authHeaderKey.isDefined) {
            val authToken = headers.get(authHeaderKey.get)
            if (authToken != null)
              session.getUserProperties.put(AuthService.AuthTokenHeader, authToken)
          }
        }
      case _ =>
    }
  }
}
