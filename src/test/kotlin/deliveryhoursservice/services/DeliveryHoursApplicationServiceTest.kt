package deliveryhoursservice.services

import deliveryhoursservice.client.ExternalDataRepository
import deliveryhoursservice.error.ApiError
import deliveryhoursservice.error.ApiException
import deliveryhoursservice.models.OpeningHoursDto
import deliveryhoursservice.models.SecondsOfDay
import deliveryhoursservice.models.TimeEntryDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeliveryHoursApplicationServiceTest {
    @Test
    fun `fetches venues and couriers in parallel`() {
        runTest {
            val gateway = mockk<ExternalDataRepository>()
            val barrier = CoroutineBarrier(2)
            val venueDto = OpeningHoursDto(monday = slot(13, 0, 20, 0))
            val courierDto = OpeningHoursDto(monday = slot(14, 0, 21, 0))

            coEvery { gateway.fetchVenueOpeningHours("123") } coAnswers {
                barrier.await()
                venueDto
            }
            coEvery { gateway.fetchCourierDeliveryHours("helsinki") } coAnswers {
                barrier.await()
                courierDto
            }
            val service = DeliveryHoursApplicationService(gateway)
            val result = service.getDeliveryHours("helsinki", "123")
            assertEquals(expectWeek("Monday" to "14-20"), result.delivery_hours)
            coVerify(exactly = 1) { gateway.fetchVenueOpeningHours("123") }
            coVerify(exactly = 1) { gateway.fetchCourierDeliveryHours("helsinki") }
        }
    }

    @Test
    fun `propagates venue exception`() {
        runTest {
            val gateway = mockk<ExternalDataRepository>()
            val service = DeliveryHoursApplicationService(gateway)
            val failure = ApiException(ApiError.BadRequest("bad venue request"))

            coEvery { gateway.fetchVenueOpeningHours("123") } throws failure
            coEvery { gateway.fetchCourierDeliveryHours(any()) } returns OpeningHoursDto()

            val thrown =
                assertFailsWith<ApiException> {
                    service.getDeliveryHours("helsinki", "123")
                }

            assertEquals(failure, thrown)
            coVerify(exactly = 1) { gateway.fetchVenueOpeningHours("123") }
        }
    }

    @Test
    fun `propagates courier exception`() {
        runTest {
            val gateway = mockk<ExternalDataRepository>()
            val service = DeliveryHoursApplicationService(gateway)
            val failure = ApiException(ApiError.BadRequest("bad courier request"))

            coEvery { gateway.fetchVenueOpeningHours(any()) } returns OpeningHoursDto()
            coEvery { gateway.fetchCourierDeliveryHours("helsinki") } throws failure

            val thrown =
                assertFailsWith<ApiException> {
                    service.getDeliveryHours("helsinki", "123")
                }

            assertEquals(failure, thrown)
            coVerify(exactly = 1) { gateway.fetchCourierDeliveryHours("helsinki") }
        }
    }

    @Test
    fun `propagates venue validation error`() {
        runTest {
            val gateway = mockk<ExternalDataRepository>()
            val service = DeliveryHoursApplicationService(gateway)
            val invalidDto =
                OpeningHoursDto(
                    monday = listOf(TimeEntryDto(open = SecondsOfDay(0), close = SecondsOfDay(0))),
                )

            coEvery { gateway.fetchVenueOpeningHours(any()) } returns invalidDto
            coEvery { gateway.fetchCourierDeliveryHours(any()) } returns OpeningHoursDto()

            assertFailsWith<ApiException> {
                service.getDeliveryHours("helsinki", "123")
            }
        }
    }

    @Test
    fun `propagates courier validation error`() {
        runTest {
            val gateway = mockk<ExternalDataRepository>()
            val service = DeliveryHoursApplicationService(gateway)
            val invalidDto =
                OpeningHoursDto(
                    tuesday = listOf(TimeEntryDto(open = SecondsOfDay(0), close = SecondsOfDay(0))),
                )
            coEvery { gateway.fetchVenueOpeningHours(any()) } returns OpeningHoursDto()
            coEvery { gateway.fetchCourierDeliveryHours(any()) } returns invalidDto
            assertFailsWith<ApiException> {
                service.getDeliveryHours("helsinki", "123")
            }
        }
    }
}

class CoroutineBarrier(private val parties: Int) {
    private val counter = java.util.concurrent.atomic.AtomicInteger(0)
    private val gate = CompletableDeferred<Unit>()

    suspend fun await() {
        if (counter.incrementAndGet() == parties) gate.complete(Unit)
        gate.await()
    }
}
