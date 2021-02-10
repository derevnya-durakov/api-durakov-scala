package dev.durak.jms

import dev.durak.model.UserEvent
import org.slf4j.Logger

import java.util.concurrent.ExecutorService

abstract class APlayerObservableOnSubscribeEmitter extends AObservableOnSubscribeEmitter[UserEvent] {
  protected def getLogger: Logger

  protected def getExecutor: ExecutorService

  override protected def onMessage(player: UserEvent): Unit = {
    getLogger.info("Message received: {}", player)
    super.onMessage(player)
  }

  override final protected def emit(event: UserEvent): Unit = {
    getLogger.info("emitting event: {}", event)
    getExecutor.execute(() => getEmitter.onNext(event))
  }
}
