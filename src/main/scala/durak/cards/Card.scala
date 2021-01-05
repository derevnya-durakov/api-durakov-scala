package durak.cards

class Card(val suit: CardSuit, val value: CardValue) {
  override def toString: String = s"$value $suit"
}
