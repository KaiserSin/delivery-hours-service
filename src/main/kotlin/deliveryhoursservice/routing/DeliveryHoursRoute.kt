package deliveryhoursservice.routing

import deliveryhoursservice.client.DeliveryHoursApiClient
import deliveryhoursservice.error.ApiError
import deliveryhoursservice.error.ApiException
import deliveryhoursservice.services.DeliveryHoursApplicationService
import io.ktor.client.HttpClient
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureRouting(httpClient: HttpClient) {
    val apiClient = DeliveryHoursApiClient(httpClient)
    val service = DeliveryHoursApplicationService(apiClient)

    routing {
        get("/delivery-hours") {
            val citySlug = call.request.queryParameters["city_slug"]
                ?: throw ApiException(ApiError.BadRequest("Missing 'city_slug'"))
            val venueId = call.request.queryParameters["venue_id"]
                ?: throw ApiException(ApiError.BadRequest("Missing 'venue_id'"))
            if (!CITY_SLUG_REGEX.matches(citySlug)) {
                throw ApiException(ApiError.BadRequest("city_slug must contain only letters and dashes"))
            }
            if (!VENUE_ID_REGEX.matches(venueId)) {
                throw ApiException(ApiError.BadRequest("venue_id must contain only digits"))
            }
            val result = service.getDeliveryHours(citySlug, venueId)
            call.respond(result)
        }
    }

}

private val CITY_SLUG_REGEX = Regex("^[a-zA-Z-]+$")
private val VENUE_ID_REGEX = Regex("^[0-9]+$")
