package dev.durak.graphql1

import dev.durak.jms.PlayerCreatedObservableOnSubscribeEmitter
import dev.durak.model.PlayerEvent
import io.reactivex.{BackpressureStrategy, Flowable, Observable}
import org.reactivestreams.Publisher
import org.springframework.stereotype.Component

@Component
class ChatPublisher(userCreatedObservableOnSubscribe: PlayerCreatedObservableOnSubscribeEmitter) {
  private val playerCreatedFlowable: Flowable[PlayerEvent] = {
    val userCreatedConnectableObservable = Observable
      .create(userCreatedObservableOnSubscribe)
      .share
      .publish
    userCreatedConnectableObservable.connect
    userCreatedConnectableObservable.toFlowable(BackpressureStrategy.BUFFER)
  }

  def getUserCreatedPublisher: Publisher[PlayerEvent] = playerCreatedFlowable
}