package durak.cards

import dev.durak.model.{Card, Ranks, Suits}

import scala.util.Random

class CardDeck private(private val cards: List[Card]) {
  //  def shuffled: CardDeck = new CardDeck(Random.shuffle(cards))

  def nonEmpty: Boolean = cards.nonEmpty

  def isEmpty: Boolean = cards.isEmpty

  def size: Int = cards.length

  def deal(count: Int): (List[Card], CardDeck) =
    (cards.take(count), new CardDeck(cards.drop(count)))

  //  def poppedTop: (Option[Card], CardDeck) =
  //    if (isEmpty)
  //      (None, new CardDeck(cards))
  //    else {
  //      (Option(cards.head), new CardDeck(cards.tail))
  //    }
  //
  //  def poppedBottom: (Option[Card], CardDeck) =
  //    if (isEmpty)
  //      (None, new CardDeck(cards))
  //    else {
  //      (Option(cards.last), new CardDeck(cards.dropRight(1)))
  //    }
  //
  //  def insertedTop(card: Card): CardDeck =
  //    new CardDeck(cards ::: List(card))
  //
  //  def insertedBottom(card: Card): CardDeck =
  //    new CardDeck(card :: cards)


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
  private val InitialCardsList: List[Card] =
    Ranks.values.toList.flatMap(rank => Suits.values.toList.map(suit => new Card(suit, rank)))

  def apply(seed: Long): CardDeck =
    generateDeck(new Random(seed))

  private def generateDeck(random: Random): CardDeck =
    new CardDeck(random.shuffle(InitialCardsList))
}