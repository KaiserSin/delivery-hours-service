package deliveryhoursservice.models

import deliveryhoursservice.error.ApiError
import deliveryhoursservice.error.ApiException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

private fun secs(value: Long) = SecondsOfDay(value)

class OpeningHoursDtoTest {
    @Test
    fun `accepts matching open and close`() {
        val dto =
            OpeningHoursDto(
                monday =
                    listOf(
                        TimeEntryDto(open = secs(0)),
                        TimeEntryDto(close = secs(18L * 3600)),
                    ),
                tuesday =
                    listOf(
                        TimeEntryDto(open = secs(10L * 3600)),
                        TimeEntryDto(close = secs(22L * 3600)),
                    ),
            )
        val result = dto.validateOrThrow()
        assertSame(dto, result)
    }

    @Test
    fun `accepts overnight wrap`() {
        val dto =
            OpeningHoursDto(
                monday = listOf(TimeEntryDto(open = secs(0))),
                tuesday = listOf(TimeEntryDto(close = secs(22L * 3600))),
            )
        val result = dto.validateOrThrow()
        assertSame(dto, result)
    }

    @Test
    fun `rejects open and close together`() {
        val dto =
            OpeningHoursDto(
                monday = listOf(TimeEntryDto(open = secs(0), close = secs(22L * 3600))),
            )
        val error = assertFailsWith<ApiException> { dto.validateOrThrow() }
        assertTrue(error.error is ApiError.Validation)
        assertEquals(
            "[monday][0]: exactly one of {open|close} must be set",
            error.error.message,
        )
    }

    @Test
    fun `rejects missing open and close`() {
        val dto =
            OpeningHoursDto(
                monday = listOf(TimeEntryDto()),
            )

        val error = assertFailsWith<ApiException> { dto.validateOrThrow() }

        assertTrue(error.error is ApiError.Validation)
        assertEquals(
            "[monday][0]: exactly one of {open|close} must be set",
            error.error.message,
        )
    }

    @Test
    fun `rejects open out of range`() {
        val dto =
            OpeningHoursDto(
                monday = listOf(TimeEntryDto(open = secs(0)), TimeEntryDto(close = secs(90_000))),
            )
        val error = assertFailsWith<ApiException> { dto.validateOrThrow() }
        assertTrue(error.error is ApiError.Validation)
        assertEquals(
            "[monday][1]: value 90000 is out of range 0..86400",
            error.error.message,
        )
    }

    @Test
    fun `rejects close out of range`() {
        val dto =
            OpeningHoursDto(
                monday = listOf(TimeEntryDto(close = secs(-2))),
            )
        val error = assertFailsWith<ApiException> { dto.validateOrThrow() }
        assertTrue(error.error is ApiError.Validation)
        assertEquals(
            "[monday][0]: value -2 is out of range 0..86400",
            error.error.message,
        )
    }

    @Test
    fun `rejects leading close`() {
        val dto =
            OpeningHoursDto(
                monday = listOf(TimeEntryDto(close = secs(600))),
            )

        val error = assertFailsWith<ApiException> { dto.validateOrThrow() }

        assertTrue(error.error is ApiError.Validation)
        assertEquals(
            "[monday][0]: close entry has no preceding open in this week",
            error.error.message,
        )
    }

    @Test
    fun `rejects consecutive open entries`() {
        val dto =
            OpeningHoursDto(
                monday =
                    listOf(
                        TimeEntryDto(open = secs(0)),
                        TimeEntryDto(open = secs(1_800)),
                    ),
            )
        val error = assertFailsWith<ApiException> { dto.validateOrThrow() }
        assertTrue(error.error is ApiError.Validation)
        assertEquals(
            "[monday][1]: expected close entry, got open",
            error.error.message,
        )
    }

    @Test
    fun `rejects close open close close`() {
        val dto =
            OpeningHoursDto(
                monday =
                    listOf(
                        TimeEntryDto(close = secs(600)),
                        TimeEntryDto(open = secs(3_600)),
                        TimeEntryDto(close = secs(7_200)),
                        TimeEntryDto(close = secs(10_800)),
                    ),
            )
        val error = assertFailsWith<ApiException> { dto.validateOrThrow() }
        assertTrue(error.error is ApiError.Validation)
        assertEquals(
            "[monday][3]: expected open entry, got close",
            error.error.message,
        )
    }

    @Test
    fun `rejects trailing open`() {
        val dto =
            OpeningHoursDto(
                monday = listOf(TimeEntryDto(open = secs(1_000))),
            )
        val error = assertFailsWith<ApiException> { dto.validateOrThrow() }
        assertTrue(error.error is ApiError.Validation)
        assertEquals(
            "[monday][0]: open entry has no matching close in this week",
            error.error.message,
        )
    }

    @Test
    fun `accepts boundary values`() {
        val dto =
            OpeningHoursDto(
                monday =
                    listOf(
                        TimeEntryDto(open = secs(0)),
                        TimeEntryDto(close = secs(86_399)),
                    ),
            )
        assertSame(dto, dto.validateOrThrow())
    }

    @Test
    fun `time entry defaults to nulls`() {
        val entry = TimeEntryDto()
        assertEquals(null, entry.open)
        assertEquals(null, entry.close)
    }
}
