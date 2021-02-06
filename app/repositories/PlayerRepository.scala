package repositories

import models.Player

import java.util.UUID

/**
 * Simple CRUD repository which provides basic operations.
 */
trait PlayerRepository extends Repository[UUID, Player]