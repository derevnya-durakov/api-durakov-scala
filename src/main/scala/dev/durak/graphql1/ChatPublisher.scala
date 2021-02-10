package dev.durak.graphql1

import dev.durak.jms.PlayerCreatedObservableOnSubscribeEmitter
import dev.durak.model.UserEvent
import io.reactivex.{BackpressureStrategy, Flowable, Observable}
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component

@Component
class ChatPublisher(userCreatedObservableOnSubscribe: PlayerCreatedObservableOnSubscribeEmitter) {
  private val userCreatedFlowable: Flowable[UserEvent] = {
    val userCreatedConnectableObservable = Observable
      .create(userCreatedObservableOnSubscribe)
      .share
      .publish
    userCreatedConnectableObservable.connect
    userCreatedConnectableObservable.toFlowable(BackpressureStrategy.BUFFER)
  }

  def getUserCreatedPublisher: Publisher[UserEvent] = userCreatedFlowable
}