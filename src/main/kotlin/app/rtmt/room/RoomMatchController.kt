package app.rtmt.room

import org.springframework.r2dbc.core.DatabaseClient
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

    @PostMapping("/{id}/end")
    fun end(@PathVariable id: Long, @RequestBody req: EndReq): Mono<Long?> =
        db.sql("UPDATE matches SET winner=:w, ended_at=NOW() WHERE id=:id")
            .bind("w", req.winner).bind("id", id)
            .fetch().rowsUpdated()
}