package durak.cards

import scala.util.Random

class CardDeck private(private var cards: List[Card]) {
  def shuffle(): Unit = cards = Random.shuffle(cards)

  def shuffled(): CardDeck = new CardDeck(Random.shuffle(cards))

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
  def create36Dec(): CardDeck = createDeck(36)

  def create52Deck(): CardDeck = createDeck(52)

  private def createDeck(cardsAmount: Int): CardDeck = {
    // todo enumerations or smth like this
    val suits = List("heart", "diamond", "club", "spade").map(n => new CardSuit(n))
    val suitsCount = suits.length;
    if (cardsAmount % suitsCount != 0)
      throw new IllegalArgumentException(s"Cards amount should be divisible by $suitsCount")
    val cardsInSuit = cardsAmount / suitsCount
    val aceValue = 14;
    val values = (aceValue until (aceValue - cardsInSuit) by -1).map(value => new CardValue(value))
    val cards = suits.flatMap(suit => values.map(value => new Card(suit, value)))
    new CardDeck(cards)
  }
}