package deliveryhoursservice.config

import io.ktor.client.engine.cio.CIOEngineConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HttpClientProviderTest {
    @Test
    fun `configures CIO engine defaults`() {
        val client = HttpClientProvider.buildClient()

        val config = client.engine.config
        assertTrue(config is CIOEngineConfig)
        assertEquals(1000, config.maxConnectionsCount)
        assertEquals(1, config.endpoint.connectAttempts)
        assertEquals(5_000, config.endpoint.keepAliveTime)
        assertEquals(20, config.endpoint.pipelineMaxSize)

        client.close()
    }

    @Test
    fun `adds default accept header`() =
        runTest {
            var seenAccept: String? = null
            val client =
                HttpClientProvider.buildClient(
                    engineFactory = MockEngine,
                    engineConfig = {
                        (this as MockEngineConfig).addHandler { request ->
                            seenAccept = request.headers[HttpHeaders.Accept]
                            respond(
                                "ok",
                                headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString()),
                            )
                        }
                    },
                )

            client.get("http://example.com/test")
            client.close()

            assertEquals("application/json", seenAccept)
        }

    @Test
    fun `retries GET on retryable status`() =
        runTest {
            var attempts = 0
            val client =
                HttpClientProvider.buildClient(
                    engineFactory = MockEngine,
                    engineConfig = {
                        (this as MockEngineConfig).addHandler { request ->
                            assertEquals(HttpMethod.Get, request.method)
                            attempts++
                            if (attempts == 1) {
                                respond(
                                    "retry",
                                    HttpStatusCode.ServiceUnavailable,
                                    headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString()),
                                )
                            } else {
                                respond(
                                    "ok",
                                    headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString()),
                                )
                            }
                        }
                    },
                )

            val response = client.get("http://example.com/retry-status").bodyAsText()
            client.close()

            assertEquals("ok", response)
            assertEquals(2, attempts)
        }

    @Test
    fun `retries GET on exception`() =
        runTest {
            var attempts = 0
            val client =
                HttpClientProvider.buildClient(
                    engineFactory = MockEngine,
                    engineConfig = {
                        (this as MockEngineConfig).addHandler {
                            attempts++
                            if (attempts == 1) {
                                throw java.net.SocketTimeoutException("boom")
                            } else {
                                respond(
                                    "ok",
                                    headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString()),
                                )
                            }
                        }
                    },
                )

            val response = client.get("http://example.com/retry-exception").bodyAsText()
            client.close()

            assertEquals("ok", response)
            assertEquals(2, attempts)
        }

    @Test
    fun `does not retry non idempotent method`() =
        runTest {
            var attempts = 0
            val client =
                HttpClientProvider.buildClient(
                    engineFactory = MockEngine,
                    engineConfig = {
                        (this as MockEngineConfig).addHandler {
                            attempts++
                            respond(
                                "fail",
                                HttpStatusCode.ServiceUnavailable,
                                headers = headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString()),
                            )
                        }
                    },
                )

            val response = client.post("http://example.com/no-retry")
            client.close()

            assertEquals(1, attempts)
            assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        }
}
