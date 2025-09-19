package app.rtmt.mm

import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/mm")
class MatchController(private val queue: MatchQueue) {
    data class EnqueueReq(val region: String, val userId: Long)

    @PostMapping("/enqueue")
    fun enqueue(@RequestBody req: EnqueueReq): Mono<Long> =
        queue.enqueue(req.region, req.userId)

    @PostMapping("/dequeue/{region}")
    fun dequeue(@PathVariable region: String): Mono<String> =
        queue.dequeue(region)

    @GetMapping("/size/{region}")
    fun size(@PathVariable region: String) = queue.size(region)
}
