package app.rtmt.room

import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/matches")
class RoomMatchController(private val db: DatabaseClient) {
    data class EndReq(val winner: Long)

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long) =
        db.sql("SELECT id, room_id, p1, p2, winner, started_at, ended_at, game_state FROM matches WHERE id=:id")
            .bind("id", id).map { r,_ ->
                mapOf(
                    "id" to r.get("id", java.lang.Long::class.java)?.toLong(),
                    "roomId" to r.get("room_id", java.lang.Long::class.java)?.toLong(),
                    "p1" to r.get("p1", java.lang.Long::class.java)?.toLong(),
                    "p2" to r.get("p2", java.lang.Long::class.java)?.toLong(),
                    "winner" to r.get("winner", java.lang.Long::class.java)?.toLong(),
                    "startedAt" to r.get("started_at", java.time.LocalDateTime::class.java),
                    "endedAt" to r.get("ended_at", java.time.LocalDateTime::class.java),
                    "gameState" to r.get("game_state", String::class.java)
                )
            }.one()

    @PostMapping("/{id}/end")
    fun end(@PathVariable id: Long, @RequestBody req: EndReq): Mono<Long?> =
        db.sql("UPDATE matches SET winner=:w, ended_at=NOW() WHERE id=:id")
            .bind("w", req.winner).bind("id", id)
            .fetch().rowsUpdated()
}