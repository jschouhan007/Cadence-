package com.example

import com.example.domain.time.DateTimeUtils
import com.example.domain.time.GreetingPeriod
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DateTimeUtilsTest {

  @Test
  fun `greeting period matches time of day`() {
    val cal = Calendar.getInstance()
    cal.set(2026, Calendar.NOVEMBER, 12, 9, 0, 0)
    assertEquals(GreetingPeriod.MORNING, DateTimeUtils.greetingPeriod(cal.timeInMillis))

    cal.set(2026, Calendar.NOVEMBER, 12, 14, 0, 0)
    assertEquals(GreetingPeriod.AFTERNOON, DateTimeUtils.greetingPeriod(cal.timeInMillis))

    cal.set(2026, Calendar.NOVEMBER, 12, 20, 0, 0)
    assertEquals(GreetingPeriod.EVENING, DateTimeUtils.greetingPeriod(cal.timeInMillis))
  }

  @Test
  fun `overdue and due today classification handles all day and timed tasks`() {
    val nowCal = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 12, 14, 0, 0) }
    val now = nowCal.timeInMillis

    val yesterdayEnd = DateTimeUtils.endOfDay(DateTimeUtils.addDays(now, -1))
    val todayEnd = DateTimeUtils.endOfDay(now)
    val earlierTodayTimed = DateTimeUtils.combineDateAndTime(now, 10, 0)
    val tomorrowEnd = DateTimeUtils.endOfDay(DateTimeUtils.addDays(now, 1))

    assertTrue(DateTimeUtils.isOverdue(yesterdayEnd, isAllDay = true, nowMillis = now))
    assertFalse(DateTimeUtils.isOverdue(todayEnd, isAllDay = true, nowMillis = now))
    assertTrue(DateTimeUtils.isOverdue(earlierTodayTimed, isAllDay = false, nowMillis = now))
    assertTrue(DateTimeUtils.isDueToday(todayEnd, nowMillis = now))
    assertTrue(DateTimeUtils.isUpcoming(tomorrowEnd, nowMillis = now))
  }

  @Test
  fun `month grid aligns to 7-day weeks`() {
    val grid = DateTimeUtils.getDaysInMonthGrid(2026, Calendar.NOVEMBER)
    assertTrue(grid.size % 7 == 0)
    assertEquals(30, grid.count { it != null })
  }
}
