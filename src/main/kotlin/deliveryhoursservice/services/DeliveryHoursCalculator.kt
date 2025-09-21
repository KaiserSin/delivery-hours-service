package deliveryhoursservice.services

import deliveryhoursservice.models.DeliveryHoursResponseDto
import deliveryhoursservice.models.OpeningHoursDto

private const val DAY_LENGTH_SECONDS = 24 * 60 * 60L
private const val WEEK_LENGTH_SECONDS = DAY_LENGTH_SECONDS * 7
private const val MIN_DELIVERY_SLOT_SECONDS = 30 * 60L
private const val DAY_BOUNDARY_AT_6_AM_SECONDS = 6L * 60 * 60
private val WEEKDAY_NAMES =
    listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

fun calculateDeliveryHours(
    venueTimeDto: OpeningHoursDto,
    courierTimeDto: OpeningHoursDto,
): DeliveryHoursResponseDto {
    val venueTime = convertTime(venueTimeDto)
    val courierTime = convertTime(courierTimeDto)
    val resultTime = intersectRanges(venueTime, courierTime)
    return rangesToDtoCutAt6am(resultTime)
}

private data class Range(
    val startSeconds: Long,
    val endSeconds: Long,
) {
    val len: Long
        get() = endSeconds - startSeconds
}

private fun convertTime(week: OpeningHoursDto): List<Range> {
    val ranges = mutableListOf<Range>()
    val dayAccessors =
        listOf(
            OpeningHoursDto::monday,
            OpeningHoursDto::tuesday,
            OpeningHoursDto::wednesday,
            OpeningHoursDto::thursday,
            OpeningHoursDto::friday,
            OpeningHoursDto::saturday,
            OpeningHoursDto::sunday,
        )
    var end = -1L
    var start = -1L
    for ((i, day) in dayAccessors.withIndex()) {
        val entries = day.get(week)
        val dayOffset = i * DAY_LENGTH_SECONDS
        for (time in entries) {
            when {
                start == -1L && time.open != -1L -> {
                    start = time.open + dayOffset
                }

                start != -1L && time.close != -1L -> {
                    ranges.add(Range(start, time.close + dayOffset))
                    start = -1L
                }

                end == -1L && time.open == -1L && time.close != -1L -> {
                    end = time.close + i * DAY_LENGTH_SECONDS + WEEK_LENGTH_SECONDS
                }

                else -> {
                    throw IllegalStateException(
                        "Bad sequence on ${day.name}: time=$time start=$start end=$end",
                    )
                }
            }
        }
    }
    if (start != -1L && end != -1L) {
        ranges.add(Range(start, end))
    }
    return ranges
}

private fun intersectRanges(
    a: List<Range>,
    b: List<Range>,
): List<Range> {
    val result = mutableListOf<Range>()
    var i = 0
    var j = 0

    while (i < a.size && j < b.size) {
        val r1 = a[i]
        val r2 = b[j]

        val s = maxOf(r1.startSeconds, r2.startSeconds)
        val e = minOf(r1.endSeconds, r2.endSeconds)

        if (s < e) {
            val intersection = Range(s, e)
            if (intersection.len >= MIN_DELIVERY_SLOT_SECONDS) {
                result.add(intersection)
            }
        }
        if (r1.endSeconds < r2.endSeconds) {
            i++
        } else {
            j++
        }
    }
    return result
}

private fun rangesToDtoCutAt6am(ranges: List<Range>): DeliveryHoursResponseDto {
    val perDay = WEEKDAY_NAMES.associateWith { mutableListOf<String>() }
    for (r in ranges) {
        var s = r.startSeconds
        val e = r.endSeconds
        while (s < e) {
            val cut = nextCut6amAfter(s)
            val endHere = minOf(e, cut)
            val dayIdx = displayDayIndex(s)
            val openStr = formatClock(s % DAY_LENGTH_SECONDS)
            val closeStr = formatClock(endHere % DAY_LENGTH_SECONDS)
            perDay[WEEKDAY_NAMES[dayIdx]]!!.add("$openStr-$closeStr")

            s = endHere
        }
    }
    val final =
        perDay.mapValues { (_, slots) ->
            if (slots.isEmpty()) "Closed" else slots.joinToString(", ")
        }
    return DeliveryHoursResponseDto(final)
}

private fun displayDayIndex(t: Long): Int = (((t - DAY_BOUNDARY_AT_6_AM_SECONDS) floorDiv DAY_LENGTH_SECONDS).toInt() % 7 + 7) % 7

private fun nextCut6amAfter(t: Long): Long {
    val dayStartAt6 =
        ((t - DAY_BOUNDARY_AT_6_AM_SECONDS) floorDiv DAY_LENGTH_SECONDS) * DAY_LENGTH_SECONDS +
            DAY_BOUNDARY_AT_6_AM_SECONDS
    return dayStartAt6 + DAY_LENGTH_SECONDS
}

infix fun Long.floorDiv(d: Long): Long = Math.floorDiv(this, d)

private fun formatClock(secondsInDay: Long): String {
    val s = ((secondsInDay % DAY_LENGTH_SECONDS) + DAY_LENGTH_SECONDS) % DAY_LENGTH_SECONDS
    val totalMin = s / 60
    val h = (totalMin / 60).toInt()
    val m = (totalMin % 60).toInt()
    return if (m == 0) "%02d".format(h) else "%02d:%02d".format(h, m)
}
