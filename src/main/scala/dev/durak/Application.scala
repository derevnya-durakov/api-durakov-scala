package dev.durak

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.sse.ServerSentEvent
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport.jsonMarshaller
import dev.durak
import dev.durak.SangriaAkkaHttp._
import dev.durak.context.GameContext
import dev.durak.repo.{GameRepository, PlayerRepository}
import sangria.ast.OperationType
import sangria.execution.{ErrorWithResolver, QueryAnalysisError}
import sangria.marshalling.circe._
import sangria.slowlog.SlowLog
import monix.execution.Scheduler.Implicits.global

import scala.util.{Failure, Success}
//import argonaut._
//import argonaut.Argonaut._
//import info.macias.sse.EventTarget
//import ....HttpServerResponse
import sangria.ast._
import sangria.execution._
//import sangria.marshalling.argonaut.ArgonautResultMarshaller
import sangria.parser.QueryParser

import scala.util.{Failure, Success}

object Application extends App with CorsSupport {

  val route: Route =
    optionalHeaderValueByName("X-Apollo-Tracing") { tracing =>
      path("graphql") {
        graphQLPlayground ~
          prepareGraphQLRequest {
            case Success(GraphQLRequest(query, variables, operationName)) => {
              val middleware = if (tracing.isDefined) SlowLog.apolloTracing :: Nil else Nil
              val operation = query.operationType(operationName)
              operation match {
                case Some(OperationType.Subscription) => {
                  import sangria.streaming.monix._
                  import sangria.execution.ExecutionScheme.Stream

//                 var resp =
                   Executor.execute(
                    schema = durak.SchemaDefinition.GameSchema,
                    queryAst = query,
                    userContext = new GameContext(new GameRepository, new PlayerRepository),
                    variables = variables,
                    operationName = operationName,
                    middleware = middleware,
                    //                deferredResolver = deferredResolver
                  ).map(serverSentEvent(target, _)).subscribe()

//                  complete(resp) // todo remove
//                  resp

//                  Executor.execute(durak.SchemaDefinition.GameSchema, query, CatalogRepo(vertx), operationName = op, variables = vars)
//                    .map(serverSentEvent(target, _)).subscribe()
                }
                case _ => {
                  //            val deferredResolver = DeferredResolver.fetchers(SchemaDefinition.players)
                  val graphQLResponse = Executor.execute(
                    schema = durak.SchemaDefinition.GameSchema,
                    queryAst = query,
                    userContext = new GameContext(new GameRepository, new PlayerRepository),
                    variables = variables,
                    operationName = operationName,
                    middleware = middleware,
                    //                deferredResolver = deferredResolver
                  ).map(OK -> _)
                    .recover {
                      case error: QueryAnalysisError => BadRequest -> error.resolveError
                      case error: ErrorWithResolver => InternalServerError -> error.resolveError
                      //                 case error: RuntimeException =>
                    }
                  complete(graphQLResponse)
                }
              }
            }
            case Failure(preparationError) => complete(BadRequest, formatError(preparationError))
          }
      }
    } ~
      (get & pathEndOrSingleSlash) {
        redirect("/graphql", PermanentRedirect)
      }

  val PORT = sys.props.get("http.port").fold(8080)(_.toInt)
  val INTERFACE = "0.0.0.0"


  import akka.actor.ActorSystem
  implicit val system = ActorSystem("sangria-server")

  import system.dispatcher
  Http().newServerAt(INTERFACE, PORT).bindFlow(corsHandler(route))
}
