package io.github.vimitt.overview.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.HomeItem
import io.github.vimitt.overview.domain.usecase.HomeGridPlacementUseCase
import io.github.vimitt.overview.ui.home.HomeWorkspaceItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DragSessionTest {

    private val placementUseCase = HomeGridPlacementUseCase(columns = 4, rows = 5)
    private val gridBounds = Rect(left = 0f, top = 100f, right = 400f, bottom = 600f)
    private val removeZoneBounds = Rect(left = 100f, top = 20f, right = 300f, bottom = 80f)
    private val sampleApp = AppInfo(
        packageName = "com.example.app",
        activityName = "MainActivity",
        label = "App",
        icon = null,
    )

    @Test
    fun `maps overview drag onto exact free cell`() {
        val target = computeHomeDropTarget(
            pointerInWindow = Offset(x = 250f, y = 450f), // coluna 2, linha 3
            gridBounds = gridBounds,
            removeZoneBounds = null,
            payload = DragPayload.FromOverview(sampleApp),
            canPlace = { cellX, cellY, spanX, spanY, ignoreId ->
                placementUseCase.canPlace(emptyList(), cellX, cellY, spanX, spanY, ignoreId)
            },
        )

        assertEquals(
            HomeDropTarget(
                cellX = 2,
                cellY = 3,
                spanX = 1,
                spanY = 1,
                isValid = true,
                isOverRemoveZone = false,
            ),
            target,
        )
    }

    @Test
    fun `marks occupied cell as invalid drop target`() {
        val occupied = listOf(
            HomeItem.AppShortcut("s1", "pkg.other", "Main", 0, cellX = 1, cellY = 2),
        )

        val target = computeHomeDropTarget(
            pointerInWindow = Offset(x = 150f, y = 350f), // coluna 1, linha 2
            gridBounds = gridBounds,
            removeZoneBounds = null,
            payload = DragPayload.FromOverview(sampleApp),
            canPlace = { cellX, cellY, spanX, spanY, ignoreId ->
                placementUseCase.canPlace(occupied, cellX, cellY, spanX, spanY, ignoreId)
            },
        )

        assertFalse(target!!.isValid)
        assertEquals(1, target.cellX)
        assertEquals(2, target.cellY)
    }

    @Test
    fun `home drag ignores own previous footprint and enters remove zone`() {
        val widgetItem = HomeItem.Widget(
            id = "w1",
            appWidgetId = 10,
            providerPackage = "pkg.widget",
            providerClass = "Provider",
            cellX = 0,
            cellY = 0,
            spanX = 2,
            spanY = 2,
        )
        val existing = listOf(widgetItem)
        val payload = DragPayload.FromHome(
            HomeWorkspaceItem.Widget(item = widgetItem, providerInfo = null, label = "Clock"),
        )

        val overOwnCell = computeHomeDropTarget(
            pointerInWindow = Offset(x = 150f, y = 220f),
            gridBounds = gridBounds,
            removeZoneBounds = removeZoneBounds,
            payload = payload,
            canPlace = { cellX, cellY, spanX, spanY, ignoreId ->
                placementUseCase.canPlace(existing, cellX, cellY, spanX, spanY, ignoreId)
            },
        )
        assertTrue(overOwnCell!!.isValid)
        assertFalse(overOwnCell.isOverRemoveZone)

        val overRemove = computeHomeDropTarget(
            pointerInWindow = Offset(x = 200f, y = 50f),
            gridBounds = gridBounds,
            removeZoneBounds = removeZoneBounds,
            payload = payload,
            canPlace = { cellX, cellY, spanX, spanY, ignoreId ->
                placementUseCase.canPlace(existing, cellX, cellY, spanX, spanY, ignoreId)
            },
        )
        assertTrue(overRemove!!.isOverRemoveZone)
    }

    @Test
    fun `returns null when pointer drops far below workspace grid`() {
        val target = computeHomeDropTarget(
            pointerInWindow = Offset(x = 200f, y = 720f),
            gridBounds = gridBounds,
            removeZoneBounds = null,
            payload = DragPayload.FromOverview(sampleApp),
            canPlace = { _, _, _, _, _ -> true },
        )

        assertNull(target)
    }
}
