package dev.durak.jms

import io.reactivex.{ObservableEmitter, ObservableOnSubscribe}

abstract class AObservableOnSubscribeEmitter[DataType] extends ObservableOnSubscribe[DataType] {
  private var emitter: ObservableEmitter[DataType] = _

  protected def getEmitter: ObservableEmitter[DataType] = this.emitter;

  override def subscribe(emitter: ObservableEmitter[DataType]): Unit = {
    this.emitter = emitter
  }

  protected def onMessage(message: DataType): Unit = {
    if (emitter != null) emit(message)
  }

  protected def emit(event: DataType): Unit
}
