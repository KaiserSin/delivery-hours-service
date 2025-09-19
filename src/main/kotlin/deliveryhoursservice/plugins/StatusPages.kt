package deliveryhoursservice.plugins

import io.ktor.server.application.Application
import deliveryhoursservice.error.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.http.*
import io.ktor.server.plugins.MissingRequestParameterException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.uri

fun Application.configureStatusPages() {
    val log = environment.log

    install(StatusPages) {
        exception<ApiException> { call, e ->
            val (code, detail) = when (val err = e.error) {
                is ApiError.BadRequest       -> HttpStatusCode.BadRequest         to err.message
                is ApiError.Unauthorized     -> HttpStatusCode.Unauthorized       to err.message
                is ApiError.Validation      -> HttpStatusCode.BadRequest          to err.message
                is ApiError.Forbidden        -> HttpStatusCode.Forbidden          to err.message
                is ApiError.NotFound         -> HttpStatusCode.NotFound           to err.message
                is ApiError.TooManyRequests  -> HttpStatusCode.TooManyRequests    to err.message
                is ApiError.ServerError      -> HttpStatusCode.InternalServerError to "Internal error"
                is ApiError.ExternalServiceError -> run {
                    log.warn(
                        "Upstream failure {} status {} body {}",
                        err.service, err.status, err.body.take(2000)
                    )
                    val upstreamCode = HttpStatusCode.fromValue(err.status)
                    val safeDetail = "Upstream ${err.service} responded ${err.status}"
                    upstreamCode to safeDetail
                }
                is ApiError.Network          -> HttpStatusCode.GatewayTimeout     to err.message
                is ApiError.Unknown          -> HttpStatusCode.InternalServerError to "Unknown error"
            }

            if (code.value in 400..499) {
                log.warn("Client error: ${e.error} for ${call.request.uri}")
            } else {
                log.error("Server error: ${e.error} for ${call.request.uri}", e)
            }

            call.respond(code, mapOf("error" to detail))
        }

        exception<MissingRequestParameterException> { call, e ->
            log.warn("Missing request parameter at ${call.request.uri}: ${e.message}")
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to e.message))
        }

        exception<Throwable> { call, e ->
            log.error("Unexpected error at ${call.request.uri}", e)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Unexpected error"))
        }
    }
}
