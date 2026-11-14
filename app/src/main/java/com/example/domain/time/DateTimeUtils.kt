package com.example.domain.time

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class GreetingPeriod {
  MORNING,
  AFTERNOON,
  EVENING,
}

object DateTimeUtils {
  fun greetingPeriod(nowMillis: Long = System.currentTimeMillis()): GreetingPeriod {
    val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    return when {
      hour < 12 -> GreetingPeriod.MORNING
      hour < 17 -> GreetingPeriod.AFTERNOON
      else -> GreetingPeriod.EVENING
    }
  }

  fun startOfDay(millis: Long): Long {
    val cal = Calendar.getInstance().apply {
      timeInMillis = millis
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
  }

  fun endOfDay(millis: Long): Long {
    val cal = Calendar.getInstance().apply {
      timeInMillis = millis
      set(Calendar.HOUR_OF_DAY, 23)
      set(Calendar.MINUTE, 59)
      set(Calendar.SECOND, 59)
      set(Calendar.MILLISECOND, 999)
    }
    return cal.timeInMillis
  }

  fun addDays(millis: Long, days: Int): Long {
    val cal = Calendar.getInstance().apply {
      timeInMillis = millis
      add(Calendar.DAY_OF_YEAR, days)
    }
    return cal.timeInMillis
  }

  fun isSameDay(a: Long, b: Long): Boolean {
    val calA = Calendar.getInstance().apply { timeInMillis = a }
    val calB = Calendar.getInstance().apply { timeInMillis = b }
    return calA.get(Calendar.YEAR) == calB.get(Calendar.YEAR) &&
      calA.get(Calendar.DAY_OF_YEAR) == calB.get(Calendar.DAY_OF_YEAR)
  }

  fun isOverdue(dueAt: Long?, isAllDay: Boolean, nowMillis: Long = System.currentTimeMillis()): Boolean {
    if (dueAt == null) return false
    return if (isAllDay) {
      dueAt < startOfDay(nowMillis)
    } else {
      dueAt < nowMillis
    }
  }

  fun isDueToday(dueAt: Long?, nowMillis: Long = System.currentTimeMillis()): Boolean {
    if (dueAt == null) return false
    return isSameDay(dueAt, nowMillis)
  }

  fun isUpcoming(dueAt: Long?, nowMillis: Long = System.currentTimeMillis()): Boolean {
    if (dueAt == null) return false
    return dueAt > endOfDay(nowMillis)
  }

  fun upcomingWeekendMillis(nowMillis: Long = System.currentTimeMillis()): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = startOfDay(nowMillis) }
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val daysUntilSaturday =
      when (dayOfWeek) {
        Calendar.SATURDAY -> 0
        Calendar.SUNDAY -> 6
        else -> Calendar.SATURDAY - dayOfWeek
      }
    cal.add(Calendar.DAY_OF_YEAR, if (daysUntilSaturday == 0) 7 else daysUntilSaturday)
    return endOfDay(cal.timeInMillis)
  }

  fun nextWeekMondayMillis(nowMillis: Long = System.currentTimeMillis()): Long {
    val cal = Calendar.getInstance().apply { timeInMillis = startOfDay(nowMillis) }
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val daysUntilMonday = ((Calendar.MONDAY - dayOfWeek + 7) % 7).let { if (it == 0) 7 else it }
    cal.add(Calendar.DAY_OF_YEAR, daysUntilMonday)
    return endOfDay(cal.timeInMillis)
  }

  fun combineDateAndTime(dateMillis: Long, hourOfDay: Int, minute: Int): Long {
    val cal = Calendar.getInstance().apply {
      timeInMillis = dateMillis
      set(Calendar.HOUR_OF_DAY, hourOfDay)
      set(Calendar.MINUTE, minute)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
  }

  fun localDateToUtcMidnight(localMillis: Long): Long {
    val localCal = Calendar.getInstance().apply { timeInMillis = localMillis }
    val utcCal =
      Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(
          localCal.get(Calendar.YEAR),
          localCal.get(Calendar.MONTH),
          localCal.get(Calendar.DAY_OF_MONTH),
          0,
          0,
          0,
        )
        set(Calendar.MILLISECOND, 0)
      }
    return utcCal.timeInMillis
  }

  fun utcMidnightToLocalStartOfDay(utcMillis: Long): Long {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    val localCal =
      Calendar.getInstance().apply {
        clear()
        set(
          utcCal.get(Calendar.YEAR),
          utcCal.get(Calendar.MONTH),
          utcCal.get(Calendar.DAY_OF_MONTH),
          0,
          0,
          0,
        )
        set(Calendar.MILLISECOND, 0)
      }
    return localCal.timeInMillis
  }

  fun formatFullDate(millis: Long): String {
    val fmt = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    return fmt.format(Date(millis))
  }

  fun formatShortDate(millis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    if (isSameDay(millis, nowMillis)) return "Today"
    if (isSameDay(millis, addDays(nowMillis, 1))) return "Tomorrow"
    if (isSameDay(millis, addDays(nowMillis, -1))) return "Yesterday"
    val fmt = SimpleDateFormat("MMM d", Locale.getDefault())
    return fmt.format(Date(millis))
  }

  fun formatTime(millis: Long): String {
    val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
    return fmt.format(Date(millis))
  }

  fun formatDueLabel(dueAt: Long?, isAllDay: Boolean, nowMillis: Long = System.currentTimeMillis()): String? {
    if (dueAt == null) return null
    val datePart = formatShortDate(dueAt, nowMillis)
    return if (isAllDay) datePart else "$datePart · ${formatTime(dueAt)}"
  }

  fun formatDateTimeShort(millis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    return "${formatShortDate(millis, nowMillis)} · ${formatTime(millis)}"
  }

  fun defaultAlertTimeMillis(nowMillis: Long = System.currentTimeMillis()): Long {
    val cal = Calendar.getInstance().apply {
      timeInMillis = nowMillis
      add(Calendar.HOUR_OF_DAY, 1)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
  }

  fun tomorrowAtTimeMillis(hourOfDay: Int = 9, minute: Int = 0, nowMillis: Long = System.currentTimeMillis()): Long {
    return combineDateAndTime(addDays(nowMillis, 1), hourOfDay, minute)
  }

  fun formatMonthYear(year: Int, monthZeroBased: Int): String {
    val cal = Calendar.getInstance().apply {
      set(Calendar.YEAR, year)
      set(Calendar.MONTH, monthZeroBased)
      set(Calendar.DAY_OF_MONTH, 1)
    }
    val fmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    return fmt.format(cal.time)
  }

  fun getDaysInMonthGrid(year: Int, monthZeroBased: Int): List<Long?> {
    val cal = Calendar.getInstance().apply {
      set(Calendar.YEAR, year)
      set(Calendar.MONTH, monthZeroBased)
      set(Calendar.DAY_OF_MONTH, 1)
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Sun=1..Sat=7
    val leadingEmpty = firstDayOfWeek - Calendar.SUNDAY
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val result = mutableListOf<Long?>()
    repeat(leadingEmpty) { result.add(null) }
    for (day in 1..daysInMonth) {
      cal.set(Calendar.DAY_OF_MONTH, day)
      result.add(cal.timeInMillis)
    }
    while (result.size % 7 != 0) {
      result.add(null)
    }
    return result
  }

  fun getWeekDays(anchorMillis: Long): List<Long> {
    val cal = Calendar.getInstance().apply {
      timeInMillis = startOfDay(anchorMillis)
      set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    }
    return (0..6).map { offset ->
      val dayCal = Calendar.getInstance().apply {
        timeInMillis = cal.timeInMillis
        add(Calendar.DAY_OF_YEAR, offset)
      }
      dayCal.timeInMillis
    }
  }
}
