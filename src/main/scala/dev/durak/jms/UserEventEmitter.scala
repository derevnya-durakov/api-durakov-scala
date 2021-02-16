package dev.durak.jms

import dev.durak.graphql.Constants
import dev.durak.model.UserEvent
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Component

import java.util.concurrent.ExecutorService

@Component
class UserEventEmitter(executor: ExecutorService) extends AbstractEmitter[UserEvent](executor) {
  @JmsListener(destination = Constants.USER_CREATED, containerFactory = "myFactory")
  override def onMessage(event: UserEvent): Unit = super.onMessage(event)
}
