package deliveryhoursservice

import deliveryhoursservice.config.OBJECT_MAPPER
import deliveryhoursservice.services.DeliveryHoursService
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.serialization.jackson.JacksonConverter

import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours

class DeliveryHoursServiceKtTest {
    @Disabled("enable the test once the endpoint is implemented")
    @Test
    fun `simple venue delivery hours`() =
        testApplication {
            val citySlug = "berlin"
            val venueId = "12345"

            val testClient =
                createClient {
                    install(ContentNegotiation) {
                        register(Json, JacksonConverter(OBJECT_MAPPER))
                    }
                }

            application {
                DeliveryHoursService(testClient)
            }

            externalServices {
                hosts(EXTERNAL_SERVICES_DEFAULT_HOST) {
                    routing {
                        get("/venue-service/venues/{venue_id}/opening-hours") {
                            assertEquals(venueId, call.parameters["venue_id"])
                            call.respondText(
                                // language=JSON
                                text =
                                    """
                                    {
                                      "monday": [
                                        {"open": ${13.hours.inWholeSeconds}},
                                        {"close": ${20.hours.inWholeSeconds}}
                                      ]
                                    }
                                    """.trimIndent(),
                                contentType = Json,
                                status = OK,
                            )
                        }

                        get("/courier-service/delivery-hours") {
                            assertEquals(citySlug, call.request.queryParameters["city"])
                            call.respondText(
                                // language=JSON
                                text =
                                    """
                                    {
                                      "monday": [
                                        {"open": ${14.hours.inWholeSeconds}},
                                        {"close": ${21.hours.inWholeSeconds}}
                                      ],
                                      "tuesday": [
                                        {"open": ${10.hours.inWholeSeconds}},
                                        {"close": ${23.hours.inWholeSeconds}}
                                      ]
                                    }
                                    """.trimIndent(),
                                contentType = Json,
                                status = OK,
                            )
                        }
                    }
                }
            }

            val response = testClient.get("/delivery-hours?city_slug=$citySlug&venue_id=$venueId")

            assertEquals(OK, response.status)
            assertEquals(
                // venue service 13-20, courier service 14-21 -> 14-20
                // language=JSON
                """
                {
                  "delivery_hours" : {
                    "Monday" : "14-20",
                    "Tuesday" : "Closed",
                    "Wednesday" : "Closed",
                    "Thursday" : "Closed",
                    "Friday" : "Closed",
                    "Saturday" : "Closed",
                    "Sunday" : "Closed"
                  }
                }
                """.trimIndent(),
                response.bodyAsText(),
            )
        }
}
