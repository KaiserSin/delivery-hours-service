package deliveryhoursservice.client

import deliveryhoursservice.error.ApiError
import deliveryhoursservice.error.ApiException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import java.net.SocketTimeoutException

class CourierApiClientTest {

    @Test
    fun `returns dto on 200`() = runBlocking {
        val engine = MockEngine { request ->
            assertEquals(
                "http://example.com/courier-service/delivery-hours?city=helsinki",
                request.url.toString()
            )
            respond(
                """
                    {
                      "monday": [
                        { "open": 0 },
                        { "close": 3600 }
                      ]
                    }
                """.trimIndent(),
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { jackson() }
        }
        val client = CourierApiClient(
            http = httpClient,
            baseUrl = "http://example.com/courier-service/delivery-hours"
        )
        val dto = client.fetchDeliveryHours("helsinki")
        assertEquals(0, dto.monday[0].open)
        assertEquals(3600, dto.monday[1].close)
    }

    @Test
    fun `throws when upstream not found`() = runBlocking {
        val engine = MockEngine {
            respond(
                """
                    { "error": "not found" }
                """.trimIndent(),
                HttpStatusCode.NotFound,
                headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { jackson() }
        }
        val client = CourierApiClient(
            http = httpClient,
            baseUrl = "http://example.com/courier-service/delivery-hours"
        )
        val error = assertFailsWith<ApiException> {
            client.fetchDeliveryHours("unknown")
        }
        assertTrue(error.error is ApiError.ExternalServiceError)
        assertEquals("Courier Service", error.error.service)
        assertEquals(HttpStatusCode.NotFound.value, error.error.status)
    }

    @Test
    fun `wraps transport exceptions`() = runBlocking {
        val engine = MockEngine { throw SocketTimeoutException("timeout") }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { jackson() }
        }
        val client = CourierApiClient(
            http = httpClient,
            baseUrl = "http://example.com/courier-service/delivery-hours"
        )
        val error = assertFailsWith<ApiException> {
            client.fetchDeliveryHours("helsinki")
        }
        assertTrue(error.error is ApiError.Network)
    }

    @Test
    fun `maps internal error to external service`() = runBlocking {
        val engine = MockEngine {
            respond(
                """
                    { "error": "upstream failure" }
                """.trimIndent(),
                HttpStatusCode.InternalServerError,
                headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { jackson() }
        }
        val client = CourierApiClient(
            http = httpClient,
            baseUrl = "http://example.com/courier-service/delivery-hours"
        )
        val error = assertFailsWith<ApiException> {
            client.fetchDeliveryHours("helsinki")
        }
        assertTrue(error.error is ApiError.ExternalServiceError)
        assertEquals("Courier Service", error.error.service)
        assertEquals(HttpStatusCode.InternalServerError.value, error.error.status)
        assertTrue(error.error.body.contains("upstream failure"))
    }
}
