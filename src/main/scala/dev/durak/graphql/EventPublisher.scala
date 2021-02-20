package dev.durak.graphql

import dev.durak.jms.{GameEventEmitter, UserEventEmitter}
import dev.durak.model.{GameEvent, UserEvent}
import io.reactivex.{BackpressureStrategy, Flowable, Observable}
import org.springframework.stereotype.Component

@Component
class EventPublisher(userEventEmitter: UserEventEmitter,
                     gameEventEmitter: GameEventEmitter) {
  private val userCreatedFlowable: Flowable[UserEvent] = {
    val userCreatedConnectableObservable = Observable
      .create(userEventEmitter)
      .share
      .publish
    userCreatedConnectableObservable.connect
    userCreatedConnectableObservable.toFlowable(BackpressureStrategy.BUFFER)
  }

  private val gameEventFlowable: Flowable[GameEvent] = {
    val gameEventConnectableObservable = Observable
      .create(gameEventEmitter)
      .share
      .publish
    gameEventConnectableObservable.connect
    gameEventConnectableObservable.toFlowable(BackpressureStrategy.BUFFER)
  }

  def getUserCreatedPublisher: Flowable[UserEvent] = userCreatedFlowable

  def getGameEventPublisher(id: String): Flowable[GameEvent] =
    gameEventFlowable.filter(_.state.id.toString == id)
}