package deliveryhoursservice.services



import deliveryhoursservice.client.ExternalApiClient
import deliveryhoursservice.models.DeliveryHoursResponseDto
import deliveryhoursservice.models.OpeningHoursDto
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope


class DeliveryHoursService(
    private val apiClient: ExternalApiClient
) {
    suspend fun getDeliveryHours(citySlug: String, venueId: String): DeliveryHoursResponseDto {
        val (venueData, courierData) = getBoth( venueId, citySlug)
        return deliveryHoursFinder(venueData, courierData)
    }

    suspend fun getBoth(
        venueId: String,
        citySlug: String
    ): Pair<OpeningHoursDto, OpeningHoursDto> = coroutineScope {
        val venueDeferred   = async { apiClient.getDataVenueService(venueId) }
        val courierDeferred = async { apiClient.getDataCourierService(citySlug) }

        val venueData = venueDeferred.await()
        val courierData = courierDeferred.await()

        venueData to courierData
    }

}