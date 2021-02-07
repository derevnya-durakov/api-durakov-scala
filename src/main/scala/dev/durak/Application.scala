package dev.durak

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer
import org.springframework.context.annotation.Bean
import org.springframework.jms.annotation.EnableJms
import org.springframework.jms.config.{DefaultJmsListenerContainerFactory, JmsListenerContainerFactory}
import org.springframework.jms.support.converter.{MappingJackson2MessageConverter, MessageType}

import java.util.concurrent.{ExecutorService, Executors}
import javax.jms.ConnectionFactory

@SpringBootApplication
@EnableJms
class Application {
  @Bean def executor: ExecutorService = Executors.newSingleThreadExecutor

  @Bean
  def myFactory(@Qualifier("jmsConnectionFactory") connectionFactory: ConnectionFactory,
                configurer: DefaultJmsListenerContainerFactoryConfigurer): JmsListenerContainerFactory[_] = {
    val factory = new DefaultJmsListenerContainerFactory
    // This provides all boot's default to this factory, including the message converter
    configurer.configure(factory, connectionFactory)
    // You could still override some of Boot's default if necessary.
    factory
  }

//  @Bean
//  def addJacksonScalaModule(): DefaultScalaModule = DefaultScalaModule

  @Bean
  def jacksonJmsMessageConverter
//  (objectMapper: ObjectMapper)
  : MappingJackson2MessageConverter = {
    val mapper = JsonMapper.builder()
      .addModule(DefaultScalaModule)
      .build()

    val converter = new MappingJackson2MessageConverter
    converter.setTargetType(MessageType.TEXT)
    converter.setTypeIdPropertyName("_type")
    converter.setObjectMapper(mapper)
    converter
  }

  //  @Bean
  //  def createSchema: GraphQLSchema = {
  //    GraphQLSchema.newSchema()
  //      .build()
  //  }
}

object Application extends App {
  SpringApplication.run(classOf[Application], args: _*)
}
