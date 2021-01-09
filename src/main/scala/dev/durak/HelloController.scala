package dev.durak

import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.{GetMapping, RestController}

@RestController
class HelloController {
  private val log = LoggerFactory.getLogger(classOf[HelloController])
  log.info("Slf4j logging works")

  @GetMapping(path = Array("/index"))
  def index: String = "Greetings from Durak!"
}
