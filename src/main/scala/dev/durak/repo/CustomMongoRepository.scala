package dev.durak.repo

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
trait CustomMongoRepository[T] extends MongoRepository[T, Long]
