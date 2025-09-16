package deliveryhoursservice.models

data class TimeEntryDto(
    val open: Int? = -1,
    val close: Int? = -1
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
