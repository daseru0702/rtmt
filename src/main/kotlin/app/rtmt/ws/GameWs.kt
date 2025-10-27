// src/main/kotlin/app/rtmt/ws/GameWs.kt
package app.rtmt.ws

import app.rtmt.metrics.GameMetrics
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
class GameWsConfig(private val db: DatabaseClient,
                   private val metrics: GameMetrics) {

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
                    metrics.incNack(NackReason.BAD_FORMAT.value)
                    return@flatMap Nacks.send(s, NackReason.BAD_FORMAT)
                }

                // (2) 보드 범위 체크
                val to = msg.payload.to
                if (to.size != 2 || to.any { it !in 0..8 }) {
                    metrics.incNack(NackReason.OUT_OF_BOARD.value)
                    return@flatMap Nacks.send(s, NackReason.OUT_OF_BOARD)
                }

                // (3) 역할 조회
                repo.findRole(rid, msg.userId).flatMap { role ->
                    if (role != "P1" && role != "P2") {
                        metrics.incNack(NackReason.TURN_MISMATCH.value)
                        return@flatMap Nacks.send(s, NackReason.TURN_MISMATCH) // 역할 미존재도 턴 불일치로 간주
                    }

                    // (4) 낙관적 락 적용
                    repo.applyMove(rid, msg.seq, role, to[0], to[1]).flatMap { updated ->
                        if (updated == 0.toLong()) {
                            metrics.incNack(NackReason.CONFLICT.value)
                            repo.loadState(rid).flatMap { js ->
                                val expect = runCatching { mapper.readTree(js).path("lastSeq").asLong(0L) + 1 }.getOrNull()
                                Nacks.send(s, NackReason.CONFLICT, expectSeq = expect)
                            }
                        } else {
                            metrics.incApply()
                            // 1) 최신 상태 로드
                            repo.loadState(rid).flatMap { js ->
                                val targets = rooms[rid]?.toList().orEmpty()

                                // 2) 상태 브로드캐스트
                                val stateMsg = """{"t":"state","state":$js}"""
                                val fanout = Mono.`when`(targets.map { c ->
                                    c.send(Mono.just(c.textMessage(stateMsg))).then()
                                })

                                // 3) 간이 승리 판정 (p1/p2 유저 id 가져오기)
                                //    room_members에서 p1/p2를 들고 왔다면 그 값을 사용하고,
                                //    없다면 (데모 기준) matches 테이블에서 p1/p2를 꺼내오는 함수 하나 추가해도 됨.
                                //    여기서는 간단히 repo에 getPlayersByRoom을 추가했다고 가정.
                                fanout.then(
                                    repo.getPlayersByRoom(rid).flatMap { (p1Id, p2Id) ->
                                        val win = GameLogic.checkWin(js, p1Id, p2Id)
                                        if (win.winnerUserId == null) Mono.empty()
                                        else {
                                            // 4) DB 종료 마킹 (idempotent)
                                            repo.endMatchByRoom(rid, win.winnerUserId).flatMap {
                                                metrics.incEnded()
                                                val endedJson = mapper.writeValueAsString(Ended(winner = win.winnerUserId))
                                                Mono.`when`(targets.map { c ->
                                                    c.send(Mono.just(c.textMessage(endedJson))).then()
                                                })
                                            }
                                        }
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
