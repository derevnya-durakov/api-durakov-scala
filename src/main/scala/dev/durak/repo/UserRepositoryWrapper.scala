package dev.durak.repo

import dev.durak.model.User
import org.springframework.stereotype.Component

@Component
class UserRepositoryWrapper
(customMongoRepository: CustomMongoRepository[User])
  extends RepositoryWrapperImpl[User](customMongoRepository)
