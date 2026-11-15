package io.github.vimitt.overview.domain.usecase

import io.github.vimitt.overview.domain.model.HomeItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeGridPlacementUseCaseTest {

    private val useCase = HomeGridPlacementUseCase(columns = 4, rows = 5)

    @Test
    fun `finds first available slot for 1x1 shortcut`() {
        val existing = listOf(
            HomeItem.AppShortcut("a", "pkg.a", "Main", 0, cellX = 0, cellY = 0),
            HomeItem.AppShortcut("b", "pkg.b", "Main", 0, cellX = 1, cellY = 0),
        )

        val slot = useCase.findAvailableSlot(existing, desiredSpanX = 1, desiredSpanY = 1)
        assertEquals(GridPlacement(2, 0, 1, 1), slot)
    }

    @Test
    fun `skips occupied cells when placing 2x2 widget`() {
        val existing = listOf(
            HomeItem.AppShortcut("a", "pkg.a", "Main", 0, cellX = 0, cellY = 0),
        )

        val slot = useCase.findAvailableSlot(existing, desiredSpanX = 2, desiredSpanY = 2)
        assertEquals(GridPlacement(1, 0, 2, 2), slot)
    }

    @Test
    fun `shrinks widget span when full size does not fit`() {
        val existing = listOf(
            HomeItem.Widget("w1", 1, "pkg", "Cls", cellX = 0, cellY = 0, spanX = 4, spanY = 4),
        )

        val slot = useCase.findAvailableSlot(existing, desiredSpanX = 4, desiredSpanY = 2)
        assertEquals(GridPlacement(0, 4, 4, 1), slot)
    }

    @Test
    fun `returns null when grid is completely full`() {
        val existing = listOf(
            HomeItem.Widget("w1", 1, "pkg", "Cls", cellX = 0, cellY = 0, spanX = 4, spanY = 5),
        )

        val slot = useCase.findAvailableSlot(existing, desiredSpanX = 1, desiredSpanY = 1)
        assertNull(slot)
    }

    @Test
    fun `canPlace ignores target item id during move or resize`() {
        val existing = listOf(
            HomeItem.Widget("w1", 1, "pkg", "Cls", cellX = 0, cellY = 0, spanX = 2, spanY = 2),
        )

        assertTrue(useCase.canPlace(existing, cellX = 1, cellY = 0, spanX = 2, spanY = 2, ignoreItemId = "w1"))
        assertFalse(useCase.canPlace(existing, cellX = 1, cellY = 0, spanX = 2, spanY = 2))
    }
}
