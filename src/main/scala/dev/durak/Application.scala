package dev.durak

import dev.durak.repo.CustomMongoRepositoryImpl
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

import java.util.concurrent.{ExecutorService, Executors}

@SpringBootApplication
@EnableMongoRepositories(repositoryBaseClass = classOf[CustomMongoRepositoryImpl[AnyRef]])
class Application {
  @Bean def executor: ExecutorService = Executors.newSingleThreadExecutor
}

object Application extends App {
  SpringApplication.run(classOf[Application], args: _*)
}
