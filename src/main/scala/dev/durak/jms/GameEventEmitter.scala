package dev.durak.jms

import dev.durak.graphql.Constants
import dev.durak.model.{GameEvent, UserEvent}
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Component

import java.util.concurrent.ExecutorService

@Component
class GameEventEmitter(executor: ExecutorService) extends AbstractEmitter[GameEvent](executor) {
  @JmsListener(destination = Constants.GAME_UPDATED, containerFactory = "myFactory")
  override def onMessage(event: GameEvent): Unit = super.onMessage(event)
}
