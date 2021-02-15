package dev.durak.repo

import dev.durak.model.internal.InternalUser
import org.springframework.stereotype.Repository

@Repository
class UserRepository extends IMemoryRepository[InternalUser]
