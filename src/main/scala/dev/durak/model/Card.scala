package dev.durak.model

case class Card(suit: Suit, rank: Rank)

object Card {
  def canBeat(attack: Card, beat: Card, trump: Suit): Boolean =
    if (attack.suit == beat.suit)
      beat.rank.ordinal() > attack.rank.ordinal()
    else
      beat.suit == trump
}