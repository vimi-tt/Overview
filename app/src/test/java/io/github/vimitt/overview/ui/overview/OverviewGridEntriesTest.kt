package io.github.vimitt.overview.ui.overview

import io.github.vimitt.overview.domain.model.AppFolder
import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.AppSortOrder
import io.github.vimitt.overview.domain.model.FolderSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class OverviewGridEntriesTest {

    private val agenda = AppInfo("com.example.agenda", "Main", "Agenda", null)
    private val jogos = AppInfo("com.example.game", "Main", "Batalha", null)
    private val musica = AppInfo("com.example.music", "Main", "Música", null)
    private val zelda = AppInfo("com.example.zelda", "Main", "Zeta", null)

    @Test
    fun `folders always stay at the beginning across all sort orders`() {
        val visibleApps = listOf(agenda, jogos, musica, zelda)
        val folders = FolderSettings(
            folders = listOf(
                AppFolder(id = "f1", name = "Utilitários", appKeys = listOf(musica.key)),
                AppFolder(id = "f2", name = "Jogos", appKeys = listOf(jogos.key)),
            ),
        )

        val asc = buildOverviewGridEntries(
            visibleApps = visibleApps,
            folderSettings = folders,
            sortOrder = AppSortOrder.ALPHABETICAL_ASC,
            showDots = false,
            badgePackages = emptySet(),
        )
        assertEquals(
            listOf("Jogos", "Utilitários", "Agenda", "Zeta"),
            asc.map { it.label },
        )

        val desc = buildOverviewGridEntries(
            visibleApps = visibleApps.reversed(),
            folderSettings = folders,
            sortOrder = AppSortOrder.ALPHABETICAL_DESC,
            showDots = false,
            badgePackages = emptySet(),
        )
        assertEquals(
            listOf("Utilitários", "Jogos", "Zeta", "Agenda"),
            desc.map { it.label },
        )

        val recent = buildOverviewGridEntries(
            visibleApps = listOf(zelda, agenda, jogos, musica),
            folderSettings = folders,
            sortOrder = AppSortOrder.RECENTLY_INSTALLED,
            showDots = false,
            badgePackages = emptySet(),
        )
        assertEquals(
            listOf("Jogos", "Utilitários", "Zeta", "Agenda"),
            recent.map { it.label },
        )
    }
}
