package deliveryhoursservice

import deliveryhoursservice.client.CourierApiClient
import deliveryhoursservice.client.ExternalDataRepository
import deliveryhoursservice.client.VenueApiClient
import deliveryhoursservice.config.AppConfig
import deliveryhoursservice.config.HttpClientProvider
import deliveryhoursservice.plugins.configureSerialization
import deliveryhoursservice.plugins.configureStatusPages
import deliveryhoursservice.routing.configureRouting
import deliveryhoursservice.services.DeliveryHoursApplicationService
import io.ktor.client.HttpClient
import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer

fun main() {
    embeddedServer(
        CIO,
        port = AppConfig.serverPort,
        host = "0.0.0.0",
        module = Application::module,
    ).start(wait = true)
}

fun Application.module(httpClient: HttpClient = HttpClientProvider.client) {
    val venueClient = VenueApiClient(httpClient)
    val courierApiClient = CourierApiClient(httpClient)
    val gateway = ExternalDataRepository(venueClient, courierApiClient)
    val queryService = DeliveryHoursApplicationService(gateway)

    configureSerialization()
    configureStatusPages()
    configureRouting(queryService)
}
