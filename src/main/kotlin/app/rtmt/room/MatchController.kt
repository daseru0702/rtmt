// src/main/kotlin/app/rtmt/room/MatchController.kt
package app.rtmt.room

import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/matches")
class MatchController(private val db: DatabaseClient) {
    data class EndReq(val winner: Long)

    @PostMapping("/{id}/end")
    fun end(@PathVariable id: Long, @RequestBody req: EndReq): Mono<Long?> =
        db.sql("UPDATE matches SET winner=:w, ended_at=NOW() WHERE id=:id")
            .bind("w", req.winner).bind("id", id)
            .fetch().rowsUpdated()
}
