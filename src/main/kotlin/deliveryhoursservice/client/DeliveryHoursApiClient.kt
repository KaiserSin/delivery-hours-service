package deliveryhoursservice.client

import deliveryhoursservice.config.COURIER_SERVICE_URL
import deliveryhoursservice.config.VENUE_SERVICE_URL
import deliveryhoursservice.error.DefaultErrorHandler
import deliveryhoursservice.error.ErrorHandler
import deliveryhoursservice.models.OpeningHoursDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess

class DeliveryHoursApiClient(
    private val http: HttpClient
) {
    private val venueErrorHandler = DefaultErrorHandler("Venue Service")
    private val courierErrorHandler = DefaultErrorHandler("Courier Service")

    suspend fun fetchVenueOpeningHours(venueId: String): OpeningHoursDto =
        executeRequest(venueErrorHandler) {
            http.get("$VENUE_SERVICE_URL/$venueId/opening-hours") {
                expectSuccess = false
            }
        }

    suspend fun fetchCourierDeliveryHours(citySlug: String): OpeningHoursDto =
        executeRequest(courierErrorHandler) {
            http.get("$COURIER_SERVICE_URL?city=$citySlug") {
                expectSuccess = false
            }
        }


    private suspend inline fun <reified T> executeRequest(
        handler: ErrorHandler,
        crossinline request: suspend () -> HttpResponse,
    ): T = try {
        val response = request()
        if (response.status.isSuccess()) response.body() else handler.handleResponse(response)
    } catch (t: Throwable) {
        handler.handleException(t)
    }
}
