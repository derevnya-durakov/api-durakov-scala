package dev.durak.model

import dev.durak.model.Suits.Suit

import scala.util.Random

class CardDeck private(cards: List[Card], val trumpSuit: Suit) {
  def deckSize: Int = cards.size

  private def this(cards: List[Card]) =
    this(cards, cards.last.suit)

  def lastTrump: Option[Card] =
    cards.lastOption

  def deal(count: Int): (List[Card], CardDeck) =
    (cards.take(count), new CardDeck(cards.drop(count), trumpSuit))

  def fillHand(sourceHand: List[Card], targetHandSize: Int): (List[Card], CardDeck) = {
    val (newHand, newDeck) = deal(targetHandSize - sourceHand.size)
    (sourceHand ::: newHand, newDeck)
  }
}

object CardDeck {
  private val InitialCardsList: List[Card] =
    Ranks.values.toList.flatMap { rank =>
      Suits.values.toList.map { suit =>
        new Card(suit, rank)
      }
    }

  def apply(seed: Int): CardDeck =
    new CardDeck(new Random(seed).shuffle(InitialCardsList))
}
