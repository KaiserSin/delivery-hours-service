package deliveryhoursservice.routing

import deliveryhoursservice.config.AppConfig
import deliveryhoursservice.error.ApiError
import deliveryhoursservice.error.ApiException
import deliveryhoursservice.models.DeliveryHoursResponseDto
import deliveryhoursservice.plugins.configureStatusPages
import deliveryhoursservice.services.DeliveryHoursApplicationService
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation as ClientContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.application.Application
import io.ktor.server.testing.testApplication
import io.ktor.server.testing.ApplicationTestBuilder
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class RoutingTest {

    @Test
    fun `missing city slug returns bad request`() = testApplication {
        application { installRoutingWithService(mockk(relaxed = true)) }
        val response = jsonClient().get("/delivery-hours?venue_id=123")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(
            mapOf("error" to "Missing 'city_slug'"),
            response.body()
        )
    }

    @Test
    fun `missing venue id returns bad request`() = testApplication {
        application { installRoutingWithService(mockk(relaxed = true)) }
        val response = jsonClient().get("/delivery-hours?city_slug=helsinki")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(
            mapOf("error" to "Missing 'venue_id'"),
            response.body()
        )
    }

    @Test
    fun `invalid city slug yields validation error`() = testApplication {
        application { installRoutingWithService(mockk(relaxed = true)) }
        val response = jsonClient().get("/delivery-hours?city_slug=hel!sinki&venue_id=123")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(
            mapOf("error" to "city_slug must contain only letters, digits or dashes (1-64 chars)"),
            response.body()
        )
    }

    @Test
    fun `invalid venue id yields validation error`() = testApplication {
        application { installRoutingWithService(mockk(relaxed = true)) }
        val response = jsonClient().get("/delivery-hours?city_slug=helsinki&venue_id=abc")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(
            mapOf("error" to "venue_id must contain only digits (1-64 chars)"),
            response.body()
        )
    }

    @Test
    fun `successful request delegates to service`() = testApplication {
        val service = mockk<DeliveryHoursApplicationService>()
        val expected = DeliveryHoursResponseDto(mapOf("Monday" to "14-20"))
        coEvery { service.getDeliveryHours("helsinki", "123") } returns expected
        application { installRoutingWithService(service) }
        val response = jsonClient().get("/delivery-hours?city_slug=helsinki&venue_id=123")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(expected, response.body())
    }

    @Test
    fun `service exception bubbles as api error`() = testApplication {
        val service = mockk<DeliveryHoursApplicationService>()
        coEvery { service.getDeliveryHours("helsinki", "123") } throws ApiException(ApiError.BadRequest("from service"))
        application { installRoutingWithService(service) }
        val response = jsonClient().get("/delivery-hours?city_slug=helsinki&venue_id=123")
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(mapOf("error" to "from service"), response.body())
    }
}

private fun Application.installRoutingWithService(service: DeliveryHoursApplicationService) {
    install(ServerContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(AppConfig.objectMapper.copy()))
    }
    configureStatusPages()
    configureRouting(service)
}

private fun ApplicationTestBuilder.jsonClient() = createClient {
    install(ClientContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(AppConfig.objectMapper.copy()))
    }
}
