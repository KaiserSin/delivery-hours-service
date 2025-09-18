package deliveryhoursservice.services

import deliveryhoursservice.client.DeliveryHoursApiClient
import deliveryhoursservice.models.DeliveryHoursResponseDto
import deliveryhoursservice.models.OpeningHoursDto
import deliveryhoursservice.models.validateOrThrow
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope


class DeliveryHoursApplicationService(
    private val apiClient: DeliveryHoursApiClient,
) {
    suspend fun getDeliveryHours(citySlug: String, venueId: String): DeliveryHoursResponseDto {
        val (venueData, courierData) = fetchOpeningHoursPair(venueId, citySlug)
        return calculateDeliveryHours(
            venueData.validateOrThrow(),
            courierData.validateOrThrow(),
        )
    }

    private suspend fun fetchOpeningHoursPair(
        venueId: String,
        citySlug: String,
    ): Pair<OpeningHoursDto, OpeningHoursDto> = coroutineScope {
        val venueDeferred = async { apiClient.fetchVenueOpeningHours(venueId) }
        val courierDeferred = async { apiClient.fetchCourierDeliveryHours(citySlug) }
        venueDeferred.await() to courierDeferred.await()
    }
}
