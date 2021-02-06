package graphql

import com.google.inject.Inject
import graphql.schemas.GameSchema
import sangria.schema.{ObjectType, fields}

/**
  * Base component for the GraphQL schema.
  *
  * @param gameSchema an object containing all queries, mutations and subscriptions to work with the Post entity
  */
class GraphQL @Inject()(gameSchema: GameSchema) {

  /**
    * Contains a graphql schema of the entire application.
    * We can add queries, mutations, etc. for each model.
    */
  val Schema = sangria.schema.Schema(
    query = ObjectType(
      "Query",
      fields(
        gameSchema.Queries: _*
      )
    ),
    mutation = Some(
      ObjectType(
        "Mutation",
        fields(
          gameSchema.Mutations: _*
        )
      )
    ),
    subscription = Some(
      ObjectType(
        "Subscription",
        fields(
          gameSchema.Subscriptions: _*
        )
      )
    )
  )
}