package deliveryhoursservice.models

data class TimeEntryDto(
    val open: Int? = null,
    val close: Int? = null
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
