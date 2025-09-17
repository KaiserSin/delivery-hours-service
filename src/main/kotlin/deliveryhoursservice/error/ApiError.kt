package deliveryhoursservice.error

sealed interface ApiError {
    val message: String
    data class BadRequest(override val message: String): ApiError
    data class Unauthorized(override val message: String): ApiError
    data class Forbidden(override val message: String): ApiError
    data class NotFound(override val message: String): ApiError
    data class Validation(override val message: String): ApiError
    data class TooManyRequests(override val message: String): ApiError
    data class ServerError(override val message: String): ApiError
    data class Network(override val message: String): ApiError
    data class ExternalServiceError(
        val service: String,
        val status: Int,
        val body: String,
        override val message: String = "Upstream $service responded $status"
    ) : ApiError
    data class Unknown(override val message: String): ApiError
}

class ApiException(val error: ApiError): RuntimeException(error.message)