// src/main/kotlin/app/rtmt/ws/GameWs.kt
package app.rtmt.ws

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap

@Configuration
class GameWsConfig(private val db: DatabaseClient) {

    @Bean fun wsAdapter() = WebSocketHandlerAdapter()
    @Bean(name = ["gameWsMapping"])
    fun gameWsMapping(): SimpleUrlHandlerMapping =
        SimpleUrlHandlerMapping(mapOf("/ws/game" to handler()), 1)

    private val rooms = ConcurrentHashMap<Long, MutableSet<WebSocketSession>>()
    private val mapper = jacksonObjectMapper()
    private val repo = GameRepo(db)

    @Bean
    fun handler(): WebSocketHandler = WebSocketHandler { s ->
        val rid = s.handshakeInfo.uri.query?.substringAfter("roomId=")?.toLongOrNull()
            ?: return@WebSocketHandler s.close()
        rooms.computeIfAbsent(rid) { mutableSetOf() }.add(s)

        val sendCurrent = repo.loadState(rid)
            .defaultIfEmpty("{}")
            .flatMap { js -> s.send(Mono.just(s.textMessage("""{"t":"state","state":$js}"""))).then() }

        val receive = s.receive()
            .map { it.payloadAsText }
            .flatMap { txt ->
                val msg = runCatching { mapper.readValue<MoveMsg>(txt) }.getOrNull()
                if (msg == null) {
                    return@flatMap s.send(Mono.just(s.textMessage("""{"t":"nack","reason":"bad_format"}"""))).then(Mono.empty())
                }

                val to = msg.payload.to
                if (to.size != 2 || to.any { it !in 0..8 }) {
                    val nack = mapper.writeValueAsString(Nack(reason = "out_of_board"))
                    return@flatMap s.send(Mono.just(s.textMessage(nack))).then(Mono.empty())
                }

                // 아래부터는 중첩 flatMap을 사용하되, 바깥 flatMap에 대한 조기 리턴은 더 이상 쓰지 않음.
                repo.findRole(rid, msg.userId).flatMap { role ->
                    if (role != "P1" && role != "P2") {
                        val nack = mapper.writeValueAsString(Nack(reason = "no_role"))
                        return@flatMap s.send(Mono.just(s.textMessage(nack))).then(Mono.empty())
                    }

                    repo.applyMove(rid, msg.seq, role, to[0], to[1]).flatMap { updated ->
                        if (updated == 0.toLong()) {
                            repo.loadState(rid).flatMap { js ->
                                val expect = runCatching { mapper.readTree(js).path("lastSeq").asLong(0L) + 1 }.getOrNull()
                                val nack = mapper.writeValueAsString(Nack(reason = "conflict_or_turn", expectSeq = expect))
                                s.send(Mono.just(s.textMessage(nack))).then()
                            }
                        } else {
                            repo.loadState(rid).flatMap { js ->
                                val targets = rooms[rid]?.toList().orEmpty()
                                Mono.`when`(
                                    targets.map { c ->
                                        c.send(Mono.just(c.textMessage("""{"t":"state","state":$js}"""))).then()
                                    }
                                )
                            }
                        }
                    }
                }
            }
            .then()


        sendCurrent.then(receive)
            .doFinally {
                rooms[rid]?.remove(s)
                if (rooms[rid]?.isEmpty() == true) rooms.remove(rid)
            }
    }
}
