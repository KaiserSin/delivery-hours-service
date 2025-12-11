package deliveryhoursservice.config

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.CIOEngineConfig
import io.ktor.client.engine.cio.endpoint
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import kotlin.random.Random

object HttpClientProvider {
    val client = buildClient()

    internal fun buildClient(
        engineFactory: HttpClientEngineFactory<*> = CIO,
        engineConfig: HttpClientEngineConfig.() -> Unit = { configureEngineDefaults() },
    ): HttpClient =
        HttpClient(engineFactory) {
            install(ContentNegotiation) { jackson() }

            install(DefaultRequest) {
                header("Accept", "application/json")
            }

            install(Logging) {
                level = LogLevel.INFO
            }

            install(HttpTimeout) {
                connectTimeoutMillis = 1_000
                socketTimeoutMillis = 2_000
                requestTimeoutMillis = 2_500
            }

            install(HttpRequestRetry) {
                maxRetries = 2
                retryIf { request, response ->
                    val retryableStatus =
                        setOf(
                            HttpStatusCode.RequestTimeout,
                            HttpStatusCode.TooManyRequests,
                            HttpStatusCode.BadGateway,
                            HttpStatusCode.ServiceUnavailable,
                            HttpStatusCode.GatewayTimeout,
                        )
                    request.method in listOf(HttpMethod.Get, HttpMethod.Head) &&
                        response.status in retryableStatus
                }
                retryOnExceptionIf { request, _ ->
                    request.method in listOf(HttpMethod.Get, HttpMethod.Head)
                }

                delayMillis { attempt ->
                    val base = 100L shl attempt.coerceAtMost(4)
                    val jitter = Random.nextLong(0, base / 5 + 1)
                    (base + jitter).coerceAtMost(1_500L)
                }
            }

            engine(engineConfig)
        }

    private fun HttpClientEngineConfig.configureEngineDefaults() {
        if (this is CIOEngineConfig) {
            maxConnectionsCount = 1000
            endpoint {
                connectAttempts = 1
                keepAliveTime = 5_000
                pipelineMaxSize = 20
            }
        }
    }
}
