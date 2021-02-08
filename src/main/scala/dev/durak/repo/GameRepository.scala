package dev.durak.repo

import dev.durak.model.internal.InternalGameState
import org.springframework.stereotype.Repository

@Repository
class GameRepository extends IMemoryRepository[InternalGameState]
