package deliveryhoursservice.client

import deliveryhoursservice.error.ApiError
import deliveryhoursservice.error.ApiException
import deliveryhoursservice.models.OpeningHoursDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class DeliveryHoursExternalGatewayTest {
    @Test
    fun `fetchVenueOpeningHours delegates to venue client`() =
        runBlocking {
            val venueClient = mockk<VenueApiClient>()
            val courierApiClient = mockk<CourierApiClient>()
            val gateway = ExternalDataRepository(venueClient, courierApiClient)
            val expected = OpeningHoursDto(monday = emptyList())
            coEvery { venueClient.fetchOpeningHours("123") } returns expected
            val result = gateway.fetchVenueOpeningHours("123")
            assertEquals(expected, result)
            coVerify(exactly = 1) { venueClient.fetchOpeningHours("123") }
            coVerify(exactly = 0) { courierApiClient.fetchDeliveryHours(any()) }
        }

    @Test
    fun `fetchCourierDeliveryHours delegates to courier client`() =
        runBlocking {
            val venueClient = mockk<VenueApiClient>()
            val courierApiClient = mockk<CourierApiClient>()
            val gateway = ExternalDataRepository(venueClient, courierApiClient)
            val expected = OpeningHoursDto(tuesday = emptyList())
            coEvery { courierApiClient.fetchDeliveryHours("helsinki") } returns expected
            val result = gateway.fetchCourierDeliveryHours("helsinki")
            assertEquals(expected, result)
            coVerify(exactly = 1) { courierApiClient.fetchDeliveryHours("helsinki") }
            coVerify(exactly = 0) { venueClient.fetchOpeningHours(any()) }
        }

    @Test
    fun `fetchVenueOpeningHours propagates exceptions`() =
        runBlocking {
            val venueClient = mockk<VenueApiClient>()
            val courierApiClient = mockk<CourierApiClient>()
            val gateway = ExternalDataRepository(venueClient, courierApiClient)
            val failure = ApiException(ApiError.Unknown("venue down"))
            coEvery { venueClient.fetchOpeningHours("123") } throws failure
            val thrown =
                assertFailsWith<ApiException> {
                    gateway.fetchVenueOpeningHours("123")
                }
            assertEquals(failure, thrown)
        }

    @Test
    fun `fetchCourierDeliveryHours propagates exceptions`() =
        runBlocking {
            val venueClient = mockk<VenueApiClient>()
            val courierApiClient = mockk<CourierApiClient>()
            val gateway = ExternalDataRepository(venueClient, courierApiClient)
            val failure = ApiException(ApiError.Unknown("courier down"))
            coEvery { courierApiClient.fetchDeliveryHours("helsinki") } throws failure
            val thrown =
                assertFailsWith<ApiException> {
                    gateway.fetchCourierDeliveryHours("helsinki")
                }
            assertEquals(failure, thrown)
        }
}
