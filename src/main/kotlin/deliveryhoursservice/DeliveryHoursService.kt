package deliveryhoursservice

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE
import com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.http.ContentType.Application.Json
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import java.lang.System.getenv
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation

val OBJECT_MAPPER =
    jsonMapper {
        propertyNamingStrategy(SNAKE_CASE)
        enable(INDENT_OUTPUT)
        addModule(kotlinModule())
    }

const val EXTERNAL_SERVICES_DEFAULT_HOST = "http://localhost:8080"
val VENUE_SERVICE_URL = getenv("VENUE_SERVICE_URL") ?: "$EXTERNAL_SERVICES_DEFAULT_HOST/venue-service"
val COURIER_SERVICE_URL = getenv("COURIER_SERVICE_URL") ?: "$EXTERNAL_SERVICES_DEFAULT_HOST/courier-service"

fun main() {
    embeddedServer(Netty, port = 8000) {
        deliveryHours()
    }.start(wait = true)
}

val appHttpClient by lazy {
    HttpClient(Java) {
        install(ClientContentNegotiation) {
            register(Json, JacksonConverter(OBJECT_MAPPER))
        }
    }
}

fun Application.deliveryHours(httpClient: HttpClient = appHttpClient) {
    install(ServerContentNegotiation) {
        register(Json, JacksonConverter(OBJECT_MAPPER))
    }

    routing {
        get("/delivery-hours") {
            /**
             * TODO: please implement this endpoint
             * The base urls for externals services are available in
             * VENUE_SERVICE_URL and COURIER_SERVICE_URL variables
             */
            val citySlug = call.request.queryParameters["city_slug"]
            val venueId = call.request.queryParameters["venue_id"]

            TODO("Please implement me! Got $citySlug $venueId")
        }
    }
}
