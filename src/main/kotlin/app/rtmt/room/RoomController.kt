// src/main/kotlin/app/rtmt/room/RoomController.kt
package app.rtmt.room

import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/rooms")
class RoomController(private val rooms: RoomService) {
    @GetMapping("/{id}")
    fun get(@PathVariable id: Long) = rooms.findRoom(id)
}

@RestController
@RequestMapping("/api/matches")
class RoomMatchController(private val db: DatabaseClient) {
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
}