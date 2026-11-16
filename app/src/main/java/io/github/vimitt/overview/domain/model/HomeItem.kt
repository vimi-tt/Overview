package io.github.vimitt.overview.domain.model

import androidx.compose.runtime.Immutable

/**
 * Configuração padrão da grade da tela inicial.
 */
object HomeGridSpec {
    const val COLUMNS = 4
    const val ROWS = 5
}

/**
 * Item posicionado na grade da tela inicial (atalho de aplicativo ou widget Android).
 */
@Immutable
sealed interface HomeItem {
    val id: String
    val cellX: Int
    val cellY: Int
    val spanX: Int
    val spanY: Int

    @Immutable
    data class AppShortcut(
        override val id: String,
        val packageName: String,
        val activityName: String,
        val userHandleHash: Int,
        override val cellX: Int,
        override val cellY: Int,
    ) : HomeItem {
        override val spanX: Int get() = 1
        override val spanY: Int get() = 1

        val appKey: String
            get() = "$packageName/$activityName/$userHandleHash"
    }

    @Immutable
    data class Widget(
        override val id: String,
        val appWidgetId: Int,
        val providerPackage: String,
        val providerClass: String,
        override val cellX: Int,
        override val cellY: Int,
        override val spanX: Int,
        override val spanY: Int,
    ) : HomeItem

    @Immutable
    data class NativeWidget(
        override val id: String,
        val widgetType: NativeWidgetType,
        override val cellX: Int,
        override val cellY: Int,
        override val spanX: Int = widgetType.defaultSpanX,
        override val spanY: Int = widgetType.defaultSpanY,
    ) : HomeItem
}
