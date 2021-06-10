package dev.durak.repo

import dev.durak.model.User
import org.springframework.stereotype.Repository

@Repository
class UserRepository extends IMemoryRepository[User]
