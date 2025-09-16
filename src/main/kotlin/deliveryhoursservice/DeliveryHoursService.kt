package deliveryhoursservice

import deliveryhoursservice.config.HttpClientProvider
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import deliveryhoursservice.plugins.configureSerialization
import deliveryhoursservice.routing.configureRouting
import io.ktor.server.cio.CIO

fun main() {
    embeddedServer(
        CIO,
        port = 8000,
        host = "0.0.0.0",
        module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureRouting(HttpClientProvider.client)
}
