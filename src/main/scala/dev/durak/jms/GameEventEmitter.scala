package dev.durak.jms

import dev.durak.model.GameEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

import java.util.concurrent.ExecutorService

@Component
class GameEventEmitter(
  executor: ExecutorService
) extends AbstractEmitter[GameEvent](executor) {
  @EventListener
  override def onMessage(event: GameEvent): Unit = super.onMessage(event)
}
