package deliveryhoursservice.integration

import deliveryhoursservice.config.AppConfig
import deliveryhoursservice.models.DeliveryHoursResponseDto
import deliveryhoursservice.module
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class DeliveryHoursComponentTest {
    private val expectedDeliveryHours =
        mapOf(
            "Monday" to "14-20",
            "Tuesday" to "Closed",
            "Wednesday" to "Closed",
            "Thursday" to "Closed",
            "Friday" to "Closed",
            "Saturday" to "Closed",
            "Sunday" to "Closed",
        )

    @Test
    fun `module responds with merged delivery hours`() =
        testApplication {
            withStubbedGateway {
                withJsonClient { client ->
                    val response = client.get("/delivery-hours?city_slug=helsinki&venue_id=123")
                    assertEquals(HttpStatusCode.OK, response.status)
                    val body = response.body<DeliveryHoursResponseDto>()
                    assertEquals(expectedDeliveryHours, body.delivery_hours)
                }
            }
        }

    @Test
    fun `module handles hundred clients sequentially`() =
        testApplication {
            withStubbedGateway {
                repeat(100) {
                    withJsonClient { client ->
                        val response = client.get("/delivery-hours?city_slug=helsinki&venue_id=123")
                        assertEquals(HttpStatusCode.OK, response.status)
                        val dto = response.body<DeliveryHoursResponseDto>()
                        assertEquals(expectedDeliveryHours, dto.delivery_hours)
                    }
                }
            }
        }

    private suspend fun ApplicationTestBuilder.withStubbedGateway(block: suspend ApplicationTestBuilder.() -> Unit) {
        val httpClient =
            HttpClient(stubbedGatewayEngine()) {
                install(ContentNegotiation) { jackson() }
            }
        try {
            application { module(httpClient = httpClient) }
            block()
        } finally {
            httpClient.close()
        }
    }

    private suspend fun <T> ApplicationTestBuilder.withJsonClient(block: suspend (HttpClient) -> T): T {
        val client =
            createClient {
                install(ContentNegotiation) {
                    register(ContentType.Application.Json, JacksonConverter(AppConfig.objectMapper.copy()))
                }
            }
        return try {
            block(client)
        } finally {
            client.close()
        }
    }

    private fun stubbedGatewayEngine() =
        MockEngine { request ->
            when (request.url.encodedPath) {
                "/venue-service/venues/123/opening-hours" ->
                    respond(
                        """
                        {
                          "monday": [
                            { "open": 46800 },
                            { "close": 72000 }
                          ]
                        }
                        """.trimIndent(),
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                "/courier-service/delivery-hours" -> {
                    assertEquals("helsinki", request.url.parameters["city"])
                    respond(
                        """
                        {
                          "monday": [
                            { "open": 50400 },
                            { "close": 75600 }
                          ]
                        }
                        """.trimIndent(),
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
                else -> error("Unexpected request ${'$'}{request.url}")
            }
        }
}
