package com.iiex.languageassistant.util

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class DateTimeUtil {
    companion object {
        @RequiresApi(Build.VERSION_CODES.O)
        fun timestampToDateTime(timestamp: Long): LocalDateTime {
            val instant = Instant.ofEpochSecond(timestamp)
            val zoneId = ZoneId.of("UTC")
            return instant.atZone(zoneId).toLocalDateTime()
        }

        @RequiresApi(Build.VERSION_CODES.O)
        fun getDateFromTimestamp(timestamp: Long): String {
            val timestampDateTime = timestampToDateTime(timestamp)
            val now = LocalDateTime.now(ZoneId.of("UTC"))

            val secondsAgo = ChronoUnit.SECONDS.between(timestampDateTime, now)
            val minutesAgo = ChronoUnit.MINUTES.between(timestampDateTime, now)
            val hoursAgo = ChronoUnit.HOURS.between(timestampDateTime, now)
            val daysAgo = ChronoUnit.DAYS.between(timestampDateTime, now)
            val weeksAgo = ChronoUnit.WEEKS.between(timestampDateTime, now)
            val monthsAgo = ChronoUnit.MONTHS.between(timestampDateTime, now)

            return when {
                secondsAgo < 60 -> "Vừa tức thì"
                minutesAgo < 60 -> "$minutesAgo phút  trước"
                hoursAgo < 24 -> "$hoursAgo giờ trước"
                daysAgo < 7 -> "$daysAgo ngày trước"
                weeksAgo < 5 -> "$weeksAgo tuần trước"
                monthsAgo < 12 -> "$monthsAgo tháng trước"
                else -> {
                    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    timestampDateTime.format(formatter)
                }
            }
        }
    }
}