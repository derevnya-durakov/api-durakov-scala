package dev.durak.graphql

import dev.durak.model.User
import dev.durak.model.external.{ExternalGameState, ExternalPlayer, ExternalRoundPair}
import dev.durak.service.{AuthService, GameService, UserService}
import graphql.kickstart.tools.GraphQLMutationResolver
import graphql.schema.DataFetchingEnvironment
import org.springframework.stereotype.Component

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

@Component
class MutationResolver(gameService: GameService,
                       playerService: UserService,
                       authService: AuthService) extends GraphQLMutationResolver {
  def addUser(nickname: String, env: DataFetchingEnvironment): User =
    authService.authenticated(env) { _ => playerService.addUser(nickname) }

  def startGame(userIds: java.lang.Iterable[String], env: DataFetchingEnvironment): ExternalGameState =
    authService.authenticated(env) { auth =>
      val state = gameService.startGame(auth, userIds.asScala.toList)
      // exception may be thrown here if authenticated user is not player
      val hand = state.players.find(_.user == auth.user).map(_.hand).get.asJava
      val players = state.players.map(p => ExternalPlayer(p.user, p.hand.size)).asJava
      val round = state.round.map(r => ExternalRoundPair(r.attack, r.beaten.toJava)).asJava
      ExternalGameState(
        state.id.toString,
        state.nonce,
        state.deck.trumpSuit,
        state.deck.lastTrump.toJava,
        state.deck.deckSize,
        state.discardPileSize,
        hand,
        players,
        round,
        state.defendingId.toString
      )
    }
}