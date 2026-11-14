package io.github.vimitt.overview.domain.usecase

import io.github.vimitt.overview.domain.model.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveDefaultDashAppsUseCaseTest {

    private val useCase = ResolveDefaultDashAppsUseCase()

    private val sampleApps = listOf(
        AppInfo("com.example.alpha", "Main", "Alpha", null),
        AppInfo("com.android.dialer", "Main", "Telefone", null),
        AppInfo("com.google.android.apps.messaging", "Main", "Mensagens", null),
        AppInfo("org.mozilla.firefox", "Main", "Firefox", null),
        AppInfo("com.android.camera2", "Main", "Câmera", null),
        AppInfo("com.android.settings", "Main", "Configurações", null),
        AppInfo("com.example.zeta", "Main", "Zeta", null),
    )

    @Test
    fun `selects up to 5 default essential apps when no pins exist`() {
        val preferred = listOf(
            setOf("com.android.dialer"),
            setOf("com.google.android.apps.messaging"),
            setOf("org.mozilla.firefox"),
            setOf("com.android.camera2"),
            setOf("com.android.settings"),
        )

        val result = useCase(
            apps = sampleApps,
            pinnedKeys = emptyList(),
            preferredPackagesBySlot = preferred,
        )

        assertEquals(5, result.size)
        assertEquals(
            listOf("Telefone", "Mensagens", "Firefox", "Câmera", "Configurações"),
            result.map { it.label },
        )
    }

    @Test
    fun `respects pinned keys order and limit`() {
        val pinned = listOf("com.example.zeta", "com.example.alpha")
        val result = useCase(
            apps = sampleApps,
            pinnedKeys = pinned,
        )

        assertEquals(listOf("Zeta", "Alpha"), result.map { it.label })
    }
}
