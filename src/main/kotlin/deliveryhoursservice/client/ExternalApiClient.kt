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
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Url
import io.ktor.http.isSuccess
import io.ktor.http.path
import io.ktor.http.takeFrom

class ExternalApiClient(
    private val http: HttpClient,
    private val errorHandler: ErrorHandler = DefaultErrorHandler()
) {

    private val urlVenue = Url(VENUE_SERVICE_URL)
    private val urlCourier = Url(COURIER_SERVICE_URL)

    suspend fun getDataVenueService(venueId: String): OpeningHoursDto =
        execute {
            http.get {
                expectSuccess = false
                url {
                    takeFrom(urlVenue)
                    path(venueId, "opening-hours")
                }
            }
        }

    suspend fun getDataCourierService(citySlug: String): OpeningHoursDto =
        execute {
            http.get {
                expectSuccess = false
                url {
                    takeFrom(urlCourier)
                }
                parameter("city", citySlug)
            }
        }

    private suspend inline fun <reified T> execute(
        crossinline request: suspend () -> HttpResponse
    ): T {
        return try {
            val resp = request()
            if (resp.status.isSuccess()) {
                resp.body()
            } else {
                errorHandler.handleResponse(resp)
            }
        } catch (t: Throwable) {
            errorHandler.handleException(t)
        }
    }
}
