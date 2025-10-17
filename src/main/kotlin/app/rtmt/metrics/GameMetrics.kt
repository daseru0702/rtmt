// src/main/kotlin/app/rtmt/metrics/GameMetrics.kt
package app.rtmt.metrics

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Component

@Component
class GameMetrics(registry: MeterRegistry) {
    private val apply = registry.counter("move.apply.count")
    fun incApply() = apply.increment()

    fun incNack(reason: String) {
        registry.counter("move.nack.count", "reason", reason).increment()
    }
}
