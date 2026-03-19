    package com.isacsilva.ufumessenger.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.isacsilva.ufumessenger.domain.model.User
import java.util.concurrent.TimeUnit

@Composable
fun rememberFormattedUserStatus(user: User?): String {
    val context = LocalContext.current
    return remember(user?.presenceStatus, user?.lastSeen) { // Recalcula se o status ou lastSeen mudar
        formatUserStatus(user, context)
    }
}

fun formatUserStatus(user: User?, context: Context): String {
    if (user == null) return ""

    return when (user.presenceStatus) {
        "Online" -> "Online"
        "Offline" -> {
            val lastSeenTime = user.lastSeen
            if (lastSeenTime == null || lastSeenTime <= 0) {
                "Offline"
            } else {
                val now = System.currentTimeMillis()
                val diff = now - lastSeenTime

                val oneMinuteInMillis = 60 * 1000L
                val oneHourInMillis = 60 * oneMinuteInMillis

                when {
                    diff < oneMinuteInMillis -> "Visto por último: agora mesmo"
                    diff < oneHourInMillis -> {
                        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                        "Visto por último: há $minutes min"
                    }
                    else -> {
                        val lastSeenCalendar = java.util.Calendar.getInstance().apply { timeInMillis = lastSeenTime }
                        val todayCalendar = java.util.Calendar.getInstance()

                        val timeFormat = android.text.format.DateFormat.getTimeFormat(context)
                        val dateFormat = android.text.format.DateFormat.getDateFormat(context)

                        if (lastSeenCalendar.get(java.util.Calendar.YEAR) == todayCalendar.get(java.util.Calendar.YEAR) &&
                            lastSeenCalendar.get(java.util.Calendar.DAY_OF_YEAR) == todayCalendar.get(java.util.Calendar.DAY_OF_YEAR)) {
                            "Visto por último: hoje às ${timeFormat.format(lastSeenCalendar.time)}"
                        } else {
                            val yesterdayCalendar = java.util.Calendar.getInstance().apply {
                                timeInMillis = todayCalendar.timeInMillis
                                add(java.util.Calendar.DAY_OF_YEAR, -1)
                            }
                            if (lastSeenCalendar.get(java.util.Calendar.YEAR) == yesterdayCalendar.get(java.util.Calendar.YEAR) &&
                                lastSeenCalendar.get(java.util.Calendar.DAY_OF_YEAR) == yesterdayCalendar.get(java.util.Calendar.DAY_OF_YEAR)) {
                                "Visto por último: ontem às ${timeFormat.format(lastSeenCalendar.time)}"
                            } else {
                                "Visto em ${dateFormat.format(lastSeenCalendar.time)} às ${timeFormat.format(lastSeenCalendar.time)}"
                            }
                        }
                    }
                }
            }
        }
        else -> user.presenceStatus ?: "" // Fallback
    }
}
