package dev.durak.repo

import dev.durak.model.GameState
import org.springframework.stereotype.Repository

@Repository
class GameStateRepository extends IMemoryRepository[GameState]
