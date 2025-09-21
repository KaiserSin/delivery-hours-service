package deliveryhoursservice.plugins

import deliveryhoursservice.config.AppConfig
import deliveryhoursservice.error.ApiError
import deliveryhoursservice.error.ApiException
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.plugins.MissingRequestParameterException
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class PluginsTest {
    @Test
    fun `configureSerialization registers jackson converter`() =
        testApplication {
            application {
                configureSerialization()
                routing {
                    get("/ping") {
                        call.respond(mapOf("ok" to true))
                    }
                }
            }
            val client =
                createClient {
                    install(ContentNegotiation) {
                        register(ContentType.Application.Json, JacksonConverter(AppConfig.objectMapper.copy()))
                    }
                }
            val response = client.get("/ping")
            assertEquals(ContentType.Application.Json, response.contentType()?.withoutParameters())
            assertEquals(mapOf("ok" to true), response.body())
        }

    @Test
    fun `configureStatusPages maps ApiException to http response`() =
        testApplication {
            application { installStatusPagesForTest { throw ApiException(ApiError.BadRequest("bad input")) } }
            val client =
                createClient {
                    install(ContentNegotiation) {
                        register(ContentType.Application.Json, JacksonConverter(AppConfig.objectMapper.copy()))
                    }
                }
            val response = client.get("/error")
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(mapOf("error" to "bad input"), response.body())
        }

    @Test
    fun `configureStatusPages maps MissingRequestParameterException to 400`() =
        testApplication {
            application { installStatusPagesForTest { throw MissingRequestParameterException("foo") } }
            val client =
                createClient {
                    install(ContentNegotiation) {
                        register(ContentType.Application.Json, JacksonConverter(AppConfig.objectMapper.copy()))
                    }
                }
            val response = client.get("/error")
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(mapOf("error" to "Request parameter foo is missing"), response.body())
        }

    @Test
    fun `configureStatusPages maps Throwable to 500`() =
        testApplication {
            application { installStatusPagesForTest { throw IllegalStateException("error") } }
            val client =
                createClient {
                    install(ContentNegotiation) {
                        register(ContentType.Application.Json, JacksonConverter(AppConfig.objectMapper.copy()))
                    }
                }
            val response = client.get("/error")
            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertEquals(mapOf("error" to "Unexpected error"), response.body())
        }

    @Test
    fun `configureStatusPages sanitizes server errors`() =
        testApplication {
            application {
                installStatusPagesForTest {
                    throw ApiException(ApiError.ServerError("sensitive detail"))
                }
            }
            val client =
                createClient {
                    install(ContentNegotiation) {
                        register(ContentType.Application.Json, JacksonConverter(AppConfig.objectMapper.copy()))
                    }
                }
            val response = client.get("/error")
            assertEquals(HttpStatusCode.InternalServerError, response.status)
            assertEquals(mapOf("error" to "Internal error"), response.body())
        }

    @Test
    fun `configureStatusPages sanitizes external service errors`() =
        testApplication {
            application {
                configureSerialization()
                configureStatusPages()
                routing {
                    get("/external") {
                        throw ApiException(
                            ApiError.ExternalServiceError(
                                service = "Courier Service",
                                status = 503,
                                body = "sensitive upstream body",
                            ),
                        )
                    }
                }
            }
            val client =
                createClient {
                    install(ContentNegotiation) {
                        register(ContentType.Application.Json, JacksonConverter(AppConfig.objectMapper.copy()))
                    }
                }
            val response = client.get("/external")
            assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
            assertEquals(
                mapOf("error" to "Upstream Courier Service responded 503"),
                response.body(),
            )
        }
}

private fun Application.installStatusPagesForTest(block: suspend () -> Unit) {
    configureSerialization()
    configureStatusPages()
    routing {
        get("/error") {
            block()
            call.respond(mapOf("should" to "not reach"))
        }
    }
}
