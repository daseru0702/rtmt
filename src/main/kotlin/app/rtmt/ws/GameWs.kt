// src/main/kotlin/app/rtmt/ws/GameWs.kt
package app.rtmt.ws

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

    @Bean
    fun gameMapping(): SimpleUrlHandlerMapping =
        SimpleUrlHandlerMapping(mapOf("/ws/game" to gameHandler()), 1)

    private val rooms = ConcurrentHashMap<Long, MutableSet<WebSocketSession>>()

    @Bean
    fun gameHandler(): WebSocketHandler = WebSocketHandler { session ->
        val rid = session.handshakeInfo.uri.query
            ?.substringAfter("roomId=")?.toLongOrNull()
            ?: return@WebSocketHandler session.close()

        rooms.computeIfAbsent(rid) { mutableSetOf() }.add(session)

        // 1) 접속 즉시 현재 상태 1회 송신
        val sendCurrent = db.sql("SELECT game_state FROM matches WHERE room_id=:rid")
            .bind("rid", rid)
            .map { row, _ -> row.get("game_state", String::class.java) ?: "{}" }
            .one()
            .defaultIfEmpty("{}")
            .flatMap { json ->
                session.send(Mono.just(session.textMessage("""{"t":"state","state":$json}"""))).then()
            }

        // 2) 수신 → lastSeq 증가(아주 단순한 더미 move), 전체 브로드캐스트
        val receive = session.receive()
            .map { it.payloadAsText }
            .flatMap {
                db.sql("""
                    UPDATE matches 
                       SET game_state = JSON_SET(
                              COALESCE(game_state, JSON_OBJECT()),
                              '$.lastSeq',
                              COALESCE(JSON_EXTRACT(game_state,'$.lastSeq'),0)+1
                           )
                    WHERE room_id=:rid
                """.trimIndent())
                    .bind("rid", rid)
                    .fetch().rowsUpdated()
                    .flatMap {
                        db.sql("SELECT game_state FROM matches WHERE room_id=:rid")
                            .bind("rid", rid)
                            .map { r, _ -> r.get("game_state", String::class.java) ?: "{}" }
                            .one()
                    }
                    .flatMap { newState ->
                        val targets = rooms[rid]?.toList().orEmpty()
                        Mono.`when`(
                            targets.map { s ->
                                s.send(Mono.just(s.textMessage("""{"t":"state","state":$newState}"""))).then()
                            }
                        )
                    }
            }
            .then()

        sendCurrent.then(receive)
            .doFinally {
                rooms[rid]?.remove(session)
                if (rooms[rid]?.isEmpty() == true) rooms.remove(rid)
            }
    }
}
