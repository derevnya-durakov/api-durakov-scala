package dev.durak

import dev.durak.context.GameContext
import dev.durak.model.Ranks.Rank
import dev.durak.model.Suits.Suit
import dev.durak.model.{Card, Player, Ranks, Suits}
import sangria.schema._

import java.util.UUID

object SchemaDefinition {
  //  val players: Fetcher[PlayerContext, Player, Player, UUID] = Fetcher.caching(
  //    (ctx: PlayerContext, ids: Seq[UUID]) =>
  //      Future.successful(ids.flatMap(ctx.findPlayerByNickname)))(HasId(_.id))

  val SuitEnum: EnumType[Suit] = EnumType(
    name = "Suit",
    values = Suits.values.toList.map(suit => EnumValue(name = suit.toString, value = suit))
  )

  val RankEnum: EnumType[Rank] = EnumType(
    name = "Rank",
    values = Ranks.values.toList.map(rank => EnumValue(name = rank.toString, value = rank))
  )

  val CardType: ObjectType[Unit, Card] = ObjectType(
    name = "Card",
    fields[Unit, Card](
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

  val PlayerType: ObjectType[Unit, Player] = ObjectType(
    name = "Player",
    fields[Unit, Player](
      Field(
        name = "id",
        fieldType = IDType,
        resolve = _.value.id.toString
      ),
      Field(
        name = "nickname",
        fieldType = StringType,
        resolve = _.value.nickname
      )
    )
  )

  val AuthType: ObjectType[Unit, Player] = ObjectType(
    name = "Auth",
    fields[Unit, Player](
      Field(
        name = "accessToken",
        fieldType = StringType,
        resolve = _.value.accessToken.toString
      ),
      Field(
        name = "player",
        fieldType = PlayerType,
        resolve = _.value
      )
    )
  )

  val IdArg: Argument[String] = Argument("id", IDType)
  val NicknameArg: Argument[String] = Argument("nickname", StringType)
  val AccessTokenArg: Argument[String] = Argument("accessToken", StringType)

  val QueryType: ObjectType[GameContext, Unit] = ObjectType(
    name = "Query",
    fields[GameContext, Unit](
      Field(
        name = "player",
        fieldType = OptionType(PlayerType),
        arguments = IdArg :: Nil,
        resolve = ctx => ctx.ctx.playerRepository.findById(UUID.fromString(ctx.arg(IdArg)))),
      Field(
        name = "players",
        fieldType = ListType(PlayerType),
        arguments = Nil,
        resolve = ctx => ctx.ctx.playerRepository.getAll.toList
      ),
      Field(
        name = "accessToken",
        fieldType = OptionType(StringType),
        arguments = NicknameArg :: Nil,
        resolve = ctx => ctx.ctx.playerRepository
          .find(_.nickname == ctx.arg(NicknameArg))
          .map(_.accessToken.toString)
      ),
      Field(
        name = "auth",
        fieldType = OptionType(AuthType),
        arguments = AccessTokenArg :: Nil,
        resolve = ctx => ctx.ctx.playerRepository
          .find(_.accessToken == UUID.fromString(ctx.arg(AccessTokenArg)))
      )
    )
  )

  //  val MutationType: ObjectType[GameContext, Unit] = ObjectType(
  //    name = "Mutation",
  //    fields[GameContext, Unit](
  //      Field(
  //        name = "createPlayer",
  //        fieldType = PlayerType,
  //        arguments = NicknameArg :: Nil,
  //        resolve = ctx => ctx.ctx.createPlayer(ctx.arg(NicknameArg))
  //      )
  //    )
  //  )

  //  val SubscriptionType = ObjectType(
  //    name = "Subscription",
  //    Field.subs(
  //
  //    )
  //  )

  val GameSchema: Schema[GameContext, Unit] = Schema(QueryType
    //    , Some(MutationType)
    //    , Some(SubscriptionType)
  )
}
