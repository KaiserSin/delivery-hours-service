package deliveryhoursservice.client

class ExternalDataRepository(
    private val venueClient: VenueApiClient,
    private val courierApiClient: CourierApiClient,
) {
    suspend fun fetchVenueOpeningHours(venueId: String) = venueClient.fetchOpeningHours(venueId)

    suspend fun fetchCourierDeliveryHours(citySlug: String) = courierApiClient.fetchDeliveryHours(citySlug)
}
