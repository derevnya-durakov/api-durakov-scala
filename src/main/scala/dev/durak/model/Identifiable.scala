package dev.durak.model

import org.springframework.data.annotation.Id

import java.util.UUID
import scala.beans.BeanProperty

trait Identifiable {
  @Id
  @BeanProperty
  var id: UUID = _
}
