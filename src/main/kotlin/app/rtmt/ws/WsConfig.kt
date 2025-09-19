package app.rtmt.ws

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import reactor.core.publisher.Mono

@Configuration
class WsConfig {
    @Bean fun adapter() = WebSocketHandlerAdapter()

    @Bean
    fun mapping(echo: WebSocketHandler) =
        SimpleUrlHandlerMapping(mapOf("/ws/echo" to echo), 1)

    @Bean
    fun echo(): WebSocketHandler = WebSocketHandler { session ->
        session.receive()
            .map { it.payloadAsText }
            .flatMap { txt -> session.send(Mono.just(session.textMessage("echo:$txt"))) }
            .then()
    }
}
