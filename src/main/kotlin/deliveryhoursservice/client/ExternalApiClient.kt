package deliveryhoursservice.client

import deliveryhoursservice.config.COURIER_SERVICE_URL
import deliveryhoursservice.config.VENUE_SERVICE_URL
import deliveryhoursservice.models.OpeningHoursDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class ExternalApiClient(
    private val httpClient: HttpClient
) {
    suspend fun getDataVenueService(venueId: String): OpeningHoursDto{
        val url = "$VENUE_SERVICE_URL/$venueId/opening-hours"
        return httpClient.get(url).body()
    }

    suspend fun getDataCourierService(citySlug: String): OpeningHoursDto{
        val url = "$COURIER_SERVICE_URL?city=$citySlug"
        return httpClient.get(url).body()
    }
}