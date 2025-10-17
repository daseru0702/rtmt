// src/main/kotlin/app/rtmt/ws/Msgs.kt
package app.rtmt.ws

data class MoveMsg(
    val t: String,           // "move"
    val seq: Long,           // 클라가 기대하는 next seq
    val userId: Long,
    val payload: MovePayload
)
data class MovePayload(val to: List<Int>) // [row,col]
data class Nack(val t: String = "nack", val reason: String, val expectSeq: Long? = null)
