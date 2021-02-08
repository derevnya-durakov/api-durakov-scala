package dev.durak.exceptions

import graphql.language.SourceLocation
import graphql.{ErrorClassification, GraphQLError}

import java.util

class GameException(message: String) extends RuntimeException(message) with GraphQLError {
  override def getMessage: String = super.getMessage

  override def getLocations: util.List[SourceLocation] = null

  override def getErrorType: ErrorClassification = null
}
