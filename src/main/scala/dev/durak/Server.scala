package dev.durak

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import de.heikoseeberger.akkahttpcirce.ErrorAccumulatingCirceSupport.jsonMarshaller
import dev.durak.SangriaAkkaHttp._
import dev.durak.context.GameContext
import dev.durak.repo.{GameRepository, PlayerRepository}
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.marshalling.circe._
import sangria.slowlog.SlowLog

import scala.util.{Failure, Success}

object Server extends App with CorsSupport {
  implicit val system = ActorSystem("sangria-server")

  import system.dispatcher

  val route: Route =
    optionalHeaderValueByName("X-Apollo-Tracing") { tracing =>
      path("graphql") {
        graphQLPlayground ~
          prepareGraphQLRequest {
            case Success(GraphQLRequest(query, variables, operationName)) => {
              val middleware = if (tracing.isDefined) SlowLog.apolloTracing :: Nil else Nil
              //            val deferredResolver = DeferredResolver.fetchers(SchemaDefinition.players)
              val graphQLResponse = Executor.execute(
                schema = SchemaDefinition.GameSchema,
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
            case Failure(preparationError) => complete(BadRequest, formatError(preparationError))
          }
      }
    } ~
      (get & pathEndOrSingleSlash) {
        redirect("/graphql", PermanentRedirect)
      }

  val PORT = sys.props.get("http.port").fold(8080)(_.toInt)
  val INTERFACE = "0.0.0.0"
  Http().newServerAt(INTERFACE, PORT).bindFlow(corsHandler(route))
}
