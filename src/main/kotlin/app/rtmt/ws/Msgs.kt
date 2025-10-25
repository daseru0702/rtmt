// src/main/kotlin/app/rtmt/ws/Msgs.kt
package app.rtmt.ws

import com.fasterxml.jackson.annotation.JsonValue

data class MoveMsg(
    val t: String,
    val seq: Long,
    val userId: Long,
    val payload: MovePayload
)

enum class NackReason(@JsonValue val value: String) {
    BAD_FORMAT("bad_format"),
    TURN_MISMATCH("turn_mismatch"),
    OUT_OF_BOARD("out_of_board"),
    ILLEGAL_MOVE("illegal_move"),
    OCCUPIED("occupied"),
    CONFLICT("conflict"),
    RATE_LIMITED("rate_limited")
}

data class MovePayload(val to: List<Int>) // [row,col]
data class Nack(
    val t: String = "nack",
    val reason: NackReason,
    val expectSeq:
    Long? = null
)

data class Ended(val t: String = "ended", val winner: Long)