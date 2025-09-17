package deliveryhoursservice.models

import deliveryhoursservice.error.ApiError
import deliveryhoursservice.error.ApiException

private const val DAY_SECONDS = 24 * 60 * 60L

data class TimeEntryDto(
    val open: Long = -1,
    val close: Long = -1
)


data class OpeningHoursDto(
    val monday: List<TimeEntryDto> = emptyList(),
    val tuesday: List<TimeEntryDto> = emptyList(),
    val wednesday: List<TimeEntryDto> = emptyList(),
    val thursday: List<TimeEntryDto> = emptyList(),
    val friday: List<TimeEntryDto> = emptyList(),
    val saturday: List<TimeEntryDto> = emptyList(),
    val sunday: List<TimeEntryDto> = emptyList()
)

fun OpeningHoursDto.validateOrThrow(): OpeningHoursDto {
    fun validateDay(dayName: String, entries: List<TimeEntryDto>) {
        entries.forEachIndexed { index, entry ->
            val hasOpen = entry.open >= 0
            val hasClose = entry.close >= 0
            if (hasOpen == hasClose) {
                throw ApiException(
                    ApiError.Validation("[$dayName][$index]: exactly one of {open|close} must be set")
                )
            }

            val value = if (hasOpen) entry.open else entry.close
            if (value !in 0..DAY_SECONDS) {
                throw ApiException(
                    ApiError.Validation("[$dayName][$index]: value $value is out of range 0..$DAY_SECONDS")
                )
            }
        }
    }

    validateDay("monday", monday)
    validateDay("tuesday", tuesday)
    validateDay("wednesday", wednesday)
    validateDay("thursday", thursday)
    validateDay("friday", friday)
    validateDay("saturday", saturday)
    validateDay("sunday", sunday)
    return this
}