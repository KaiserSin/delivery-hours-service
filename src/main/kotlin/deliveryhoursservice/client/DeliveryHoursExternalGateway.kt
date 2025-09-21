package deliveryhoursservice.client

class DeliveryHoursExternalGateway(
    private val venueClient: VenueApiClient,
    private val courierClient: CourierApiClient,
) {
    suspend fun fetchVenueOpeningHours(venueId: String) = venueClient.fetchOpeningHours(venueId)

    suspend fun fetchCourierDeliveryHours(citySlug: String) = courierClient.fetchDeliveryHours(citySlug)
}
