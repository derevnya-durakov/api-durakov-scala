package dev.durak.jms

import dev.durak.model.UserEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

import java.util.concurrent.ExecutorService

@Component
class UserEventEmitter(executor: ExecutorService) extends AbstractEmitter[UserEvent](executor) {
  @EventListener
  override def onMessage(event: UserEvent): Unit = super.onMessage(event)
}
