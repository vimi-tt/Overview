package io.github.vimitt.overview.domain.usecase

import io.github.vimitt.overview.domain.model.AppInfo
import java.text.Normalizer

/**
 * Filtra aplicativos por termo de busca ignorando acentuação e maiúsculas/minúsculas.
 */
class FilterAppsUseCase {

    operator fun invoke(apps: List<AppInfo>, query: String): List<AppInfo> {
        val normalizedQuery = query.trim().normalizeForSearch()
        if (normalizedQuery.isEmpty()) return apps

        return apps.filter { app ->
            app.label.normalizeForSearch().contains(normalizedQuery) ||
                app.packageName.lowercase().contains(normalizedQuery)
        }
    }

    private fun String.normalizeForSearch(): String {
        val decomposed = Normalizer.normalize(this, Normalizer.Form.NFD)
        return DIACRITICS_REGEX.replace(decomposed, "").lowercase()
    }

    private companion object {
        val DIACRITICS_REGEX = "\\p{InCombiningDiacriticalMarks}+".toRegex()
    }
}
