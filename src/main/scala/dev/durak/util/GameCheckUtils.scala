package dev.durak.util

import dev.durak.exceptions.GameException
import dev.durak.model._

object GameCheckUtils {
  def playersEqual(one: Player, another: Player): Boolean =
    one.user.id == another.user.id

  def getRoundRanks(round: List[RoundPair]): Set[Rank] = {
    round.flatMap { pair =>
      val cards = pair.attack :: Nil
      if (pair.defence.isDefined)
        pair.defence.get :: cards
      else
        cards
    }.map(_.rank).toSet
  }

  private def iAmDefender(me: Player, state: GameState): Unit =
    if (!playersEqual(me, state.defender))
      throw new GameException("You are not defending and cannot do this")

  private def iAmNotDefender(me: Player, state: GameState): Unit =
    if (playersEqual(me, state.defender))
      throw new GameException("You are defending and cannot do this")

  private def roundHasAnyUnbeatenCard(state: GameState): Unit =
    if (state.round.forall(_.defence.isDefined))
      throw new GameException("You cannot do this if round has no any unbeaten card")

  private def iAmNotDone(me: Player): Unit =
    if (me.done.isDefined)
      throw new GameException("You are already done and cannot do this")

  private def roundHasAnyCard(game: GameState): Unit =
    if (game.round.isEmpty)
      throw new GameException("No cards in round. You cannot do this")

  private def iDidNotSayBeat(me: Player): Unit =
    if (me.saidBeat)
      throw new GameException("You did say beat and cannot do this")

  private def defenderIsNotTaking(game: GameState): Unit =
    if (game.isTaking)
      throw new GameException("Defender is taking and cannot do this")

  private def iHaveCard(me: Player, card: Card): Unit =
    if (!me.hand.contains(card))
      throw new GameException("You don't have this card and cannot do this")

  private def roundHaveThisCardUnbeaten(card: Card, game: GameState): Unit = {
    val roundPair = game.round.find(_.attack == card)
      .getOrElse(throw new GameException("Round has not such card"))
    if (roundPair.defence.isDefined)
      throw new GameException("Card already beaten")
  }

  private def defenceCardIsStronger(attackCard: Card, defenceCard: Card, game: GameState): Unit =
    if (!Card.canBeat(attackCard, defenceCard, game.deck.trumpSuit))
      throw new GameException("Defence card is weaker than attacking card")

  private def IAmAttackerOrAttackerDidFirstAttack(me: Player, game: GameState): Unit =
    if (!playersEqual(me, game.attacker) && game.round.isEmpty)
      throw new GameException("You are tossing. Wait for first move of attacker")

  private def roundEmptyOrHasCardWithSuchRank(card: Card, game: GameState): Unit =
    if (game.round.nonEmpty && !getRoundRanks(game.round).contains(card.rank))
      throw new GameException("No such card rank in round")

  private def gameDidEnd(game: GameState): Unit =
    if (game.durak.isEmpty)
      throw new GameException("Game did not ended yet")

  private def gameDidNotEnd(game: GameState): Unit =
    if (game.durak.isDefined)
      throw new GameException("Game already ended")

  def iCanTake(me: Player, game: GameState): Unit = {
    gameDidNotEnd(game)
    iAmDefender(me, game)
    roundHasAnyUnbeatenCard(game)
  }

  def iCanSayBeat(me: Player, game: GameState): Unit = {
    gameDidNotEnd(game)
    iAmNotDefender(me, game)
    iAmNotDone(me)
    roundHasAnyCard(game)
    iDidNotSayBeat(me)
  }

  def iCanDefend(me: Player, game: GameState, attackCard: Card, defenceCard: Card): Unit = {
    gameDidNotEnd(game)
    iAmDefender(me, game)
    defenderIsNotTaking(game)
    iHaveCard(me, defenceCard)
    roundHaveThisCardUnbeaten(attackCard, game)
    defenceCardIsStronger(attackCard, defenceCard, game)
  }

  def iCanAttack(me: Player, game: GameState, attackCard: Card): Unit = {
    gameDidNotEnd(game)
    iAmNotDefender(me, game)
    iHaveCard(me, attackCard)
    iDidNotSayBeat(me)
    IAmAttackerOrAttackerDidFirstAttack(me, game)
    roundEmptyOrHasCardWithSuchRank(attackCard, game)
    // todo
    if (game.discardPileSize == 0 && game.round.size >= 5)
      throw new GameException("Round already have 5 cards (first round)")
    if (game.round.size >= 6)
      throw new GameException("Round already have 6 cards")
    val unbeatenCount = game.round.count(_.defence.isEmpty) + 1
    if (unbeatenCount > game.defender.hand.size)
      throw new GameException("Defending player doesn't have enough cards to beat it")
  }

  def iCanStartNextGame(game: GameState): Unit =
    gameDidEnd(game)
}
