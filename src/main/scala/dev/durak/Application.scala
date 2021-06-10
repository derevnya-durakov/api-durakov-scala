package dev.durak

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean

import java.util.concurrent.{ExecutorService, Executors}

@SpringBootApplication
class Application {
  @Bean def executor: ExecutorService = Executors.newSingleThreadExecutor
}

object Application extends App {
  SpringApplication.run(classOf[Application], args: _*)
}
