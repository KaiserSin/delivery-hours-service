package deliveryhoursservice.models

import deliveryhoursservice.error.ApiError
import deliveryhoursservice.error.ApiException

private const val DAY_SECONDS = 24 * 60 * 60L

data class TimeEntryDto(
    val open: SecondsOfDay? = null,
    val close: SecondsOfDay? = null,
)

data class OpeningHoursDto(
    val monday: List<TimeEntryDto> = emptyList(),
    val tuesday: List<TimeEntryDto> = emptyList(),
    val wednesday: List<TimeEntryDto> = emptyList(),
    val thursday: List<TimeEntryDto> = emptyList(),
    val friday: List<TimeEntryDto> = emptyList(),
    val saturday: List<TimeEntryDto> = emptyList(),
    val sunday: List<TimeEntryDto> = emptyList(),
)

fun OpeningHoursDto.validateOrThrow(): OpeningHoursDto {
    val days =
        listOf(
            "monday" to monday,
            "tuesday" to tuesday,
            "wednesday" to wednesday,
            "thursday" to thursday,
            "friday" to friday,
            "saturday" to saturday,
            "sunday" to sunday,
        )

    var expectingClose = false
    var pendingOpenDay: String? = null
    var pendingOpenIndex: Int? = null
    var leadingCloseDay: String? = null
    var leadingCloseIndex: Int? = null

    days.forEach { (dayName, entries) ->
        entries.forEachIndexed { index, entry ->
            val hasOpen = entry.open != null
            val hasClose = entry.close != null
            if (hasOpen == hasClose) {
                throw ApiException(
                    ApiError.Validation("[$dayName][$index]: exactly one of {open|close} must be set"),
                )
            }
            val value = (entry.open ?: entry.close)!!.value
            if (value !in 0..DAY_SECONDS) {
                throw ApiException(
                    ApiError.Validation("[$dayName][$index]: value $value is out of range 0..$DAY_SECONDS"),
                )
            }
            if (hasOpen) {
                if (expectingClose) {
                    throw ApiException(
                        ApiError.Validation("[$dayName][$index]: expected close entry, got open"),
                    )
                }
                expectingClose = true
                pendingOpenDay = dayName
                pendingOpenIndex = index
            } else {
                if (!expectingClose) {
                    if (leadingCloseDay != null) {
                        throw ApiException(
                            ApiError.Validation("[$dayName][$index]: expected open entry, got close"),
                        )
                    }
                    leadingCloseDay = dayName
                    leadingCloseIndex = index
                } else {
                    expectingClose = false
                    pendingOpenDay = null
                    pendingOpenIndex = null
                }
            }
        }
    }
    if (expectingClose) {
        if (leadingCloseDay == null) {
            throw ApiException(
                ApiError.Validation("[$pendingOpenDay][${pendingOpenIndex!!}]: open entry has no matching close in this week"),
            )
        }
    } else if (leadingCloseDay != null) {
        throw ApiException(
            ApiError.Validation("[$leadingCloseDay][${leadingCloseIndex!!}]: close entry has no preceding open in this week"),
        )
    }
    return this
}
