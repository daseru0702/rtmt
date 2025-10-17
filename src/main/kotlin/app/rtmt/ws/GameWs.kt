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
                // (1) 파싱 실패
                val msg = runCatching { mapper.readValue<MoveMsg>(txt) }.getOrNull()
                if (msg == null) {
                    return@flatMap Nacks.send(s, NackReason.BAD_FORMAT)
                }

                // (2) 보드 범위 체크
                val to = msg.payload.to
                if (to.size != 2 || to.any { it !in 0..8 }) {
                    return@flatMap Nacks.send(s, NackReason.OUT_OF_BOARD)
                }

                // (3) 역할 조회
                repo.findRole(rid, msg.userId).flatMap { role ->
                    if (role != "P1" && role != "P2") {
                        return@flatMap Nacks.send(s, NackReason.TURN_MISMATCH) // 역할 미존재도 턴 불일치로 간주
                    }

                    // (4) 낙관적 락 적용
                    repo.applyMove(rid, msg.seq, role, to[0], to[1]).flatMap { updated ->
                        if (updated == 0.toLong()) {
                            repo.loadState(rid).flatMap { js ->
                                val expect = runCatching { mapper.readTree(js).path("lastSeq").asLong(0L) + 1 }.getOrNull()
                                Nacks.send(s, NackReason.CONFLICT, expectSeq = expect)
                            }
                        } else {
                            // (성공) 최신 상태 브로드캐스트
                            repo.loadState(rid).flatMap { js ->
                                val targets = rooms[rid]?.toList().orEmpty()
                                Mono.`when`(targets.map { c ->
                                    c.send(Mono.just(c.textMessage("""{"t":"state","state":$js}"""))).then()
                                })
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
