package dev.durak.model

import java.util.UUID

class Player(val id: UUID, val accessToken: UUID, val nickname: String) extends Identifiable
