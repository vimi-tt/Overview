package io.github.vimitt.overview.domain.usecase

import io.github.vimitt.overview.domain.model.AppInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterAppsUseCaseTest {

    private val useCase = FilterAppsUseCase()

    private val sampleApps = listOf(
        AppInfo("com.android.camera2", "CameraActivity", "Câmera", null),
        AppInfo("org.mozilla.firefox", "App", "Firefox", null),
        AppInfo("com.android.settings", "Settings", "Configurações", null),
        AppInfo("com.android.dialer", "DialtactsActivity", "Telefone", null),
    )

    @Test
    fun `empty query returns all apps`() {
        val result = useCase(sampleApps, "   ")
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
    fun `search also matches package name`() {
        val result = useCase(sampleApps, "mozilla")
        assertEquals(listOf("Firefox"), result.map { it.label })
    }
}
