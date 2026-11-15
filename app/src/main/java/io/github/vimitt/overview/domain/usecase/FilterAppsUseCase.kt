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
        matchPackageName: Boolean = false,
    ): List<AppInfo> {
        val normalizedQuery = query.trim().normalizeForSearch()
        val isSearching = normalizedQuery.isNotEmpty()
        val queryTerms = if (isSearching) {
            normalizedQuery.split(TOKEN_SPLIT_REGEX).filter { it.isNotEmpty() }
        } else {
            emptyList()
        }

        val rankedApps = apps.mapNotNull { app ->
            val isHidden = app.key in hiddenAppKeys
            if (!isSearching) {
                if (!isHidden) RankedApp(app = app, rank = 0) else null
            } else {
                if (isHidden && !showHiddenInSearch) return@mapNotNull null
                val rank = computeMatchRank(
                    app = app,
                    normalizedQuery = normalizedQuery,
                    queryTerms = queryTerms,
                    matchPackageName = matchPackageName,
                )
                rank?.let { RankedApp(app = app, rank = it) }
            }
        }

        if (rankedApps.size <= 1) return rankedApps.map { it.app }

        val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
        return rankedApps.sortedWith { first, second ->
            if (isSearching && first.rank != second.rank) {
                first.rank.compareTo(second.rank)
            } else {
                when (sortOrder) {
                    AppSortOrder.ALPHABETICAL_ASC -> collator.compare(first.app.label, second.app.label)
                    AppSortOrder.ALPHABETICAL_DESC -> collator.compare(second.app.label, first.app.label)
                    AppSortOrder.RECENTLY_INSTALLED -> {
                        val timeCompare = second.app.firstInstallTimeMillis
                            .compareTo(first.app.firstInstallTimeMillis)
                        if (timeCompare != 0) timeCompare else collator.compare(first.app.label, second.app.label)
                    }
                }
            }
        }.map { it.app }
    }

    private fun computeMatchRank(
        app: AppInfo,
        normalizedQuery: String,
        queryTerms: List<String>,
        matchPackageName: Boolean,
    ): Int? {
        val normalizedLabel = app.label.normalizeForSearch()
        if (normalizedLabel.startsWith(normalizedQuery)) {
            return 0
        }

        val labelTokens = extractLabelTokens(app.label)
        val matchesSingleWordPrefix = labelTokens.any { it.startsWith(normalizedQuery) }
        val matchesMultiTermPrefix = queryTerms.size > 1 &&
            queryTerms.all { term -> labelTokens.any { it.startsWith(term) } }
        if (matchesSingleWordPrefix || matchesMultiTermPrefix) {
            return 1
        }

        if (matchPackageName) {
            val packageTokens = app.packageName
                .lowercase()
                .split(PACKAGE_SPLIT_REGEX)
                .filter { it.isNotEmpty() }
            if (packageTokens.any { it.startsWith(normalizedQuery) }) {
                return 2
            }
        }

        return null
    }

    private fun extractLabelTokens(label: String): List<String> {
        val rawWords = label.split(TOKEN_SPLIT_REGEX).filter { it.isNotEmpty() }
        val tokens = mutableListOf<String>()
        for (word in rawWords) {
            val normalizedWord = word.normalizeForSearch()
            if (normalizedWord.isNotEmpty()) {
                tokens += normalizedWord
            }
            // Divide também CamelCase (ex.: "YouTube" -> "you", "tube") sem quebrar letras contínuas.
            val camelParts = CAMEL_CASE_SPLIT_REGEX.split(word).filter { it.isNotEmpty() }
            if (camelParts.size > 1) {
                for (part in camelParts) {
                    val normalizedPart = part.normalizeForSearch()
                    if (normalizedPart.isNotEmpty() && normalizedPart != normalizedWord) {
                        tokens += normalizedPart
                    }
                }
            }
        }
        return tokens
    }

    private fun String.normalizeForSearch(): String {
        val decomposed = Normalizer.normalize(this, Normalizer.Form.NFD)
        return WHITESPACE_COLLAPSE_REGEX.replace(
            DIACRITICS_REGEX.replace(decomposed, "").lowercase(),
            " ",
        )
    }

    private data class RankedApp(
        val app: AppInfo,
        val rank: Int,
    )

    private companion object {
        val DIACRITICS_REGEX = "\\p{InCombiningDiacriticalMarks}+".toRegex()
        val WHITESPACE_COLLAPSE_REGEX = "\\s+".toRegex()
        val TOKEN_SPLIT_REGEX = "[\\s_\\-./:+&]+".toRegex()
        val PACKAGE_SPLIT_REGEX = "[._\\-]+".toRegex()
        val CAMEL_CASE_SPLIT_REGEX = "(?<=[\\p{Ll}\\p{Nd}])(?=[\\p{Lu}])".toRegex()
    }
}
