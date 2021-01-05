package durak.cards

import scala.util.Random

class CardDeck private(private var cards: List[Card]) {
  def shuffle(): Unit = cards = Random.shuffle(cards)

  def shuffled: CardDeck = new CardDeck(Random.shuffle(cards))

  def nonEmpty: Boolean = cards.nonEmpty

  def pop(): Option[Card] = {
    if (isEmpty)
      None
    else {
      val headCard = cards.head
      cards = cards.tail
      Option(headCard)
    }
  }

  def isEmpty: Boolean = cards.isEmpty
}

object CardDeck {
  private val maxDeckSize = 52;

  def create36Dec: CardDeck = CardDeck(36)

  def create52Deck: CardDeck = CardDeck(52)

  def apply(deckSize: Int): CardDeck = {
    if (deckSize > maxDeckSize)
      throw new IllegalArgumentException(s"Max deck size is $maxDeckSize")
    if (deckSize <= 0)
      throw new IllegalArgumentException("Deck size should be positive")
    val suitsCount = CardSuit.suits.length;
    if (deckSize % suitsCount != 0)
      throw new IllegalArgumentException(s"Cards amount should be divisible by $suitsCount")
    val cardsInSuit = deckSize / suitsCount
    val aceValue = 14;
    val values = (aceValue until (aceValue - cardsInSuit) by -1).map(value => new CardValue(value))
    val cards = CardSuit.suits.flatMap(suit => values.map(value => new Card(suit, value)))
    new CardDeck(cards)
  }
}