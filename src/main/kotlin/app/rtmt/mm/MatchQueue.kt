package app.rtmt.mm

import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.time.Duration

@Component
class MatchQueue(private val redis: ReactiveStringRedisTemplate) {
    private fun key(region: String) = "mm:queue:$region"
    // 왼쪽 push, 오른쪽 pop (FIFO)
    fun enqueue(region: String, userId: Long): Mono<Long> =
        redis.opsForList().leftPush(key(region), userId.toString())

    fun dequeue(region: String): Mono<String> =
        redis.opsForList().rightPop(key(region))

    fun size(region: String): Mono<Long> =
        redis.opsForList().size(key(region))

    // 블로킹 pop (타임아웃)
    fun bpop(region: String, timeout: Duration = Duration.ofSeconds(5)) =
        redis.opsForList().rightPop(key(region), timeout)
}
