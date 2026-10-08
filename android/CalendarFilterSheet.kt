// CalendarFilterSheet: a date filter in a Material 3 bottom sheet that stays the same size however
// many years of data you have. It opens on a year of months with counts, then a month of days with
// a dot where something exists. Pick a whole year, a month or a day.
//
//   if (open) CalendarFilterSheet(counts, value, onPick = { value = it; open = false }, onDismiss = { open = false })
//
// counts: "yyyy-MM-dd" -> number of items that day (build the keys with dayKey).
// value:  null (any time) | "yyyy" | "yyyy-MM" | "yyyy-MM-dd". Filter with dayKey(time).startsWith(value).
package com.voypalante.calendarfilter

import android.icu.text.DateFormatSymbols
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** "yyyy-MM-dd" for an instant, in the phone's time zone. */
fun dayKey(ms: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = ms }
    return "%04d-%02d-%02d".format(Locale.US, c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
}

data class CalendarFilterLabels(
    val anyTime: String = "Any time",
    val previous: String = "Previous",
    val next: String = "Next",
    val allMonthsOf: (Int) -> String = { "All months of $it" },
    val count: (Int) -> String = { n -> "$n ${if (n == 1) "entry" else "entries"}" },
) {
    companion object {
        val Spanish = CalendarFilterLabels(
            anyTime = "Cualquier fecha",
            previous = "Anterior",
            next = "Siguiente",
            allMonthsOf = { "Todos los meses de $it" },
            count = { n -> "$n ${if (n == 1) "entrada" else "entradas"}" },
        )
    }
}

/** Text for a button showing the current value: null, "2026", "October 2026", "Oct 6, 2026". */
fun calendarFilterLabel(value: String?, locale: Locale = Locale.getDefault()): String? {
    if (value.isNullOrEmpty()) return null
    val parts = value.split("-").map { it.toInt() }
    val c = Calendar.getInstance().apply { clear(); set(parts[0], (parts.getOrNull(1) ?: 1) - 1, parts.getOrNull(2) ?: 1) }
    fun fmt(skeleton: String) = SimpleDateFormat(android.text.format.DateFormat.getBestDateTimePattern(locale, skeleton), locale).format(c.time)
    return when (parts.size) {
        1 -> value
        2 -> fmt("MMMMyyyy").replaceFirstChar { it.uppercase(locale) }
        else -> fmt("dMMMyyyy")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarFilterSheet(
    counts: Map<String, Int>,
    value: String?,
    onPick: (String?) -> Unit,
    onDismiss: () -> Unit,
    labels: CalendarFilterLabels = CalendarFilterLabels(),
    locale: Locale = Locale.getDefault(),
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val cs = MaterialTheme.colorScheme
    val now = remember { Calendar.getInstance() }
    val years = remember(counts) { counts.keys.map { it.take(4).toInt() }.toSortedSet() }
    val minYear = years.firstOrNull() ?: now.get(Calendar.YEAR)
    val maxYear = maxOf(years.lastOrNull() ?: now.get(Calendar.YEAR), now.get(Calendar.YEAR))
    var year by remember { mutableStateOf(value?.take(4)?.toIntOrNull() ?: years.lastOrNull() ?: now.get(Calendar.YEAR)) }
    var month by remember { mutableStateOf(value?.takeIf { it.length >= 7 }?.substring(5, 7)?.toInt()) }
    fun sum(prefix: String) = counts.entries.sumOf { if (it.key.startsWith(prefix)) it.value else 0 }
    fun monthName(m: Int, pattern: String): String {
        val cal = Calendar.getInstance().apply { clear(); set(2026, m - 1, 1) }
        return SimpleDateFormat(pattern, locale).format(cal.time).trimEnd('.').replaceFirstChar { it.uppercase(locale) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 16.dp)) {
            val m = month
            val key = if (m == null) "$year" else "%04d-%02d".format(Locale.US, year, m)
            val atStart = if (m == null) year <= minYear else year <= minYear && m == 1
            val atEnd = if (m == null) year >= maxYear else year >= maxYear && m == 12
            fun step(dir: Int) {
                if (m == null) { year += dir; return }
                var nm = m + dir
                if (nm < 1) { nm = 12; year-- } else if (nm > 12) { nm = 1; year++ }
                month = nm
            }

            // Header: < title (tap it to pick the whole year or month) >
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton({ step(-1) }, enabled = !atStart) { Text("‹", fontSize = 26.sp) }
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .background(if (value == key) cs.surfaceVariant else Color.Transparent)
                        .clickable(enabled = sum(key) > 0) { onPick(key) }.padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(if (m == null) "$year" else "${monthName(m, "LLLL")} $year", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
                    Text(labels.count(sum(key)), fontSize = 12.sp, color = cs.onSurfaceVariant)
                }
                IconButton({ step(1) }, enabled = !atEnd) { Text("›", fontSize = 26.sp) }
            }
            Spacer(Modifier.height(12.dp))

            if (m == null) {
                // Twelve months, three per row, with how many items each has.
                for (row in 0 until 4) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (col in 0 until 3) {
                            val mm = row * 3 + col + 1
                            val mk = "%04d-%02d".format(Locale.US, year, mm)
                            val n = sum(mk)
                            val on = value?.startsWith(mk) == true
                            Column(
                                Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                                    .background(if (on) cs.inverseSurface else if (n > 0) cs.surfaceVariant else Color.Transparent)
                                    .clickable(enabled = n > 0) { month = mm }
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text(monthName(mm, "LLL"), fontWeight = FontWeight.Medium,
                                    color = if (on) cs.inverseOnSurface else if (n > 0) cs.onSurface else cs.onSurface.copy(alpha = .35f))
                                Text(if (n > 0) "$n" else " ", fontSize = 11.sp,
                                    color = if (on) cs.inverseOnSurface.copy(alpha = .7f) else cs.onSurfaceVariant)
                            }
                        }
                    }
                }
            } else {
                // Weekday initials and the days, starting on the locale's first day of the week.
                val first = Calendar.getInstance(locale).firstDayOfWeek // Sunday = 1
                val cal = Calendar.getInstance().apply { clear(); set(year, m - 1, 1) }
                val lead = (cal.get(Calendar.DAY_OF_WEEK) - first + 7) % 7
                val days = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                val narrow = DateFormatSymbols.getInstance(locale).getWeekdays(DateFormatSymbols.STANDALONE, DateFormatSymbols.NARROW)
                Row(Modifier.fillMaxWidth()) {
                    for (i in 0 until 7) {
                        Text(narrow[(first - 1 + i) % 7 + 1].uppercase(locale), fontSize = 11.sp, color = cs.onSurfaceVariant,
                            textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    }
                }
                Spacer(Modifier.height(6.dp))
                val today = dayKey(System.currentTimeMillis())
                for (row in 0 until (lead + days + 6) / 7) {
                    Row(Modifier.fillMaxWidth()) {
                        for (col in 0 until 7) {
                            val d = row * 7 + col - lead + 1
                            Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                                if (d in 1..days) {
                                    val dk = "%s-%02d".format(Locale.US, key, d)
                                    val n = counts[dk] ?: 0
                                    val on = value == dk
                                    Box(
                                        Modifier.size(40.dp).clip(CircleShape)
                                            .background(if (on) cs.inverseSurface else Color.Transparent)
                                            .then(if (dk == today && !on) Modifier.border(1.dp, cs.outlineVariant, CircleShape) else Modifier)
                                            .clickable(enabled = n > 0) { onPick(dk) },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text("$d", fontWeight = if (n > 0) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (on) cs.inverseOnSurface else if (n > 0) cs.onSurface else cs.onSurface.copy(alpha = .4f))
                                        if (n > 0) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp).size(4.dp).clip(CircleShape)
                                            .background(if (on) cs.inverseOnSurface else accent))
                                    }
                                }
                            }
                        }
                    }
                }
                Text(
                    labels.allMonthsOf(year), color = accent, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { month = null }.padding(10.dp),
                )
            }

            Spacer(Modifier.height(4.dp))
            HorizontalDivider()
            Text(
                labels.anyTime, color = accent, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onPick(null) }.padding(12.dp),
            )
        }
    }
}
