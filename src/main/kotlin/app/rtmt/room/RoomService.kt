// src/main/kotlin/app/rtmt/room/RoomService.kt
package app.rtmt.room

import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.LocalDateTime

data class RoomView(
    val id: Long,
    val region: String,
    val status: String,
    val createdAt: LocalDateTime,
)

@Service
class RoomService(private val db: DatabaseClient) {

    fun create(region: String, p1: Long, p2: Long): Mono<Long> =
        db.sql("INSERT INTO rooms(region) VALUES(:region)")
            .bind("region", region)
            .filter { st -> st.returnGeneratedValues("id") }
            .map { row, _ -> row.get("id", java.lang.Long::class.java)!!.toLong() }
            .one()
            .flatMap { roomId ->
                val ins1 = db.sql("INSERT INTO room_members(room_id,user_id,role) VALUES(:rid,:uid,'P1')")
                    .bind("rid", roomId).bind("uid", p1).fetch().rowsUpdated()
                val ins2 = db.sql("INSERT INTO room_members(room_id,user_id,role) VALUES(:rid,:uid,'P2')")
                    .bind("rid", roomId).bind("uid", p2).fetch().rowsUpdated()
                val ins3 = db.sql("INSERT INTO matches(room_id,p1,p2,started_at) VALUES(:rid,:p1,:p2,NOW())")
                    .bind("rid", roomId).bind("p1", p1).bind("p2", p2).fetch().rowsUpdated()

                reactor.core.publisher.Mono.`when`(ins1, ins2, ins3).thenReturn(roomId)
            }

    fun findRoom(roomId: Long): Mono<RoomView> =
        db.sql("SELECT id, region, status, created_at FROM rooms WHERE id=:rid")
            .bind("rid", roomId)
            .map { row, _ ->
                RoomView(
                    id        = row.get("id", java.lang.Long::class.java)!!.toLong(),
                    region    = row.get("region", String::class.java)!!,
                    status    = row.get("status", String::class.java)!!,
                    createdAt = row.get("created_at", LocalDateTime::class.java)!!,
                )
            }
            .one()
}
