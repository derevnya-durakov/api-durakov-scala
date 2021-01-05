package durak

import durak.cards.CardDeck

object Main extends App {
  val deck: CardDeck = CardDeck.create52Deck.shuffled
  while (deck.nonEmpty)
    println(deck.pop().get)
}
