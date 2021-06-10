package dev.durak.jms

import io.reactivex.{ObservableEmitter, ObservableOnSubscribe}

import java.util.concurrent.ExecutorService

abstract class AbstractEmitter[DataType](
  executor: ExecutorService
) extends ObservableOnSubscribe[DataType] {
  private var emitter: ObservableEmitter[DataType] = _

  override def subscribe(emitter: ObservableEmitter[DataType]): Unit = {
    this.emitter = emitter
  }

  protected def onMessage(message: DataType): Unit = {
    if (emitter != null) emit(message)
  }

  protected def emit(event: DataType): Unit = {
    executor.execute(() => emitter.onNext(event))
  }
}
