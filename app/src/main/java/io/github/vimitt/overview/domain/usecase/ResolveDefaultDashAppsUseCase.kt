package io.github.vimitt.overview.domain.usecase

import io.github.vimitt.overview.domain.model.AppInfo
import io.github.vimitt.overview.domain.model.SettingsDefaults

/**
 * Resolve até [maxItems] aplicativos favoritos para o Dash da tela inicial.
 *
 * Se o usuário já possuir favoritos fixados nas preferências, utiliza essa lista;
 * caso contrário, seleciona automaticamente categorias essenciais
 * (telefone, mensagens, navegador, câmera, configurações) quando instaladas.
 */
class ResolveDefaultDashAppsUseCase {

    operator fun invoke(
        apps: List<AppInfo>,
        pinnedKeys: List<String> = emptyList(),
        preferredPackagesBySlot: List<Set<String>> = emptyList(),
        maxItems: Int = SettingsDefaults.DEFAULT_DOCK_ICONS,
    ): List<AppInfo> {
        if (apps.isEmpty()) return emptyList()
        val limit = maxItems.coerceIn(
            SettingsDefaults.MIN_DOCK_ICONS,
            SettingsDefaults.MAX_DOCK_ICONS,
        )

        if (pinnedKeys.isNotEmpty()) {
            val byKey = apps.associateBy { it.key }
            val byPackage = apps.associateBy { it.packageName }
            val resolvedPinned = pinnedKeys.mapNotNull { key ->
                byKey[key] ?: byPackage[key]
            }.distinctBy { it.key }
            if (resolvedPinned.isNotEmpty()) {
                return resolvedPinned.take(limit)
            }
        }

        val selected = mutableListOf<AppInfo>()
        val usedKeys = mutableSetOf<String>()

        // 1. Tenta resolver via pacotes padrão consultados no sistema (ex.: intent padrão de discador/navegador)
        for (candidates in preferredPackagesBySlot) {
            val match = apps.firstOrNull { app ->
                app.packageName in candidates && app.key !in usedKeys
            }
            if (match != null) {
                selected += match
                usedKeys += match.key
            }
            if (selected.size >= limit) return selected
        }

        // 2. Fallback por palavras-chave comuns de pacotes e nomes caso algum slot não tenha sido preenchido
        for (keywords in FALLBACK_SLOT_KEYWORDS) {
            if (selected.size >= limit) break
            val match = apps.firstOrNull { app ->
                app.key !in usedKeys && keywords.any { kw ->
                    app.packageName.contains(kw, ignoreCase = true) ||
                        app.label.contains(kw, ignoreCase = true)
                }
            }
            if (match != null) {
                selected += match
                usedKeys += match.key
            }
        }

        // 3. Se ainda faltarem itens (ex.: emulador mínimo), completa com os primeiros apps disponíveis
        if (selected.size < limit) {
            for (app in apps) {
                if (app.key !in usedKeys) {
                    selected += app
                    usedKeys += app.key
                }
                if (selected.size >= limit) break
            }
        }

        return selected
    }

    private companion object {
        val FALLBACK_SLOT_KEYWORDS: List<List<String>> = listOf(
            listOf("dialer", "phone", "telefone"),
            listOf("messaging", "mms", "messages", "mensagens"),
            listOf("browser", "chrome", "firefox", "brave", "vivaldi", "navegador"),
            listOf("camera", "câmera"),
            listOf("settings", "configurações", "configuracoes"),
        )
    }
}
