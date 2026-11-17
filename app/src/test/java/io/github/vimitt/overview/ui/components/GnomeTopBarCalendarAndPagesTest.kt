package io.github.vimitt.overview.ui.components

import io.github.vimitt.overview.domain.model.CalendarEventItem
import io.github.vimitt.overview.domain.model.HomeGridSpec
import io.github.vimitt.overview.domain.model.HomeItem
import io.github.vimitt.overview.domain.model.NativeWidgetType
import io.github.vimitt.overview.ui.home.HomeUiState
import io.github.vimitt.overview.ui.home.HomeWorkspaceItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class GnomeTopBarCalendarAndPagesTest {

    @Test
    fun `shiftMonth wraps across year boundaries`() {
        assertEquals(2025 to Calendar.DECEMBER, shiftMonth(2026, Calendar.JANUARY, -1))
        assertEquals(2027 to Calendar.JANUARY, shiftMonth(2026, Calendar.DECEMBER, 1))
    }

    @Test
    fun `buildMonthMatrix places first day correctly for Sunday and Monday starts`() {
        // November 2026: Nov 1 is Sunday.
        val sundayFirst = buildMonthMatrix(2026, Calendar.NOVEMBER, Calendar.SUNDAY)
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), sundayFirst.first())
        assertEquals(30, sundayFirst.flatten().filterNotNull().last())

        val mondayFirst = buildMonthMatrix(2026, Calendar.NOVEMBER, Calendar.MONDAY)
        assertEquals(listOf(null, null, null, null, null, null, 1), mondayFirst.first())
        assertEquals(30, mondayFirst.flatten().filterNotNull().size)
    }

    @Test
    fun `buildEventsByDayMap keeps UTC all-day events on exact calendar date`() {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2026, Calendar.NOVEMBER, 17, 0, 0, 0)
        }
        val beginMs = utc.timeInMillis
        val endMs = beginMs + 24L * 60L * 60L * 1000L

        val event = CalendarEventItem(
            id = 42L,
            title = "All day",
            beginMs = beginMs,
            endMs = endMs,
            allDay = true,
        )

        val map = buildEventsByDayMap(
            events = listOf(event),
            targetYear = 2026,
            targetMonth = Calendar.NOVEMBER,
        )

        assertEquals(setOf(17), map.keys)
        assertEquals(listOf(event), map[17])
    }

    @Test
    fun `homeUiState pageCount respects stored empty pages and clamps to MAX_PAGES`() {
        val emptyWithThreePages = HomeUiState(storedPageCount = 3)
        assertEquals(3, emptyWithThreePages.pageCount)

        val itemOnPageFour = HomeWorkspaceItem.NativeWidget(
            item = HomeItem.NativeWidget(
                id = "nw_1",
                widgetType = NativeWidgetType.WEATHER,
                cellX = 0,
                cellY = 0,
                page = 3,
            ),
            label = "Weather",
        )
        val withItemOnHigherPage = HomeUiState(
            workspaceItems = listOf(itemOnPageFour),
            storedPageCount = 1,
        )
        assertEquals(4, withItemOnHigherPage.pageCount)

        val clamped = HomeUiState(storedPageCount = HomeGridSpec.MAX_PAGES + 4)
        assertEquals(HomeGridSpec.MAX_PAGES, clamped.pageCount)
        assertTrue(clamped.pageCount == 5)
    }
}
