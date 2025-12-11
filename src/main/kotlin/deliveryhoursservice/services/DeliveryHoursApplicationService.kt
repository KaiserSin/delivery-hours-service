package deliveryhoursservice.services

import deliveryhoursservice.client.ExternalDataRepository
import deliveryhoursservice.models.DeliveryHoursResponseDto
import deliveryhoursservice.models.validateOrThrow
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class DeliveryHoursApplicationService(
    private val gateway: ExternalDataRepository,
) {
    suspend fun getDeliveryHours(
        citySlug: String,
        venueId: String,
    ): DeliveryHoursResponseDto {
        val (venue, courier) =
            coroutineScope {
                val venueDeferred = async { gateway.fetchVenueOpeningHours(venueId) }
                val courierDeferred = async { gateway.fetchCourierDeliveryHours(citySlug) }
                venueDeferred.await() to courierDeferred.await()
            }
        return calculateDeliveryHours(
            venue.validateOrThrow(),
            courier.validateOrThrow(),
        )
    }
}
