package durak.cards

class CardValue(val value: Int) {
  override def toString = s"CardValue($value)"

  override def equals(other: Any): Boolean = other match {
    case that: CardValue =>
      (that canEqual this) &&
        value == that.value
    case _ => false
  }

  def canEqual(other: Any): Boolean = other.isInstanceOf[CardValue]

  override def hashCode(): Int = {
    val state = Seq(value)
    state.map(_.hashCode()).foldLeft(0)((a, b) => 31 * a + b)
  }
}

object CardValue {
  val AceValue = 14
}