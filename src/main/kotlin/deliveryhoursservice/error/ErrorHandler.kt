package deliveryhoursservice.error

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.statement.*
import io.ktor.http.*
import java.net.ConnectException
import java.net.SocketTimeoutException as JSocketTimeoutException
import java.nio.channels.UnresolvedAddressException

interface ErrorHandler {
    suspend fun handleResponse(response: HttpResponse): Nothing
    fun handleException(t: Throwable): Nothing
}

class DefaultErrorHandler : ErrorHandler {

    override suspend fun handleResponse(response: HttpResponse): Nothing {
        val body = runCatching { response.bodyAsText().take(500) }.getOrElse { "<no body>" }
        val s = response.status

        val error = when {
            s == HttpStatusCode.BadRequest      -> ApiError.BadRequest("400 Bad Request: $body")
            s == HttpStatusCode.Unauthorized    -> ApiError.Unauthorized("401 Unauthorized: $body")
            s == HttpStatusCode.Forbidden       -> ApiError.Forbidden("403 Forbidden: $body")
            s == HttpStatusCode.NotFound        -> ApiError.NotFound("404 Not Found: $body")
            s == HttpStatusCode.TooManyRequests -> ApiError.TooManyRequests("429 Too Many Requests: $body")
            s.value in 500..599           -> ApiError.ServerError("${s.value} Server Error: $body")
            else                                -> ApiError.Unknown("${s.value} ${s.description}: $body")
        }
        throw ApiException(error)
    }

    override fun handleException(t: Throwable): Nothing {
        val error = when (t) {
            is HttpRequestTimeoutException,
            is ConnectTimeoutException,
            is SocketTimeoutException,
            is JSocketTimeoutException,
            is ConnectException,
            is UnresolvedAddressException ->
                ApiError.Network(t.message ?: "network error")
            else -> ApiError.Unknown(t.message ?: "unknown error")
        }
        throw ApiException(error)
    }
}
