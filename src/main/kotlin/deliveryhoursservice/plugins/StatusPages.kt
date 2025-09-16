package deliveryhoursservice.plugins

import io.ktor.server.application.Application
import deliveryhoursservice.error.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.http.*
import io.ktor.server.plugins.statuspages.StatusPages

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<ApiException> { call, e ->
            val (code, body) = when (e.error) {
                is ApiError.BadRequest       -> HttpStatusCode.BadRequest        to mapOf("error" to e.error.message)
                is ApiError.Unauthorized     -> HttpStatusCode.Unauthorized      to mapOf("error" to e.error.message)
                is ApiError.Forbidden        -> HttpStatusCode.Forbidden         to mapOf("error" to e.error.message)
                is ApiError.NotFound         -> HttpStatusCode.NotFound          to mapOf("error" to e.error.message)
                is ApiError.TooManyRequests  -> HttpStatusCode.TooManyRequests   to mapOf("error" to e.error.message)
                is ApiError.ServerError,
                is ApiError.ExternalServiceError -> HttpStatusCode.BadGateway    to mapOf("error" to e.error.message)
                is ApiError.Network          -> HttpStatusCode.GatewayTimeout    to mapOf("error" to e.error.message)
                is ApiError.Unknown          -> HttpStatusCode.InternalServerError to mapOf("error" to e.error.message)
            }
            call.respond(code, body)
        }
    }
}

private fun errorBody(message: String) = mapOf("error" to message)