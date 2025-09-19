package deliveryhoursservice.services

import deliveryhoursservice.models.TimeEntryDto

internal val CLOSED_WEEK: Map<String, String> = mapOf(
    "Monday" to "Closed",
    "Tuesday" to "Closed",
    "Wednesday" to "Closed",
    "Thursday" to "Closed",
    "Friday" to "Closed",
    "Saturday" to "Closed",
    "Sunday" to "Closed"
)

internal fun expectWeek(vararg overrides: Pair<String, String>): Map<String, String> =
    CLOSED_WEEK + overrides

internal fun slot(openHour: Int, openMinute: Int, closeHour: Int, closeMinute: Int): List<TimeEntryDto> =
    listOf(openEntry(openHour, openMinute), closeEntry(closeHour, closeMinute))

internal fun openEntry(hour: Int, minute: Int = 0): TimeEntryDto =
    TimeEntryDto(open = seconds(hour, minute))

internal fun closeEntry(hour: Int, minute: Int = 0): TimeEntryDto =
    TimeEntryDto(close = seconds(hour, minute))

private fun seconds(hour: Int, minute: Int = 0): Long = ((hour * 60) + minute) * 60L
