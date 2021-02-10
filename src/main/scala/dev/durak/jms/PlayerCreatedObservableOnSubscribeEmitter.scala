package dev.durak.jms

import dev.durak.model.UserEvent
import org.slf4j.{Logger, LoggerFactory}
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Component

import java.util.concurrent.ExecutorService

@Component
class PlayerCreatedObservableOnSubscribeEmitter(executor: ExecutorService)
  extends APlayerObservableOnSubscribeEmitter {

  private val logger = LoggerFactory.getLogger(getClass)

  protected def getLogger: Logger = logger

  @JmsListener(destination = "PLAYER_CREATED", containerFactory = "myFactory")
  override def onMessage(user: UserEvent): Unit = super.onMessage(user)

  override protected def getExecutor: ExecutorService = executor
}
