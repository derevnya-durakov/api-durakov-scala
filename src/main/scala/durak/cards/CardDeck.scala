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

  override def toString = s"CardDeck($cards)"

  override def equals(other: Any): Boolean = other match {
    case that: CardDeck =>
      (that canEqual this) &&
        cards == that.cards
    case _ => false
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[CardDeck]

  override def hashCode(): Int = {
    val state = Seq(cards)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

object CardDeck {
  private val MAX_DECK_SIZE = 52;
  private val ACE_VALUE = 14;

  def create36Dec: CardDeck = CardDeck(36)

  def apply(deckSize: Int): CardDeck = {
    require(deckSize <= MAX_DECK_SIZE, s"Max deck size is $MAX_DECK_SIZE")
    require(deckSize > 0, "Deck size should be positive")
    val suitsCount = CardSuit.suits.length;
    require(deckSize % suitsCount == 0, s"Cards amount should be divisible by $suitsCount")
    val cardsInSuit = deckSize / suitsCount
    val values = (ACE_VALUE until (ACE_VALUE - cardsInSuit) by -1).map(value => new CardValue(value))
    val cards = CardSuit.suits.flatMap(suit => values.map(value => new Card(suit, value)))
    new CardDeck(cards)
  }

  def create52Deck: CardDeck = CardDeck(52)
}