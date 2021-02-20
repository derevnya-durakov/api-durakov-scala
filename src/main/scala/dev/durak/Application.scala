package dev.durak

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean

import java.util.concurrent.{ExecutorService, Executors}

@SpringBootApplication
class Application {
  @Bean def executor: ExecutorService = Executors.newSingleThreadExecutor

//  @Bean
//  def jacksonJmsMessageConverter: MappingJackson2MessageConverter = {
//    val mapper = JsonMapper.builder()
//      .addModule(DefaultScalaModule)
//      .build()
//
//    val converter = new MappingJackson2MessageConverter
//    converter.setTargetType(MessageType.TEXT)
//    converter.setTypeIdPropertyName("_type")
//    converter.setObjectMapper(mapper)
//    converter
//  }
}

object Application extends App {
  SpringApplication.run(classOf[Application], args: _*)
}
