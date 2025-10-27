package app.rtmt.ws

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

object GameLogic {
    private val mapper = jacksonObjectMapper()

    data class Win(val winnerUserId: Long?)

    fun checkWin(stateJson: String, p1Id: Long, p2Id: Long): Win {
        val root: JsonNode = mapper.readTree(stateJson)
        val pawns = root.path("pawns")
        val p1 = pawns.path("P1")  // [col,row]
        val p2 = pawns.path("P2")
        val p1Row = p1.get(1)?.asInt() ?: -1
        val p2Row = p2.get(1)?.asInt() ?: -1

        return when {
            p1Row == 8 -> Win(p1Id)
            p2Row == 0 -> Win(p2Id)
            else -> Win(null)
        }
    }
}
