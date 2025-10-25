// src/main/kotlin/app/rtmt/ws/GameRepo.kt
package app.rtmt.ws

import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Mono

class GameRepo(private val db: DatabaseClient) {
    fun findRole(roomId: Long, userId: Long): Mono<String> =
        db.sql("SELECT role FROM room_members WHERE room_id=:r AND user_id=:u")
            .bind("r", roomId).bind("u", userId)
            .map { r,_ -> r.get("role", String::class.java) ?: "" }
            .one()

    fun loadState(roomId: Long): Mono<String> =
        db.sql("SELECT game_state FROM matches WHERE room_id=:r")
            .bind("r", roomId)
            .map { r,_ -> r.get("game_state", String::class.java) ?: "{}" }
            .one()
    fun endMatchByRoom(roomId: Long, winnerUserId: Long): Mono<Long?> =
        db.sql("""
            UPDATE matches 
               SET winner = :winner, ended_at = NOW() 
             WHERE room_id = :rid AND winner IS NULL
        """.trimIndent())
            .bind("winner", winnerUserId)
            .bind("rid", roomId)
            .fetch()
            .rowsUpdated()

    /**
     * 낙관적 락으로 상태 갱신: lastSeq가 (seq-1)이고 turn이 role과 일치할 때만 갱신
     */
    fun applyMove(roomId: Long, seq: Long, role: String, toRow: Int, toCol: Int) =
        db.sql(
            """
            UPDATE matches
               SET game_state = JSON_SET(
                     game_state,
                     '$.pawns."$role"',        JSON_ARRAY(:r,:c),
                     '$.turn',                  IF('$role'='P1','P2','P1'),
                     '$.lastSeq',               :seq
                   )
             WHERE room_id=:rid
               AND JSON_EXTRACT(game_state,'$.lastSeq') = :prev
               AND JSON_EXTRACT(game_state,'$.turn')    = :role
            """.trimIndent()
        )
            .bind("rid", roomId).bind("seq", seq).bind("prev", seq - 1)
            .bind("role", role).bind("r", toRow).bind("c", toCol)
            .fetch().rowsUpdated()
}
