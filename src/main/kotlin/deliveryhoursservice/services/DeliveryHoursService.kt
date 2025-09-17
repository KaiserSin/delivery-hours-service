package deliveryhoursservice.services

import deliveryhoursservice.client.ExternalApiClient
import deliveryhoursservice.models.OpeningHoursDto
import deliveryhoursservice.models.TimeEntryDto
import kotlin.reflect.full.memberProperties

private const val DAY_END = 24 * 60 * 60

class DeliveryHoursService(
    private val apiClient: ExternalApiClient
) {
    suspend fun getDeliveryHours(citySlug: String, venueId: String): OpeningHoursDto {


        return apiClient.getDataVenueService(venueId)
    }

    private val weekDays: List<String> = listOf("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")

    private data class Range(val s: Long, val e: Long) { val len get() = e - s }

    private fun convertTime(week: OpeningHoursDto): List<Range> {
        var answer = mutableListOf<Range>()
        for (day in OpeningHoursDto::class.memberProperties){
            print(day.get(week))
        }
        return answer
    }


}