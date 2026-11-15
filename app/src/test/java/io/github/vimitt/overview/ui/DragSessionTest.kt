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

    @Test
    fun `maps overview drag using custom grid columns and rows`() {
        val target = computeHomeDropTarget(
            pointerInWindow = Offset(x = 390f, y = 590f),
            gridBounds = gridBounds,
            removeZoneBounds = null,
            payload = DragPayload.FromOverview(sampleApp),
            canPlace = { cellX, cellY, spanX, spanY, ignoreId ->
                placementUseCase.canPlace(
                    existingItems = emptyList(),
                    cellX = cellX,
                    cellY = cellY,
                    spanX = spanX,
                    spanY = spanY,
                    ignoreItemId = ignoreId,
                    columns = 6,
                    rows = 6,
                )
            },
            gridColumns = 6,
            gridRows = 6,
        )

        assertEquals(5, target!!.cellX)
        assertEquals(5, target.cellY)
        assertTrue(target.isValid)
    }

    @Test
    fun `maps app drags onto corresponding dock slot`() {
        val dockBounds = Rect(left = 50f, top = 700f, right = 450f, bottom = 780f) // largura 400 -> 5 slots de 80px

        val targetSlot0 = computeDockDropTarget(
            pointerInWindow = Offset(x = 80f, y = 740f),
            dockBounds = dockBounds,
            itemCount = 5,
            payload = DragPayload.FromOverview(sampleApp),
        )
        val targetSlot3 = computeDockDropTarget(
            pointerInWindow = Offset(x = 310f, y = 740f),
            dockBounds = dockBounds,
            itemCount = 5,
            payload = DragPayload.FromDock(sampleApp, sourceIndex = 0),
        )

        assertEquals(DockDropTarget(targetIndex = 0), targetSlot0)
        assertEquals(DockDropTarget(targetIndex = 3), targetSlot3)
    }

    @Test
    fun `ignores widget drag over dock bounds`() {
        val dockBounds = Rect(left = 50f, top = 700f, right = 450f, bottom = 780f)
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
        val widgetPayload = DragPayload.FromHome(
            HomeWorkspaceItem.Widget(item = widgetItem, providerInfo = null, label = "Clock"),
        )

        val target = computeDockDropTarget(
            pointerInWindow = Offset(x = 200f, y = 740f),
            dockBounds = dockBounds,
            itemCount = 5,
            payload = widgetPayload,
        )

        assertNull(target)
    }

    @Test
    fun `dock drag triggers remove zone when dragged over remove pill`() {
        val overRemove = computeHomeDropTarget(
            pointerInWindow = Offset(x = 200f, y = 50f),
            gridBounds = gridBounds,
            removeZoneBounds = removeZoneBounds,
            payload = DragPayload.FromDock(sampleApp, sourceIndex = 2),
            canPlace = { _, _, _, _, _ -> true },
        )

        assertTrue(overRemove!!.isOverRemoveZone)
    }
}
