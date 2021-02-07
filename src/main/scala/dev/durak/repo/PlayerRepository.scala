package dev.durak.repo

import dev.durak.model.Player
import org.springframework.stereotype.Repository

@Repository
class PlayerRepository extends IMemoryRepository[Player]
