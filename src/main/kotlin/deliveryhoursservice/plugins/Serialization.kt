package deliveryhoursservice.plugins

import deliveryhoursservice.config.OBJECT_MAPPER
import io.ktor.http.ContentType
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(OBJECT_MAPPER.copy()))
    }
}

