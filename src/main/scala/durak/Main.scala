package durak

import durak.cards.CardDeck

object Main {
  def main(args: Array[String]): Unit = {
    val deck: CardDeck = CardDeck.create52Deck.shuffled
    while (deck.nonEmpty)
      println(deck.pop().get)
  }
}
