package io.github.vimitt.overview.domain.usecase

import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.AppSortOrder
import io.github.vimitt.overview.domain.model.WebSearchEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterAppsUseCaseTest {

    private val useCase = FilterAppsUseCase()

    private val sampleApps = listOf(
        AppInfo("com.android.camera2", "CameraActivity", "Câmera", null, firstInstallTimeMillis = 100L),
        AppInfo("com.android.settings", "Settings", "Configurações", null, firstInstallTimeMillis = 300L),
        AppInfo("org.mozilla.firefox", "App", "Firefox", null, firstInstallTimeMillis = 400L),
        AppInfo("com.android.dialer", "DialtactsActivity", "Telefone", null, firstInstallTimeMillis = 200L),
    )

    @Test
    fun `empty query returns all visible apps sorted alphabetically ascending`() {
        val result = useCase(sampleApps.reversed(), "   ")
        assertEquals(sampleApps, result)
    }

    @Test
    fun `search ignores accents and case`() {
        val cameraResult = useCase(sampleApps, "camera")
        assertEquals(listOf("Câmera"), cameraResult.map { it.label })

        val settingsResult = useCase(sampleApps, "CONFIGURACOES")
        assertEquals(listOf("Configurações"), settingsResult.map { it.label })
    }

    @Test
    fun `search respects prefix matching and does not match substrings inside words`() {
        val extraApps = sampleApps + listOf(
            AppInfo("com.google.android.calendar", "CalendarActivity", "Agenda", null),
            AppInfo("com.google.android.youtube", "YouTubeActivity", "YouTube", null),
            AppInfo("com.example.minhaoi", "MainActivity", "Minha Oi", null),
            AppInfo("com.example.oifibra", "MainActivity", "Oi Fibra", null),
        )

        val oiDisabledPkg = useCase(extraApps, "oi", matchPackageName = false)
        assertEquals(listOf("Oi Fibra", "Minha Oi"), oiDisabledPkg.map { it.label })

        // Mesmo com matchPackageName ativado, "oi" não casa "android" no meio da palavra.
        val oiEnabledPkg = useCase(extraApps, "oi", matchPackageName = true)
        assertEquals(listOf("Oi Fibra", "Minha Oi"), oiEnabledPkg.map { it.label })

        // CamelCase permite buscar "tube" em "YouTube".
        val tubeResult = useCase(extraApps, "tube")
        assertEquals(listOf("YouTube"), tubeResult.map { it.label })

        // Prefixo por múltiplas palavras encontra "Oi Fibra" por "oi f".
        val multiWord = useCase(extraApps, "oi f")
        assertEquals(listOf("Oi Fibra"), multiWord.map { it.label })
    }

    @Test
    fun `search only matches package segments when matchPackageName is enabled`() {
        val byDefault = useCase(sampleApps, "mozilla")
        assertEquals(emptyList<String>(), byDefault)

        val byPackageEnabled = useCase(
            apps = sampleApps,
            query = "mozilla",
            matchPackageName = true,
        )
        assertEquals(listOf("Firefox"), byPackageEnabled.map { it.label })
    }

    @Test
    fun `supports descending and recently installed sort orders`() {
        val descResult = useCase(sampleApps, "", sortOrder = AppSortOrder.ALPHABETICAL_DESC)
        assertEquals(
            listOf("Telefone", "Firefox", "Configurações", "Câmera"),
            descResult.map { it.label },
        )

        val recentResult = useCase(sampleApps, "", sortOrder = AppSortOrder.RECENTLY_INSTALLED)
        assertEquals(
            listOf("Firefox", "Configurações", "Telefone", "Câmera"),
            recentResult.map { it.label },
        )
    }

    @Test
    fun `hides hidden apps from grid and reveals in search only when enabled`() {
        val firefoxKey = sampleApps[2].key

        val gridResult = useCase(
            apps = sampleApps,
            query = "",
            hiddenAppKeys = setOf(firefoxKey),
            showHiddenInSearch = true,
        )
        assertEquals(listOf("Câmera", "Configurações", "Telefone"), gridResult.map { it.label })

        val blockedSearchResult = useCase(
            apps = sampleApps,
            query = "fire",
            hiddenAppKeys = setOf(firefoxKey),
            showHiddenInSearch = false,
        )
        assertEquals(emptyList<String>(), blockedSearchResult.map { it.label })

        val allowedSearchResult = useCase(
            apps = sampleApps,
            query = "fire",
            hiddenAppKeys = setOf(firefoxKey),
            showHiddenInSearch = true,
        )
        assertEquals(listOf("Firefox"), allowedSearchResult.map { it.label })
    }

    @Test
    fun `web search engine encodes query properly`() {
        assertEquals(
            "https://duckduckgo.com/?q=gnome%20shell",
            WebSearchEngine.DUCKDUCKGO.buildSearchUrl("gnome shell"),
        )
        assertEquals(
            "https://www.google.com/search?q=android%2B15",
            WebSearchEngine.GOOGLE.buildSearchUrl("android+15"),
        )
    }
}
