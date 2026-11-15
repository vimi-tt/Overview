package io.github.vimitt.overview.domain.model

import android.content.ComponentName
import android.os.UserHandle
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Representa um aplicativo lançável exibido no Dash ou na grade do Overview.
 *
 * @property packageName Nome do pacote Android do aplicativo.
 * @property activityName Nome completo da Activity principal de entrada.
 * @property label Nome legível exibido ao usuário.
 * @property icon Ícone pré-renderizado em memória (carregado fora da main thread).
 * @property userHandle Perfil de usuário proprietário da atividade (ou `null` para o padrão).
 */
@Immutable
data class AppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
    val icon: ImageBitmap?,
    val userHandle: UserHandle? = null,
    val firstInstallTimeMillis: Long = 0L,
) {
    /** Identificador estável usado como chave em listas e caches. */
    val key: String
        get() = "$packageName/$activityName/${userHandle?.hashCode() ?: 0}"

    fun toComponentName(): ComponentName = ComponentName(packageName, activityName)
}
