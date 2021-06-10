package dev.durak.repo

import dev.durak.model.Auth
import org.springframework.stereotype.Repository

@Repository
class AuthRepository extends IMemoryRepository[Auth]
