// src/main/kotlin/app/rtmt/ws/NackSender.kt
package app.rtmt.ws

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.web.reactive.socket.WebSocketSession
import reactor.core.publisher.Mono

object Nacks {
    private val mapper = jacksonObjectMapper()

    fun send(session: WebSocketSession, reason: NackReason, expectSeq: Long? = null): Mono<Void> {
        val json = mapper.writeValueAsString(Nack(reason = reason, expectSeq = expectSeq))
        return session.send(Mono.just(session.textMessage(json))).then()
    }
}
