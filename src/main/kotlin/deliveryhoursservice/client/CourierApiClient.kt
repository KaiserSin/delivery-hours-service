package deliveryhoursservice.client

import deliveryhoursservice.config.AppConfig
import deliveryhoursservice.error.DefaultErrorHandler
import deliveryhoursservice.error.ErrorHandler
import deliveryhoursservice.models.OpeningHoursDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess

class CourierApiClient(
    private val http: HttpClient,
    private val errorHandler: ErrorHandler = DefaultErrorHandler("Courier Service"),
    private val baseUrl: String = AppConfig.courierServiceUrl,
) {
    suspend fun fetchDeliveryHours(citySlug: String): OpeningHoursDto =
        executeRequest {
            http.get("$baseUrl?city=$citySlug") {
                expectSuccess = false
            }
        }

    private suspend inline fun <reified T> executeRequest(crossinline block: suspend () -> HttpResponse): T =
        runCatching { block() }
            .fold(
                onSuccess = { if (it.status.isSuccess()) it.body() else errorHandler.handleResponse(it) },
                onFailure = { throw errorHandler.handleException(it) },
            )
}
