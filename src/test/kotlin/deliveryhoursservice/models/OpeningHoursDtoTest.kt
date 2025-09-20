package deliveryhoursservice.models

import deliveryhoursservice.error.ApiError
import deliveryhoursservice.error.ApiException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class OpeningHoursDtoTest {

    @Test
    fun `accepts matching open and close`() {
        val dto = OpeningHoursDto(
            monday = listOf(
                TimeEntryDto(open = 0),
                TimeEntryDto(close = 18L * 3600)
            ),
            tuesday = listOf(
                TimeEntryDto(open = 10L * 3600),
                TimeEntryDto(close = 22L * 3600)
            )
        )
        val result = dto.validateOrThrow()
        assertSame(dto, result)
    }

    @Test
    fun `accepts overnight wrap`() {
        val dto = OpeningHoursDto(
            monday = listOf(TimeEntryDto(open = 0)),
            tuesday = listOf(TimeEntryDto(close = 22L * 3600))
        )
        val result = dto.validateOrThrow()
        assertSame(dto, result)
    }

    @Test
    fun `rejects open and close together`() {
        val dto = OpeningHoursDto(
            monday = listOf(TimeEntryDto(open = 0, close = 22L * 3600))
        )
        val error = assertFailsWith<ApiException> { dto.validateOrThrow() }
        assertTrue(error.error is ApiError.Validation)
        assertEquals(
            "[monday][0]: exactly one of {open|close} must be set",
            error.error.message
        )
    }

    @Test
    fun `rejects missing open and close`() {
        val dto = OpeningHoursDto(
            monday = listOf(TimeEntryDto())
        )

        val error = assertFailsWith<ApiException> { dto.validateOrThrow() }

        assertTrue(error.error is ApiError.Validation)
        assertEquals(
            "[monday][0]: exactly one of {open|close} must be set",
            error.error.message
        )
    }

    @Test
    fun `rejects open out of range`() {
        val dto = OpeningHoursDto(
            monday = listOf(TimeEntryDto(open = 0), TimeEntryDto(close = 90_000))
        )
        val error = assertFailsWith<ApiException> { dto.validateOrThrow() }
        assertTrue(error.error is ApiError.Validation)
        assertEquals(
            "[monday][1]: value 90000 is out of range 0..86400",
            error.error.message
        )
    }

    @Test
    fun `rejects close out of range`() {
        val dto = OpeningHoursDto(
            monday = listOf(TimeEntryDto(close = -2))
        )
        val error = assertFailsWith<ApiException> { dto.validateOrThrow() }
        assertTrue(error.error is ApiError.Validation)
        assertEquals(
            "[monday][0]: value -2 is out of range 0..86400",
            error.error.message
        )
    }

    @Test
    fun `rejects leading close`() {
        val dto = OpeningHoursDto(
            monday = listOf(TimeEntryDto(close = 600))
        )

        val error = assertFailsWith<ApiException> { dto.validateOrThrow() }

        assertTrue(error.error is ApiError.Validation)
        assertEquals(
            "[monday][0]: close entry has no preceding open in this week",
            error.error.message
        )
    }

    @Test
    fun `rejects consecutive open entries`() {
        val dto = OpeningHoursDto(
            monday = listOf(
                TimeEntryDto(open = 0),
                TimeEntryDto(open = 1_800)
            )
        )
        val error = assertFailsWith<ApiException> { dto.validateOrThrow() }
        assertTrue(error.error is ApiError.Validation)
        assertEquals(
            "[monday][1]: expected close entry, got open",
            error.error.message
        )
    }

    @Test
    fun `rejects close open close close`() {
        val dto = OpeningHoursDto(
            monday = listOf(
                TimeEntryDto(close = 600),
                TimeEntryDto(open = 3_600),
                TimeEntryDto(close = 7_200),
                TimeEntryDto(close = 10_800)
            )
        )
        val error = assertFailsWith<ApiException> { dto.validateOrThrow() }
        assertTrue(error.error is ApiError.Validation)
        assertEquals(
            "[monday][3]: expected open entry, got close",
            error.error.message
        )
    }

    @Test
    fun `rejects trailing open`() {
        val dto = OpeningHoursDto(
            monday = listOf(TimeEntryDto(open = 1_000))
        )
        val error = assertFailsWith<ApiException> { dto.validateOrThrow() }
        assertTrue(error.error is ApiError.Validation)
        assertEquals(
            "[monday][0]: open entry has no matching close in this week",
            error.error.message
        )
    }

    @Test
    fun `accepts boundary values`() {
        val dto = OpeningHoursDto(
            monday = listOf(
                TimeEntryDto(open = 0),
                TimeEntryDto(close = 86_399)
            )
        )
        assertSame(dto, dto.validateOrThrow())
    }

    @Test
    fun `time entry defaults to sentinel`() {
        val entry = TimeEntryDto()
        assertEquals(-1, entry.open)
        assertEquals(-1, entry.close)
    }
}
