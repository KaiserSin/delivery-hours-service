package deliveryhoursservice

import deliveryhoursservice.models.DeliveryHoursResponseDto
import deliveryhoursservice.models.OpeningHoursDto
import deliveryhoursservice.models.TimeEntryDto
import deliveryhoursservice.services.DeliveryHoursService

private const val DAY_END = 24 * 60 * 60L
private const val WEEK_END = 24 * 60 * 60 * 7L
private const val MIN_INTERSECTION = 30 * 60L
private const val CUT_6AM   = 6L  * 60 * 60
private val WEEK_DAYS_CAP = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

fun main() {
    val venue = OpeningHoursDto(
        monday = listOf(
            TimeEntryDto(open = 46800),
            TimeEntryDto(close = 72000)
        ),
        tuesday = listOf(
            TimeEntryDto(open = 48600),
            TimeEntryDto(close = 54000),
            TimeEntryDto(open = 57600)
        ),
        wednesday = listOf(
            TimeEntryDto(close = 3600),
            TimeEntryDto(open = 46800),
            TimeEntryDto(close = 54000)
        ),
        thursday = listOf(
            TimeEntryDto(open = 46800),
            TimeEntryDto(close = 54000)
        )
    )

    val courier = OpeningHoursDto(
        monday = listOf(
            TimeEntryDto(open = 50400),
            TimeEntryDto(close = 75600)
        ),
        tuesday = listOf(
            TimeEntryDto(open = 32400),
            TimeEntryDto(close = 50400),
            TimeEntryDto(open = 61200)
        ),
        wednesday = listOf(
            TimeEntryDto(close = 1800),
            TimeEntryDto(open = 32400),
            TimeEntryDto(close = 46800)
        ),
        thursday = listOf(
            TimeEntryDto(open = 52260),
            TimeEntryDto(close = 57600)
        )
    )

    println(deliveryHoursFinder(venue, courier ))
}

fun deliveryHoursFinder(venueTimeDto: OpeningHoursDto, courierTimeDto: OpeningHoursDto): DeliveryHoursResponseDto{
    val venueTime = convertTime(venueTimeDto)
    val courierTime = convertTime(courierTimeDto)
    val resultTime = intersectRanges(venueTime, courierTime)
    println(resultTime)
    return rangesToDtoCutAt6am(resultTime)
}

private data class Range(val s: Long, val e: Long) { val len get() = e - s }

private fun convertTime(week: OpeningHoursDto): List<Range> {
    val answer = mutableListOf<Range>()
    val props = listOf(
        OpeningHoursDto::monday,
        OpeningHoursDto::tuesday,
        OpeningHoursDto::wednesday,
        OpeningHoursDto::thursday,
        OpeningHoursDto::friday,
        OpeningHoursDto::saturday,
        OpeningHoursDto::sunday
    )

    var end = -1L
    var start = -1L

    for ((i, day) in props.withIndex()) {
        val entries = day.get(week)
        val dayOffset = i * DAY_END

        for (time in entries) {
            when {
                start == -1L && time.open != -1L -> {
                    start = time.open + dayOffset
                }
                start != -1L && time.close != -1L -> {
                    answer.add(Range(start, time.close + dayOffset))
                    start = -1L
                }
                end == -1L && time.open == -1L && time.close != -1L -> {
                    end = time.close + i  * DAY_END + WEEK_END
                }
                else -> {
                    throw IllegalStateException(
                        "Bad sequence on ${day.name}: time=$time start=$start end=$end"
                    )
                }
            }
        }
    }
    if (start != -1L && end != -1L) {
        answer.add(Range(start, end))
    }
    return answer
}

private fun intersectRanges(a: List<Range>, b: List<Range>): List<Range> {
    val result = mutableListOf<Range>()
    var i = 0
    var j = 0

    while (i < a.size && j < b.size) {
        val r1 = a[i]
        val r2 = b[j]

        val s = maxOf(r1.s, r2.s)
        val e = minOf(r1.e, r2.e)

        if (s < e) {
            val intersection = Range(s, e)
            if (intersection.len >= MIN_INTERSECTION) {
                result.add(intersection)
            }
        }
        if (r1.e < r2.e) {
            i++
        } else {
            j++
        }
    }
    return result
}

private fun rangesToDtoCutAt6am(ranges: List<Range>): DeliveryHoursResponseDto {
    val perDay = WEEK_DAYS_CAP.associateWith { mutableListOf<String>() }
    for (r in ranges) {
        var s = r.s
        val e = r.e
        while (s < e) {
            val cut = nextCut6amAfter(s)
            val endHere = minOf(e, cut)
            val dayIdx = displayDayIndex(s)
            val openStr = formatClock(s % DAY_END)
            val closeStr = formatClock(endHere % DAY_END)
            perDay[WEEK_DAYS_CAP[dayIdx]]!!.add("$openStr-$closeStr")

            s = endHere
        }
    }
    val final = perDay.mapValues { (_, slots) ->
        if (slots.isEmpty()) "Closed" else slots.joinToString(", ")
    }
    return DeliveryHoursResponseDto(final)
}

private fun displayDayIndex(t: Long): Int =
    (((t - CUT_6AM) floorDiv DAY_END).toInt() % 7 + 7) % 7


private fun nextCut6amAfter(t: Long): Long {
    val dayStartAt6 = ((t - CUT_6AM) floorDiv DAY_END) * DAY_END + CUT_6AM
    return dayStartAt6 + DAY_END
}

infix fun Long.floorDiv(d: Long): Long = Math.floorDiv(this, d)

private fun formatClock(secondsInDay: Long): String {
    val s = ((secondsInDay % DAY_END) + DAY_END) % DAY_END
    val totalMin = s / 60
    val h = (totalMin / 60).toInt()
    val m = (totalMin % 60).toInt()
    return if (m == 0) "%02d".format(h) else "%02d:%02d".format(h, m)
}



