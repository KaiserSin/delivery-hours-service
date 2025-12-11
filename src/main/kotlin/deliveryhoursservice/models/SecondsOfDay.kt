package deliveryhoursservice.models

import java.time.Duration

@JvmInline
value class SecondsOfDay(val value: Long) {
    fun toDuration(): Duration = Duration.ofSeconds(value)
}
