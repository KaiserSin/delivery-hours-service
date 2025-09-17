package deliveryhoursservice.routing

import deliveryhoursservice.client.ExternalApiClient
import deliveryhoursservice.error.ApiError
import deliveryhoursservice.error.ApiException
import deliveryhoursservice.services.DeliveryHoursService
import io.ktor.client.HttpClient
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureRouting(httpClient: HttpClient) {
    val apiClient = ExternalApiClient(httpClient)
    val service = DeliveryHoursService(apiClient)

    routing {
        get("/delivery-hours") {
            val citySlug = call.request.queryParameters["city_slug"]
                ?: throw ApiException(ApiError.BadRequest("Missing 'city_slug'"))
            val venueId = call.request.queryParameters["venue_id"]
                ?: throw ApiException(ApiError.BadRequest("Missing 'venue_id'"))
            val result = service.getDeliveryHours(citySlug, venueId)
            call.respond(result)
        }
    }
}
