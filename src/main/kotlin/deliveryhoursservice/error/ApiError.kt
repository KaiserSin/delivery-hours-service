package deliveryhoursservice.error

sealed class ApiError(open val message: String) {
    data class BadRequest(override val message: String) : ApiError(message)
    data class Unauthorized(override val message: String) : ApiError(message)
    data class Forbidden(override val message: String) : ApiError(message)
    data class NotFound(override val message: String) : ApiError(message)
    data class TooManyRequests(override val message: String) : ApiError(message)
    data class ServerError(override val message: String) : ApiError(message)
    data class Network(override val message: String) : ApiError(message)
    data class Unknown(override val message: String) : ApiError(message)
}

class ApiException(val error: ApiError) : RuntimeException(error.message)