package deliveryhoursservice.routing

import deliveryhoursservice.error.ApiError
import deliveryhoursservice.error.ApiException
import deliveryhoursservice.services.DeliveryHoursApplicationService
import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureRouting(service: DeliveryHoursApplicationService) {
    routing {
        get("/delivery-hours") {
            val citySlug =
                call.request.queryParameters["city_slug"]
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: throw ApiException(ApiError.BadRequest("Missing 'city_slug'"))
            val venueId =
                call.request.queryParameters["venue_id"]
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: throw ApiException(ApiError.BadRequest("Missing 'venue_id'"))
            if (!CITY_SLUG_REGEX.matches(citySlug)) {
                throw ApiException(
                    ApiError.BadRequest("city_slug must contain only letters, digits or dashes (1-64 chars)"),
                )
            }
            if (!VENUE_ID_REGEX.matches(venueId)) {
                throw ApiException(
                    ApiError.BadRequest("venue_id must contain only digits (1-64 chars)"),
                )
            }
            val result = service.getDeliveryHours(citySlug, venueId)
            call.respond(result)
        }
    }
}

private val CITY_SLUG_REGEX = Regex("^[a-zA-Z0-9-]{1,64}$")
private val VENUE_ID_REGEX = Regex("^[0-9]{1,64}$")
