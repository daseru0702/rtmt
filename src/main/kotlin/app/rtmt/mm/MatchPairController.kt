package app.rtmt.mm

import app.rtmt.room.RoomService
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono
import reactor.kotlin.core.util.function.component1
import reactor.kotlin.core.util.function.component2

@RestController
@RequestMapping("/api/mm")
class MatchPairController(
    private val queue: MatchQueue,
    private val rooms: RoomService
) {
    data class PairResp(
        val matched: Boolean,
        val reason: String? = null,
        val roomId: Long? = null,
        val players: List<Long>? = null
    )

    @PostMapping("/match-pair/{region}")
    fun matchPair(@PathVariable region: String): Mono<PairResp> =
        queue.dequeue(region).zipWith(queue.dequeue(region))
            .flatMap { (a, b) ->
                if (a == null || b == null) {
                    // 한쪽이 비었으면 되돌려주고 종료(선택: 되돌림 구현)
                    return@flatMap Mono.just(PairResp(false, "not_enough_in_queue"))
                }
                val p1 = a.toLong()
                val p2 = b.toLong()
                rooms.create(region, p1, p2).map { rid ->
                    PairResp(true, roomId = rid, players = listOf(p1, p2))
                }
            }
            .defaultIfEmpty(PairResp(false, "not_enough_in_queue"))
}
