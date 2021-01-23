package dev.durak.graphql

import dev.durak.model.Ranks.Rank
import dev.durak.model.Suits.Suit
import dev.durak.model.{Card, Ranks, Suits}
import sangria.schema._

object SchemaDefinition {
  val SuitEnum: EnumType[Suit] = EnumType(
    name = "Suit",
    values = Suits.values.toList.map(suit => EnumValue(name = suit.toString, value = suit))
  )
  val RankEnum: EnumType[Rank] = EnumType(
    name = "Rank",
    values = Ranks.values.toList.map(rank => EnumValue(name = rank.toString, value = rank))
  )
  val Card: ObjectType[Unit, Card] = ObjectType(
    name = "Card",
    fields = fields[Unit, Card](
      Field(
        name = "suit",
        fieldType = SuitEnum,
        resolve = _.value.suit
      ),
      Field(
        name = "rank",
        fieldType = RankEnum,
        resolve = _.value.rank
      )
    )
  )
}
