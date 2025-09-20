package deliveryhoursservice.services

import deliveryhoursservice.models.OpeningHoursDto
import kotlin.test.Test
import kotlin.test.assertEquals

class DeliveryHoursCalculatorTest {

    @Test
    fun `intersects distinct monday slots`() {
        val venue = OpeningHoursDto(monday = slot(13, 0, 20, 0))
        val courier = OpeningHoursDto(monday = slot(14, 0, 21, 0))
        val response = calculateDeliveryHours(venue, courier)
        assertEquals(expectWeek("Monday" to "14-20"), response.delivery_hours)
    }

    @Test
    fun `intersects nested monday slots`() {
        val venue = OpeningHoursDto(monday = slot(13, 0, 20, 0))
        val courier = OpeningHoursDto(monday = slot(8, 0, 23, 0))
        val response = calculateDeliveryHours(venue, courier)
        assertEquals(expectWeek("Monday" to "13-20"), response.delivery_hours)
    }

    @Test
    fun `returns closed week when no overlap`() {
        val venue = OpeningHoursDto(monday = slot(8, 0, 10, 0))
        val courier = OpeningHoursDto(monday = slot(12, 0, 15, 0))
        val response = calculateDeliveryHours(venue, courier)
        assertEquals(CLOSED_WEEK, response.delivery_hours)
    }

    @Test
    fun `drops slots shorter than thirty minutes`() {
        val venue = OpeningHoursDto(monday = slot(13, 0, 13, 45))
        val courier = OpeningHoursDto(monday = slot(13, 10, 13, 25))
        val response = calculateDeliveryHours(venue, courier)
        assertEquals(CLOSED_WEEK, response.delivery_hours)
    }

    @Test
    fun `preserves slot order and formatting`() {
        val venue = OpeningHoursDto(
            monday = slot(8, 0, 12, 0)+slot(14, 30, 18, 0),
        )
        val courier = OpeningHoursDto(
            monday = slot(7, 0, 13, 0)+slot(14, 0, 20, 0),
        )
        val response = calculateDeliveryHours(venue, courier)
        assertEquals(expectWeek("Monday" to "08-12, 14:30-18"), response.delivery_hours)
    }

    @Test
    fun `keeps thirty minute intersection`() {
        val venue = OpeningHoursDto(monday = slot(10, 0, 11, 0))
        val courier = OpeningHoursDto(monday = slot(10, 30, 11, 30))
        val response = calculateDeliveryHours(venue, courier)
        assertEquals(expectWeek("Monday" to "10:30-11"), response.delivery_hours)
    }

    @Test
    fun `splits overnight slot at six am`() {
        val venue = OpeningHoursDto(tuesday = slot(5, 0, 8, 0))
        val courier = OpeningHoursDto(tuesday = slot(5, 0, 8, 0))
        val response = calculateDeliveryHours(venue, courier)
        assertEquals(
            expectWeek("Monday" to "05-06", "Tuesday" to "06-08"),
            response.delivery_hours
        )
    }

    @Test
    fun `wraps sunday slot into next week`() {
        val venue = OpeningHoursDto(
            sunday = listOf(openEntry(21)),
            monday = listOf(closeEntry(6))
        )
        val courier = OpeningHoursDto(
            sunday = listOf(openEntry(20)),
            monday = listOf(closeEntry(7))
        )
        val response = calculateDeliveryHours(venue, courier)
        assertEquals(expectWeek("Sunday" to "21-06"), response.delivery_hours)
    }

    @Test
    fun `formats minutes with leading zeros`() {
        val venue = OpeningHoursDto(monday = slot(6, 18, 18, 41))
        val courier = OpeningHoursDto(monday = slot(9, 21, 21, 35))
        val response = calculateDeliveryHours(venue, courier)
        assertEquals(expectWeek("Monday" to "09:21-18:41"), response.delivery_hours)
    }
}
