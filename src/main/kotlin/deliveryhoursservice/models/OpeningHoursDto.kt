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

    var sequenceState = SequenceState()

    days.forEach { (dayName, entries) ->
        entries.forEachIndexed { index, entry ->
            val kind = normalizeEntry(dayName, index, entry)
            sequenceState = sequenceState.consume(dayName, index, kind)
        }
    }
    sequenceState.validateWeekEnd()
    return this
}

private enum class EntryKind { OPEN, CLOSE }

private data class Position(val day: String, val index: Int)

private data class SequenceState(
    val expectingClose: Boolean = false,
    val pendingOpen: Position? = null,
    val leadingClose: Position? = null,
) {
    fun consume(day: String, index: Int, kind: EntryKind): SequenceState =
        when (kind) {
            EntryKind.OPEN -> handleOpen(day, index)
            EntryKind.CLOSE -> handleClose(day, index)
        }

    private fun handleOpen(day: String, index: Int): SequenceState {
        if (expectingClose) {
            throw ApiException(ApiError.Validation("[$day][$index]: expected close entry, got open"))
        }
        return copy(
            expectingClose = true,
            pendingOpen = Position(day, index),
        )
    }

    private fun handleClose(day: String, index: Int): SequenceState {
        if (!expectingClose) {
            if (leadingClose != null) {
                throw ApiException(ApiError.Validation("[$day][$index]: expected open entry, got close"))
            }
            return copy(leadingClose = Position(day, index))
        }
        return copy(
            expectingClose = false,
            pendingOpen = null,
        )
    }

    fun validateWeekEnd() {
        if (expectingClose) {
            if (leadingClose == null) {
                val open = pendingOpen!!
                throw ApiException(
                    ApiError.Validation(
                        "[${open.day}][${open.index}]: open entry has no matching close in this week",
                    ),
                )
            }
        } else if (leadingClose != null) {
            throw ApiException(
                ApiError.Validation(
                    "[${leadingClose.day}][${leadingClose.index}]: close entry has no preceding open in this week",
                ),
            )
        }
    }
}

private fun normalizeEntry(dayName: String, index: Int, entry: TimeEntryDto): EntryKind {
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
    return if (hasOpen) EntryKind.OPEN else EntryKind.CLOSE
}
