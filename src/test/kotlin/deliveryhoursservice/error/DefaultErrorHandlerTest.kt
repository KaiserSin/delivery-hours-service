package deliveryhoursservice.error

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import java.nio.channels.UnresolvedAddressException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class DefaultErrorHandlerTest {
    @Test
    fun `maps upstream http statuses to ExternalServiceError`() =
        runTest {
            val response = mockResponse(HttpStatusCode.ServiceUnavailable, "upstream down")
            val handler = DefaultErrorHandler("Upstream")

            val error = assertFailsWith<ApiException> { handler.handleResponse(response) }.error

            assertTrue(error is ApiError.ExternalServiceError)
            assertEquals("Upstream", error.service)
            assertEquals(HttpStatusCode.ServiceUnavailable.value, error.status)
            assertTrue(error.body.contains("upstream down"))
        }

    @Test
    fun `maps individual http statuses to corresponding ApiError`() =
        runTest {
            val handler = DefaultErrorHandler()
            assertError<ApiError.BadRequest>(handler, HttpStatusCode.BadRequest) { err ->
                assertTrue(err.message.contains("400"))
            }
            assertError<ApiError.Unauthorized>(handler, HttpStatusCode.Unauthorized)
            assertError<ApiError.Forbidden>(handler, HttpStatusCode.Forbidden)
            assertError<ApiError.NotFound>(handler, HttpStatusCode.NotFound)
            assertError<ApiError.TooManyRequests>(handler, HttpStatusCode.TooManyRequests)
            assertError<ApiError.ServerError>(handler, HttpStatusCode.InternalServerError)
            assertError<ApiError.Unknown>(handler, HttpStatusCode.fromValue(418)) { err ->
                assertTrue(err.message.contains("418"))
            }
        }

    @Test
    fun `handleException returns same ApiException`() {
        val apiException = ApiException(ApiError.BadRequest("bad"))
        val thrown = assertFailsWith<ApiException> { DefaultErrorHandler().handleException(apiException) }
        assertSame(apiException, thrown)
    }

    @Test
    fun `handleException maps network issues`() {
        val error = assertFailsWith<ApiException> {
            DefaultErrorHandler().handleException(UnresolvedAddressException())
        }.error
        assertTrue(error is ApiError.Network)
    }

    @Test
    fun `handleException maps unknown throwable`() {
        val error = assertFailsWith<ApiException> {
            DefaultErrorHandler().handleException(IllegalStateException("oops"))
        }.error
        assertTrue(error is ApiError.Unknown)
        assertTrue(error.message.contains("oops"))
    }

    private suspend fun mockResponse(status: HttpStatusCode, body: String = "body") =
        HttpClient(
            MockEngine {
                respond(
                    body,
                    status,
                    headersOf(HttpHeaders.ContentType, ContentType.Text.Plain.toString()),
                )
            },
        ).get("http://example.com")

    private suspend inline fun <reified T : ApiError> assertError(
        handler: DefaultErrorHandler,
        status: HttpStatusCode,
        crossinline assertBlock: (T) -> Unit = {},
    ) {
        val error =
            assertFailsWith<ApiException> {
                handler.handleResponse(mockResponse(status, "error body"))
            }.error
        assertTrue(error is T)
        assertBlock(error)
    }
}
