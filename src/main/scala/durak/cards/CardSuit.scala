package durak.cards

class CardSuit(val name: String) {
  override def toString: String = name
}

object CardSuit {
  val suits: List[CardSuit] = List("heart", "diamond", "club", "spade").map(n => new CardSuit(n))
}