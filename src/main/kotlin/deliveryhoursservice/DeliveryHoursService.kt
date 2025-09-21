package deliveryhoursservice

import deliveryhoursservice.client.CourierApiClient
import deliveryhoursservice.client.DeliveryHoursExternalGateway
import deliveryhoursservice.client.VenueApiClient
import deliveryhoursservice.config.HttpClientProvider
import deliveryhoursservice.plugins.configureSerialization
import deliveryhoursservice.plugins.configureStatusPages
import deliveryhoursservice.routing.configureRouting
import deliveryhoursservice.services.DeliveryHoursApplicationService
import io.ktor.server.application.Application
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer

fun main() {
    embeddedServer(
        CIO,
        port = 8000,
        host = "0.0.0.0",
        module = Application::module,
    ).start(wait = true)
}

fun Application.module() {
    val httpClient = HttpClientProvider.client
    val venueClient = VenueApiClient(httpClient)
    val courierClient = CourierApiClient(httpClient)
    val gateway = DeliveryHoursExternalGateway(venueClient, courierClient)
    val queryService = DeliveryHoursApplicationService(gateway)

    configureSerialization()
    configureStatusPages()
    configureRouting(queryService)
}
