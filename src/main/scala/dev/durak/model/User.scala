package dev.durak.model

import org.springframework.data.annotation.Id
import java.util.UUID
import scala.beans.BeanProperty

case class User(@BeanProperty nickname: String) extends Identifiable

//object User {
//  def apply(nickname: String): User =
//    User(id = UUID.randomUUID(), nickname)
//}
