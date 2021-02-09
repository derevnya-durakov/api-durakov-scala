package dev.durak.service

import dev.durak.model.{Auth, Player}
import dev.durak.repo.ICrudRepository
import org.springframework.stereotype.Component

@Component
class AuthService(playerRepository: ICrudRepository[Player],
                  authRepository: ICrudRepository[Auth]) {

}

object AuthService {
  val AuthTokenHeader = "x-auth-token"
}