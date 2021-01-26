package dev.durak.exceptions

import sangria.execution.UserFacingError

case class DurakException(msg: String) extends Exception(msg) with UserFacingError {
  override def getMessage: String = msg
}
