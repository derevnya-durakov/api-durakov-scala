//package dev.durak
//
//import dev.durak.model.Card
//import dev.durak.model.Suits.Suit
//import durak.cards.CardDeck
//
//import scala.collection.mutable
//
//// todo synchronize methods
//class Game(val playersIds: List[Long], seed: Long)  {
//  private var _deck: CardDeck = CardDeck(seed)
//  private var _trumpCard: Card = {
//    val dealResult = _deck.deal(1)
//    _deck = dealResult._2
//    dealResult._1.head
//  }
//  val trumpSuit: Suit = _trumpCard.suit
//  // [13, 7, 12, 2, 1]
//  //      *  ^
//  val playersCards: mutable.Map[Long, List[Card]] = mutable.Map()
//  initialDealCards()
//
//  private def findMin(cards: List[Card], suit: CardSuit): Option[CardValue] =
//    cards
//      .filter(_.suit == suit)
//      .map(_.rank)
//      .minByOption(_.value)
//
//  private var _attackerPlayerId: Long = {
//    playersCards.map(t => t._1 -> findMin(t._2, trumpSuit))
//      .filter(_._2.isDefined)
//      .map(t => t._1 -> t._2.get)
//      .minByOption(_._2.value)
//      .map(_._1)
//      .getOrElse(playersIds.head)
//  }
//  private var _defenderPlayerId: Long = nextPlayer(_attackerPlayerId)
//  private val field: mutable.Map[Card, Card] = mutable.Map()
//
//   def reboundSize(): Int = deckSize - _deck.size - playersCards.values.map(_.size).sum
//
//  private def nextPlayer(currentPlayer: Long): Long = {
//    // todo skip players who has no cards on hands
//    val nextId = playersIds.indexOf(currentPlayer) + 1
//    if (nextId < playersIds.size) nextId else 0
//  }
//
//  private def initialDealCards(): Unit = {
//    playersIds.foreach(dealCardsForPlayer)
//  }
//
//  private def dealCards(): Unit = {
//    dealCardsForPlayer(_attackerPlayerId)
//    playersIds
//      .filterNot(_ != _attackerPlayerId)
//      .filterNot(_ != _defenderPlayerId)
//      .foreach(dealCardsForPlayer)
//    dealCardsForPlayer(_defenderPlayerId)
//  }
//
//  private def dealCardsForPlayer(playerId: Long): Unit = {
//    val playerCards = playersCards.getOrElse(playerId, Nil)
//    val cardsToAdd = (playerCards.size until MaxHandSize)
//      .map(_ => _deck.popTop())
//      .filter(_.isDefined)
//      .map(_.get)
//      .toList
//    playersCards += (playerId -> (playerCards ::: cardsToAdd))
//  }
//
//   def attack(playerId: Long, card: Card): Boolean = {
//    if (!playersCards.getOrElse(playerId, Nil).contains(card))
//      false
//    else if (playersCards.getOrElse(_defenderPlayerId, Nil).isEmpty)
//      false
//    else if (reboundSize == 0 && field.size >= MaxHandSize - 1)
//      false
//    else {
//      playersCards += playersCards.getOrElse(playerId, Nil).filter(_ != card)
//      field += (card -> null)
//      true
//    }
////     todo automatic check for no attack can be done and end the turn
//  }
//
//   def defend(playerId: Long, topCard: Card, bottomCard: Card): Boolean = {
//    if (playerId != _defenderPlayerId)
//      false
//    else if (!playersCards.getOrElse(playerId, Nil).contains(topCard))
//      false
//    else if (!field.contains(bottomCard))
//      false
//    else if (field(bottomCard) != null)
//      false
//    else {
//      playersCards += playersCards.getOrElse(playerId, Nil).filter(_ != topCard)
//      field += (bottomCard -> topCard)
//      true
//    }
//
//    // todo automatic check for no attack can be done and end the turn
//  }
//
//   def takeField(playerId: Long): Boolean = {
//    if (playerId != _defenderPlayerId)
//      false
//    else if (field.isEmpty)
//      false
//    else if (!field.values.exists(_ == null))
//      false
//    else if (somebody can attack)
//      false
//    else {
//      val cardsToTake = field.keys.toList ::: field.values.filterNot(_ == null).toList
//      playersCards.put(playerId, playersCards.getOrElse(playerId, Nil) ::: cardsToTake)
//      true
//    }
//  }
//
//   private def compareCards(a: Card, b: Card): Int = {
//
//  }
//
//   def attackerPlayerId: Long = _attackerPlayerId
//
//   def defenderPlayerId: Long = _defenderPlayerId
//}
//
//object Game {
//  val MaxHandSize = 6
//}
