package graphql.schemas

import akka.stream.Materializer
import com.google.inject.Inject
import graphql.UserContext
import graphql.resolvers.PlayerResolver
import models.{Player, PlayerEvent}
import sangria.macros.derive.{ObjectTypeName, deriveObjectType}
import sangria.schema._
import sangria.streaming.akkaStreams._
import services.PubSubService

import java.util.UUID
import scala.concurrent.ExecutionContext

/**
  * Contains the definitions of all query, mutations and subscriptions
  * that work with the entity 'Post'. Also it is a construction element
  * for the build graphql schema of the entire application.
  *
  * @param playerResolver  an object containing all resolve functions to work with the entity of 'Post'
  * @param pubSubService an instance of an implementation of PubSubService which is used to publish events
  *                      or subscribe to some mutations
  * @param ec            execute program logic asynchronously, typically but not necessarily on a thread pool
  * @param mat           an instance of an implementation of Materializer SPI (Service Provider Interface)
  */
class GameSchema @Inject()(playerResolver: PlayerResolver,
                           pubSubService: PubSubService[PlayerEvent])
                          (implicit ec: ExecutionContext, mat: Materializer) {

  /**
    * Convert a Post object to a Sangria graphql object.
    * Sangria macros deriveObjectType creates an ObjectType with fields found in the Post entity.
    */
//  implicit val PlayerType: ObjectType[Unit, Post] = deriveObjectType[Unit, Post](ObjectTypeName("Player"))


implicit val PlayerType: ObjectType[Unit, Player] = ObjectType(
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

  implicit val PlayerEventType: ObjectType[Unit, PlayerEvent] = deriveObjectType[Unit, PlayerEvent](ObjectTypeName("PlayerEvent"))

  /**
    * Enumeration with names for GraphQL fields of queries, mutations, and subscriptions
    */
  object FieldNames extends Enumeration {
    val players: Value = Value("players")
    val addPlayer: Value = Value("addPlayer")
    val findPlayer: Value = Value("findPlayer")
//    val deletePlayer: Value = Value("deletePost")
//    val editPlayer: Value = Value("editPost")
    val accessToken: Value = Value("accessToken")
    val auth: Value = Value("auth")
    val playersUpdated: Value = Value("playersUpdated")

    implicit def valueToString(value: Value): String = value.toString
  }

  import FieldNames._

  val IdArg: Argument[String] = Argument("id", IDType)
  val NicknameArg: Argument[String] = Argument("nickname", StringType)
  val AccessTokenArg: Argument[String] = Argument("accessToken", StringType)

  /**
    * List of queries to work with the entity of Post
    */
  val Queries: List[Field[UserContext, Unit]] = List(
    Field(
      name = players,
      fieldType = ListType(PlayerType),
      resolve = _ => playerResolver.players
    ),
    Field(
      name = findPlayer,
      fieldType = OptionType(PlayerType),
      arguments = List(IdArg),
      resolve = sangriaContext => playerResolver.findPlayer(sangriaContext.arg(IdArg))
    ),
    Field(
      name = accessToken,
      fieldType = OptionType(StringType),
      arguments = List(NicknameArg),
      resolve = sangriaContext =>
        playerResolver.accessTokenByNickname(sangriaContext.arg(NicknameArg))
    ),
    Field(
      name = auth,
      fieldType = OptionType(AuthType),
      arguments = List(AccessTokenArg),
      resolve = sangriaContext =>
        playerResolver.playerByAccessToken(sangriaContext.arg(AccessTokenArg))
    )
  )

  /**
    * List of mutations to work with the entity of Post.
    */
  val Mutations: List[Field[UserContext, Unit]] = List(
    Field(
      name = addPlayer,
      fieldType = PlayerType,
      arguments = List(NicknameArg),
      resolve = sangriaContext =>
        playerResolver.addPlayer(
          sangriaContext.arg(NicknameArg)
        ).map {
          createdPlayer =>
            pubSubService.publish(PlayerEvent(addPlayer, createdPlayer))
            createdPlayer
        }
    )
//    ,
//    Field(
//      name = editPlayer,
//      fieldType = PostType,
//      arguments = List(
//        Argument("id", LongType),
//        Argument("title", StringType),
//        Argument("content", StringType)
//      ),
//      resolve = sangriaContext =>
//        playerResolver.updatePost(
//          Post(
//            Some(sangriaContext.args.arg[Long]("id")),
//            sangriaContext.args.arg[String]("title"),
//            sangriaContext.args.arg[String]("content")
//          )
//        ).map {
//          updatedPost =>
//            pubSubService.publish(PostEvent(editPlayer, updatedPost))
//            updatedPost
//        }
//    ),
//    Field(
//      name = deletePlayer,
//      fieldType = OptionType(PostType),
//      arguments = List(
//        Argument("id", LongType)
//      ),
//      resolve = sangriaContext => {
//        val postId = sangriaContext.args.arg[Long]("id")
//        playerResolver.deletePost(postId)
//          .map {
//            maybeDeletedPost =>
//              maybeDeletedPost.foreach(deletedPost => pubSubService.publish(PostEvent(deletePlayer, deletedPost)))
//              maybeDeletedPost
//          }
//      }
//    )
  )

  /**
    * List of subscriptions to work with the entity of Post.
    */
  val Subscriptions: List[Field[UserContext, Unit]] = List(
    Field.subs(
      name = playersUpdated,
      fieldType = PlayerEventType,
      resolve = sangriaContext => {
        pubSubService.subscribe(Seq(addPlayer
//          , deletePlayer, editPlayer
        ))(sangriaContext.ctx)
      }
    )
  )
}
