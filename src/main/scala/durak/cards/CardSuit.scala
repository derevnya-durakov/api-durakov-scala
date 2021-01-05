package durak.cards

class CardSuit(val name: String) {
  override def toString = s"CardSuit($name)"

  override def equals(other: Any): Boolean = other match {
    case that: CardSuit =>
      (that canEqual this) &&
        name == that.name
    case _ => false
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[CardSuit]

  override def hashCode(): Int = {
    val state = Seq(name)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

object CardSuit {
  val Suits: List[CardSuit] = List("heart", "diamond", "club", "spade").map(n => new CardSuit(n))
}