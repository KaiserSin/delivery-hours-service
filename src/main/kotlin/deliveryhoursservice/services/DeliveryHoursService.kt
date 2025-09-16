package deliveryhoursservice.services

import deliveryhoursservice.client.ExternalApiClient
import deliveryhoursservice.models.OpeningHoursDto
import io.ktor.client.HttpClient

class DeliveryHoursService(
    private val apiClient: ExternalApiClient
) {
    suspend fun getDeliveryHours(citySlug: String, venueId: String): OpeningHoursDto {
        return apiClient.getDataVenueService(venueId)
    }
}