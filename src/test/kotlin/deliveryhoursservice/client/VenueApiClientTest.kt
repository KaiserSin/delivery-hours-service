package deliveryhoursservice.client

import deliveryhoursservice.error.ApiError
import deliveryhoursservice.error.ApiException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.runBlocking
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith



class VenueApiClientTest {
    @Test
    fun fetchOpeningHoursReturnsDto() = runBlocking {
        val engine = MockEngine { request ->
            assertEquals(
                "http://example.com/venue-service/venues/123/opening-hours",
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
        val client = VenueApiClient(
            http = httpClient,
            baseUrl = "http://example.com/venue-service/venues"
        )
        val dto = client.fetchOpeningHours("123")
        assertEquals(0, dto.monday[0].open)
        assertEquals(3600, dto.monday[1].close)
    }

    @Test
    fun `fetchOpeningHours throws ApiException on non 200`() = runBlocking {
        val engine = MockEngine { request ->
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
        val client = VenueApiClient(
            http = httpClient,
            baseUrl = "http://example.com/venue-service/venues"
        )
        val error = assertFailsWith<ApiException> {
            client.fetchOpeningHours("unknown")
        }
        assertTrue(error.error is ApiError.ExternalServiceError)
        assertEquals("Venue Service", error.error.service)
        assertEquals(HttpStatusCode.NotFound.value, error.error.status)
    }

    @Test
    fun `fetchOpeningHours wraps transport exceptions into ApiError Network`() = runBlocking {
        val engine = MockEngine { throw SocketTimeoutException("error") }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) { jackson() }
        }
        val client = VenueApiClient(
            http = httpClient,
            baseUrl = "http://example.com/venue-service/venues"
        )
        val error = assertFailsWith<ApiException> {
            client.fetchOpeningHours("123")
        }
        assertTrue(error.error is ApiError.Network)
    }

    @Test
    fun `fetchOpeningHours maps 500 to external service error`() = runBlocking {
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
        val client = VenueApiClient(
            http = httpClient,
            baseUrl = "http://example.com/venue-service/venues"
        )
        val error = assertFailsWith<ApiException> {
            client.fetchOpeningHours("123")
        }
        assertTrue(error.error is ApiError.ExternalServiceError)
        assertEquals("Venue Service", error.error.service)
        assertEquals(500, error.error.status)
        assertTrue(error.error.body.contains("upstream failure"))
    }
}