package dev.durak.model

import dev.durak.model.Ranks.Rank
import dev.durak.model.Suits.Suit

case class Card(suit: Suit, rank: Rank)

object Card {
  def canBeat(attack: Card, beat: Card, trump: Suit): Boolean =
    if (attack.suit == beat.suit)
      beat.rank > attack.rank
    else
      beat.suit == trump
}