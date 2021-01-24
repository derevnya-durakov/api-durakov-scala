package dev.durak.exceptions

import sangria.execution.UserFacingError

class DurakException(msg: String) extends Exception(msg) with UserFacingError {
  override def getMessage: String = msg
}
