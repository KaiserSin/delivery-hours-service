package deliveryhoursservice.config

import java.lang.System.getenv

const val EXTERNAL_SERVICES_DEFAULT_HOST = "http://localhost:8080"

val VENUE_SERVICE_URL: String =
    getenv("VENUE_SERVICE_URL") ?: "$EXTERNAL_SERVICES_DEFAULT_HOST/venue-service/venues"

val COURIER_SERVICE_URL: String =
    getenv("COURIER_SERVICE_URL") ?: "$EXTERNAL_SERVICES_DEFAULT_HOST/courier-service/delivery-hours"