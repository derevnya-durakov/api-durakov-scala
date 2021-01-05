package durak.cards

class Card(val suit: CardSuit, val value: CardValue) {
  override def toString = s"Card($suit, $value)"

  override def equals(other: Any): Boolean = other match {
    case that: Card =>
      (that canEqual this) &&
        suit == that.suit &&
        value == that.value
    case _ => false
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[Card]

  override def hashCode(): Int = {
    val state = Seq(suit, value)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}
