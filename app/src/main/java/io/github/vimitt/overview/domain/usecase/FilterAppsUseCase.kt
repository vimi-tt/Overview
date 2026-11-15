package io.github.vimitt.overview.domain.usecase

import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.AppSortOrder
import java.text.Collator
import java.text.Normalizer

/**
 * Filtra e ordena aplicativos considerando busca textual, apps ocultos
 * e critério de ordenação configurado na grade de apps.
 */
class FilterAppsUseCase {

    operator fun invoke(
        apps: List<AppInfo>,
        query: String,
        sortOrder: AppSortOrder = AppSortOrder.ALPHABETICAL_ASC,
        hiddenAppKeys: Set<String> = emptySet(),
        showHiddenInSearch: Boolean = false,
        matchPackageName: Boolean = true,
    ): List<AppInfo> {
        val normalizedQuery = query.trim().normalizeForSearch()
        val isSearching = normalizedQuery.isNotEmpty()

        val visibleApps = apps.filter { app ->
            val isHidden = app.key in hiddenAppKeys
            if (!isSearching) {
                !isHidden
            } else {
                val matchesLabel = app.label.normalizeForSearch().contains(normalizedQuery)
                val matchesPkg = matchPackageName && app.packageName.lowercase().contains(normalizedQuery)
                (matchesLabel || matchesPkg) && (!isHidden || showHiddenInSearch)
            }
        }

        if (visibleApps.size <= 1) return visibleApps

        val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
        return when (sortOrder) {
            AppSortOrder.ALPHABETICAL_ASC -> {
                visibleApps.sortedWith { first, second ->
                    collator.compare(first.label, second.label)
                }
            }

            AppSortOrder.ALPHABETICAL_DESC -> {
                visibleApps.sortedWith { first, second ->
                    collator.compare(second.label, first.label)
                }
            }

            AppSortOrder.RECENTLY_INSTALLED -> {
                visibleApps.sortedWith { first, second ->
                    val timeCompare = second.firstInstallTimeMillis.compareTo(first.firstInstallTimeMillis)
                    if (timeCompare != 0) timeCompare else collator.compare(first.label, second.label)
                }
            }
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
