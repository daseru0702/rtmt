// src/main/kotlin/app/rtmt/metrics/GameMetrics.kt
package app.rtmt.metrics

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class GameMetrics(
    private val registry: MeterRegistry   // ← 프로퍼티로 보관
) {
    private val apply = registry.counter("move.apply.count")
    private val ended = registry.counter("match.end.count")

    fun incApply() {
        apply.increment()
    }

    fun incEnded() {
        ended.increment()
    }
    fun incNack(reason: String) {
        registry.counter("move.nack.count", "reason", reason).increment()
    }
}
