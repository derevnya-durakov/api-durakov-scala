package modules

import com.google.inject.{AbstractModule, Scopes}
import models.Player
import repositories.{PlayerRepository, PlayerRepositoryImpl}

import java.util.UUID

/**
  * Contains bind of post repository to its implementation in order to use it in DI.
  */
class PlayerModule extends AbstractModule {

  /** @inheritdoc */
  override def configure(): Unit = {
    bind(classOf[PlayerRepository]).to(classOf[PlayerRepositoryImpl]).in(Scopes.SINGLETON)
  }
}