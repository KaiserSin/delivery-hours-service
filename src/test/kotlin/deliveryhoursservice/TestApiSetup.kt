package deliveryhoursservice

import deliveryhoursservice.client.CourierApiClient
import deliveryhoursservice.client.DeliveryHoursExternalGateway
import deliveryhoursservice.client.VenueApiClient
import deliveryhoursservice.config.AppConfig
import deliveryhoursservice.plugins.configureSerialization
import deliveryhoursservice.plugins.configureStatusPages
import deliveryhoursservice.routing.configureRouting
import deliveryhoursservice.services.DeliveryHoursApplicationService
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType.Application.Json
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.testing.ApplicationTestBuilder

fun ApplicationTestBuilder.httpClient(): HttpClient = createClient {
    install(ContentNegotiation) {
        register(Json, JacksonConverter(AppConfig.objectMapper.copy()))
    }
}

fun ApplicationTestBuilder.installDeliveryHoursApi() {
    val gatewayHttpClient = httpClient()
    application {
        configureSerialization()
        configureStatusPages()
        val gateway = DeliveryHoursExternalGateway(
            VenueApiClient(gatewayHttpClient),
            CourierApiClient(gatewayHttpClient),
        )
        val service = DeliveryHoursApplicationService(gateway)
        configureRouting(service)
    }
}
