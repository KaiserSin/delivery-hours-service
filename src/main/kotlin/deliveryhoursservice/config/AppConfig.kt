package deliveryhoursservice.config

import ch.qos.logback.core.util.OptionHelper.getEnv
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule

object AppConfig {
    private const val EXTERNAL_SERVICES_DEFAULT_HOST = "http://localhost:8080"
    private const val DEFAULT_SERVER_PORT = 8000

    val venueServiceUrl: String =
        getEnv("VENUE_SERVICE_URL") ?: "$EXTERNAL_SERVICES_DEFAULT_HOST/venue-service/venues"

    val courierServiceUrl: String =
        getEnv("COURIER_SERVICE_URL") ?: "$EXTERNAL_SERVICES_DEFAULT_HOST/courier-service/delivery-hours"

    val serverPort: Int =
        getEnv("SERVER_PORT")?.toIntOrNull()?.takeIf { it in 1..65535 } ?: DEFAULT_SERVER_PORT

    val objectMapper =
        jsonMapper {
            propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            enable(SerializationFeature.INDENT_OUTPUT)
            disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            addModule(kotlinModule())
        }
}
