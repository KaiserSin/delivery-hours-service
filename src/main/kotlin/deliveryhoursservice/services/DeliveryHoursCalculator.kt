package deliveryhoursservice.services

import deliveryhoursservice.models.DeliveryHoursResponseDto
import deliveryhoursservice.models.OpeningHoursDto
import java.time.Duration

private val DAY_LENGTH: Duration = Duration.ofDays(1)
private val WEEK_LENGTH: Duration = DAY_LENGTH.multipliedBy(7)
private val MIN_DELIVERY_SLOT: Duration = Duration.ofMinutes(30)
private val DAY_BOUNDARY_AT_6_AM: Duration = Duration.ofHours(6)
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
    val start: Duration,
    val end: Duration,
) {
    val len: Duration
        get() = end.minus(start)
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
    var wrapAroundClose: Duration? = null
    var pendingOpen: Duration? = null
    for ((i, day) in dayAccessors.withIndex()) {
        val entries = day.get(week)
        val dayOffset = DAY_LENGTH.multipliedBy(i.toLong())
        for (time in entries) {
            when {
                pendingOpen == null && time.open != null -> {
                    pendingOpen = dayOffset.plus(time.open.toDuration())
                }

                pendingOpen != null && time.close != null -> {
                    ranges.add(Range(pendingOpen, dayOffset.plus(time.close.toDuration())))
                    pendingOpen = null
                }

                wrapAroundClose == null && time.open == null && time.close != null -> {
                    wrapAroundClose =
                        dayOffset
                            .plus(time.close.toDuration())
                            .plus(WEEK_LENGTH)
                }

                else -> {
                    throw IllegalStateException(
                        "Bad sequence on ${day.name}: time=$time start=$pendingOpen end=$wrapAroundClose",
                    )
                }
            }
        }
    }
    if (pendingOpen != null && wrapAroundClose != null) {
        ranges.add(Range(pendingOpen, wrapAroundClose))
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

        val s = maxOf(r1.start, r2.start)
        val e = minOf(r1.end, r2.end)

        if (s < e) {
            val intersection = Range(s, e)
            if (intersection.len >= MIN_DELIVERY_SLOT) {
                result.add(intersection)
            }
        }
        if (r1.end < r2.end) {
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
        var s = r.start
        val e = r.end
        while (s < e) {
            val cut = nextCut6amAfter(s)
            val endHere = minOf(e, cut)
            val dayIdx = displayDayIndex(s)
            val openStr = formatClock(s.mod(DAY_LENGTH))
            val closeStr = formatClock(endHere.mod(DAY_LENGTH))
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

private fun displayDayIndex(t: Duration): Int = (((t.minus(DAY_BOUNDARY_AT_6_AM)).floorDiv(DAY_LENGTH).toInt() % 7 + 7) % 7)

private fun nextCut6amAfter(t: Duration): Duration {
    val dayStartAt6 =
        DAY_LENGTH.multipliedBy((t.minus(DAY_BOUNDARY_AT_6_AM)).floorDiv(DAY_LENGTH)).plus(DAY_BOUNDARY_AT_6_AM)
    return dayStartAt6.plus(DAY_LENGTH)
}

private fun Duration.floorDiv(d: Duration): Long = Math.floorDiv(this.seconds, d.seconds)

private fun Duration.mod(d: Duration): Duration = Duration.ofSeconds(Math.floorMod(this.seconds, d.seconds))

private fun formatClock(secondsInDay: Duration): String {
    val seconds = Math.floorMod(secondsInDay.seconds, DAY_LENGTH.seconds)
    val totalMin = seconds / 60
    val h = (totalMin / 60).toInt()
    val m = (totalMin % 60).toInt()
    return if (m == 0) "%02d".format(h) else "%02d:%02d".format(h, m)
}
