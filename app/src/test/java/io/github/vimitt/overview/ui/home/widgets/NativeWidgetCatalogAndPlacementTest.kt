package io.github.vimitt.overview.ui.home.widgets

import io.github.vimitt.overview.domain.model.HomeGridSpec
import io.github.vimitt.overview.domain.model.HomeItem
import io.github.vimitt.overview.domain.model.NativeWidgetType
import io.github.vimitt.overview.domain.usecase.GridPlacement
import io.github.vimitt.overview.domain.usecase.HomeGridPlacementUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeWidgetCatalogAndPlacementTest {

    @Test
    fun `catalog exposes all 10 native widgets with valid spans`() {
        assertEquals(10, NativeWidgetType.entries.size)
        for (type in NativeWidgetType.entries) {
            assertTrue(type.defaultSpanX in type.minSpanX..HomeGridSpec.COLUMNS)
            assertTrue(type.defaultSpanY in type.minSpanY..HomeGridSpec.ROWS)
            assertNotEquals(0, type.titleRes)
            assertNotEquals(0, type.descriptionRes)
        }
    }

    @Test
    fun `native widgets participate cleanly in workspace grid placement`() {
        val placementUseCase = HomeGridPlacementUseCase(columns = 4, rows = 5)
        val summary = HomeItem.NativeWidget(
            id = "nw_1",
            widgetType = NativeWidgetType.DAILY_SUMMARY,
            cellX = 0,
            cellY = 0,
            spanX = NativeWidgetType.DAILY_SUMMARY.defaultSpanX,
            spanY = NativeWidgetType.DAILY_SUMMARY.defaultSpanY,
        )

        assertFalse(placementUseCase.canPlace(listOf(summary), cellX = 1, cellY = 0, spanX = 2, spanY = 1))

        val weatherSlot = placementUseCase.findAvailableSlot(
            existingItems = listOf(summary),
            desiredSpanX = NativeWidgetType.WEATHER.defaultSpanX,
            desiredSpanY = NativeWidgetType.WEATHER.defaultSpanY,
        )
        assertEquals(GridPlacement(cellX = 0, cellY = 1, spanX = 4, spanY = 2), weatherSlot)
    }

    @Test
    fun `wmo weather code mapping covers clear rain fog snow and storm`() {
        val mapped = listOf(0, 2, 3, 45, 53, 63, 73, 95).map(::wmoWeatherDescriptionRes)
        assertEquals(8, mapped.size)
        assertTrue(mapped.distinct().size == 8)
    }
}
