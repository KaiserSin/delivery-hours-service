package deliveryhoursservice.client

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class VenueApiClientTest {
    @Test
    fun `returns dto on 200`() =
        runBlocking {
            val engine =
                MockEngine { request ->
                    assertEquals(
                        "http://example.com/venue-service/venues/123/opening-hours",
                        request.url.toString(),
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
                        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            val httpClient =
                HttpClient(engine) {
                    install(ContentNegotiation) { jackson() }
                }
            val client =
                VenueApiClient(
                    http = httpClient,
                    baseUrl = "http://example.com/venue-service/venues",
                )
            val dto = client.fetchOpeningHours("123")
            assertEquals(0, dto.monday[0].open?.value)
            assertEquals(3600, dto.monday[1].close?.value)
        }
}
